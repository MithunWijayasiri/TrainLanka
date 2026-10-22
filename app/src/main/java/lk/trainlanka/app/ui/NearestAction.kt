package lk.trainlanka.app.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch
import lk.trainlanka.app.location.currentLocation
import lk.trainlanka.app.location.isLocationEnabled
import lk.trainlanka.app.location.nearestStation

private val LOCATION_PERMISSIONS = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)

/**
 * Returns an action that fills From with the nearest station. Permission is requested only when invoked.
 * Failures land in [AppViewModel.locationProblem]; [onDone] runs after every attempt.
 */
@Composable
fun rememberNearestStationAction(
    vm: AppViewModel,
    onDone: () -> Unit = {},
): () -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    fun locate() =
        scope.launch {
            val tt = vm.timetable.value ?: return@launch
            if (!isLocationEnabled(context)) {
                vm.setLocationProblem(LocationProblem.OFF)
            } else {
                val station = currentLocation(context)?.let { nearestStation(tt.stations, it) }
                if (station == null) vm.setLocationProblem(LocationProblem.NO_FIX) else vm.setFrom(station.id)
            }
            onDone()
        }

    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            if (result.values.any { it }) {
                locate()
            } else {
                vm.setLocationProblem(LocationProblem.DENIED)
                onDone()
            }
        }

    return {
        if (hasLocationPermission(context)) locate() else launcher.launch(LOCATION_PERMISSIONS)
    }
}

private fun hasLocationPermission(context: Context) =
    LOCATION_PERMISSIONS.any {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }

/** Explains a failed nearest-station attempt; offers the settings screen that fixes it, if any. */
@Composable
fun LocationProblemDialog(
    problem: LocationProblem,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val (title, text) =
        when (problem) {
            LocationProblem.OFF -> {
                "Location is off" to "Turn on location on your phone to find your nearest station, or pick one from the list."
            }

            LocationProblem.DENIED -> {
                "Location not allowed" to "Allow location for Train Lanka to find your nearest station, or pick one from the list."
            }

            LocationProblem.NO_FIX -> {
                "Couldn’t get your location" to "Try again in a moment, or pick a station from the list."
            }
        }
    val fix =
        when (problem) {
            LocationProblem.OFF -> {
                "Turn on location" to Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            }

            LocationProblem.DENIED -> {
                "Open settings" to
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
            }

            LocationProblem.NO_FIX -> {
                null
            }
        }
    val close = @Composable { TextButton(onClick = onDismiss) { Text("Close") } }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = {
            if (fix == null) {
                close()
            } else {
                TextButton(onClick = {
                    onDismiss()
                    context.startActivity(fix.second)
                }) { Text(fix.first) }
            }
        },
        dismissButton = close.takeIf { fix != null },
    )
}
