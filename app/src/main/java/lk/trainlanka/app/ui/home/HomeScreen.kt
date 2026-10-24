package lk.trainlanka.app.ui.home

import android.app.Activity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxDefaults
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import lk.trainlanka.app.R
import lk.trainlanka.app.data.RemovedRoute
import lk.trainlanka.app.data.Route
import lk.trainlanka.app.data.Timetable
import lk.trainlanka.app.domain.formatCountdown
import lk.trainlanka.app.domain.formatDay
import lk.trainlanka.app.domain.formatTime
import lk.trainlanka.app.domain.formatVersionDate
import lk.trainlanka.app.domain.hasDirectTrains
import lk.trainlanka.app.domain.nextDepartures
import lk.trainlanka.app.ui.AppViewModel
import lk.trainlanka.app.ui.Banner
import lk.trainlanka.app.ui.EmptyState
import lk.trainlanka.app.ui.LocationProblemDialog
import lk.trainlanka.app.ui.PickerField
import lk.trainlanka.app.ui.PrimaryButton
import lk.trainlanka.app.ui.SNACKBAR_MILLIS
import lk.trainlanka.app.ui.Snackbar
import lk.trainlanka.app.ui.TextAction
import lk.trainlanka.app.ui.TimeText
import lk.trainlanka.app.ui.rememberNearestStationAction
import lk.trainlanka.app.ui.theme.AppTheme
import lk.trainlanka.app.ui.theme.Sora
import lk.trainlanka.app.ui.theme.isDark
import sh.calvin.reorderable.ReorderableCollectionItemScope
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import java.time.LocalDateTime

@Composable
fun HomeScreen(
    vm: AppViewModel,
    onPickStation: (PickerField) -> Unit,
    onOpenRoute: (Route) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val tt by vm.timetable.collectAsStateWithLifecycle()
    val starred by vm.starred.collectAsStateWithLifecycle()
    val recent by vm.recent.collectAsStateWithLifecycle()
    val bannerVersion by vm.bannerVersion.collectAsStateWithLifecycle()
    val locationProblem by vm.locationProblem.collectAsStateWithLifecycle()
    val now by vm.now.collectAsStateWithLifecycle()

    // Local order while dragging; re-synced whenever the stored order changes.
    var starredOrder by remember(starred) { mutableStateOf(starred) }
    val latestOrder by rememberUpdatedState(starredOrder)

    val listState = rememberLazyListState()
    val reorderState =
        rememberReorderableLazyListState(listState) { from, to ->
            val list = starredOrder.toMutableList()
            val fromIndex = list.indexOfFirst { it.key() == from.key }
            val toIndex = list.indexOfFirst { it.key() == to.key }
            list.add(toIndex, list.removeAt(fromIndex))
            starredOrder = list
        }

    val open = { route: Route ->
        vm.openRoute(route)
        onOpenRoute(route)
    }

    val scope = rememberCoroutineScope()
    var removed by remember { mutableStateOf<RemovedRoute?>(null) }
    LaunchedEffect(removed) {
        if (removed != null) {
            delay(SNACKBAR_MILLIS)
            removed = null
        }
    }
    val remove = { route: Route -> scope.launch { removed = vm.removeRoute(route) } }

    locationProblem?.let { LocationProblemDialog(it) { vm.setLocationProblem(null) } }
    LightStatusBarIcons(vm)

    val colors = AppTheme.colors
    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)),
    ) {
        Column(Modifier.fillMaxSize()) {
            val timetable = tt
            if (timetable == null) {
                Band(onOpenSettings)
                return@Column
            }
            val header: @Composable () -> Unit = {
                Column {
                    Band(onOpenSettings, Modifier.overlapNext(CardOverlap), subtitle = bandSubtitle(now, starred.size))
                    SearchCard(vm, timetable, onPickStation, open)
                    bannerVersion?.let { version ->
                        Banner(
                            icon = { Icon(Icons.Outlined.Download, contentDescription = null, Modifier.size(20.dp)) },
                            text = "New timetable available (${formatVersionDate(version)}). You can keep using the current one.",
                            modifier = Modifier.padding(top = 16.dp),
                        ) {
                            TextAction("Later") { vm.dismissUpdate(version) }
                            TextAction("Update", underline = true) { vm.applyUpdate() }
                        }
                    }
                }
            }
            if (starred.isEmpty() && recent.isEmpty()) {
                header()
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    EmptyState(
                        "No saved routes yet",
                        "Search a route, then tap ☆ to star it. Starred and recent routes show up here with their next train.",
                    )
                }
                return@Column
            }
            LazyColumn(Modifier.weight(1f), state = listState) {
                item(key = "search") { header() }
                if (starredOrder.isNotEmpty()) {
                    item(key = "starred-label") { ListHeader("Starred") }
                    items(starredOrder, key = { it.key() }) { route ->
                        ReorderableItem(reorderState, key = route.key()) { isDragging ->
                            Box(Modifier.padding(horizontal = 16.dp, vertical = 6.dp).clip(TicketShape)) {
                                SwipeToRemove(onRemove = { remove(route) }) {
                                    Ticket(vm, timetable, route, isDragging, onDragStopped = { vm.setStarred(latestOrder) }) { open(route) }
                                }
                            }
                        }
                    }
                }
                if (recent.isNotEmpty()) {
                    item(key = "recent-label") { ListHeader("Recent") }
                    items(recent, key = { "recent-${it.key()}" }) { route ->
                        SwipeToRemove(onRemove = { remove(route) }) {
                            RecentRow(timetable, route) { open(route) }
                        }
                    }
                }
            }
        }
        // Keeps the status bar on the band colour once the band scrolls away.
        Box(
            Modifier
                .fillMaxWidth()
                .windowInsetsTopHeight(WindowInsets.safeDrawing)
                .background(colors.band.first().second),
        )
        removed?.let { r ->
            Snackbar(if (r.starred) "Removed from starred" else "Removed from recent", Modifier.align(Alignment.BottomCenter)) {
                TextAction("Undo", underline = true, color = MaterialTheme.colorScheme.inversePrimary) {
                    removed = null
                    vm.restoreRoute(r)
                }
            }
        }
    }
}

