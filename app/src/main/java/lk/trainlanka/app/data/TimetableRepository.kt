package lk.trainlanka.app.data

import android.content.Context
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

sealed interface CheckResult {
    data object UpToDate : CheckResult
    data class Available(val version: String) : CheckResult
    data object Failed : CheckResult
}

/**
 * Current timetable: newer of the last applied download in filesDir and the bundled asset.
 * A newer download waits in [pendingFile] until applied, so a failed or partial fetch never replaces good data.
 */
class TimetableRepository(private val context: Context, private val prefs: Prefs) {
    private val currentFile = File(context.filesDir, "timetable.json")
    private val pendingFile = File(context.filesDir, "timetable-pending.json")
    private val mutex = Mutex()

    private val _timetable = MutableStateFlow<Timetable?>(null)
    val timetable: StateFlow<Timetable?> = _timetable.asStateFlow()

    private val _pendingVersion = MutableStateFlow<String?>(null)
    /** Version of a downloaded, not yet applied timetable. */
    val pendingVersion: StateFlow<String?> = _pendingVersion.asStateFlow()

    suspend fun load(): Timetable = mutex.withLock {
        _timetable.value ?: withContext(Dispatchers.IO) {
            val bundled = Timetable.parse(context.assets.open("timetable.json").bufferedReader().use { it.readText() })
            val downloaded = currentFile.takeIf { it.exists() }
                ?.let { f -> runCatching { Timetable.parse(f.readText()) }.getOrElse { f.delete(); null } }
            val tt = downloaded?.takeIf { it.version > bundled.version } ?: bundled
            _pendingVersion.value = pendingFile.takeIf { it.exists() }
                ?.let { f -> runCatching { Timetable.parse(f.readText()).version }.getOrElse { f.delete(); null } }
                ?.takeIf { it > tt.version }
            tt.also { _timetable.value = it }
        }
    }

    suspend fun await(): Timetable = _timetable.value ?: load()

    /** Downloads the published timetable; a newer one is stored as pending. Records the check result. */
    suspend fun check(): CheckResult {
        val current = await()
        val result = try {
            val text = withContext(Dispatchers.IO) { download() }
            val remote = Timetable.parse(text)
            if (remote.version > current.version) {
                mutex.withLock {
                    withContext(Dispatchers.IO) {
                        val tmp = File(context.filesDir, "timetable-download.json")
                        tmp.writeText(text)
                        check(tmp.renameTo(pendingFile)) { "Could not store downloaded timetable" }
                    }
                    _pendingVersion.value = remote.version
                }
                CheckResult.Available(remote.version)
            } else {
                CheckResult.UpToDate
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            CheckResult.Failed
        }
        prefs.setCheck(System.currentTimeMillis(), failed = result == CheckResult.Failed)
        return result
    }

    /** Switches to the pending timetable. */
    suspend fun applyPending() = mutex.withLock {
        withContext(Dispatchers.IO) {
            if (!pendingFile.exists()) return@withContext
            val tt = Timetable.parse(pendingFile.readText())
            check(pendingFile.renameTo(currentFile)) { "Could not apply downloaded timetable" }
            _timetable.value = tt
            _pendingVersion.value = null
        }
    }

    private fun download(): String {
        val conn = URL(DATA_URL).openConnection() as HttpURLConnection
        conn.connectTimeout = 15_000
        conn.readTimeout = 30_000
        try {
            check(conn.responseCode == 200) { "HTTP ${conn.responseCode}" }
            return conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }

    private companion object {
        const val DATA_URL = "https://raw.githubusercontent.com/MithunWijayasiri/TrainLanka/master/data/timetable.json"
    }
}
