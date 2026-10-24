package lk.trainlanka.app.ui.results

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import lk.trainlanka.app.data.Route
import lk.trainlanka.app.data.Timetable
import lk.trainlanka.app.domain.Trip
import lk.trainlanka.app.domain.formatCountdown
import lk.trainlanka.app.domain.formatDay
import lk.trainlanka.app.domain.formatDuration
import lk.trainlanka.app.domain.formatTime
import lk.trainlanka.app.domain.hasDirectTrains
import lk.trainlanka.app.domain.tripsOn
import lk.trainlanka.app.domain.upcoming
import lk.trainlanka.app.ui.AppViewModel
import lk.trainlanka.app.ui.EmptyState
import lk.trainlanka.app.ui.HolidayNote
import lk.trainlanka.app.ui.PrimaryButton
import lk.trainlanka.app.ui.Segment
import lk.trainlanka.app.ui.SegmentedBar
import lk.trainlanka.app.ui.SegmentedBarHeight
import lk.trainlanka.app.ui.TimeText
import lk.trainlanka.app.ui.TopBar
import lk.trainlanka.app.ui.TypeBadge
import lk.trainlanka.app.ui.theme.AppTheme
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset

private val TrainTypes =
    listOf(
        "Intercity Express" to "Inter Ex",
        "Express" to "Express",
        "Semi Express" to "Sem. Ex",
        "Slow" to "Slow",
    )

private val DateBarMargin = 16.dp
private val DateBarShadow = 3.dp

private sealed interface DayMode {
    data object Today : DayMode

    data object Tomorrow : DayMode

    data class Picked(
        val date: LocalDate,
    ) : DayMode
}

private val DayModeSaver: Saver<DayMode, String> =
    Saver(
        save = {
            when (it) {
                DayMode.Today -> "today"
                DayMode.Tomorrow -> "tomorrow"
                is DayMode.Picked -> it.date.toString()
            }
        },
        restore = {
            when (it) {
                "today" -> DayMode.Today
                "tomorrow" -> DayMode.Tomorrow
                else -> DayMode.Picked(LocalDate.parse(it))
            }
        },
    )

private val RouteSaver =
    listSaver<Route, Int>(
        save = { listOf(it.fromId, it.toId) },
        restore = { Route(it[0], it[1]) },
    )

private data class ResultsData(
    val hero: Trip?,
    val heroLabel: String,
    val countdown: Boolean,
    val departed: List<Trip>,
    val before: List<Trip>,
    val caption: String?,
    val after: List<Trip>,
)

private fun dayName(
    date: LocalDate,
    today: LocalDate,
) = if (date == today.plusDays(1)) "TOMORROW" else formatDay(date).uppercase()

private fun dayOf(
    mode: DayMode,
    today: LocalDate,
): LocalDate =
    when (mode) {
        DayMode.Today -> today
        DayMode.Tomorrow -> today.plusDays(1)
        is DayMode.Picked -> mode.date
    }

private fun buildData(
    tt: Timetable,
    route: Route,
    mode: DayMode,
    now: LocalDateTime,
): ResultsData {
    val today = now.toLocalDate()
    if (mode == DayMode.Today) {
        val up = upcoming(tt, route, today)
        val (departed, next) = up.today.partition { it.dep.isBefore(now) }
        val nextDay = up.nextDay
        val caption = nextDay?.let { if (it == today.plusDays(1)) "Tomorrow · ${formatDay(it)}" else formatDay(it) }
        return when {
            next.isNotEmpty() -> {
                ResultsData(
                    next.first(),
                    "NEXT TRAIN · TODAY",
                    true,
                    departed,
                    next.drop(1),
                    caption,
                    up.nextDayTrips,
                )
            }

            nextDay != null -> {
                ResultsData(
                    up.nextDayTrips.first(),
                    "NEXT TRAIN · ${dayName(nextDay, today)}",
                    true,
                    departed,
                    emptyList(),
                    caption,
                    up.nextDayTrips.drop(1),
                )
            }

            else -> {
                ResultsData(null, "", false, departed, emptyList(), null, emptyList())
            }
        }
    }
    val date = dayOf(mode, today)
    val trips = tripsOn(tt, route, date)
    return ResultsData(
        hero = trips.firstOrNull(),
        heroLabel = "FIRST TRAIN · ${dayName(date, today)}",
        countdown = false,
        departed = emptyList(),
        before = trips.drop(1),
        caption = null,
        after = emptyList(),
    )
}

