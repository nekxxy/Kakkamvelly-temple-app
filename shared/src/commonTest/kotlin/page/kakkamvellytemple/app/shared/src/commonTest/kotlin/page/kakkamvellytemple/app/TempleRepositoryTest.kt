package page.kakkamvellytemple.app

import kotlin.test.*
import page.kakkamvellytemple.app.data.remote.ContentBundle
import page.kakkamvellytemple.app.data.repository.ContentStore
import page.kakkamvellytemple.app.data.repository.StaticData
import page.kakkamvellytemple.app.data.repository.TempleRepository
import page.kakkamvellytemple.app.util.ISTClock

class TempleRepositoryTest {

    private val repo = TempleRepository()

    @BeforeTest fun reset() { ContentStore.install(ContentBundle(version = Int.MIN_VALUE)) }

    @Test fun nextFestivalIsInFutureAndSorted() {
        val now = ISTClock.epochMs()
        val next = repo.getNextFestival()
        val all = StaticData.FESTIVALS
        assertTrue(all.zipWithNext().all { (a, b) -> a.dateUtcMs <= b.dateUtcMs }, "festivals sorted")
        if (all.any { it.dateUtcMs > now }) assertTrue(next.dateUtcMs > now)
    }

    @Test fun currentPoojaIndexWithinBounds() {
        val idx = repo.getCurrentPoojaIndex()
        val count = StaticData.MORNING_POOJAS.size + StaticData.EVENING_POOJAS.size
        assertTrue(idx in 0 until count)
    }

    @Test fun darshanStatusConsistentWithWindow() {
        val s = repo.getDarshanStatus()
        val mins = ISTClock.istTime().totalMinutes
        val inMorning = mins >= StaticData.MORNING_OPEN && mins < StaticData.MORNING_CLOSE
        val inEvening = mins >= StaticData.EVENING_OPEN && mins < StaticData.EVENING_CLOSE
        assertEquals(inMorning || inEvening, s.isOpen)
        if (s.isOpen) assertTrue(s.minutesUntilChange > 0)
    }

    @Test fun annadhanamIsFutureOrToday() {
        val a = repo.getAnnadhanam()
        assertTrue(a.daysAway >= 0)
        if (a.isToday) assertEquals(0, a.daysAway)
    }
}
