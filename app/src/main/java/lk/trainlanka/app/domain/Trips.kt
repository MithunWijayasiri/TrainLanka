package lk.trainlanka.app.domain

import lk.trainlanka.app.data.Route
import lk.trainlanka.app.data.Timetable
import lk.trainlanka.app.data.Train
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

val COLOMBO: ZoneId = ZoneId.of("Asia/Colombo")

fun nowInColombo(): LocalDateTime = LocalDateTime.now(COLOMBO).withSecond(0).withNano(0)

/** One train run between two stations on a specific start day. */
data class Trip(
    val train: Train,
    val fromIndex: Int,
    val toIndex: Int,
    val startDate: LocalDate,
    val dep: LocalDateTime,
    val arr: LocalDateTime,
) {
    val minutes: Long get() =
        java.time.Duration
            .between(dep, arr)
            .toMinutes()
}

/** Trains stopping at [Route.fromId] then later at [Route.toId], with stop indexes. */
private fun legs(
    tt: Timetable,
    route: Route,
): List<Triple<Train, Int, Int>> =
    tt.trains.mapNotNull { train ->
        val stops = train.stopList
        val from = stops.indexOfFirst { it.stationId == route.fromId }
        if (from < 0) return@mapNotNull null
        val to = (from + 1 until stops.size).firstOrNull { stops[it].stationId == route.toId } ?: return@mapNotNull null
        Triple(train, from, to)
    }

fun hasDirectTrains(
    tt: Timetable,
    route: Route,
): Boolean = legs(tt, route).isNotEmpty()

/** Trips departing [route] origin on [date], sorted by departure. Includes trains that started the day before. */
fun tripsOn(
    tt: Timetable,
    route: Route,
    date: LocalDate,
): List<Trip> =
    legs(tt, route)
        .flatMap { (train, from, to) ->
            listOf(date.minusDays(1), date).mapNotNull { start ->
                if (start.dayOfWeek !in train.weekdays) return@mapNotNull null
                val midnight = start.atStartOfDay()
                val dep = midnight.plusMinutes(train.stopList[from].dep.toLong())
                if (dep.toLocalDate() != date) return@mapNotNull null
                Trip(train, from, to, start, dep, midnight.plusMinutes(train.stopList[to].arr.toLong()))
            }
        }.sortedBy { it.dep }

data class Upcoming(
    val today: List<Trip>,
    /** First later day with trips (within a week), or null. */
    val nextDay: LocalDate?,
    val nextDayTrips: List<Trip>,
)

/** Today's trips plus the next day that has trips, for the rollover after the last train. */
fun upcoming(
    tt: Timetable,
    route: Route,
    today: LocalDate,
): Upcoming {
    val todayTrips = tripsOn(tt, route, today)
    val next =
        (1L..7L)
            .asSequence()
            .map { today.plusDays(it) }
            .map { it to tripsOn(tt, route, it) }
            .firstOrNull { it.second.isNotEmpty() }
    return Upcoming(todayTrips, next?.first, next?.second.orEmpty())
}

/** Next departures from [now], rolling into later days. */
fun nextDepartures(
    tt: Timetable,
    route: Route,
    now: LocalDateTime,
    count: Int,
): List<Trip> {
    val up = upcoming(tt, route, now.toLocalDate())
    return (up.today.filter { !it.dep.isBefore(now) } + up.nextDayTrips).take(count)
}
