package lk.trainlanka.app.ui.stops

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import lk.trainlanka.app.data.Route
import lk.trainlanka.app.domain.formatDuration
import lk.trainlanka.app.domain.formatMinutes
import lk.trainlanka.app.ui.AppViewModel
import lk.trainlanka.app.ui.TimeText
import lk.trainlanka.app.ui.TopBar
import lk.trainlanka.app.ui.theme.AppTheme

private enum class StopKind { OUTSIDE, BOARD, MIDDLE, GET_OFF }

@Composable
fun StopsScreen(vm: AppViewModel, trainId: Int, route: Route, onBack: () -> Unit) {
    val tt = vm.timetable.collectAsStateWithLifecycle().value ?: return
    val clock24 = vm.settings.collectAsStateWithLifecycle().value.clock24
    val train = tt.trainsById.getValue(trainId)
    val stops = train.stopList
    val fromIndex = stops.indexOfFirst { it.stationId == route.fromId }
        .takeIf { it >= 0 } ?: error("Train $trainId does not stop at station ${route.fromId}")
    val toIndex = (fromIndex + 1 until stops.size).firstOrNull { stops[it].stationId == route.toId }
        ?: error("Train $trainId does not stop at station ${route.toId} after station ${route.fromId}")
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = maxOf(0, fromIndex - 1))
    val colors = AppTheme.colors

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.safeDrawingPadding()) {
            TopBar(
                title = "#${train.no} · ${train.type}",
                onBack = onBack,
                subtitle = "${tt.station(stops.first().stationId).name} → ${tt.station(stops.last().stationId).name}",
            )
            Row(
                Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = 8.dp)
                    .background(colors.highlight, RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            ) {
                Text(
                    buildAnnotatedString {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("Your trip") }
                        append(" ${tt.station(route.fromId).name} → ${tt.station(route.toId).name}")
                    },
                    Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp,
                )
                Text(
                    formatDuration((stops[toIndex].arr - stops[fromIndex].dep).toLong()),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp,
                )
            }
            Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 4.dp)) {
                Text("ARR · DEP", Modifier.width(96.dp), color = colors.faint, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(28.dp))
                Text("STATION", color = colors.faint, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            LazyColumn(Modifier.weight(1f), state = listState) {
                itemsIndexed(stops) { i, stop ->
                    val kind = when {
                        i < fromIndex || i > toIndex -> StopKind.OUTSIDE
                        i == fromIndex -> StopKind.BOARD
                        i == toIndex -> StopKind.GET_OFF
                        else -> StopKind.MIDDLE
                    }
                    val times = when {
                        i == 0 -> listOf(stop.dep)
                        i == stops.lastIndex -> listOf(stop.arr)
                        stop.arr == stop.dep -> listOf(stop.arr)
                        else -> listOf(stop.arr, stop.dep)
                    }
                    StopRow(kind, times, tt.station(stop.stationId).name, clock24)
                }
            }
        }
    }
}

@Composable
private fun StopRow(kind: StopKind, times: List<Int>, name: String, clock24: Boolean) {
    val primary = MaterialTheme.colorScheme.primary
    val onSurface = MaterialTheme.colorScheme.onSurface
    val endpoint = kind == StopKind.BOARD || kind == StopKind.GET_OFF
    val shape = when (kind) {
        StopKind.BOARD -> RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
        StopKind.GET_OFF -> RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp)
        else -> RectangleShape
    }
    val box = if (kind == StopKind.OUTSIDE) Modifier.padding(horizontal = 16.dp)
    else Modifier.padding(horizontal = 8.dp).background(AppTheme.colors.highlight, shape).padding(horizontal = 8.dp)
    val textColor = if (kind == StopKind.OUTSIDE) AppTheme.colors.faint else onSurface
    val textSize = if (kind == StopKind.OUTSIDE) 13.sp else 14.sp

    Row(
        Modifier.fillMaxWidth().height(if (endpoint) 48.dp else 34.dp).then(box),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TimeCell(times, clock24, textSize, if (endpoint) FontWeight.Bold else FontWeight.Normal, textColor, Modifier.width(96.dp))
        Rail(kind, Modifier.width(28.dp).fillMaxHeight())
        if (endpoint) {
            Column {
                Text(name, color = onSurface, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    if (kind == StopKind.BOARD) "BOARD HERE" else "GET OFF",
                    color = primary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        } else {
            Text(name, color = textColor, fontSize = textSize, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

// 12-hour arrival/departure pairs stack on two lines to fit the 96dp column.
@Composable
private fun TimeCell(
    times: List<Int>,
    clock24: Boolean,
    fontSize: TextUnit,
    weight: FontWeight,
    color: Color,
    modifier: Modifier,
) {
    val text = times.map { formatMinutes(it, clock24) }
    when {
        text.size == 1 -> TimeText(text[0], fontSize, modifier, color, weight)
        clock24 -> Text(text.joinToString(" · "), modifier, color = color, fontSize = fontSize, fontWeight = weight, maxLines = 1)
        else -> Column(modifier) { text.forEach { TimeText(it, 12.sp, color = color, fontWeight = weight) } }
    }
}

@Composable
private fun Rail(kind: StopKind, modifier: Modifier) {
    val railColor = AppTheme.colors.rail
    val primary = MaterialTheme.colorScheme.primary
    val surface = MaterialTheme.colorScheme.surface
    Canvas(modifier) {
        val cx = size.width / 2
        val cy = size.height / 2
        when (kind) {
            StopKind.OUTSIDE -> drawLine(
                railColor, Offset(cx, 0f), Offset(cx, size.height), 2.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx())),
            )
            StopKind.BOARD -> drawLine(primary, Offset(cx, cy), Offset(cx, size.height), 4.dp.toPx())
            StopKind.MIDDLE -> drawLine(primary, Offset(cx, 0f), Offset(cx, size.height), 4.dp.toPx())
            StopKind.GET_OFF -> drawLine(primary, Offset(cx, 0f), Offset(cx, cy), 4.dp.toPx())
        }
        if (kind == StopKind.BOARD || kind == StopKind.GET_OFF) {
            drawCircle(primary, 8.dp.toPx(), Offset(cx, cy))
        } else {
            drawCircle(if (kind == StopKind.OUTSIDE) railColor else primary, 6.dp.toPx(), Offset(cx, cy))
            drawCircle(surface, 4.dp.toPx(), Offset(cx, cy))
        }
    }
}
