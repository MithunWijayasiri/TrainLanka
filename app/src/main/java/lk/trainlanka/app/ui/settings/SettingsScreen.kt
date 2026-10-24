package lk.trainlanka.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import lk.trainlanka.app.ui.Snackbar
import lk.trainlanka.app.ui.TextAction
import lk.trainlanka.app.ui.TopBar
import lk.trainlanka.app.ui.theme.AppTheme

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

                SectionLabel("LANGUAGE")
                RadioRow("English", selected = true) {}
                RadioRow("සිංහල · Sinhala", selected = false, enabled = false, comingSoon = true) {}
                RadioRow("தமிழ் · Tamil", selected = false, enabled = false, comingSoon = true) {}
                Divider()

                SectionLabel("THEME")
                RadioRow("Light", settings.theme == ThemeMode.LIGHT) { vm.setTheme(ThemeMode.LIGHT) }
                RadioRow("Dark", settings.theme == ThemeMode.DARK) { vm.setTheme(ThemeMode.DARK) }
                RadioRow("System", settings.theme == ThemeMode.SYSTEM) { vm.setTheme(ThemeMode.SYSTEM) }
                Divider()

                SectionLabel("TIME FORMAT")
                RadioRow("12-hour (6:55 PM)", !settings.clock24) { vm.setClock24(false) }
                RadioRow("24-hour (18:55)", settings.clock24) { vm.setClock24(true) }
                Divider()

                SectionLabel("UNSELECTED TRAIN TYPES")
                RadioRow("Compact (greyed out)", !settings.hideFiltered) { vm.setHideFiltered(false) }
                RadioRow("Hide completely", settings.hideFiltered) { vm.setHideFiltered(true) }
                Divider()

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

                SectionLabel("CREDITS")
                Text(
                    "© OpenStreetMap contributors, geoBoundaries",
                    Modifier.padding(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 16.dp),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp,
                )
                Divider()

                Box(Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 16.dp), contentAlignment = Alignment.CenterStart) {
                    Text("App version ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})", color = colors.muted, fontSize = 14.sp)
                }
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
private fun Divider() = HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

@Composable
private fun RadioRow(
    label: String,
    selected: Boolean,
    enabled: Boolean = true,
    comingSoon: Boolean = false,
    onClick: () -> Unit,
) {
    val faint = AppTheme.colors.faint
    val primary = MaterialTheme.colorScheme.primary
    val ring =
        when {
            !enabled -> faint
            selected -> primary
            else -> MaterialTheme.colorScheme.outline
        }
    Row(
        Modifier
            .fillMaxWidth()
            .height(56.dp)
            .selectable(selected, enabled = enabled, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(Modifier.size(20.dp).border(2.dp, ring, CircleShape), contentAlignment = Alignment.Center) {
            if (selected) Box(Modifier.size(10.dp).background(primary, CircleShape))
        }
        Text(label, Modifier.weight(1f), color = if (enabled) MaterialTheme.colorScheme.onSurface else faint, fontSize = 16.sp)
        if (comingSoon) {
            Text(
                "Coming soon",
                Modifier.border(1.dp, faint, RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 2.dp),
                color = faint,
                fontSize = 12.sp,
            )
        }
    }
}
