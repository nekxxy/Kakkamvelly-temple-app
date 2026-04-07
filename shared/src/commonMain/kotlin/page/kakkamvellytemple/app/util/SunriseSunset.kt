package page.kakkamvellytemple.app.util

import kotlinx.datetime.LocalDate
import kotlin.math.*

/** NOAA solar calculator — accurate to ±2 min for Kozhikode */
object SunriseSunset {
    fun calculate(date: LocalDate, latDeg: Double = 11.6814, lonDeg: Double = 75.6478): Pair<String, String> {
        val R = PI / 180.0
        val y = date.year; val mo = date.monthNumber; val d = date.dayOfMonth
        val A = (14 - mo) / 12
        val Y = y + 4800 - A; val M = mo + 12 * A - 3
        val JDN = d + (153 * M + 2) / 5 + 365 * Y + Y / 4 - Y / 100 + Y / 400 - 32045
        val JD = JDN - 0.5
        val T = (JD - 2451545.0) / 36525.0
        val L0 = (280.46646 + T * (36000.76983 + T * 0.0003032)) % 360.0
        val Mm = (357.52911 + T * (35999.05029 - 0.0001537 * T)) % 360.0
        val Mr = Mm * R
        val C = sin(Mr) * (1.914602 - T * (0.004817 + 0.000014 * T)) +
                sin(2 * Mr) * (0.019993 - 0.000101 * T) + sin(3 * Mr) * 0.000289
        val sunLon = L0 + C
        val omega = 125.04 - 1934.136 * T
        val lam = sunLon - 0.00569 - 0.00478 * sin(omega * R)
        val eps0 = 23.0 + (26.0 + ((21.448 - T * (46.815 + T * (0.00059 - T * 0.001813))) / 60.0)) / 60.0
        val oblCorr = eps0 + 0.00256 * cos(omega * R)
        val sinDec = sin(oblCorr * R) * sin(lam * R)
        val dec = asin(sinDec) * (180.0 / PI)
        val y2 = tan(oblCorr * R / 2.0).pow(2)
        val eqT = 4.0 * (180.0 / PI) * (
            y2 * sin(2 * L0 * R) - 2 * 0.016708634 * sin(Mm * R) +
            4 * 0.016708634 * y2 * sin(Mm * R) * cos(2 * L0 * R) -
            0.5 * y2.pow(2) * sin(4 * L0 * R) -
            1.25 * 0.016708634.pow(2) * sin(2 * Mm * R)
        )
        val cosHA = (cos(90.833 * R) - sin(latDeg * R) * sin(dec * R)) /
                    (cos(latDeg * R) * cos(dec * R))
        if (abs(cosHA) > 1.0) return "N/A" to "N/A"
        val HA = acos(cosHA) * (180.0 / PI)
        val solarNoon = 720.0 - 4.0 * lonDeg - eqT
        return minsToIST(solarNoon - HA * 4) to minsToIST(solarNoon + HA * 4)
    }

    private fun minsToIST(utcMins: Double): String {
        val ist = ((utcMins + 330.0) % 1440.0 + 1440.0) % 1440.0
        val h = ist.toInt() / 60; val m = ist.toInt() % 60
        val ampm = if (h < 12) "AM" else "PM"
        val h12 = if (h % 12 == 0) 12 else h % 12
        return "$h12:${m.toString().padStart(2, '0')} $ampm"
    }
}