/** Swipe left to remove; also exposed as an accessibility action. */
@Composable
private fun SwipeToRemove(
    onRemove: () -> Unit,
    content: @Composable () -> Unit,
) {
    // Not saveable: LazyColumn restores saved state by key, so an undone route would come back dismissed and be removed again.
    val positionalThreshold = SwipeToDismissBoxDefaults.positionalThreshold
    val state = remember { SwipeToDismissBoxState(SwipeToDismissBoxValue.Settled, positionalThreshold) }
    SwipeToDismissBox(
        state,
        backgroundContent = {
            if (state.dismissDirection == SwipeToDismissBoxValue.EndToStart) {
                Box(
                    Modifier.fillMaxSize().background(MaterialTheme.colorScheme.error).padding(end = 20.dp),
                    contentAlignment = Alignment.CenterEnd,
                ) {
                    Icon(Icons.Rounded.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.onError)
                }
            }
        },
        modifier =
            Modifier.semantics {
                customActions =
                    listOf(
                        CustomAccessibilityAction("Remove") {
                            onRemove()
                            true
                        },
                    )
            },
        enableDismissFromStartToEnd = false,
        onDismiss = { onRemove() },
    ) {
        Box(Modifier.background(MaterialTheme.colorScheme.background)) { content() }
    }
}

private val CardOverlap = 48.dp
private val TicketShape = RoundedCornerShape(18.dp)
private val StubWidth = 112.dp

/** Light status bar icons over the band; the theme's icons come back when Home leaves. */
@Composable
private fun LightStatusBarIcons(vm: AppViewModel) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val dark = settings.theme.isDark()
    val view = LocalView.current
    val controller = remember(view) { WindowCompat.getInsetsController((view.context as Activity).window, view) }
    // Launched after MainActivity's enableEdgeToEdge effect, so it runs last.
    LaunchedEffect(dark) { controller.isAppearanceLightStatusBars = false }
    DisposableEffect(dark) { onDispose { controller.isAppearanceLightStatusBars = !dark } }
}

/** "Sat 24 Oct · 3 starred routes". */
private fun bandSubtitle(
    now: LocalDateTime,
    starred: Int,
): String =
    formatDay(now.toLocalDate()) +
        when (starred) {
            0 -> ""
            1 -> " · 1 starred route"
            else -> " · $starred starred routes"
        }

