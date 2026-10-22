package lk.trainlanka.app

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import lk.trainlanka.app.ui.AppNav
import lk.trainlanka.app.ui.AppViewModel
import lk.trainlanka.app.ui.theme.TrainLankaTheme
import lk.trainlanka.app.ui.theme.isDark

class MainActivity : ComponentActivity() {
    private val vm: AppViewModel by viewModels()
    private val launches = Channel<Intent>(Channel.CONFLATED)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) launches.trySend(intent)
        setContent {
            val settings by vm.settings.collectAsStateWithLifecycle()
            val dark = settings.theme.isDark()
            LaunchedEffect(dark) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark },
                    // Library default scrims, kept for 3-button navigation
                    navigationBarStyle =
                        SystemBarStyle.auto(
                            Color.argb(0xe6, 0xFF, 0xFF, 0xFF),
                            Color.argb(0x80, 0x1b, 0x1b, 0x1b),
                        ) { dark },
                )
            }
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
