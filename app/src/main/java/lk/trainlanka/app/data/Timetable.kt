package lk.trainlanka.app.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.DayOfWeek
import java.time.LocalDate

@Serializable
data class Station(
    val id: Int,
    val name: String,
    val province: String?,
    val lat: Double?,
    val lng: Double?,
    val aliases: List<String>,
    val phone: String?,
) {
    /** "Colombo Fort (Pettah)"; plain name when no aliases. */
    val label: String get() = if (aliases.isEmpty()) name else "$name (${aliases.joinToString()})"
}

/** One stop: station, arrival and departure in minutes since midnight of the train's start day (may exceed 1440). */
data class Stop(
    val stationId: Int,
    val arr: Int,
    val dep: Int,
)

@Serializable
data class Train(
    val id: Int,
    val no: String,
    val type: String,
    val days: List<String>,
    private val stops: List<List<Int>>,
) {
    val stopList: List<Stop> by lazy { stops.map { Stop(it[0], it[1], it[2]) } }

    /** Weekdays the train starts on; Poya/holiday tokens are ignored. */
    val weekdays: Set<DayOfWeek> by lazy { days.mapNotNull(DAY_TOKENS::get).toSet() }

    private companion object {
        val DAY_TOKENS =
            mapOf(
                "Mon" to DayOfWeek.MONDAY,
                "Tue" to DayOfWeek.TUESDAY,
                "Wed" to DayOfWeek.WEDNESDAY,
                "Thu" to DayOfWeek.THURSDAY,
                "Fri" to DayOfWeek.FRIDAY,
                "Sat" to DayOfWeek.SATURDAY,
                "Sun" to DayOfWeek.SUNDAY,
            )
    }
}

@Serializable
data class Timetable(
    val version: String,
    val stations: List<Station>,
    val trains: List<Train>,
) {
    val stationsById: Map<Int, Station> by lazy { stations.associateBy { it.id } }
    val trainsById: Map<Int, Train> by lazy { trains.associateBy { it.id } }

    fun station(id: Int): Station = stationsById.getValue(id)

    /** "From → To" station names. */
    fun routeLabel(route: Route) = "${station(route.fromId).name} → ${station(route.toId).name}"

    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        fun parse(text: String): Timetable {
            val tt = json.decodeFromString<Timetable>(text)
            require(tt.trains.isNotEmpty() && tt.stations.isNotEmpty()) { "Empty timetable" }
            LocalDate.parse(tt.version)
            tt.trains.forEach { train ->
                require(train.stopList.all { it.stationId in tt.stationsById }) { "Unknown station in train ${train.id}" }
            }
            return tt
        }
    }
}
