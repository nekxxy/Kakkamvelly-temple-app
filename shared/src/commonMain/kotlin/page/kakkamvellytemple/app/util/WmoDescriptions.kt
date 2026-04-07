package page.kakkamvellytemple.app.util

object WmoDescriptions {
    val MAP = mapOf(
        0 to ("☀️" to "Clear"), 1 to ("🌤" to "Mostly Clear"),
        2 to ("⛅" to "Partly Cloudy"), 3 to ("☁️" to "Overcast"),
        45 to ("🌫" to "Foggy"), 48 to ("🌫" to "Icy Fog"),
        51 to ("🌦" to "Light Drizzle"), 53 to ("🌦" to "Drizzle"),
        55 to ("🌦" to "Heavy Drizzle"), 61 to ("🌧" to "Light Rain"),
        63 to ("🌧" to "Moderate Rain"), 65 to ("🌧" to "Heavy Rain"),
        71 to ("❄️" to "Light Snow"), 73 to ("❄️" to "Snow"),
        75 to ("❄️" to "Heavy Snow"), 80 to ("🌦" to "Showers"),
        81 to ("🌧" to "Heavy Showers"), 82 to ("⛈" to "Violent Showers"),
        95 to ("⛈" to "Thunderstorm"), 96 to ("⛈" to "Thunderstorm + Hail"),
        99 to ("⛈" to "Severe Thunderstorm")
    )
    fun get(code: Int) = MAP[code] ?: ("🌤" to "Fair")
}
