package lk.trainlanka.app.ui

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import lk.trainlanka.app.data.Route
import lk.trainlanka.app.domain.Trip
import lk.trainlanka.app.ui.home.HomeScreen
import lk.trainlanka.app.ui.picker.PickerScreen
import lk.trainlanka.app.ui.results.ResultsScreen
import lk.trainlanka.app.ui.settings.SettingsScreen
import lk.trainlanka.app.ui.stops.StopsScreen
import lk.trainlanka.app.widget.EXTRA_FROM
import lk.trainlanka.app.widget.EXTRA_TO
import lk.trainlanka.app.widget.EXTRA_TRAIN

/** [launches] = intents from the home widget (initial launch and onNewIntent). */
@Composable
fun AppNav(
    vm: AppViewModel,
    launches: Flow<Intent>,
) {
    val nav = rememberNavController()
    LaunchedEffect(nav) { launches.collect { openFromWidget(vm, nav, it) } }

    NavHost(nav, startDestination = HomeDest) {
        composable<HomeDest> { entry ->
            HomeScreen(
                vm,
                onPickStation = { field -> entry.ifResumed { nav.navigate(PickerDest(field)) } },
                onOpenRoute = { route -> entry.ifResumed { nav.navigate(ResultsDest(route.fromId, route.toId)) } },
                onOpenSettings = { entry.ifResumed { nav.navigate(SettingsDest) } },
            )
        }
        composable<PickerDest> { entry ->
            PickerScreen(vm, entry.toRoute<PickerDest>().field, onDone = { entry.ifResumed { nav.popBackStack() } })
        }
        composable<ResultsDest> { entry ->
            val dest = entry.toRoute<ResultsDest>()
            ResultsScreen(
                vm,
                Route(dest.from, dest.to),
                onBack = { entry.ifResumed { nav.popBackStack() } },
                onOpenTrip = { trip -> entry.ifResumed { nav.navigate(trip.stopsDest()) } },
                onChangeStations = { entry.ifResumed { nav.popBackStack(HomeDest, inclusive = false) } },
            )
        }
        composable<StopsDest> { entry ->
            val dest = entry.toRoute<StopsDest>()
            StopsScreen(vm, dest.trainId, Route(dest.from, dest.to), onBack = { entry.ifResumed { nav.popBackStack() } })
        }
        composable<SettingsDest> { entry ->
            SettingsScreen(vm, onBack = { entry.ifResumed { nav.popBackStack() } })
        }
    }
}

/** Runs [action] only while this screen is resumed, so repeat taps during a transition can't pop Home. */
private fun NavBackStackEntry.ifResumed(action: () -> Unit) {
    if (lifecycle.currentState == Lifecycle.State.RESUMED) action()
}

private fun Trip.stopsDest() =
    StopsDest(
        trainId = train.id,
        from = train.stopList[fromIndex].stationId,
        to = train.stopList[toIndex].stationId,
    )

/** Ignores ids the current timetable no longer has (widget not yet refreshed after an update). */
private suspend fun openFromWidget(
    vm: AppViewModel,
    nav: NavHostController,
    intent: Intent,
) {
    if (!intent.hasExtra(EXTRA_FROM)) return
    val tt = vm.timetable.filterNotNull().first()
    val route = Route(intent.getIntExtra(EXTRA_FROM, 0), intent.getIntExtra(EXTRA_TO, 0))
    if (route.fromId !in tt.stationsById || route.toId !in tt.stationsById) return
    vm.openRoute(route)
    nav.navigate(ResultsDest(route.fromId, route.toId)) { popUpTo(HomeDest) }
    if (!intent.hasExtra(EXTRA_TRAIN)) return
    val train = tt.trainsById[intent.getIntExtra(EXTRA_TRAIN, 0)] ?: return
    val stations = train.stopList.map { it.stationId }
    val from = stations.indexOf(route.fromId)
    if (from >= 0 && route.toId in stations.drop(from + 1)) nav.navigate(StopsDest(train.id, route.fromId, route.toId))
}
