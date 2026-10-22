package lk.trainlanka.app.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class Route(
    val fromId: Int,
    val toId: Int,
) {
    fun swapped() = Route(toId, fromId)

    fun key() = "$fromId-$toId"

    companion object {
        fun parse(key: String): Route {
            val (from, to) = key.split('-').map(String::toInt)
            return Route(from, to)
        }
    }
}

/** Where a removed route was, so it can be put back. */
data class RemovedRoute(
    val route: Route,
    val starred: Boolean,
    val index: Int,
)

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class Settings(
    val theme: ThemeMode,
    val clock24: Boolean,
)

/** Result of the last update check; null time = never checked. */
data class CheckStatus(
    val at: Long?,
    val failed: Boolean,
)

private val Context.dataStore by preferencesDataStore("prefs")

class Prefs(
    context: Context,
) {
    private val store = context.dataStore

    val starred: Flow<List<Route>> = store.data.map { routes(it[STARRED]) }
    val recent: Flow<List<Route>> = store.data.map { routes(it[RECENT]) }
    val settings: Flow<Settings> =
        store.data.map {
            Settings(
                theme = it[THEME]?.let(ThemeMode::valueOf) ?: ThemeMode.SYSTEM,
                clock24 = it[CLOCK_24] ?: false,
            )
        }
    val checkStatus: Flow<CheckStatus> = store.data.map { CheckStatus(it[CHECK_AT], it[CHECK_FAILED] ?: false) }
    val dismissedUpdate: Flow<String?> = store.data.map { it[DISMISSED_UPDATE] }

    suspend fun toggleStar(route: Route) =
        store.edit {
            val list = routes(it[STARRED])
            it[STARRED] = join(if (route in list) list - route else list + route)
            it[RECENT] = join(routes(it[RECENT]) - route)
        }

    /** Applies the order of [visible]; stored routes missing from it (hidden by the timetable) keep their slots. */
    suspend fun setStarred(visible: List<Route>) =
        store.edit {
            val stored = routes(it[STARRED])
            val order = visible.filter { r -> r in stored }.iterator()
            it[STARRED] = join(stored.map { r -> if (r in visible) order.next() else r })
        }

    /** Recent excludes starred routes; newest first. */
    suspend fun addRecent(route: Route) =
        store.edit {
            if (route in routes(it[STARRED])) return@edit
            it[RECENT] = join((listOf(route) + (routes(it[RECENT]) - route)).take(MAX_RECENT))
        }

    /** Removes [route] from starred or recent; it must be in one of them. */
    suspend fun remove(route: Route): RemovedRoute {
        var removed: RemovedRoute? = null
        store.edit {
            for ((key, starred) in listOf(STARRED to true, RECENT to false)) {
                val list = routes(it[key])
                val index = list.indexOf(route)
                if (index < 0) continue
                it[key] = join(list - route)
                removed = RemovedRoute(route, starred, index)
            }
        }
        return checkNotNull(removed) { "Route $route is not saved" }
    }

    /** Puts [removed] back at its old position; no-op if the route was starred again meanwhile. */
    suspend fun restore(removed: RemovedRoute) =
        store.edit {
            val route = removed.route
            val starred = routes(it[STARRED])
            if (route in starred) return@edit
            val recent = routes(it[RECENT]) - route
            if (removed.starred) {
                it[STARRED] = join(starred.toMutableList().apply { add(minOf(removed.index, size), route) })
                it[RECENT] = join(recent)
            } else {
                it[RECENT] = join(recent.toMutableList().apply { add(minOf(removed.index, size), route) }.take(MAX_RECENT))
            }
        }

    suspend fun setTheme(mode: ThemeMode) = store.edit { it[THEME] = mode.name }

    suspend fun setClock24(on: Boolean) = store.edit { it[CLOCK_24] = on }

    suspend fun setCheck(
        at: Long,
        failed: Boolean,
    ) = store.edit {
        it[CHECK_AT] = at
        it[CHECK_FAILED] = failed
    }

    suspend fun dismissUpdate(version: String) = store.edit { it[DISMISSED_UPDATE] = version }

    private companion object {
        const val MAX_RECENT = 3
        val STARRED = stringPreferencesKey("starred")
        val RECENT = stringPreferencesKey("recent")
        val THEME = stringPreferencesKey("theme")
        val CLOCK_24 = booleanPreferencesKey("clock24")
        val CHECK_AT = longPreferencesKey("check_at")
        val CHECK_FAILED = booleanPreferencesKey("check_failed")
        val DISMISSED_UPDATE = stringPreferencesKey("dismissed_update")

        fun routes(value: String?): List<Route> = value?.split(',')?.filter(String::isNotEmpty)?.map(Route::parse) ?: emptyList()

        fun join(list: List<Route>) = list.joinToString(",") { it.key() }
    }
}
