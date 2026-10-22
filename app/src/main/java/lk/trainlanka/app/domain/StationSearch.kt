package lk.trainlanka.app.domain

import lk.trainlanka.app.data.Station

enum class MatchKind { NAME, ALIAS, FUZZY }

data class StationMatch(
    val station: Station,
    val kind: MatchKind,
    val alias: String?,
)

const val OTHER_PROVINCE = "Other"

/** Stations A–Z, grouped by the first letter of the name in upper case. */
fun groupByLetter(stations: List<Station>): Map<Char, List<Station>> =
    stations.sortedBy { it.name.lowercase() }.groupBy { it.name.first().uppercaseChar() }

/** Ranked: exact name → alias → name prefix → word prefix → substring → fuzzy. */
fun searchStations(
    stations: List<Station>,
    query: String,
): List<StationMatch> {
    val q = normalize(query)
    if (q.isEmpty()) return emptyList()
    return stations
        .mapNotNull { s -> score(s, q) }
        .sortedWith(compareBy({ it.first }, { it.second.station.name }))
        .map { it.second }
}

private fun score(
    s: Station,
    q: String,
): Pair<Int, StationMatch>? {
    val name = normalize(s.name)
    val words =
        s.name
            .lowercase()
            .split(' ', '-', '(', ')')
            .map(::normalize)
            .filter(String::isNotEmpty)
    val exactAlias = s.aliases.firstOrNull { normalize(it) == q }
    val alias = s.aliases.firstOrNull { normalize(it).startsWith(q) }
    return when {
        name == q -> 0 to StationMatch(s, MatchKind.NAME, null)
        exactAlias != null -> 1 to StationMatch(s, MatchKind.ALIAS, exactAlias)
        name.startsWith(q) -> 2 to StationMatch(s, MatchKind.NAME, null)
        alias != null -> 3 to StationMatch(s, MatchKind.ALIAS, alias)
        words.any { it.startsWith(q) } -> 4 to StationMatch(s, MatchKind.NAME, null)
        name.contains(q) -> 5 to StationMatch(s, MatchKind.NAME, null)
        isFuzzy(q, name, words) -> 6 to StationMatch(s, MatchKind.FUZZY, null)
        else -> null
    }
}

private fun isFuzzy(
    q: String,
    name: String,
    words: List<String>,
): Boolean {
    if (q.length < 3) return false
    val limit = if (q.length <= 5) 1 else 2
    return (words + name).any { w ->
        distance(q, w) <= limit || (w.length > q.length && distance(q, w.take(q.length)) <= limit)
    }
}

private fun normalize(s: String) = s.lowercase().filter(Char::isLetterOrDigit)

/** Optimal string alignment distance (Levenshtein + adjacent transposition). */
private fun distance(
    a: String,
    b: String,
): Int {
    val d = Array(a.length + 1) { IntArray(b.length + 1) }
    for (i in 0..a.length) d[i][0] = i
    for (j in 0..b.length) d[0][j] = j
    for (i in 1..a.length) {
        for (j in 1..b.length) {
            val cost = if (a[i - 1] == b[j - 1]) 0 else 1
            d[i][j] = minOf(d[i - 1][j] + 1, d[i][j - 1] + 1, d[i - 1][j - 1] + cost)
            if (i > 1 && j > 1 && a[i - 1] == b[j - 2] && a[i - 2] == b[j - 1]) d[i][j] = minOf(d[i][j], d[i - 2][j - 2] + 1)
        }
    }
    return d[a.length][b.length]
}
