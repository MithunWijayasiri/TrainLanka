package lk.trainlanka.app.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import lk.trainlanka.app.BuildConfig
import lk.trainlanka.app.data.ThemeMode
import lk.trainlanka.app.domain.formatCheckTime
import lk.trainlanka.app.domain.formatVersionDate
import lk.trainlanka.app.ui.AppViewModel
import lk.trainlanka.app.ui.OutlinedPill
import lk.trainlanka.app.ui.SNACKBAR_MILLIS
import lk.trainlanka.app.ui.SectionLabel
import lk.trainlanka.app.ui.Segment
import lk.trainlanka.app.ui.SegmentedBar
import lk.trainlanka.app.ui.Snackbar
import lk.trainlanka.app.ui.TextAction
import lk.trainlanka.app.ui.TopBar
import lk.trainlanka.app.ui.theme.AppTheme

private const val GITHUB_URL = "https://github.com/MithunWijayasiri/TrainLanka"

@Composable
fun SettingsScreen(
    vm: AppViewModel,
    onBack: () -> Unit,
) {
    val version = (vm.timetable.collectAsStateWithLifecycle().value ?: return).version
    val settings by vm.settings.collectAsStateWithLifecycle()
    val status by vm.checkStatus.collectAsStateWithLifecycle()
    val pendingVersion by vm.pendingVersion.collectAsStateWithLifecycle()
    val checking by vm.checking.collectAsStateWithLifecycle()
    val colors = AppTheme.colors
    val uriHandler = LocalUriHandler.current
    val scope = rememberCoroutineScope()
    var failureShown by remember { mutableStateOf(false) }

    LaunchedEffect(failureShown) {
        if (failureShown) {
            delay(SNACKBAR_MILLIS)
            failureShown = false
        }
    }
    val check: () -> Unit = {
        failureShown = false
        scope.launch { failureShown = !vm.checkNow() }
    }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(Modifier.safeDrawingPadding()) {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                TopBar("Settings", onBack, titleSize = 22.sp)

                SectionLabel("THEME")
                OptionBar {
                    Segment("Light", settings.theme == ThemeMode.LIGHT) { vm.setTheme(ThemeMode.LIGHT) }
                    Segment("Dark", settings.theme == ThemeMode.DARK) { vm.setTheme(ThemeMode.DARK) }
                    Segment("System", settings.theme == ThemeMode.SYSTEM) { vm.setTheme(ThemeMode.SYSTEM) }
                }

                SectionLabel("TIME FORMAT", Modifier.padding(top = 8.dp))
                OptionBar {
                    Segment("12-hour", !settings.clock24, sub = "6:55 PM") { vm.setClock24(false) }
                    Segment("24-hour", settings.clock24, sub = "18:55") { vm.setClock24(true) }
                }

                SectionLabel("UNSELECTED TRAIN TYPES", Modifier.padding(top = 8.dp))
                OptionBar {
                    Segment("Compact", !settings.hideFiltered, sub = "Greyed out") { vm.setHideFiltered(false) }
                    Segment("Hide", settings.hideFiltered, sub = "Not shown") { vm.setHideFiltered(true) }
                }
                Text(
                    if (settings.hideFiltered) {
                        "Trains you switch off on Results are removed from the list."
                    } else {
                        "Trains you switch off on Results stay in the list, small and grey."
                    },
                    Modifier.padding(start = 16.dp, top = 8.dp, end = 16.dp),
                    color = colors.muted,
                    fontSize = 13.sp,
                )

                SectionLabel("LANGUAGE", Modifier.padding(top = 8.dp))
                OptionBar {
                    Segment("English", selected = true) {}
                    Segment("සිංහල", selected = false, sub = "Soon", enabled = false) {}
                    Segment("தமிழ்", selected = false, sub = "Soon", enabled = false) {}
                }
                Divider(Modifier.padding(top = 20.dp))

                SectionLabel("TIMETABLE DATA")
                Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Version ${version.replace('-', '.')}", color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp)
                    Text("Timetable dated ${formatVersionDate(version)} · stored offline", color = colors.muted, fontSize = 13.sp)
                    val checkedAt = status.at
                    val (statusText, statusColor) =
                        when {
                            checkedAt == null -> {
                                "Not checked yet" to colors.muted
                            }

                            status.failed -> {
                                "Last check failed ${formatCheckTime(checkedAt, settings.clock24)} ✕" to MaterialTheme.colorScheme.error
                            }

                            pendingVersion != null -> {
                                "Last checked ${formatCheckTime(checkedAt, settings.clock24)} · New timetable available" to
                                    MaterialTheme.colorScheme.primary
                            }

                            else -> {
                                "Last checked ${formatCheckTime(checkedAt, settings.clock24)} · Up to date ✓" to
                                    MaterialTheme.colorScheme.primary
                            }
                        }
                    Text(statusText, color = statusColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                Box(Modifier.padding(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 16.dp)) {
                    OutlinedPill(if (checking) "Checking…" else "Check for update", enabled = !checking, onClick = check)
                }
                Divider()

                SectionLabel("ABOUT")
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clickable(role = Role.Button) { uriHandler.openUri(GITHUB_URL) }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text("View on GitHub", color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp)
                    Text(GITHUB_URL.removePrefix("https://"), color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
                }
                Text(
                    "App version ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                    Modifier.padding(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 16.dp),
                    color = colors.muted,
                    fontSize = 14.sp,
                )
            }

            if (failureShown) {
                Snackbar("Couldn’t update. Still using the saved timetable.", Modifier.align(Alignment.BottomCenter)) {
                    TextAction("Retry", underline = true, color = colors.amber, onClick = check)
                }
            }
        }
    }
}

@Composable
private fun Divider(modifier: Modifier = Modifier) = HorizontalDivider(modifier, color = MaterialTheme.colorScheme.outlineVariant)

@Composable
private fun OptionBar(content: @Composable RowScope.() -> Unit) = SegmentedBar(Modifier.fillMaxWidth().padding(horizontal = 16.dp), content)
