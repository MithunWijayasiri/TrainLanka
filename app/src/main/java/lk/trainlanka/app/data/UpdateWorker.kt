package lk.trainlanka.app.data

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import lk.trainlanka.app.TrainLankaApp
import java.util.concurrent.TimeUnit

/** Daily background check; a newer timetable is only downloaded, the user applies it from the banner. */
class UpdateWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        TrainLankaApp.from(applicationContext).timetables.check()
        return Result.success()
    }

    companion object {
        fun schedule(context: Context) {
            val request =
                PeriodicWorkRequestBuilder<UpdateWorker>(1, TimeUnit.DAYS)
                    .setConstraints(Constraints(requiredNetworkType = NetworkType.CONNECTED))
                    .build()
            WorkManager
                .getInstance(context)
                .enqueueUniquePeriodicWork("timetable-update", ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}
