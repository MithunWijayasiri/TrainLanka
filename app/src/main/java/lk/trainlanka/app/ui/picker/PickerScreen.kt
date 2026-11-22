package lk.trainlanka.app.ui.picker

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardReturn
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import lk.trainlanka.app.data.Place
import lk.trainlanka.app.data.Route
import lk.trainlanka.app.domain.MatchKind
import lk.trainlanka.app.domain.OTHER_PROVINCE
import lk.trainlanka.app.domain.StationMatch
import lk.trainlanka.app.domain.groupByLetter
import lk.trainlanka.app.domain.searchStations
import lk.trainlanka.app.ui.AppViewModel
import lk.trainlanka.app.ui.PickerField
import lk.trainlanka.app.ui.PlaceChip
import lk.trainlanka.app.ui.icon
import lk.trainlanka.app.ui.rememberNearestStationAction
import lk.trainlanka.app.ui.theme.AppTheme

/** What a picked station fills: a search field, or a saved place. */
sealed interface PickTarget {
    data class Search(
        val field: PickerField,
    ) : PickTarget

    data class Saved(
        val place: Place,
    ) : PickTarget
}

/**
 * A search field pick ends with [onOpenRoute] instead of [onDone] once both stations are set and have direct trains.
 * A saved place pick always ends with [onDone].
 */
@Composable
fun PickerScreen(
    vm: AppViewModel,
    target: PickTarget,
    onDone: () -> Unit,
    onOpenRoute: (Route) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val timetable by vm.timetable.collectAsStateWithLifecycle()
    val places by vm.places.collectAsStateWithLifecycle()

    fun finish(picked: Boolean) {
        val route = vm.openableRoute()?.takeIf { picked }
        if (route == null) onDone() else onOpenRoute(route)
    }

    val useNearest = rememberNearestStationAction(vm, onDone = ::finish)
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val tt = timetable ?: return
    val byLetter = remember(tt) { groupByLetter(tt.stations) }

    fun pick(id: Int) {
        when (target) {
            is PickTarget.Saved -> {
                vm.setPlace(target.place, id)
                onDone()
            }

            is PickTarget.Search -> {
                if (target.field == PickerField.FROM) vm.setFrom(id) else vm.setTo(id)
                finish(picked = true)
            }
        }
    }

    val searching = query.isNotBlank()
    val matches = if (searching) searchStations(tt.stations, query) else emptyList()

    Column(Modifier.fillMaxSize().systemBarsPadding().imePadding()) {
        SearchField(
            query = query,
            onQueryChange = {
                query = it
                scope.launch { listState.scrollToItem(0) }
            },
            placeholder =
                when (target) {
                    is PickTarget.Saved -> "Choose your ${target.place.label} station"
                    is PickTarget.Search -> if (target.field == PickerField.FROM) "Where are you boarding?" else "Where are you going?"
                },
            onBack = onDone,
            onSubmit = { matches.firstOrNull()?.let { pick(it.station.id) } },
        )
        when (target) {
            is PickTarget.Saved -> SavingAs(target.place)
            is PickTarget.Search -> Shortcuts(useNearest.takeIf { target.field == PickerField.FROM }, places, ::pick)
        }
        Box(Modifier.fillMaxSize()) {
            LazyColumn(Modifier.fillMaxSize(), state = listState) {
                if (searching) {
                    if (matches.isEmpty()) {
                        item(key = "empty") { NoResults(query.trim()) }
                    }
                    items(matches.size, key = { "m${matches[it].station.id}" }) { i ->
                        MatchRow(matches[i], query.trim(), first = i == 0) { pick(matches[i].station.id) }
                    }
                } else {
                    for ((letter, stations) in byLetter) {
                        stickyHeader(key = "l$letter") { LetterHeader(letter) }
                        items(stations, key = { "s${it.id}" }) { s -> StationRow(s.label) { pick(s.id) } }
                    }
                }
            }
            if (!searching) {
                val headerIndex =
                    remember(byLetter) {
                        var index = 0
                        byLetter.mapValues { (_, stations) -> index.also { index += 1 + stations.size } }
                    }
                LetterIndex(headerIndex.keys, Modifier.align(Alignment.CenterEnd)) { letter ->
                    headerIndex[letter]?.let { scope.launch { listState.scrollToItem(it) } }
                }
            }
        }
    }
}

/** Contacts-style A–Z bar: tap or drag to jump; letters without stations are greyed out. */
@Composable
private fun LetterIndex(
    available: Set<Char>,
    modifier: Modifier,
    onLetter: (Char) -> Unit,
) {
    val currentOnLetter by rememberUpdatedState(onLetter)
    Column(
        modifier.fillMaxHeight().width(28.dp).pointerInput(Unit) {
            fun letterAt(y: Float) = LETTERS[(y / size.height * LETTERS.size).toInt().coerceIn(LETTERS.indices)]
            awaitEachGesture {
                val down = awaitFirstDown()
                down.consume()
                var last = letterAt(down.position.y).also(currentOnLetter)
                drag(down.id) { change ->
                    change.consume()
                    val letter = letterAt(change.position.y)
                    if (letter != last) last = letter.also(currentOnLetter)
                }
            }
        },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly,
    ) {
        for (letter in LETTERS) {
            val jump =
                if (letter in available) {
                    Modifier.semantics {
                        onClick("Jump to $letter") {
                            currentOnLetter(letter)
                            true
                        }
                    }
                } else {
                    Modifier
                }
            Text(
                letter.toString(),
                jump,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (letter in available) MaterialTheme.colorScheme.primary else AppTheme.colors.faint,
            )
        }
    }
}

