package lk.trainlanka.app.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.CancellationSignal
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import lk.trainlanka.app.data.Station
import kotlin.coroutines.resume

/** Whether the phone's location setting is on. */
fun isLocationEnabled(context: Context) = LocationManagerCompat.isLocationEnabled(context.getSystemService(LocationManager::class.java))

/** One-shot foreground fix; null when location is off or unavailable. Caller must hold a location permission. */
@SuppressLint("MissingPermission")
suspend fun currentLocation(context: Context): Location? {
    val lm = context.getSystemService(LocationManager::class.java)
    val provider =
        listOf(LocationManager.FUSED_PROVIDER, LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            .firstOrNull { lm.isProviderEnabled(it) } ?: return null
    return suspendCancellableCoroutine { cont ->
        val signal = CancellationSignal()
        cont.invokeOnCancellation { signal.cancel() }
        LocationManagerCompat.getCurrentLocation(lm, provider, signal, ContextCompat.getMainExecutor(context)) {
            cont.resume(it)
        }
    }
}

/** Closest station with coordinates. */
fun nearestStation(
    stations: List<Station>,
    location: Location,
): Station? {
    val result = FloatArray(1)
    return stations.filter { it.lat != null && it.lng != null }.minByOrNull {
        Location.distanceBetween(location.latitude, location.longitude, it.lat!!, it.lng!!, result)
        result[0]
    }
}
