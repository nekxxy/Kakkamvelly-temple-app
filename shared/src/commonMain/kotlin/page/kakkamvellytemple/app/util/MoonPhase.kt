package page.kakkamvellytemple.app.util

import page.kakkamvellytemple.app.data.model.MoonPhase

object MoonPhaseCalc {
    private val NEW_MOON_EPOCH_MS = 1704844800000L // Jan 10 2024 known new moon

    fun calculate(nowMs: Long = ISTClock.epochMs()): MoonPhase {
        val synodic = 29.53058770576
        val phase = ((((nowMs - NEW_MOON_EPOCH_MS).toDouble() / 86400000.0) % synodic) + synodic) % synodic
        val idx = ((phase / synodic) * 8).toInt().coerceIn(0, 7)
        val icons  = listOf("🌑","🌒","🌓","🌔","🌕","🌖","🌗","🌘")
        val namesMl = listOf("അമാവാസി","വളരുന്ന","ഒന്നാം ചതുർഥി","വളരുന്ന","പൗർണ്ണമി","കുറയുന്ന","മൂന്നാം ചതുർഥി","കുറയുന്ന")
        val namesEn = listOf("New Moon","Waxing Crescent","First Quarter","Waxing Gibbous","Full Moon","Waning Gibbous","Last Quarter","Waning Crescent")
        return MoonPhase(icons[idx], namesMl[idx], namesEn[idx])
    }
}
