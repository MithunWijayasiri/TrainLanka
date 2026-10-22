package lk.trainlanka.app.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ColumnScope
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import lk.trainlanka.app.MainActivity
import lk.trainlanka.app.TrainLankaApp
import lk.trainlanka.app.data.Route
import lk.trainlanka.app.data.Timetable
import lk.trainlanka.app.domain.COLOMBO
import lk.trainlanka.app.domain.Trip
import lk.trainlanka.app.domain.formatTime
import lk.trainlanka.app.domain.nextDepartures
import lk.trainlanka.app.domain.nowInColombo

const val EXTRA_FROM = "lk.trainlanka.app.FROM"
const val EXTRA_TO = "lk.trainlanka.app.TO"
const val EXTRA_TRAIN = "lk.trainlanka.app.TRAIN"

private const val MAX_ROWS = 3

private val Teal = ColorProvider(day = Color(0xFF0F5C57), night = Color(0xFF7FCFC5))
private val TextPrimary = ColorProvider(day = Color(0xFF1D1D1D), night = Color(0xFFE3E8E7))
private val Muted = ColorProvider(day = Color(0xFF555555), night = Color(0xFFA9B4B2))
private val Surface = ColorProvider(day = Color(0xFFFFFFFF), night = Color(0xFF1A2322))
private val Divider = ColorProvider(day = Color(0xFFE6E6E6), night = Color(0xFF2C3534))
private val ButtonFill = ColorProvider(day = Color(0xFFE0EFED), night = Color(0xFF26403D))
private val Star = ColorProvider(day = Color(0xFFE09B12), night = Color(0xFFE09B12))

private class WidgetData(val route: Route, val tt: Timetable, val trips: List<Trip>, val clock24: Boolean)

class RouteWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = TrainLankaApp.from(context)
        val clock24 = app.prefs.settings.first().clock24
        val tt = app.timetables.await()
        val route = app.prefs.starred.first()
            .firstOrNull { it.fromId in tt.stationsById && it.toId in tt.stationsById }
        val data = route?.let {
            val trips = withContext(Dispatchers.Default) { nextDepartures(tt, it, nowInColombo(), MAX_ROWS) }
            WidgetData(it, tt, trips, clock24)
        }
        scheduleRefresh(context, data?.trips?.firstOrNull())
        provideContent {
            Column(
                modifier = GlanceModifier.fillMaxSize()
                    .background(Surface)
                    .cornerRadius(24.dp)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            ) {
                if (data == null) EmptyState(context) else RouteContent(context, data)
            }
        }
    }

    @Composable
    private fun ColumnScope.RouteContent(context: Context, data: WidgetData) {
        val route = data.route
        val tt = data.tt
        Row(
            modifier = GlanceModifier.fillMaxWidth().padding(bottom = 6.dp)
                .clickable(actionStartActivity(routeIntent(context, route))),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("★ ", style = TextStyle(color = Star, fontSize = 15.sp))
            Text(
                text = tt.routeLabel(route),
                style = TextStyle(color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold),
                maxLines = 1,
                modifier = GlanceModifier.defaultWeight(),
            )
            Text("Train Lanka", style = TextStyle(color = Muted, fontSize = 11.sp), maxLines = 1)
        }
        if (data.trips.isEmpty()) {
            Box(modifier = GlanceModifier.fillMaxWidth().defaultWeight(), contentAlignment = Alignment.CenterStart) {
                Text("No upcoming trains", style = TextStyle(color = Muted, fontSize = 13.sp))
            }
            return
        }
        data.trips.forEach { trip ->
            Box(modifier = GlanceModifier.fillMaxWidth().height(1.dp).background(Divider)) {}
            val destination = tt.station(trip.train.stopList.last().stationId).name
            Row(
                modifier = GlanceModifier.fillMaxWidth().defaultWeight()
                    .clickable(actionStartActivity(tripIntent(context, route, trip))),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = formatTime(trip.dep, data.clock24),
                    style = TextStyle(color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    modifier = GlanceModifier.width(92.dp),
                )
                Text(
                    text = "${trip.train.type} · to $destination",
                    style = TextStyle(color = Muted, fontSize = 13.sp),
                    maxLines = 1,
                    modifier = GlanceModifier.defaultWeight(),
                )
            }
        }
    }

    @Composable
    private fun ColumnScope.EmptyState(context: Context) {
        Column(
            modifier = GlanceModifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("☆", style = TextStyle(color = Muted, fontSize = 22.sp))
            Text(
                text = "Star a route in Train Lanka",
                style = TextStyle(color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold),
                modifier = GlanceModifier.padding(top = 2.dp, bottom = 8.dp),
            )
            Box(
                modifier = GlanceModifier
                    .background(ButtonFill)
                    .cornerRadius(20.dp)
                    .padding(horizontal = 18.dp, vertical = 8.dp)
                    .clickable(actionStartActivity(baseIntent(context))),
            ) {
                Text("Open app", style = TextStyle(color = Teal, fontSize = 13.sp, fontWeight = FontWeight.Bold))
            }
        }
    }

    companion object {
        suspend fun refresh(context: Context) = RouteWidget().updateAll(context)
    }
}

class RouteWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = RouteWidget()
}

/** Fired by the alarm set in [scheduleRefresh]. */
class WidgetRefreshReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        TrainLankaApp.from(context).scope.launch {
            try {
                RouteWidget.refresh(context)
            } finally {
                pending.finish()
            }
        }
    }
}

private fun baseIntent(context: Context) = Intent(context, MainActivity::class.java)
    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)

private fun routeIntent(context: Context, route: Route) = baseIntent(context)
    .setData(Uri.parse("trainlanka://route/${route.fromId}/${route.toId}"))
    .putExtra(EXTRA_FROM, route.fromId)
    .putExtra(EXTRA_TO, route.toId)

private fun tripIntent(context: Context, route: Route, trip: Trip) = baseIntent(context)
    .setData(Uri.parse("trainlanka://trip/${trip.train.id}/${trip.startDate}"))
    .putExtra(EXTRA_FROM, route.fromId)
    .putExtra(EXTRA_TO, route.toId)
    .putExtra(EXTRA_TRAIN, trip.train.id)

// Inexact alarm one minute after the first listed departure, so the list drops it.
private fun scheduleRefresh(context: Context, first: Trip?) {
    val alarms = context.getSystemService(AlarmManager::class.java)
    val pending = PendingIntent.getBroadcast(
        context,
        0,
        Intent(context, WidgetRefreshReceiver::class.java),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )
    if (first == null) {
        alarms.cancel(pending)
        return
    }
    val at = first.dep.plusMinutes(1).atZone(COLOMBO).toInstant().toEpochMilli()
    alarms.set(AlarmManager.RTC, at, pending)
}
