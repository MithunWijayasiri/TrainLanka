package lk.trainlanka.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import lk.trainlanka.app.TrainLankaApp
import lk.trainlanka.app.data.CheckResult
import lk.trainlanka.app.data.CheckStatus
import lk.trainlanka.app.data.RemovedRoute
import lk.trainlanka.app.data.Route
import lk.trainlanka.app.data.Settings
import lk.trainlanka.app.data.ThemeMode
import lk.trainlanka.app.data.Timetable
import lk.trainlanka.app.domain.hasDirectTrains
import lk.trainlanka.app.domain.nowInColombo
import lk.trainlanka.app.widget.RouteWidget
import java.time.LocalDateTime

enum class LocationProblem { DENIED, OFF, NO_FIX }

class AppViewModel(
    app: Application,
) : AndroidViewModel(app) {
    private val graph = TrainLankaApp.from(app)
    private val prefs = graph.prefs
    private val timetables = graph.timetables

    val timetable: StateFlow<Timetable?> = timetables.timetable

    /** Wall clock in Colombo, ticking on each minute. */
    val now: StateFlow<LocalDateTime> =
        flow {
            while (true) {
                val t = nowInColombo()
                emit(t)
                delay(60_000L - LocalDateTime.now().second * 1000L)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), nowInColombo())

    val settings: StateFlow<Settings> =
        prefs.settings
            .stateIn(viewModelScope, SharingStarted.Eagerly, Settings(ThemeMode.SYSTEM, clock24 = false))

    /** Saved routes whose stations still exist in the current timetable. */
    val starred: StateFlow<List<Route>> =
        combine(prefs.starred, timetable) { list, tt -> valid(list, tt) }
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val recent: StateFlow<List<Route>> =
        combine(prefs.recent, timetable) { list, tt -> valid(list, tt) }
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val checkStatus: StateFlow<CheckStatus> =
        prefs.checkStatus
            .stateIn(viewModelScope, SharingStarted.Eagerly, CheckStatus(null, false))
    val pendingVersion: StateFlow<String?> = timetables.pendingVersion

    /** Pending version to announce in the home banner, unless dismissed. */
    val bannerVersion: StateFlow<String?> =
        combine(pendingVersion, prefs.dismissedUpdate) { pending, dismissed ->
            pending?.takeIf { it != dismissed }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _from = MutableStateFlow<Int?>(null)
    val from: StateFlow<Int?> = _from.asStateFlow()
    private val _to = MutableStateFlow<Int?>(null)
    val to: StateFlow<Int?> = _to.asStateFlow()

    private val _locationProblem = MutableStateFlow<LocationProblem?>(null)
    val locationProblem: StateFlow<LocationProblem?> = _locationProblem.asStateFlow()

    private val _checking = MutableStateFlow(false)
    val checking: StateFlow<Boolean> = _checking.asStateFlow()

    fun setFrom(id: Int) {
        _from.value = id
        _locationProblem.value = null
    }

    fun setTo(id: Int) {
        _to.value = id
    }

    /** Route of the picked stations, once both are set and differ. */
    fun pickedRoute(): Route? {
        val f = _from.value ?: return null
        val t = _to.value ?: return null
        return if (f == t) null else Route(f, t)
    }

    /** [pickedRoute], if it has direct trains. */
    fun openableRoute(): Route? {
        val tt = timetable.value ?: return null
        return pickedRoute()?.takeIf { hasDirectTrains(tt, it) }
    }

    fun swapStations() {
        val f = _from.value
        _from.value = _to.value
        _to.value = f
    }

    fun setLocationProblem(problem: LocationProblem?) {
        _locationProblem.value = problem
    }

    fun openRoute(route: Route) =
        viewModelScope.launch {
            _from.value = route.fromId
            _to.value = route.toId
            prefs.addRecent(route)
        }

    fun toggleStar(route: Route) =
        viewModelScope.launch {
            prefs.toggleStar(route)
            refreshWidget()
        }

    fun setStarred(list: List<Route>) =
        viewModelScope.launch {
            prefs.setStarred(list)
            refreshWidget()
        }

    /** Removes a starred or recent route; pass the result to [restoreRoute] to undo. */
    suspend fun removeRoute(route: Route): RemovedRoute = prefs.remove(route).also { refreshWidget() }

    fun restoreRoute(removed: RemovedRoute) =
        viewModelScope.launch {
            prefs.restore(removed)
            refreshWidget()
        }

    fun setTheme(mode: ThemeMode) = viewModelScope.launch { prefs.setTheme(mode) }

    fun setClock24(on: Boolean) =
        viewModelScope.launch {
            prefs.setClock24(on)
            refreshWidget()
        }

    fun dismissUpdate(version: String) = viewModelScope.launch { prefs.dismissUpdate(version) }

    fun applyUpdate() = viewModelScope.launch { applyPending() }

    /** User-started check: a newer timetable is applied right away. Returns false on failure. */
    suspend fun checkNow(): Boolean {
        _checking.value = true
        try {
            return when (timetables.check()) {
                is CheckResult.Available -> {
                    applyPending()
                    true
                }

                CheckResult.UpToDate -> {
                    true
                }

                CheckResult.Failed -> {
                    false
                }
            }
        } finally {
            _checking.value = false
        }
    }

    /** Applies the pending timetable and clears picked stations it no longer has. */
    private suspend fun applyPending() {
        timetables.applyPending()
        val tt = timetables.await()
        if (_from.value !in tt.stationsById) _from.value = null
        if (_to.value !in tt.stationsById) _to.value = null
        refreshWidget()
    }

    private suspend fun refreshWidget() = RouteWidget.refresh(getApplication())

    private fun valid(
        list: List<Route>,
        tt: Timetable?,
    ): List<Route> =
        if (tt == null) {
            emptyList()
        } else {
            list.filter { it.fromId in tt.stationsById && it.toId in tt.stationsById }
        }
}