/** Reports [overlap] less height, so the next sibling draws over this one's bottom edge. */
private fun Modifier.overlapNext(overlap: Dp) =
    layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        layout(placeable.width, placeable.height - overlap.roundToPx()) { placeable.place(0, 0) }
    }

/** Brand band behind the status bar: logo, wordmark and greeting, fading into the page. */
@Composable
private fun Band(
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
) {
    val colors = AppTheme.colors
    Column(
        modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(*colors.band.toTypedArray()))
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
            .padding(bottom = CardOverlap + 16.dp),
    ) {
        Row(
            Modifier.fillMaxWidth().height(64.dp).padding(start = 16.dp, end = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painterResource(R.drawable.ic_launcher_foreground),
                contentDescription = null,
                Modifier.size(32.dp).clip(RoundedCornerShape(3.dp)).background(colors.amber),
            )
            Text(
                "Train Lanka",
                Modifier.weight(1f),
                fontFamily = Sora,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = colors.onBand,
            )
            IconButton(onClick = onOpenSettings) { Icon(Icons.Rounded.Settings, contentDescription = "Settings", tint = colors.onBand) }
        }
        Text(
            "Where to today?",
            Modifier.padding(start = 20.dp, top = 12.dp, end = 20.dp),
            fontFamily = Sora,
            fontSize = 28.sp,
            lineHeight = 34.sp,
            fontWeight = FontWeight.Bold,
            color = colors.onHero,
        )
        if (subtitle != null) {
            Text(subtitle, Modifier.padding(start = 20.dp, top = 6.dp, end = 20.dp), fontSize = 14.sp, color = colors.onHeroMuted)
        }
    }
}

/**
 * Raised From/To card with a round swap button, plus a Show trains button that is disabled until the route has direct trains.
 * Filling From by location opens the route when To is already set.
 */
