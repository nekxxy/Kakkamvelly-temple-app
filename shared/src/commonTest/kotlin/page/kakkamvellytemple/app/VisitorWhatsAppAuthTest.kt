package page.kakkamvellytemple.app

import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import kotlin.test.*
import page.kakkamvellytemple.app.data.remote.*
import page.kakkamvellytemple.app.data.repository.*

/** Mock HTTP only. These tests do not send WhatsApp/SMS or enroll any account. */
class VisitorWhatsAppAuthTest {
    private val settings = SupabaseContentSource.Settings("https://test.supabase.co", "test-key")
    private val phone = "+15555550123"
    private val bodies = mutableListOf<JsonObject>()
    private fun repo(enabled: Boolean = false): VisitorWhatsAppAuth {
        val client = HttpClient(MockEngine { req ->
            val raw = (req.body as OutgoingContent.ByteArrayContent).bytes().decodeToString()
            bodies += Json.parseToJsonElement(raw).jsonObject
            respond(if (req.url.encodedPath.endsWith("/verify"))
                """{"access_token":"mock-token","user":{"id":"visitor-1"}}""" else "{}",
                HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        }) { install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) } }
        return VisitorWhatsAppAuth(settings, client, enabled)
    }
    @Test fun disabledByDefaultMakesNoNetworkCall(): Unit = runBlocking {
        val r = repo()
        assertFalse(r.deliveryEnabled)
        assertIs<NotApprovedException>(r.requestCode(phone).exceptionOrNull())
        assertTrue(r.verifyCode(phone, "123456").isFailure)
        assertTrue(bodies.isEmpty())
    }
    @Test fun explicitWhatsAppChannelAndPhoneVerification(): Unit = runBlocking {
        val r = repo(true)
        assertTrue(r.requestCode(phone).isSuccess)
        assertEquals("whatsapp", bodies[0]["channel"]?.jsonPrimitive?.content)
        assertEquals(phone, bodies[0]["phone"]?.jsonPrimitive?.content)
        assertTrue(r.verifyCode(phone, "123456").isSuccess)
        assertEquals("sms", bodies[1]["type"]?.jsonPrimitive?.content, "Supabase phone OTP verification type, not delivery channel")
        assertEquals("visitor-1", (r.state as VisitorAuthState.SignedIn).userId)
        r.signOut()
        assertIs<VisitorAuthState.SignedOut>(r.state)
    }
    @Test fun malformedPhoneAndUnrequestedCodeDoNotCallNetwork(): Unit = runBlocking {
        val r = repo(true)
        assertTrue(r.requestCode("9400755858").isFailure)
        assertTrue(r.requestCode("+91abc").isFailure)
        assertTrue(r.verifyCode(phone, "123456").isFailure)
        assertTrue(bodies.isEmpty())
    }
    @Test fun changedNumberAndBadCodeDoNotVerify(): Unit = runBlocking {
        val r = repo(true)
        r.requestCode(phone)
        assertTrue(r.verifyCode("+15555550124", "123456").isFailure)
        assertTrue(r.verifyCode(phone, "bad").isFailure)
        assertEquals(1, bodies.size)
    }
    @Test fun wrongCodeCanBeRetriedWithoutSendingAnotherMessage(): Unit = runBlocking {
        var verifies = 0
        var sends = 0
        val client = HttpClient(MockEngine { req ->
            if (req.url.encodedPath.endsWith("/otp")) {
                sends++; respond("{}", HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
            } else {
                verifies++
                respond(if (verifies == 1) "{}" else """{"access_token":"mock","user":{"id":"v"}}""",
                    if (verifies == 1) HttpStatusCode.Forbidden else HttpStatusCode.OK,
                    headersOf(HttpHeaders.ContentType, "application/json"))
            }
        }) { install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) } }
        val r = VisitorWhatsAppAuth(settings, client, true)
        r.requestCode(phone)
        assertTrue(r.verifyCode(phone, "000000").isFailure)
        assertIs<VisitorAuthState.AwaitingCode>(r.state)
        assertTrue(r.verifyCode(phone, "123456").isSuccess)
        assertEquals(1, sends)
        assertEquals(2, verifies)
    }
    @Test fun visitorObjectIsNotAnAdminTokenProvider() {
        val r: Any = repo()
        assertFalse(r is AdminAuthTokenProvider)
        assertFalse(r is AuthRepository, "never inject visitor auth as admin screen auth")
    }
}
