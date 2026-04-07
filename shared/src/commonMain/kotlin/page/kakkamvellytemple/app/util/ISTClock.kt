package page.kakkamvellytemple.app.util

import kotlinx.datetime.*
import page.kakkamvellytemple.app.data.model.ISTTime

object ISTClock {
    private val IST = TimeZone.of("Asia/Kolkata")

    fun now(): LocalDateTime = Clock.System.now().toLocalDateTime(IST)

    fun istTime(): ISTTime {
        val ldt = now()
        return ISTTime(ldt.hour, ldt.minute, ldt.second)
    }

    fun todayDate(): LocalDate = now().date

    fun epochMs(): Long = Clock.System.now().toEpochMilliseconds()

    fun formatCountdown(diffMs: Long): CountdownParts {
        val total = diffMs.coerceAtLeast(0)
        val d = total / 86_400_000
        val h = (total % 86_400_000) / 3_600_000
        val m = (total % 3_600_000) / 60_000
        val s = (total % 60_000) / 1_000
        return CountdownParts(d.toInt(), h.toInt(), m.toInt(), s.toInt())
    }
}

data class CountdownParts(val days: Int, val hours: Int, val minutes: Int, val seconds: Int) {
    fun pad(n: Int) = n.toString().padStart(2, '0')
    val dStr get() = pad(days)
    val hStr get() = pad(hours)
    val mStr get() = pad(minutes)
    val sStr get() = pad(seconds)
    val totalMs get() = days * 86_400_000L + hours * 3_600_000L + minutes * 60_000L + seconds * 1_000L
}
