package page.kakkamvellytemple.app

import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData  // MockEngine respond type
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlin.test.*
import page.kakkamvellytemple.app.data.remote.ContentBundle
import page.kakkamvellytemple.app.data.remote.ContactDto
import page.kakkamvellytemple.app.data.remote.DarshanDto
import page.kakkamvellytemple.app.data.remote.SupabaseAdminRepository
import page.kakkamvellytemple.app.data.remote.SupabaseContentSource
import page.kakkamvellytemple.app.data.repository.AdminDeniedException
import page.kakkamvellytemple.app.data.repository.NotApprovedException

/** Denial-path tests for the admin write repository. Every backend response is
 *  mocked; these verify the app's handling contract (denied is never success),
 *  not the live database. Live anon denial is verified separately against the
 *  project; live authenticated non-admin denial stays an open E2E item until a
 *  real enrollment exists. */
class SupabaseAdminRepositoryTest {

    private val settings = SupabaseContentSource.Settings("https://test.supabase.co", "test-key")

    private fun clientFor(responder: MockRequestHandleScope.(HttpRequestData) -> HttpResponseData) =
        HttpClient(MockEngine(responder)) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }

    private fun MockRequestHandleScope.okJson(body: String) =
        respond(body, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))

    private val sampleBundle = ContentBundle(
        darshan = DarshanDto(330, 540, 1065, 1125),
        contacts = listOf(ContactDto("സെക്രട്ടറി", "Secretary", "+919446844145")),
    )

    @Test fun publishWithoutSessionIsDeniedAndMakesNoHttpCalls() = runBlocking {
        var calls = 0
        val repo = SupabaseAdminRepository(settings, { null }, clientFor { calls++; okJson("[]") })
        val result = repo.publishContent(sampleBundle)
        assertTrue(result.isFailure)
        assertIs<AdminDeniedException>(result.exceptionOrNull())
        assertEquals(0, calls)
    }

    @Test fun nonAdminMembershipIsDeniedBeforeAnyWrite() = runBlocking {
        val posts = mutableListOf<String>()
        val repo = SupabaseAdminRepository(settings, { "fake-token" }, clientFor { req ->
            when {
                req.url.encodedPath.endsWith("/admin_users") -> okJson("[]")  // not a member
                else -> { posts += req.url.encodedPath; okJson("[]") }
            }
        })
        val result = repo.publishContent(sampleBundle)
        assertTrue(result.isFailure)
        assertIs<AdminDeniedException>(result.exceptionOrNull())
        assertTrue(posts.isEmpty(), "no content write may happen for a non-admin")
    }

    @Test fun forbiddenWriteMapsToDenied(): Unit = runBlocking {
        val repo = SupabaseAdminRepository(settings, { "fake-token" }, clientFor { req ->
            when {
                req.url.encodedPath.endsWith("/admin_users") -> okJson("""[{"user_id":"u1"}]""")
                else -> respond("""{"code":"42501"}""", HttpStatusCode.Unauthorized,
                    headersOf(HttpHeaders.ContentType, "application/json"))
            }
        })
        val result = repo.publishContent(sampleBundle)
        assertTrue(result.isFailure)
        assertIs<AdminDeniedException>(result.exceptionOrNull())
    }

    @Test fun emptyRepresentationIsDenialNotSuccess(): Unit = runBlocking {
        // PostgREST returns success with zero rows for UPDATE/DELETE that fail RLS.
        val repo = SupabaseAdminRepository(settings, { "fake-token" }, clientFor { req ->
            when {
                req.url.encodedPath.endsWith("/admin_users") -> okJson("""[{"user_id":"u1"}]""")
                else -> okJson("[]")  // silently applied nothing
            }
        })
        val result = repo.publishContent(sampleBundle)
        assertTrue(result.isFailure)
        assertIs<AdminDeniedException>(result.exceptionOrNull())
    }

    @Test fun successfulPublishRequiresReturnedRows() = runBlocking {
        val written = mutableListOf<String>()
        val repo = SupabaseAdminRepository(settings, { "fake-token" }, clientFor { req ->
            when {
                req.url.encodedPath.endsWith("/admin_users") -> okJson("""[{"user_id":"u1"}]""")
                req.url.encodedPath.endsWith("/darshan_settings") -> {
                    written += "darshan_settings"
                    okJson("""[{"id":1}]""")
                }
                req.url.encodedPath.endsWith("/contacts") -> {
                    written += "contacts"
                    okJson("""[{"id":5}]""")
                }
                else -> respond("{}", HttpStatusCode.NotFound)
            }
        })
        val result = repo.publishContent(sampleBundle)
        assertTrue(result.isSuccess, "expected success, got ${result.exceptionOrNull()}")
        assertEquals(listOf("darshan_settings", "contacts"), written)
    }

    @Test fun unsupportedSectionsAreRejectedNotDropped(): Unit = runBlocking {
        val repo = SupabaseAdminRepository(settings, { "fake-token" }, clientFor { req ->
            okJson("""[{"user_id":"u1"}]""")
        })
        val withFestivals = sampleBundle.copy(
            festivals = listOf(page.kakkamvellytemple.app.data.remote.FestivalDto("ഉത്സവം", "Festival", "🎉", 0L))
        )
        val result = repo.publishContent(withFestivals)
        assertTrue(result.isFailure)
        assertIs<UnsupportedOperationException>(result.exceptionOrNull())
    }

    @Test fun notificationsStayUnsent(): Unit = runBlocking {
        val repo = SupabaseAdminRepository(settings, { "fake-token" }, clientFor { okJson("[]") })
        val result = repo.sendNotification("t", "b", "topic")
        assertTrue(result.isFailure)
        assertIs<NotApprovedException>(result.exceptionOrNull())
    }
}
