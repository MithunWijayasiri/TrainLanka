package lk.trainlanka.app.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch
import lk.trainlanka.app.location.currentLocation
import lk.trainlanka.app.location.nearestStation

private val LOCATION_PERMISSIONS = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)

/**
 * Returns an action that fills From with the nearest station. Permission is requested only when invoked.
 * Failures land in [AppViewModel.locationProblem]; [onDone] runs after every attempt.
 */
@Composable
fun rememberNearestStationAction(vm: AppViewModel, onDone: () -> Unit = {}): () -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    fun locate() = scope.launch {
        val tt = vm.timetable.value ?: return@launch
        val station = currentLocation(context)?.let { nearestStation(tt.stations, it) }
        if (station == null) vm.setLocationProblem(LocationProblem.UNAVAILABLE) else vm.setFrom(station.id)
        onDone()
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        if (result.values.any { it }) locate()
        else {
            vm.setLocationProblem(LocationProblem.DENIED)
            onDone()
        }
    }

    return {
        if (hasLocationPermission(context)) locate() else launcher.launch(LOCATION_PERMISSIONS)
    }
}

private fun hasLocationPermission(context: Context) = LOCATION_PERMISSIONS.any {
    ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
}

fun openAppSettings(context: Context) {
    context.startActivity(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    )
}
