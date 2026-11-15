package page.kakkamvellytemple.app

import kotlin.test.*
import kotlinx.serialization.json.Json
import page.kakkamvellytemple.app.data.remote.ContentBundle
import page.kakkamvellytemple.app.data.repository.ContentStore
import page.kakkamvellytemple.app.data.repository.StaticData

class ContentStoreTest {

    @BeforeTest fun reset() { ContentStore.install(ContentBundle(version = Int.MIN_VALUE)) }

    @Test fun parsesBundleWithUnknownKeys() {
        val raw = """{"version":3,"updatedAt":"2026-11-13","futureField":{"x":1},
            "darshan":{"morningOpenMin":300,"morningCloseMin":540,"eveningOpenMin":1050,"eveningCloseMin":1130}}"""
        val b = Json { ignoreUnknownKeys = true }.decodeFromString<ContentBundle>(raw)
        assertEquals(3, b.version)
        assertEquals(300, b.darshan?.morningOpenMin)
        assertNull(b.vazhipad)
    }

    @Test fun fallsBackToDefaultsWhenNoBundle() {
        ContentStore.install(ContentBundle(version = 0))  // empty bundle: every section null
        assertTrue(StaticData.MORNING_POOJAS.isNotEmpty())
        assertTrue(StaticData.VAZHIPAD_ITEMS.size > 30)
        assertEquals(5 * 60 + 30, StaticData.MORNING_OPEN)
        assertEquals("+919446844145", StaticData.CONTACTS[1].phone)
    }

    @Test fun remoteSectionOverridesOnlyThatSection() {
        val b = ContentBundle(
            version = 1,
            darshan = page.kakkamvellytemple.app.data.remote.DarshanDto(300, 500, 1000, 1100)
        )
        ContentStore.install(b)
        assertEquals(300, StaticData.MORNING_OPEN)
        assertEquals(1100, StaticData.EVENING_CLOSE)
        // untouched sections keep defaults
        assertTrue(StaticData.MORNING_POOJAS.size >= 4)
        assertTrue(StaticData.VAZHIPAD_ITEMS.size > 30)
    }

    @Test fun emptyRemoteListFallsBackToDefault() {
        val b = ContentBundle(version = 2, todayEvents = emptyList())
        ContentStore.install(b)
        assertTrue(StaticData.TODAY_EVENTS.isNotEmpty())
    }

    @Test fun festivalDtoMappingSortsByDate() {
        val b = ContentBundle(version = 3, festivals = listOf(
            page.kakkamvellytemple.app.data.remote.FestivalDto("ബ", "B", "🌸", 2000L),
            page.kakkamvellytemple.app.data.remote.FestivalDto("അ", "A", "🌺", 1000L)
        ))
        ContentStore.install(b)
        val f = StaticData.FESTIVALS
        assertEquals(2, f.size)
        assertEquals("അ", f[0].nameMl)
        assertEquals(1000L, f[0].dateUtcMs)
    }
}
