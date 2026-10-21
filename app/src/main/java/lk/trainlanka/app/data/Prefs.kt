package lk.trainlanka.app.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

data class Route(val fromId: Int, val toId: Int) {
    fun swapped() = Route(toId, fromId)
    fun key() = "$fromId-$toId"

    companion object {
        fun parse(key: String): Route {
            val (from, to) = key.split('-').map(String::toInt)
            return Route(from, to)
        }
    }
}

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class Settings(
    val theme: ThemeMode,
    val clock24: Boolean,
)

/** Result of the last update check; null time = never checked. */
data class CheckStatus(val at: Long?, val failed: Boolean)

private val Context.dataStore by preferencesDataStore("prefs")

class Prefs(context: Context) {
    private val store = context.dataStore

    val starred: Flow<List<Route>> = store.data.map { routes(it[STARRED]) }
    val recent: Flow<List<Route>> = store.data.map { routes(it[RECENT]) }
    val settings: Flow<Settings> = store.data.map {
        Settings(
            theme = it[THEME]?.let(ThemeMode::valueOf) ?: ThemeMode.SYSTEM,
            clock24 = it[CLOCK_24] ?: false,
        )
    }
    val checkStatus: Flow<CheckStatus> = store.data.map { CheckStatus(it[CHECK_AT], it[CHECK_FAILED] ?: false) }
    val dismissedUpdate: Flow<String?> = store.data.map { it[DISMISSED_UPDATE] }
    val openProvinces: Flow<Set<String>> = store.data.map { it[OPEN_PROVINCES] ?: setOf("Western") }

    suspend fun firstStarred(): Route? = starred.first().firstOrNull()

    suspend fun toggleStar(route: Route) = store.edit {
        val list = routes(it[STARRED])
        it[STARRED] = join(if (route in list) list - route else list + route)
        it[RECENT] = join(routes(it[RECENT]) - route)
    }

    suspend fun setStarred(list: List<Route>) = store.edit { it[STARRED] = join(list) }

    /** Recent excludes starred routes; newest first. */
    suspend fun addRecent(route: Route) = store.edit {
        if (route in routes(it[STARRED])) return@edit
        it[RECENT] = join((listOf(route) + (routes(it[RECENT]) - route)).take(MAX_RECENT))
    }

    suspend fun setTheme(mode: ThemeMode) = store.edit { it[THEME] = mode.name }
    suspend fun setClock24(on: Boolean) = store.edit { it[CLOCK_24] = on }
    suspend fun setCheck(at: Long, failed: Boolean) = store.edit {
        it[CHECK_AT] = at
        it[CHECK_FAILED] = failed
    }
    suspend fun dismissUpdate(version: String) = store.edit { it[DISMISSED_UPDATE] = version }
    suspend fun setProvinceOpen(province: String, open: Boolean) = store.edit {
        val set = it[OPEN_PROVINCES] ?: setOf("Western")
        it[OPEN_PROVINCES] = if (open) set + province else set - province
    }

    private companion object {
        const val MAX_RECENT = 3
        val STARRED = stringPreferencesKey("starred")
        val RECENT = stringPreferencesKey("recent")
        val THEME = stringPreferencesKey("theme")
        val CLOCK_24 = booleanPreferencesKey("clock24")
        val CHECK_AT = longPreferencesKey("check_at")
        val CHECK_FAILED = booleanPreferencesKey("check_failed")
        val DISMISSED_UPDATE = stringPreferencesKey("dismissed_update")
        val OPEN_PROVINCES = stringSetPreferencesKey("open_provinces")

        fun routes(value: String?): List<Route> =
            value?.split(',')?.filter(String::isNotEmpty)?.map(Route::parse) ?: emptyList()

        fun join(list: List<Route>) = list.joinToString(",") { it.key() }
    }
}
