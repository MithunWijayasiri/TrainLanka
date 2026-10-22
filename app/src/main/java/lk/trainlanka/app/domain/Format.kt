package lk.trainlanka.app.domain

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val TIME_24 = DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH)
private val TIME_12 = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH)
private val DAY = DateTimeFormatter.ofPattern("EEE d MMM", Locale.ENGLISH)
private val DATE = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)
private val DAY_MONTH = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)

fun formatTime(
    t: LocalDateTime,
    clock24: Boolean,
): String = t.format(if (clock24) TIME_24 else TIME_12)

/** Stop time in minutes since the train's start-day midnight. */
fun formatMinutes(
    minutes: Int,
    clock24: Boolean,
): String = formatTime(LocalDate.of(2000, 1, 1).atStartOfDay().plusMinutes(minutes.toLong()), clock24)

/** "2h 40m", "45m". */
fun formatDuration(minutes: Long): String =
    when {
        minutes < 60 -> "${minutes}m"
        else -> "${minutes / 60}h %02dm".format(minutes % 60)
    }

/** "in 15 min", "in 1h 50m", "now". */
fun formatCountdown(
    now: LocalDateTime,
    dep: LocalDateTime,
): String {
    val m = Duration.between(now, dep).toMinutes()
    return when {
        m <= 0 -> "now"
        m < 60 -> "in $m min"
        else -> "in ${m / 60}h ${m % 60}m"
    }
}

/** "Thu 22 Oct". */
fun formatDay(date: LocalDate): String = date.format(DAY)

/** Timetable version "2026-10-21" → "21 Oct 2026". */
fun formatVersionDate(version: String): String = LocalDate.parse(version).format(DATE)

/** "today 06:12", "yesterday 18:30", "14 Oct 06:12". */
fun formatCheckTime(
    epochMillis: Long,
    clock24: Boolean,
): String {
    val at = LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), COLOMBO)
    val today = nowInColombo().toLocalDate()
    val day =
        when (at.toLocalDate()) {
            today -> "today"
            today.minusDays(1) -> "yesterday"
            else -> at.format(DAY_MONTH)
        }
    return "$day ${formatTime(at, clock24)}"
}
