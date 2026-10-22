package lk.trainlanka.app.ui.picker

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.automirrored.rounded.KeyboardReturn
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
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
import lk.trainlanka.app.domain.MatchKind
import lk.trainlanka.app.domain.OTHER_PROVINCE
import lk.trainlanka.app.domain.StationMatch
import lk.trainlanka.app.domain.groupByProvince
import lk.trainlanka.app.domain.searchStations
import lk.trainlanka.app.ui.AppViewModel
import lk.trainlanka.app.ui.PickerField
import lk.trainlanka.app.ui.rememberNearestStationAction
import lk.trainlanka.app.ui.theme.AppTheme

@Composable
fun PickerScreen(vm: AppViewModel, field: PickerField, onDone: () -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    val openProvinces by vm.openProvinces.collectAsStateWithLifecycle()
    val timetable by vm.timetable.collectAsStateWithLifecycle()
    val useNearest = rememberNearestStationAction(vm, onDone = onDone)
    val tt = timetable ?: return

    fun pick(id: Int) {
        if (field == PickerField.FROM) vm.setFrom(id) else vm.setTo(id)
        onDone()
    }

    val searching = query.isNotBlank()
    val matches = if (searching) searchStations(tt.stations, query) else emptyList()

    Column(Modifier.fillMaxSize().systemBarsPadding().imePadding()) {
        SearchField(
            query = query,
            onQueryChange = { query = it },
            placeholder = if (field == PickerField.FROM) "From: search station" else "To: search station",
            onBack = onDone,
            onSubmit = { matches.firstOrNull()?.let { pick(it.station.id) } },
        )
        LazyColumn(Modifier.fillMaxSize()) {
            if (field == PickerField.FROM) {
                item(key = "nearest") { NearestRow(showSubtitle = !searching, onClick = useNearest) }
            }
            if (searching) {
                if (matches.isEmpty()) {
                    item(key = "empty") { NoResults(query.trim()) }
                }
                items(matches.size, key = { "m${matches[it].station.id}" }) { i ->
                    MatchRow(matches[i], query.trim(), first = i == 0) { pick(matches[i].station.id) }
                }
            } else {
                for ((province, stations) in groupByProvince(tt.stations)) {
                    val open = province in openProvinces
                    item(key = "p$province") {
                        ProvinceHeader(province, open) { vm.setProvinceOpen(province, !open) }
                    }
                    if (open) {
                        items(stations, key = { "s${it.id}" }) { s -> StationRow(s.label) { pick(s.id) } }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    onBack: () -> Unit,
    onSubmit: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp).height(56.dp)
            .clip(RoundedCornerShape(28.dp)).background(AppTheme.colors.card),
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

@Composable
private fun NearestRow(showSubtitle: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(64.dp).clickable(onClick = onClick)
            .bottomLine(MaterialTheme.colorScheme.outlineVariant).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.MyLocation,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("Use nearest station", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            if (showSubtitle) Text("Uses your location once", fontSize = 12.sp, color = AppTheme.colors.muted)
        }
    }
}

@Composable
private fun ProvinceHeader(province: String, open: Boolean, onToggle: () -> Unit) {
    val base = Modifier.fillMaxWidth().clickable(onClick = onToggle)
    Row(
        if (open) base.height(52.dp).background(AppTheme.colors.card).padding(horizontal = 16.dp)
        else base.height(48.dp).topLine(MaterialTheme.colorScheme.outlineVariant).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(province, Modifier.weight(1f), fontSize = 15.sp, fontWeight = FontWeight.Bold)
        if (open) {
            Icon(Icons.Rounded.ExpandMore, contentDescription = "Collapse $province")
        } else {
            Icon(Icons.Rounded.ChevronRight, contentDescription = "Expand $province", tint = AppTheme.colors.muted)
        }
    }
}

@Composable
private fun StationRow(name: String, onClick: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().height(48.dp).clickable(onClick = onClick)
            .bottomLine(MaterialTheme.colorScheme.outlineVariant).padding(start = 32.dp, end = 16.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(name, fontSize = 15.sp, maxLines = 1)
    }
}

@Composable
private fun MatchRow(match: StationMatch, query: String, first: Boolean, onClick: () -> Unit) {
    val base = Modifier.fillMaxWidth().clickable(onClick = onClick)
    Row(
        if (first) base.height(64.dp).background(AppTheme.colors.highlight).padding(horizontal = 16.dp)
        else base.height(60.dp).bottomLine(MaterialTheme.colorScheme.outlineVariant).padding(horizontal = 16.dp),
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

private fun matchTitle(match: StationMatch, query: String): AnnotatedString {
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

private fun Modifier.bottomLine(color: Color) = drawBehind {
    val y = size.height - 0.5.dp.toPx()
    drawLine(color, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
}

private fun Modifier.topLine(color: Color) = drawBehind {
    val y = 0.5.dp.toPx()
    drawLine(color, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
}
