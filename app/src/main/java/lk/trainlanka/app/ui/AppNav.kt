package lk.trainlanka.app.ui

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import kotlinx.coroutines.flow.Flow
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
fun AppNav(vm: AppViewModel, launches: Flow<Intent>) {
    val nav = rememberNavController()
    LaunchedEffect(nav) { launches.collect { openFromWidget(vm, nav, it) } }

    NavHost(nav, startDestination = HomeDest) {
        composable<HomeDest> {
            HomeScreen(
                vm,
                onPickStation = { nav.navigate(PickerDest(it)) },
                onOpenRoute = { nav.navigate(ResultsDest(it.fromId, it.toId)) },
                onOpenSettings = { nav.navigate(SettingsDest) },
            )
        }
        composable<PickerDest> {
            PickerScreen(vm, it.toRoute<PickerDest>().field, onDone = { nav.popBackStack() })
        }
        composable<ResultsDest> {
            val dest = it.toRoute<ResultsDest>()
            ResultsScreen(
                vm,
                Route(dest.from, dest.to),
                onBack = { nav.popBackStack() },
                onOpenTrip = { trip -> nav.navigate(trip.stopsDest()) },
                onChangeStations = { nav.popBackStack(HomeDest, inclusive = false) },
            )
        }
        composable<StopsDest> {
            val dest = it.toRoute<StopsDest>()
            StopsScreen(vm, dest.trainId, Route(dest.from, dest.to), onBack = { nav.popBackStack() })
        }
        composable<SettingsDest> {
            SettingsScreen(vm, onBack = { nav.popBackStack() })
        }
    }
}

private fun Trip.stopsDest() = StopsDest(
    trainId = train.id,
    from = train.stopList[fromIndex].stationId,
    to = train.stopList[toIndex].stationId,
)

private fun openFromWidget(vm: AppViewModel, nav: NavHostController, intent: Intent) {
    if (!intent.hasExtra(EXTRA_FROM)) return
    val route = Route(intent.getIntExtra(EXTRA_FROM, 0), intent.getIntExtra(EXTRA_TO, 0))
    vm.openRoute(route)
    nav.navigate(ResultsDest(route.fromId, route.toId)) { popUpTo(HomeDest) }
    if (intent.hasExtra(EXTRA_TRAIN)) nav.navigate(StopsDest(intent.getIntExtra(EXTRA_TRAIN, 0), route.fromId, route.toId))
}
