package lk.trainlanka.app

import android.util.Log

/**
 * Temporary log for device testing, read with `adb logcat -s TrainLanka`.
 * Removed before release: delete this file and every `debugLog(` call.
 */
fun debugLog(message: String, error: Throwable? = null) {
    Log.i("TrainLanka", message, error)
}
