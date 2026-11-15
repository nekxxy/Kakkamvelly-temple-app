package page.kakkamvellytemple.app

import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.HttpResponseData
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlin.test.*
import page.kakkamvellytemple.app.data.remote.ContentSync
import page.kakkamvellytemple.app.data.remote.SupabaseContentSource

/** Mapping and failure-mode tests for the Supabase PostgREST content source.
 *  All backend states are mocked; no live network. The connected path stays
 *  gated by ContentSync.CONNECTED_SETUP_APPROVED (false) - verified here too. */
class SupabaseContentSourceTest {

    @BeforeTest fun configure() {
        SupabaseContentSource.configure("https://test.supabase.co", "test-key")
    }

    private fun clientFor(responder: MockRequestHandleScope.(io.ktor.client.request.HttpRequestData) -> HttpResponseData) =
        HttpClient(MockEngine(responder)) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }

    private fun MockRequestHandleScope.ok(body: String) =
        respond(body, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))

    private fun MockRequestHandleScope.byPath(
        darshan: String = "[]",
        poojas: String = "[]",
        contacts: String = "[]",
        events: String = "[]",
        gallery: String = "[]",
        vazhipad: String = "[]",
        festivals: String = "[]",
        request: io.ktor.client.request.HttpRequestData,
    ): HttpResponseData {
        val p = request.url.encodedPath
        return when {
            p.endsWith("/darshan_settings") -> ok(darshan)
            p.endsWith("/poojas") -> ok(poojas)
            p.endsWith("/contacts") -> ok(contacts)
            p.endsWith("/events") -> ok(events)
            p.endsWith("/gallery") -> ok(gallery)
            p.endsWith("/vazhipad") -> ok(vazhipad)
            p.endsWith("/festivals") -> ok(festivals)
            else -> respond("{}", HttpStatusCode.NotFound)
        }
    }

    @Test fun emptyBackendKeepsEverySectionNull() = runBlocking {
        val b = SupabaseContentSource.fetchBundle(clientFor { byPath(request = it) })
        assertNull(b.morningPoojas); assertNull(b.eveningPoojas)
        assertNull(b.contacts); assertNull(b.todayEvents)
        assertNull(b.gallery); assertNull(b.darshan)
        assertNull(b.vazhipad); assertNull(b.festivals)
        assertTrue(b.version > 0)  // fetch-time fallback so a connected fetch always wins
    }

    @Test fun fullBackendMapsAllSections() = runBlocking {
        val b = SupabaseContentSource.fetchBundle(clientFor {
            byPath(
                darshan = """[{"morning_open_min":330,"morning_close_min":540,"evening_open_min":1065,"evening_close_min":1125,"updated_at":"2026-11-15T04:30:00+00:00"}]""",
                poojas = """[
                    {"session":"morning","name_ml":"à´à´· à´ªàµà´","name_en":"Usha Pooja","time_label":"6:00 AM","hour":6,"minute":0},
                    {"session":"evening","name_ml":"à´¦àµà´ªà´¾à´°à´¾à´§à´¨","name_en":"Deepa Aradhana","time_label":"5:45 PM","hour":17,"minute":45},
                    {"session":"morning","name_ml":"à´¨à´¿àµ¼à´®àµà´®à´¾à´²àµà´¯à´","name_en":"Nirmalyam","time_label":"5:30 AM","hour":5,"minute":30}
                ]""",
                contacts = """[{"role_ml":"à´¸àµà´àµà´°à´àµà´à´±à´¿","role_en":"Secretary","phone":"+919446844145","whatsapp":"+919446844145"}]""",
                events = """[{"icon":null,"title_ml":"à´¨à´¿à´¤àµà´¯ à´ªàµà´","title_en":"Nitya Pooja","time_ml":"5:30 AM","time_en":"5:30 AM"}]""",
                gallery = """[{"photo_path":"temple-entrance-deepam","caption_ml":"à´ªàµà´°à´µàµà´¶à´¨à´","caption_en":"Entrance"}]""",
                vazhipad = """[
                    {"name_ml":"à´¨à´¿à´¤àµà´¯à´ªàµà´","name_en":"Nitya Pooja","amount_inr":150.00,"description_ml":"à´¦àµà´¨à´à´¦à´¿à´¨ à´ªàµà´"},
                    {"name_ml":"à´ªàµà´·àµà´ªà´¾à´àµà´à´²à´¿","name_en":"Pushpanjali","amount_inr":50.0,"description_ml":null}
                ]""",
                festivals = """[
                    {"name_ml":"à´µà´¿à´·àµ 2027","name_en":"Vishu Â· Kerala New Year","icon":null,"date_utc_ms":1776126600000,"note_ml":null,"is_special":true},
                    {"name_ml":"à´¶à´¿à´µà´°à´¾à´¤àµà´°à´¿ 2027","name_en":"Maha Shivaratri","icon":"\uD83C\uDF19","date_utc_ms":1771980600000,"note_ml":"à´àµ¼à´àµà´ à´à´°à´¾à´§à´¨","is_special":false}
                ]""",
                request = it,
            )
        })
        val d = b.darshan!!
        assertEquals(330, d.morningOpenMin); assertEquals(1125, d.eveningCloseMin)
        assertEquals(listOf("Usha Pooja", "Nirmalyam"), b.morningPoojas!!.map { p -> p.nameEn })
        assertEquals(listOf("Deepa Aradhana"), b.eveningPoojas!!.map { p -> p.nameEn })
        assertEquals(6, b.morningPoojas!![0].hour)
        assertEquals("+919446844145", b.contacts!![0].phone)
        assertEquals("ð", b.todayEvents!![0].icon)  // null icon -> default
        assertEquals("temple-entrance-deepam", b.gallery!![0].resName)
        // vazhipad: numeric(10,2) amounts become whole-rupee Ints; null description -> ""
        assertEquals(listOf("Nitya Pooja", "Pushpanjali"), b.vazhipad!!.map { v -> v.nameEn })
        assertEquals(150, b.vazhipad!![0].amount)
        assertEquals("", b.vazhipad!![1].description)
        // festivals: null icon -> default ð¸; flags and notes mapped
        assertEquals(listOf("Vishu Â· Kerala New Year", "Maha Shivaratri"), b.festivals!!.map { f -> f.nameEn })
        assertEquals("\uD83C\uDF38", b.festivals!![0].icon)
        assertTrue(b.festivals!![0].isSpecial)
        assertEquals("à´àµ¼à´àµà´ à´à´°à´¾à´§à´¨", b.festivals!![1].noteMl)
        assertEquals(1771980600000L, b.festivals!![1].dateUtcMs)
        // version derived from darshan_settings.updated_at
        assertEquals(kotlinx.datetime.Instant.parse("2026-11-15T04:30:00+00:00").epochSeconds.toInt(), b.version)
        assertEquals("2026-11-15T04:30:00+00:00", b.updatedAt)
    }

    @Test fun vazhipadAndFestivalsOnlyMapAndLeaveOthersNull(): Unit = runBlocking {
        val b = SupabaseContentSource.fetchBundle(clientFor {
            byPath(
                vazhipad = """[{"name_ml":"à´µà´´à´¿à´ªà´¾à´àµ","name_en":"Offering","amount_inr":200.0,"description_ml":"à´ªàµà´°à´¤àµà´¯àµà´à´"}]""",
                festivals = """[{"name_ml":"à´¤à´¿à´°àµà´µàµà´£à´","name_en":"Thiruvonam","icon":"\uD83C\uDF3A","date_utc_ms":1788229800000,"note_ml":"à´à´£à´","is_special":false}]""",
                request = it,
            )
        })
        assertEquals(200, b.vazhipad!![0].amount)
        assertEquals("à´ªàµà´°à´¤àµà´¯àµà´à´", b.vazhipad!![0].description)
        assertEquals("\uD83C\uDF3A", b.festivals!![0].icon)
        assertFalse(b.festivals!![0].isSpecial)
        assertNull(b.morningPoojas); assertNull(b.contacts)
        assertNull(b.todayEvents); assertNull(b.gallery); assertNull(b.darshan)
    }

    @Test fun partialBackendLeavesOtherSectionsNull() = runBlocking {
        val b = SupabaseContentSource.fetchBundle(clientFor {
            byPath(
                poojas = """[{"session":"morning","name_ml":"à´à´·","name_en":"Usha","time_label":"6 AM","hour":6,"minute":0}]""",
                request = it,
            )
        })
        assertEquals(1, b.morningPoojas!!.size)
        assertNull(b.eveningPoojas); assertNull(b.contacts)
        assertNull(b.todayEvents); assertNull(b.gallery); assertNull(b.darshan)
    }

    @Test fun rowsWithMissingRequiredFieldsAreSkipped() = runBlocking {
        val b = SupabaseContentSource.fetchBundle(clientFor {
            byPath(
                poojas = """[
                    {"session":"morning","name_ml":"x","name_en":"NoTime","time_label":null,"hour":null,"minute":null},
                    {"session":"morning","name_ml":"y","name_en":"HasTime","time_label":"7 AM","hour":7,"minute":15}
                ]""",
                contacts = """[
                    {"role_ml":"a","role_en":"NoPhone","phone":null,"whatsapp":null},
                    {"role_ml":"b","role_en":"HasPhone","phone":"+911","whatsapp":null}
                ]""",
                gallery = """[
                    {"photo_path":null,"caption_ml":"x","caption_en":"NoPhoto"},
                    {"photo_path":"lamp","caption_ml":"y","caption_en":"Lamp"}
                ]""",
                request = it,
            )
        })
        assertEquals(listOf("HasTime"), b.morningPoojas!!.map { p -> p.nameEn })
        assertEquals(listOf("HasPhone"), b.contacts!!.map { c -> c.roleEn })
        assertEquals(listOf("Lamp"), b.gallery!!.map { g -> g.captionEn })
    }

    @Test fun malformedJsonThrows() {
        assertFailsWith<Throwable> {
            runBlocking {
                SupabaseContentSource.fetchBundle(clientFor {
                    byPath(darshan = """{"not":"an array"}""", request = it)
                })
            }
        }
    }

    @Test fun httpErrorThrows() {
        assertFailsWith<Throwable> {
            runBlocking {
                SupabaseContentSource.fetchBundle(clientFor {
                    if (it.url.encodedPath.endsWith("/darshan_settings"))
                        respond("""{"message":"db down"}""", HttpStatusCode.InternalServerError)
                    else ok("[]")
                })
            }
        }
    }

    @Test fun unconfiguredSourceThrows() {
        SupabaseContentSource.configure("", "")
        assertNull(SupabaseContentSource.settings)
        assertFailsWith<Throwable> {
            runBlocking { SupabaseContentSource.fetchBundle(clientFor { ok("[]") }) }
        }
    }

    @Test fun notificationsStayGatedOff() {
        // Real notification sends remain held: the app must not subscribe to FCM topics.
        assertFalse(ContentSync.NOTIFICATION_SETUP_APPROVED)
    }

    @Test fun connectedSetupIsApprovedForContentFetch() {
        // Connected content read was approved by the user on 2026-11-15.
        assertTrue(ContentSync.CONNECTED_SETUP_APPROVED)
    }

    @Test fun emptyBackendBundleDoesNotCountAsVerifiedRemote() = runBlocking {
        // Provenance rule: an empty backend must not clear the
        // user-visible unverified-content notice.
        val b = SupabaseContentSource.fetchBundle(clientFor { byPath(request = it) })
        assertFalse(b.hasAnySection())
        page.kakkamvellytemple.app.data.repository.ContentStore.install(
            b, source = page.kakkamvellytemple.app.data.repository.ContentStore.Source.REMOTE_BACKEND)
        assertFalse(page.kakkamvellytemple.app.data.repository.ContentStore.isShowingVerifiedRemote)
    }

    @Test fun partialRemoteKeepsNoticeUp() = runBlocking {
        // A partial backend (poojas only) leaves festivals etc. on packaged
        // defaults - the unverified-content notice must stay.
        val b = SupabaseContentSource.fetchBundle(clientFor {
            byPath(
                poojas = """[{"session":"morning","name_ml":"à´à´·","name_en":"Usha","time_label":"6 AM","hour":6,"minute":0}]""",
                request = it,
            )
        })
        assertTrue(b.hasAnySection())
        page.kakkamvellytemple.app.data.repository.ContentStore.install(
            b, source = page.kakkamvellytemple.app.data.repository.ContentStore.Source.REMOTE_BACKEND)
        assertFalse(page.kakkamvellytemple.app.data.repository.ContentStore.isShowingVerifiedRemote)
    }

    private fun fullBundle(verified: Boolean) = page.kakkamvellytemple.app.data.remote.ContentBundle(
        version = 1,
        morningPoojas = listOf(page.kakkamvellytemple.app.data.remote.PoojaDto("m","M","6 AM",6,0)),
        eveningPoojas = listOf(page.kakkamvellytemple.app.data.remote.PoojaDto("e","E","6 PM",18,0)),
        contacts = listOf(page.kakkamvellytemple.app.data.remote.ContactDto("r","R","+911",null)),
        vazhipad = listOf(page.kakkamvellytemple.app.data.remote.VazhipadDto("v","V",10,"")),
        todayEvents = listOf(page.kakkamvellytemple.app.data.remote.TodayEventDto("i","t","T","a","b")),
        festivals = listOf(page.kakkamvellytemple.app.data.remote.FestivalDto("f","F","i",0L,"",false)),
        gallery = listOf(page.kakkamvellytemple.app.data.remote.GalleryDto("g","c","C")),
        darshan = page.kakkamvellytemple.app.data.remote.DarshanDto(330,540,1065,1125),
        verified = verified,
    )

    @Test fun fullRemoteWithoutVerifiedMarkerKeepsNoticeUp() {
        // Remote origin is not committee approval: rows present, marker absent.
        page.kakkamvellytemple.app.data.repository.ContentStore.install(
            fullBundle(verified = false),
            source = page.kakkamvellytemple.app.data.repository.ContentStore.Source.REMOTE_BACKEND)
        assertFalse(page.kakkamvellytemple.app.data.repository.ContentStore.isShowingVerifiedRemote)
    }

    @Test fun fullVerifiedRemoteClearsNotice() {
        // Every section remote-supplied AND backend marks committee-verified.
        page.kakkamvellytemple.app.data.repository.ContentStore.install(
            fullBundle(verified = true),
            source = page.kakkamvellytemple.app.data.repository.ContentStore.Source.REMOTE_BACKEND)
        assertTrue(page.kakkamvellytemple.app.data.repository.ContentStore.isShowingVerifiedRemote)
    }

    @Test fun localDemoInstallClearsRemoteProvenance() {
        page.kakkamvellytemple.app.data.repository.ContentStore.install(
            fullBundle(verified = true),
            source = page.kakkamvellytemple.app.data.repository.ContentStore.Source.LOCAL_DEMO)
        assertFalse(page.kakkamvellytemple.app.data.repository.ContentStore.isShowingVerifiedRemote)
    }
}