private val LETTERS = ('A'..'Z').toList()

@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    onBack: () -> Unit,
    onSubmit: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .height(56.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(AppTheme.colors.card),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back") }
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (query.isEmpty()) {
                Text(placeholder, fontSize = 16.sp, color = AppTheme.colors.muted, maxLines = 1)
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = TextStyle(fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onSubmit() }),
            )
        }
        if (query.isNotEmpty()) {
            IconButton(onClick = { onQueryChange("") }) {
                Icon(Icons.Rounded.Close, contentDescription = "Clear", tint = AppTheme.colors.muted)
            }
        }
    }
}

/** Nearest station (From only) and saved places; a saved place fills the field like a list pick. */
@Composable
private fun Shortcuts(
    nearest: (() -> Unit)?,
    places: Map<Place, Int>,
    onPick: (Int) -> Unit,
) {
    if (nearest == null && places.isEmpty()) return
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(start = 16.dp, top = 4.dp, end = 16.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (nearest != null) NearestPill(nearest)
        for ((place, id) in places) PlaceChip(place, set = true) { onPick(id) }
    }
}

@Composable
private fun SavingAs(place: Place) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, top = 4.dp, end = 16.dp, bottom = 12.dp)
            .background(AppTheme.colors.highlight, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(place.icon, contentDescription = null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
        Text("Saving as ${place.label}", fontSize = 14.sp)
    }
}

@Composable
private fun NearestPill(onClick: () -> Unit) {
    val primary = MaterialTheme.colorScheme.primary
    Row(
        Modifier
            .height(40.dp)
            .clip(CircleShape)
            .border(1.dp, primary, CircleShape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(start = 12.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(Icons.Rounded.MyLocation, contentDescription = null, Modifier.size(18.dp), tint = primary)
        Text("Nearest station", color = primary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun LetterHeader(letter: Char) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(36.dp)
            .background(AppTheme.colors.card)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(letter.toString(), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun StationRow(
    name: String,
    onClick: () -> Unit,
) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clickable(onClick = onClick)
            .bottomLine(MaterialTheme.colorScheme.outlineVariant)
            .padding(start = 16.dp, end = 40.dp), // end clears the letter index
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(name, fontSize = 15.sp, maxLines = 1)
    }
}

@Composable
private fun MatchRow(
    match: StationMatch,
    query: String,
    first: Boolean,
    onClick: () -> Unit,
) {
    val base = Modifier.fillMaxWidth().clickable(onClick = onClick)
    Row(
        if (first) {
            base.height(64.dp).background(AppTheme.colors.highlight).padding(horizontal = 16.dp)
        } else {
            base.height(60.dp).bottomLine(MaterialTheme.colorScheme.outlineVariant).padding(horizontal = 16.dp)
        },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(matchTitle(match, query), fontSize = 16.sp, maxLines = 1)
            Text(matchSubtitle(match), fontSize = 12.sp, color = AppTheme.colors.muted, maxLines = 1)
        }
        if (first) Icon(Icons.AutoMirrored.Rounded.KeyboardReturn, contentDescription = null, tint = AppTheme.colors.faint)
    }
}

@Composable
private fun NoResults(query: String) {
    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
        Text(
            "No stations match “$query”",
            fontSize = 15.sp,
            color = AppTheme.colors.muted,
            textAlign = TextAlign.Center,
        )
    }
}

private fun matchTitle(
    match: StationMatch,
    query: String,
): AnnotatedString {
    val name = match.station.name
    val bold = SpanStyle(fontWeight = FontWeight.Bold)
    return buildAnnotatedString {
        if (match.kind == MatchKind.ALIAS) {
            append("$name (")
            withStyle(bold) { append(checkNotNull(match.alias)) }
            append(")")
            return@buildAnnotatedString
        }
        val at = name.indexOf(query, ignoreCase = true)
        if (at < 0) {
            append(name)
        } else {
            append(name.substring(0, at))
            withStyle(bold) { append(name.substring(at, at + query.length)) }
            append(name.substring(at + query.length))
        }
    }
}

private fun matchSubtitle(match: StationMatch): String {
    val province = match.station.province ?: OTHER_PROVINCE
    return when (match.kind) {
        MatchKind.ALIAS -> "$province · matches alias"
        MatchKind.FUZZY -> "$province · similar spelling"
        MatchKind.NAME -> province
    }
}

private fun Modifier.bottomLine(color: Color) =
    drawBehind {
        val y = size.height - 0.5.dp.toPx()
        drawLine(color, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
    }
