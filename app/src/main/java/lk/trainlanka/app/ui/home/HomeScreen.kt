package lk.trainlanka.app.ui.home

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import lk.trainlanka.app.data.RemovedRoute
import lk.trainlanka.app.data.Route
import lk.trainlanka.app.data.Timetable
import lk.trainlanka.app.domain.formatCountdown
import lk.trainlanka.app.domain.formatTime
import lk.trainlanka.app.domain.formatVersionDate
import lk.trainlanka.app.domain.hasDirectTrains
import lk.trainlanka.app.domain.nextDepartures
import lk.trainlanka.app.ui.AppViewModel
import lk.trainlanka.app.ui.Banner
import lk.trainlanka.app.ui.EmptyState
import lk.trainlanka.app.ui.LocationProblemDialog
import lk.trainlanka.app.ui.PickerField
import lk.trainlanka.app.ui.SNACKBAR_MILLIS
import lk.trainlanka.app.ui.Snackbar
import lk.trainlanka.app.ui.TextAction
import lk.trainlanka.app.ui.TimeText
import lk.trainlanka.app.ui.TopBar
import lk.trainlanka.app.ui.rememberNearestStationAction
import lk.trainlanka.app.ui.theme.AppTheme
import sh.calvin.reorderable.ReorderableCollectionItemScope
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

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

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).safeDrawingPadding()) {
        Column(Modifier.fillMaxSize()) {
            TopBar(
                title = "Train Lanka",
                titleColor = MaterialTheme.colorScheme.primary,
                titleSize = 22.sp,
                actions = {
                    IconButton(onClick = onOpenSettings) { Icon(Icons.Rounded.Settings, contentDescription = "Settings") }
                },
            )
            val timetable = tt ?: return@Column
            bannerVersion?.let { version ->
                Banner(
                    icon = { Icon(Icons.Outlined.Download, contentDescription = null, Modifier.size(20.dp)) },
                    text = "New timetable available (${formatVersionDate(version)}). You can keep using the current one.",
                ) {
                    TextAction("Later") { vm.dismissUpdate(version) }
                    TextAction("Update", underline = true) { vm.applyUpdate() }
                }
            }
            if (starred.isEmpty() && recent.isEmpty()) {
                SearchCard(vm, timetable, onPickStation, open)
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    EmptyState(
                        "No saved routes yet",
                        "Search a route, then tap ☆ to star it. Starred and recent routes show up here with their next train.",
                    )
                }
                return@Column
            }
            LazyColumn(Modifier.weight(1f), state = listState) {
                item(key = "search") { SearchCard(vm, timetable, onPickStation, open) }
                if (starredOrder.isNotEmpty()) {
                    item(key = "starred-label") { ListHeader("Starred") }
                    items(starredOrder, key = { it.key() }) { route ->
                        ReorderableItem(reorderState, key = route.key()) { isDragging ->
                            SwipeToRemove(onRemove = { remove(route) }) {
                                StarredRow(vm, timetable, route, isDragging, onDragStopped = { vm.setStarred(latestOrder) }) { open(route) }
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
        removed?.let { r ->
            Snackbar(if (r.starred) "Removed from starred" else "Removed from recent", Modifier.align(Alignment.BottomCenter)) {
                TextAction("Undo", underline = true, color = AppTheme.colors.amber) {
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
    val state = rememberSwipeToDismissBoxState()
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

/**
 * From/To rows with a swap strip, plus a notice when the picked route has no direct trains.
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
    val noTrains = remember(tt, from, to) { vm.pickedRoute()?.let { !hasDirectTrains(tt, it) } ?: false }
    val shape = RoundedCornerShape(16.dp)

    Column(Modifier.padding(horizontal = 16.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .clip(shape)
                .border(1.dp, MaterialTheme.colorScheme.secondaryContainer, shape),
        ) {
            Column(Modifier.weight(1f)) {
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
                HorizontalDivider(Modifier.padding(start = 36.dp), color = MaterialTheme.colorScheme.outlineVariant)
                StationRow(
                    label = "To",
                    value = to?.let { tt.station(it).name },
                    placeholder = "Destination station",
                    filledDot = true,
                    onClick = { onPickStation(PickerField.TO) },
                )
            }
            VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Box(
                Modifier
                    .fillMaxHeight()
                    .width(52.dp)
                    .background(AppTheme.colors.card)
                    .clickable(role = Role.Button, onClick = vm::swapStations),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.SwapVert, contentDescription = "Swap stations", tint = MaterialTheme.colorScheme.onSecondaryContainer)
            }
        }
        if (noTrains) NoTrainsNotice()
    }
}

@Composable
private fun NoTrainsNotice() {
    Row(
        Modifier
            .padding(top = 10.dp)
            .fillMaxWidth()
            .background(AppTheme.colors.banner, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(Icons.Outlined.Info, contentDescription = null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text("No direct trains on this route", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text("Train Lanka shows direct trains only.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun StationRow(
    label: String,
    value: String?,
    placeholder: String,
    filledDot: Boolean,
    onClick: () -> Unit,
    trailing: (@Composable () -> Unit)? = null,
) {
    val primary = MaterialTheme.colorScheme.primary
    Row(
        Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clickable(onClick = onClick)
            .padding(start = 16.dp, end = if (trailing == null) 16.dp else 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(
                    10.dp,
                ).then(if (filledDot) Modifier.background(primary, CircleShape) else Modifier.border(2.5.dp, primary, CircleShape)),
        )
        Text(label, Modifier.widthIn(min = 36.dp), fontSize = 13.sp, color = AppTheme.colors.muted)
        Text(
            value ?: placeholder,
            Modifier.weight(1f),
            fontSize = 16.sp,
            fontWeight = if (value == null) FontWeight.Normal else FontWeight.Bold,
            color = if (value == null) AppTheme.colors.muted else Color.Unspecified,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        trailing?.invoke()
    }
}

@Composable
private fun ListHeader(text: String) {
    Text(text, Modifier.padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 6.dp), fontSize = 16.sp, fontWeight = FontWeight.Bold)
}

@Composable
private fun ReorderableCollectionItemScope.StarredRow(
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
    val elevation by animateDpAsState(if (isDragging) 8.dp else 0.dp, label = "dragElevation")

    Column(
        Modifier
            .shadow(elevation)
            .background(if (isDragging) AppTheme.colors.highlight else Color.Transparent)
            .clickable(onClick = onClick),
    ) {
        Row(
            Modifier.fillMaxWidth().height(68.dp).padding(start = 4.dp, end = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(width = 40.dp, height = 48.dp).draggableHandle(onDragStopped = onDragStopped),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.DragHandle, contentDescription = "Reorder", tint = AppTheme.colors.faint)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(tt.routeLabel(route), fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (departures.size > 1) {
                    Text(
                        "Then " + departures.drop(1).joinToString(" · ") { formatTime(it.dep, settings.clock24) },
                        fontSize = 13.sp,
                        color = AppTheme.colors.muted,
                        maxLines = 1,
                    )
                }
            }
            val next = departures.firstOrNull()
            if (next == null) {
                Text("No trains", fontSize = 14.sp, color = AppTheme.colors.muted)
            } else {
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    TimeText(formatTime(next.dep, settings.clock24), fontSize = 22.sp, color = MaterialTheme.colorScheme.primary)
                    Text(
                        formatCountdown(now, next.dep),
                        Modifier
                            .background(
                                AppTheme.colors.highlight,
                                RoundedCornerShape(10.dp),
                            ).padding(horizontal = 8.dp, vertical = 2.dp),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
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
