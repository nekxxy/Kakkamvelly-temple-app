package page.kakkamvellytemple.app

import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlin.test.*
import page.kakkamvellytemple.app.data.remote.SupabaseContentSource

/** The bundle's verified flag comes only from the backend content_meta markers and
 *  is conservative: every required marker must be present and true. Any failure,
 *  absence or false value keeps the bundle unverified so the app notice stays up. */
class ContentMetaVerificationTest {

    @BeforeTest fun configure() {
        SupabaseContentSource.configure("https://test.supabase.co", "test-key")
    }

    private fun clientFor(meta: String?, metaStatus: HttpStatusCode = HttpStatusCode.OK) =
        HttpClient(MockEngine { request: HttpRequestData ->
            val p = request.url.encodedPath
            when {
                p.endsWith("/content_meta") ->
                    if (meta == null) respond("{}", metaStatus)
                    else respond(meta, metaStatus, headersOf(HttpHeaders.ContentType, "application/json"))
                else -> respond("[]", HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
            }
        }) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }

    private val allVerified = """[
        {"section":"darshan_settings","verified":true},
        {"section":"poojas","verified":true},
        {"section":"contacts","verified":true},
        {"section":"events","verified":true},
        {"section":"gallery","verified":true},
        {"section":"vazhipad","verified":true},
        {"section":"festivals","verified":true}
    ]"""

    @Test fun metaEndpointFailureKeepsUnverified() = runBlocking {
        val b = SupabaseContentSource.fetchBundle(clientFor(null, HttpStatusCode.InternalServerError))
        assertFalse(b.verified)
    }

    @Test fun absentMarkersKeepUnverified() = runBlocking {
        val b = SupabaseContentSource.fetchBundle(clientFor("[]"))
        assertFalse(b.verified)
    }

    @Test fun partialMarkersKeepUnverified() = runBlocking {
        val partial = """[
            {"section":"darshan_settings","verified":true},
            {"section":"poojas","verified":true},
            {"section":"contacts","verified":true},
            {"section":"events","verified":true},
            {"section":"gallery","verified":true}
        ]"""  // vazhipad + festivals markers absent
        val b = SupabaseContentSource.fetchBundle(clientFor(partial))
        assertFalse(b.verified)
    }

    @Test fun falseMarkerKeepsUnverified() = runBlocking {
        val oneFalse = allVerified.replace("\"section\":\"poojas\",\"verified\":true", "\"section\":\"poojas\",\"verified\":false")
        val b = SupabaseContentSource.fetchBundle(clientFor(oneFalse))
        assertFalse(b.verified)
    }

    @Test fun allMarkersVerifiedMarksBundle() = runBlocking {
        val b = SupabaseContentSource.fetchBundle(clientFor(allVerified))
        assertTrue(b.verified)
    }
}
