package page.kakkamvellytemple.app.data.repository

import page.kakkamvellytemple.app.data.model.*
import page.kakkamvellytemple.app.util.ISTClock
import page.kakkamvellytemple.app.util.MoonPhaseCalc
import page.kakkamvellytemple.app.util.SunriseSunset
import kotlinx.datetime.*

class TempleRepository {

    fun getNextFestival(): Festival {
        val now = ISTClock.epochMs()
        return StaticData.FESTIVALS.firstOrNull { it.dateUtcMs > now }
            ?: StaticData.FESTIVALS.last()
    }

    fun getUpcomingFestivals(): List<Festival> {
        val now = ISTClock.epochMs()
        val next = getNextFestival()
        return StaticData.FESTIVALS.filter { it.dateUtcMs > now && it != next }
    }

    fun getDarshanStatus(): DarshanStatus {
        val ist = ISTClock.istTime()
        val cur = ist.totalMinutes
        val MO = StaticData.MORNING_OPEN; val MC = StaticData.MORNING_CLOSE
        val EO = StaticData.EVENING_OPEN; val EC = StaticData.EVENING_CLOSE

        return when {
            cur >= MO && cur < MC -> {
                val left = MC - cur
                DarshanStatus(true, "ക്ഷേത്രം തുറന്നിരിക്കുന്നു", "Open", left)
            }
            cur >= EO && cur < EC -> {
                val left = EC - cur
                DarshanStatus(true, "ക്ഷേത്രം തുറന്നിരിക്കുന്നു", "Open", left)
            }
            cur < MO -> DarshanStatus(false, "ക്ഷേത്രം അടഞ്ഞിരിക്കുന്നു", "5:30 AM", MO - cur)
            cur < EO -> DarshanStatus(false, "ക്ഷേത്രം അടഞ്ഞിരിക്കുന്നു", "5:45 PM", EO - cur)
            else     -> DarshanStatus(false, "ക്ഷേത്രം അടഞ്ഴിരിക്കുന്നു", "Tomorrow 5:30 AM", (24*60 - cur) + MO)
        }
    }

    fun getCurrentPoojaIndex(): Int {
        val mins = ISTClock.istTime().totalMinutes
        val all = StaticData.MORNING_POOJAS + StaticData.EVENING_POOJAS
        return all.indexOfLast { it.hour * 60 + it.minute <= mins }.coerceAtLeast(0)
    }

    fun getSunTimes(): SunTimes {
        val today = ISTClock.todayDate()
        val (sr, ss) = SunriseSunset.calculate(today)
        return SunTimes(sr, ss)
    }

    fun getMoonPhase(): MoonPhase = MoonPhaseCalc.calculate()

    fun getAnnadhanam(): AnnadhanamInfo {
        val IST = TimeZone.of("Asia/Kolkata")
        val today = ISTClock.todayDate()

        fun firstSunday(year: Int, month: Month): LocalDate {
            var d = LocalDate(year, month, 1)
            while (d.dayOfWeek != DayOfWeek.SUNDAY) d = d.plus(1, DateTimeUnit.DAY)
            return d
        }

        var next = firstSunday(today.year, today.month)
        if (next < today) next = firstSunday(
            if (today.monthNumber == 12) today.year + 1 else today.year,
            if (today.monthNumber == 12) Month.JANUARY else Month(today.monthNumber + 1)
        )

        val diff = today.until(next, DateTimeUnit.DAY)
        val months = listOf("ജനുവരി","ഫെബ്രുവരി","മാർച്ച്","ഏപ്രിൽ","മേയ്","ജൂൺ",
                            "ജൂലൈ","ഓഗസ്റ്റ്","സെപ്റ്റംബർ","ഒക്ടോബർ","നവംബർ","ഡിസംബർ")
        val dateStr = "${next.dayOfMonth} ${months[next.monthNumber-1]} ${next.year}"

        return AnnadhanamInfo(dateStr, diff, diff == 0, diff == 1)
    }
}