@Composable
fun ResultsScreen(
    vm: AppViewModel,
    route: Route,
    onBack: () -> Unit,
    onOpenTrip: (Trip) -> Unit,
    onChangeStations: () -> Unit,
) {
    val tt = vm.timetable.collectAsStateWithLifecycle().value ?: return
    val now by vm.now.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val starred by vm.starred.collectAsStateWithLifecycle()

    var current by rememberSaveable(stateSaver = RouteSaver) { mutableStateOf(route) }
    var mode by rememberSaveable(stateSaver = DayModeSaver) { mutableStateOf<DayMode>(DayMode.Today) }
    var showEarlier by rememberSaveable { mutableStateOf(false) }
    var showPicker by remember { mutableStateOf(false) }

    val hasDirect = remember(tt, current) { hasDirectTrains(tt, current) }
    val data =
        remember(tt, current, mode, now, hasDirect) {
            if (hasDirect) buildData(tt, current, mode, now) else null
        }

    fun swap() {
        current = current.swapped()
        vm.openRoute(current)
    }

    val isStarred = current in starred
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding()) {
        TopBar(
            title = tt.routeLabel(current),
            onBack = onBack,
            actions = {
                if (!hasDirect) {
                    IconButton(onClick = ::swap) { Icon(Icons.Rounded.SwapVert, contentDescription = "Swap stations") }
                }
                IconButton(onClick = { vm.toggleStar(current) }) {
                    Icon(
                        if (isStarred) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                        contentDescription = if (isStarred) "Remove from saved" else "Save route",
                        tint = if (isStarred) AppTheme.colors.star else AppTheme.colors.muted,
                    )
                }
            },
        )
        if (data == null) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                EmptyState(
                    "No direct trains on this route",
                    "Train Lanka shows direct trains only.",
                )
            }
            PrimaryButton(
                "Change stations",
                enabled = true,
                modifier = Modifier.navigationBarsPadding().padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                onClick = onChangeStations,
            )
        } else {
            val day = dayOf(mode, now.toLocalDate())
            Box(Modifier.weight(1f).fillMaxWidth()) {
                ResultsList(
                    tt,
                    data,
                    now,
                    settings.clock24,
                    settings.unselectedTypes,
                    settings.hideFiltered,
                    day,
                    mode == DayMode.Today,
                    showEarlier,
                    onToggleEarlier = { showEarlier = !showEarlier },
                    onToggleType = vm::toggleTrainType,
                    onOpenTrip = onOpenTrip,
                    modifier = Modifier.fillMaxSize(),
                )
                DateBar(
                    mode,
                    onSelect = { mode = it },
                    onPick = { showPicker = true },
                    onSwap = ::swap,
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
        }
    }

    if (showPicker) {
        PickDateDialog(
            today = now.toLocalDate(),
            initial = (mode as? DayMode.Picked)?.date,
            onDismiss = { showPicker = false },
            onPicked = {
                mode = DayMode.Picked(it)
                showPicker = false
            },
        )
    }
}

