package lk.trainlanka.app

import android.app.Application
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import lk.trainlanka.app.data.Prefs
import lk.trainlanka.app.data.TimetableRepository
import lk.trainlanka.app.data.UpdateWorker

class TrainLankaApp : Application() {
    val scope = CoroutineScope(SupervisorJob())
    lateinit var prefs: Prefs
        private set
    lateinit var timetables: TimetableRepository
        private set

    override fun onCreate() {
        super.onCreate()
        prefs = Prefs(this)
        timetables = TimetableRepository(this, prefs)
        scope.launch { timetables.load() }
        UpdateWorker.schedule(this)
    }

    companion object {
        fun from(context: Context) = context.applicationContext as TrainLankaApp
    }
}