@Composable
private fun SearchCard(
    vm: AppViewModel,
    tt: Timetable,
    onPickStation: (PickerField) -> Unit,
    onOpenRoute: (Route) -> Unit,
) {
    val from by vm.from.collectAsStateWithLifecycle()
    val to by vm.to.collectAsStateWithLifecycle()
    val locate = rememberNearestStationAction(vm) { found -> if (found) vm.openableRoute()?.let(onOpenRoute) }
    val picked = remember(from, to) { vm.pickedRoute() }
    val openable = remember(tt, picked) { picked?.takeIf { hasDirectTrains(tt, it) } }
    val colors = AppTheme.colors
    val line = MaterialTheme.colorScheme.outlineVariant
    val shape = RoundedCornerShape(24.dp)

    Column(
        Modifier
            .padding(horizontal = 16.dp)
            .shadow(8.dp, shape)
            .background(colors.raised, shape)
            .padding(top = 4.dp, bottom = 16.dp),
    ) {
        Box(contentAlignment = Alignment.CenterEnd) {
            Column {
                StationRow(
                    label = "From",
                    value = from?.let { tt.station(it).name },
                    placeholder = "Boarding station",
                    filledDot = false,
                    onClick = { onPickStation(PickerField.FROM) },
                ) {
                    IconButton(onClick = locate) {
                        Icon(
                            Icons.Rounded.MyLocation,
                            contentDescription = "Use my location",
                            Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                HorizontalDivider(Modifier.padding(start = 36.dp), color = line)
                StationRow(
                    label = "To",
                    value = to?.let { tt.station(it).name },
                    placeholder = "Destination station",
                    filledDot = true,
                    onClick = { onPickStation(PickerField.TO) },
                ) { Spacer(Modifier.size(48.dp)) }
            }
            Box(
                Modifier
                    .padding(end = 56.dp)
                    .size(48.dp)
                    .clip(CircleShape)
                    .clickable(role = Role.Button, onClick = vm::swapStations)
                    .padding(4.dp)
                    .background(colors.raised, CircleShape)
                    .border(1.dp, line, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.SwapVert, contentDescription = "Swap stations", Modifier.size(20.dp))
            }
        }
        PrimaryButton(
            if (picked != null && openable == null) "No direct trains on this route" else "Show trains",
            enabled = openable != null,
            modifier = Modifier.padding(start = 16.dp, top = 8.dp, end = 16.dp).semantics { liveRegion = LiveRegionMode.Polite },
            color = colors.amber,
            contentColor = colors.onAmber,
        ) { onOpenRoute(checkNotNull(openable)) }
    }
}

@Composable
private fun StationRow(
    label: String,
    value: String?,
    placeholder: String,
    filledDot: Boolean,
    onClick: () -> Unit,
    trailing: @Composable () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clickable(onClick = onClick)
            .padding(start = 16.dp, end = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StationDot(filledDot)
        Text(label, Modifier.widthIn(min = 36.dp), fontSize = 13.sp, color = AppTheme.colors.muted)
        Text(
            value ?: placeholder,
            Modifier.weight(1f).padding(end = 44.dp), // clears the swap button
            fontSize = 16.sp,
            fontWeight = if (value == null) FontWeight.Normal else FontWeight.Bold,
            color = if (value == null) AppTheme.colors.muted else Color.Unspecified,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        trailing()
    }
}

/** Ring for the boarding station, filled dot for the destination. */
@Composable
private fun StationDot(filled: Boolean) {
    val colors = AppTheme.colors
    val dot = Modifier.size(10.dp)
    Box(if (filled) dot.background(colors.amber, CircleShape) else dot.border(2.5.dp, colors.muted, CircleShape))
}

@Composable
private fun ListHeader(text: String) {
    Text(
        text,
        Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 4.dp),
        fontFamily = Sora,
        fontSize = 17.sp,
        fontWeight = FontWeight.Bold,
    )
}

/** Starred route as a ticket: stations and later departures, next departure on the stub. Long-press to reorder. */
@Composable
private fun ReorderableCollectionItemScope.Ticket(
    vm: AppViewModel,
    tt: Timetable,
    route: Route,
    isDragging: Boolean,
    onDragStopped: () -> Unit,
    onClick: () -> Unit,
) {
    val now by vm.now.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val departures = remember(tt, route, now) { nextDepartures(tt, route, now, 3) }
    val colors = AppTheme.colors
    val page = MaterialTheme.colorScheme.background
    val line = MaterialTheme.colorScheme.outlineVariant

    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 96.dp)
            .height(IntrinsicSize.Min)
            .background(if (isDragging) colors.highlight else colors.ticket)
            .drawWithContent {
                drawContent()
                // Perforation between body and stub, with a notch at each end.
                val x = size.width - StubWidth.toPx()
                val dash = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx()))
                drawLine(line, Offset(x, 12.dp.toPx()), Offset(x, size.height - 12.dp.toPx()), 2.dp.toPx(), pathEffect = dash)
                drawCircle(page, 9.dp.toPx(), Offset(x, 0f))
                drawCircle(page, 9.dp.toPx(), Offset(x, size.height))
            }.longPressDraggableHandle(onDragStopped = onDragStopped)
            .clickable(onClick = onClick),
    ) {
        Column(
            Modifier.weight(1f).fillMaxHeight().padding(start = 16.dp, top = 14.dp, end = 12.dp, bottom = 14.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(Modifier.padding(top = 5.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    StationDot(filled = false)
                    Box(Modifier.size(width = 2.dp, height = 14.dp).background(line))
                    StationDot(filled = true)
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (id in listOf(route.fromId, route.toId)) {
                        Text(
                            tt.station(id).name,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            if (departures.size > 1) {
                Text(
                    "Then " + departures.drop(1).joinToString(" · ") { formatTime(it.dep, settings.clock24) },
                    fontSize = 12.sp,
                    color = colors.muted,
                    maxLines = 1,
                )
            }
        }
        Column(
            Modifier.width(StubWidth).fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val next = departures.firstOrNull()
            if (next == null) {
                Text("No trains", fontSize = 14.sp, color = colors.muted)
            } else {
                TimeText(formatTime(next.dep, settings.clock24), fontSize = 22.sp)
                val (chipBg, chipFg) = colors.countdown
                Text(
                    formatCountdown(now, next.dep),
                    Modifier.background(chipBg, RoundedCornerShape(10.dp)).padding(horizontal = 8.dp, vertical = 2.dp),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = chipFg,
                )
            }
        }
    }
}

@Composable
private fun RecentRow(
    tt: Timetable,
    route: Route,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.History, contentDescription = null, tint = AppTheme.colors.muted)
        Text(tt.routeLabel(route), Modifier.weight(1f), fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = AppTheme.colors.muted)
    }
}