@Composable
private fun ResultsList(
    tt: Timetable,
    data: ResultsData,
    now: LocalDateTime,
    clock24: Boolean,
    unselectedTypes: Set<String>,
    hideFiltered: Boolean,
    day: LocalDate,
    isToday: Boolean,
    showEarlier: Boolean,
    onToggleEarlier: () -> Unit,
    onToggleType: (String) -> Unit,
    onOpenTrip: (Trip) -> Unit,
    modifier: Modifier,
) {
    fun destination(trip: Trip) =
        tt
            .station(
                trip.train.stopList
                    .last()
                    .stationId,
            ).name

    fun key(
        prefix: String,
        trip: Trip,
    ) = "$prefix-${trip.train.id}-${trip.startDate}"

    fun visible(trips: List<Trip>) = if (hideFiltered) trips.filter { it.train.type !in unselectedTypes } else trips

    @Composable
    fun UpcomingRow(trip: Trip) {
        if (trip.train.type in unselectedTypes) {
            CompactRow(trip, destination(trip), clock24) { onOpenTrip(trip) }
        } else {
            TrainRow(trip, destination(trip), clock24) { onOpenTrip(trip) }
        }
    }

    LazyColumn(
        modifier,
        contentPadding =
            PaddingValues(
                bottom = SegmentedBarHeight + DateBarMargin + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
            ),
    ) {
        if (showEarlier) {
            items(data.departed, key = { key("d", it) }) { DepartedRow(it, clock24) }
        }
        val hero = data.hero
        if (hero == null) {
            item(key = "none") {
                Text(
                    if (isToday) "No upcoming trains" else "No trains on ${formatDay(day)}",
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 32.dp),
                    color = AppTheme.colors.muted,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            item(key = key("h", hero)) {
                HeroCard(
                    hero,
                    data.heroLabel,
                    destination(hero),
                    clock24,
                    countdown = if (data.countdown) formatCountdown(now, hero.dep) else null,
                    onClick = { onOpenTrip(hero) },
                )
            }
        }
        item(key = "type-filter") {
            Row(
                Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                for ((type, label) in TrainTypes) {
                    TypePill(type, label, type !in unselectedTypes, { onToggleType(type) }, Modifier.weight(1f))
                }
            }
        }
        if (data.departed.isNotEmpty()) {
            item(key = "earlier-link") {
                val n = data.departed.size
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clickable(onClick = onToggleEarlier)
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Text(
                        if (showEarlier) "↓ Hide earlier trains" else "↑ Show $n earlier ${if (n == 1) "train" else "trains"}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        textDecoration = TextDecoration.Underline,
                    )
                }
            }
        }
        items(visible(data.before), key = { key("b", it) }) { UpcomingRow(it) }
        if (data.caption != null) {
            item(key = "rollover") { Rollover(data.caption) }
        }
        items(visible(data.after), key = { key("a", it) }) { UpcomingRow(it) }
        item(key = "footer") { HolidayNote(Modifier.padding(top = 8.dp)) }
    }
}

@Composable
private fun HeroCard(
    trip: Trip,
    label: String,
    destination: String,
    clock24: Boolean,
    countdown: String?,
    onClick: () -> Unit,
) {
    val colors = AppTheme.colors
    Column(
        Modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 12.dp, top = 4.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(colors.hero)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(label, color = colors.onHero, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.06.em)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TimeText(formatTime(trip.dep, clock24), 48.sp, Modifier.alignByBaseline(), color = colors.onHero)
            if (countdown != null) {
                Text(
                    countdown,
                    Modifier.weight(1f).alignByBaseline(),
                    color = colors.amber,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.End,
                )
            }
        }
        Text(
            "Arrives ${formatTime(trip.arr, clock24)} · ${formatDuration(trip.minutes)}",
            color = colors.onHero,
            fontSize = 15.sp,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "${trip.train.type} #${trip.train.no} · to $destination",
                Modifier.weight(1f, fill = false),
                color = colors.onHeroMuted,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Icon(Icons.Rounded.ChevronRight, contentDescription = null, Modifier.size(18.dp), tint = colors.onHeroMuted)
        }
    }
}

