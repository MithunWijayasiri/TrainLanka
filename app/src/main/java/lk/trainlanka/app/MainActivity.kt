package lk.trainlanka.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import lk.trainlanka.app.ui.AppNav
import lk.trainlanka.app.ui.AppViewModel
import lk.trainlanka.app.ui.theme.TrainLankaTheme

class MainActivity : ComponentActivity() {
    private val vm: AppViewModel by viewModels()
    private val launches = Channel<Intent>(Channel.CONFLATED)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) launches.trySend(intent)
        setContent {
            val settings by vm.settings.collectAsStateWithLifecycle()
            TrainLankaTheme(settings.theme) {
                Surface(color = MaterialTheme.colorScheme.background) {
                    AppNav(vm, launches.receiveAsFlow())
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        launches.trySend(intent)
    }
}
