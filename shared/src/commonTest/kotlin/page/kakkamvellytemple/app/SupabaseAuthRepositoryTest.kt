package page.kakkamvellytemple.app

import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.http.content.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlin.test.*
import page.kakkamvellytemple.app.data.remote.SupabaseAuthRepository
import page.kakkamvellytemple.app.data.remote.SupabaseContentSource
import page.kakkamvellytemple.app.data.repository.AdminAuthState
import page.kakkamvellytemple.app.data.repository.NotApprovedException

/** OTP sign-in contract tests with a mocked GoTrue. The production wiring is
 *  constructed DISABLED: no real OTP can be sent until sign-in is enabled. */
class SupabaseAuthRepositoryTest {

    private val settings = SupabaseContentSource.Settings("https://test.supabase.co", "test-key")

    private fun clientFor(responder: MockRequestHandleScope.(HttpRequestData) -> HttpResponseData) =
        HttpClient(MockEngine(responder)) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }

    private fun MockRequestHandleScope.okJson(body: String) =
        respond(body, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))

    @Test fun disabledRepoSendsNoOtpAndFailsClosed(): Unit = runBlocking {
        var calls = 0
        val repo = SupabaseAuthRepository(settings, clientFor { calls++; okJson("{}") }, enabled = false)
        assertTrue(repo.requestOtp("admin@example.com").isFailure)
        assertIs<NotApprovedException>(repo.requestOtp("admin@example.com").exceptionOrNull())
        assertEquals(0, calls)
        assertIs<AdminAuthState.SignedOut>(repo.state)
        assertNull(repo.accessToken())
    }

    @Test fun requestOtpPostsEmailPayloadToGoTrue(): Unit = runBlocking {
        var captured: HttpRequestData? = null
        val repo = SupabaseAuthRepository(settings, clientFor { req -> captured = req; okJson("{}") }, enabled = true)
        assertTrue(repo.requestOtp("admin@example.com").isSuccess)
        val req = captured!!
        assertTrue(req.url.encodedPath.endsWith("/auth/v1/otp"))
        assertEquals("test-key", req.headers["apikey"])
        val body = (req.body as OutgoingContent.ByteArrayContent).bytes().decodeToString()
        assertTrue(body.contains("\"email\":\"admin@example.com\""), body)
        assertIs<AdminAuthState.AwaitingOtp>(repo.state)
    }

    @Test fun phoneChannelPostsPhonePayload(): Unit = runBlocking {
        var captured: HttpRequestData? = null
        val repo = SupabaseAuthRepository(settings, clientFor { req -> captured = req; okJson("{}") },
            channel = SupabaseAuthRepository.Channel.PHONE, enabled = true)
        assertTrue(repo.requestOtp("+919400755858").isSuccess)
        val body = (captured!!.body as OutgoingContent.ByteArrayContent).bytes().decodeToString()
        assertTrue(body.contains("\"phone\":\"+919400755858\""), body)
    }

    @Test fun verifySuccessStoresSessionAndSignsIn(): Unit = runBlocking {
        val repo = SupabaseAuthRepository(settings, clientFor { req ->
            when {
                req.url.encodedPath.endsWith("/auth/v1/otp") -> okJson("{}")
                req.url.encodedPath.endsWith("/auth/v1/verify") ->
                    okJson("""{"access_token":"tok-123","user":{"id":"u-1"}}""")
                else -> respond("{}", HttpStatusCode.NotFound)
            }
        }, enabled = true)
        repo.requestOtp("admin@example.com")
        assertTrue(repo.verifyOtp("admin@example.com", "123456").isSuccess)
        val s = repo.state
        assertIs<AdminAuthState.SignedIn>(s)
        assertEquals("u-1", s.userId)
        assertEquals("tok-123", repo.accessToken())
        repo.signOut()
        assertIs<AdminAuthState.SignedOut>(repo.state)
        assertNull(repo.accessToken())
    }

    @Test fun verifyFailureKeepsNoSession(): Unit = runBlocking {
        val repo = SupabaseAuthRepository(settings, clientFor {
            respond("""{"msg":"bad code"}""", HttpStatusCode.Forbidden,
                headersOf(HttpHeaders.ContentType, "application/json"))
        }, enabled = true)
        assertTrue(repo.verifyOtp("admin@example.com", "000000").isFailure)
        assertIs<AdminAuthState.Failed>(repo.state)
        assertNull(repo.accessToken())
    }

    @Test fun requestOtpHttpFailureSurfaces(): Unit = runBlocking {
        val repo = SupabaseAuthRepository(settings, clientFor {
            respond("""{"msg":"rate limited"}""", HttpStatusCode.TooManyRequests,
                headersOf(HttpHeaders.ContentType, "application/json"))
        }, enabled = true)
        assertTrue(repo.requestOtp("admin@example.com").isFailure)
        assertIs<AdminAuthState.SignedOut>(repo.state)
    }
}