@Composable
private fun TrainRow(
    trip: Trip,
    destination: String,
    clock24: Boolean,
    onClick: () -> Unit,
) {
    val muted = AppTheme.colors.muted
    Column(Modifier.fillMaxWidth()) {
        Column(
            Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TimeText(formatTime(trip.dep, clock24), 26.sp, Modifier.alignByBaseline())
                Text("→ ${formatTime(trip.arr, clock24)}", Modifier.alignByBaseline(), color = muted, fontSize = 16.sp)
                Text(
                    formatDuration(trip.minutes),
                    Modifier.weight(1f).alignByBaseline(),
                    color = muted,
                    fontSize = 14.sp,
                    textAlign = TextAlign.End,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TypeBadge(trip.train.type)
                Text("#${trip.train.no} · to $destination", color = muted, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
private fun TypePill(
    type: String,
    label: String,
    selected: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier,
) {
    val colors = AppTheme.colors
    val (bg, fg) = colors.badges.getValue(type)
    val faint = colors.faint
    val look =
        if (selected) {
            Modifier.background(bg, CircleShape).border(1.dp, fg, CircleShape)
        } else {
            Modifier.drawBehind {
                val stroke = 1.dp.toPx()
                drawRoundRect(
                    color = faint,
                    topLeft = Offset(stroke / 2, stroke / 2),
                    size = Size(size.width - stroke, size.height - stroke),
                    cornerRadius = CornerRadius(size.height / 2),
                    style = Stroke(stroke, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx()))),
                )
            }
        }
    Box(
        modifier
            .heightIn(min = 48.dp)
            .toggleable(selected, role = Role.Checkbox, onValueChange = { onToggle() })
            .semantics { contentDescription = type },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(36.dp)
                .then(look)
                .padding(horizontal = 4.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                label,
                Modifier.clearAndSetSemantics {},
                color = if (selected) fg else colors.muted,
                fontSize = 13.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun CompactRow(
    trip: Trip,
    destination: String,
    clock24: Boolean,
    onClick: () -> Unit,
) {
    val faint = AppTheme.colors.faint
    Column(Modifier.fillMaxWidth()) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(40.dp)
                .background(AppTheme.colors.compactRow)
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TimeText(formatTime(trip.dep, clock24), 18.sp, Modifier.alignByBaseline(), color = faint)
                Text(
                    "→ ${formatTime(trip.arr, clock24)} · ${trip.train.type} #${trip.train.no}",
                    Modifier.weight(1f).alignByBaseline(),
                    color = faint,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(formatDuration(trip.minutes), Modifier.alignByBaseline(), color = faint, fontSize = 13.sp)
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
private fun DepartedRow(
    trip: Trip,
    clock24: Boolean,
) {
    val faint = AppTheme.colors.faint
    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            TimeText(formatTime(trip.dep, clock24), 20.sp, Modifier.alignByBaseline(), color = faint, strike = true)
            Text(
                "→ ${formatTime(trip.arr, clock24)} · ${trip.train.type} #${trip.train.no}",
                Modifier.weight(1f).alignByBaseline(),
                color = faint,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text("Departed", Modifier.alignByBaseline(), color = faint, fontSize = 12.sp)
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
private fun Rollover(caption: String) {
    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            HorizontalDivider(Modifier.weight(1f), color = MaterialTheme.colorScheme.outline)
            Text("No more trains today", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            HorizontalDivider(Modifier.weight(1f), color = MaterialTheme.colorScheme.outline)
        }
        Text(
            caption,
            Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 8.dp),
            color = AppTheme.colors.muted,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun DateBar(
    mode: DayMode,
    onSelect: (DayMode) -> Unit,
    onPick: () -> Unit,
    onSwap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val picked = (mode as? DayMode.Picked)?.date
    Row(
        modifier
            .navigationBarsPadding()
            .padding(start = 12.dp, end = 12.dp, bottom = DateBarMargin),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SegmentedBar(Modifier.weight(1f).shadow(DateBarShadow, RoundedCornerShape(26.dp))) {
            Segment("Today", mode == DayMode.Today, role = Role.Tab) { onSelect(DayMode.Today) }
            Segment("Tomorrow", mode == DayMode.Tomorrow, role = Role.Tab) { onSelect(DayMode.Tomorrow) }
            Segment(picked?.let(::formatDay) ?: "Date", picked != null, dropdown = picked == null, role = Role.Tab, onClick = onPick)
        }
        Box(
            Modifier
                .size(SegmentedBarHeight)
                .shadow(DateBarShadow, CircleShape)
                .background(AppTheme.colors.hero, CircleShape)
                .clickable(role = Role.Button, onClick = onSwap),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.SwapVert, contentDescription = "Swap stations", tint = AppTheme.colors.onHero)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PickDateDialog(
    today: LocalDate,
    initial: LocalDate?,
    onDismiss: () -> Unit,
    onPicked: (LocalDate) -> Unit,
) {
    val state =
        rememberDatePickerState(
            initialSelectedDateMillis = initial?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli(),
            selectableDates =
                object : SelectableDates {
                    override fun isSelectableDate(utcTimeMillis: Long) = utcDate(utcTimeMillis) >= today

                    override fun isSelectableYear(year: Int) = year >= today.year
                },
        )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = { state.selectedDateMillis?.let { onPicked(utcDate(it)) } },
                enabled = state.selectedDateMillis != null,
            ) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    ) { DatePicker(state) }
}

private fun utcDate(millis: Long): LocalDate = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
