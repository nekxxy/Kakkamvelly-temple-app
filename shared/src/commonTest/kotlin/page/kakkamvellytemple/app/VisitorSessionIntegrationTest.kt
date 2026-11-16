package page.kakkamvellytemple.app

import io.ktor.client.*
import io.ktor.client.request.HttpResponseData
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlin.test.*
import page.kakkamvellytemple.app.data.remote.*
import page.kakkamvellytemple.app.data.repository.*
import page.kakkamvellytemple.app.data.session.*

class VisitorSessionIntegrationTest {
    private val settings = SupabaseContentSource.Settings("https://test.supabase.co", "test-key")
    private val phone = "+15555550123"
    private val sessionJson = """{"access_token":"access","refresh_token":"refresh","expires_in":3600,"user":{"id":"visitor"}}"""
    private class Store : VisitorSessionStore {
        var value: VisitorSession? = null; var failSave = false
        override suspend fun load() = value
        override suspend fun save(session: VisitorSession) { if (failSave) error("disk"); value = session }
        override suspend fun clear() { value = null }
    }
    private fun client(handler: MockRequestHandleScope.(io.ktor.client.request.HttpRequestData) -> HttpResponseData) =
        HttpClient(MockEngine(handler)) { install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) } }
    @Test fun verifiedSessionPersistsAndRestoresWithoutOtpSend(): Unit = runBlocking {
        var sends = 0; var logout = 0
        val c = client { req ->
            val path = req.url.encodedPath
            if (path.endsWith("/otp")) sends++
            if (path.endsWith("/logout")) { logout++; assertEquals("local", req.url.parameters["scope"]); assertEquals("Bearer access",req.headers["Authorization"]) }
            respond(if (path.endsWith("/verify")) sessionJson else "{}", HttpStatusCode.OK,
                headersOf(HttpHeaders.ContentType,"application/json"))
        }
        val disk = Store(); val first = VisitorWhatsAppAuth(settings,c,true,disk,{ 100 })
        first.requestCode(phone); assertTrue(first.verifyCode(phone,"123456").isSuccess)
        assertNotNull(disk.value)
        val restarted = VisitorWhatsAppAuth(settings,c,true,disk,{ 100 })
        assertTrue(restarted.restoreSession().isSuccess)
        assertIs<VisitorAuthState.SignedIn>(restarted.state); assertEquals(1,sends)
        restarted.signOut(); assertEquals(1,logout); assertNull(disk.value)
    }
    @Test fun storageFailureNeverExposesSuccessfulLogin(): Unit = runBlocking {
        val disk = Store(); disk.failSave = true
        val c = client { req -> respond(if (req.url.encodedPath.endsWith("/verify")) sessionJson else "{}",
            HttpStatusCode.OK,headersOf(HttpHeaders.ContentType,"application/json")) }
        val r = VisitorWhatsAppAuth(settings,c,true,disk,{100})
        r.requestCode(phone); assertTrue(r.verifyCode(phone,"123456").isFailure)
        assertFalse(r.state is VisitorAuthState.SignedIn); assertNull(disk.value)
    }
    @Test fun offlineLogoutClearsLocallyButReportsUnconfirmedRevocation(): Unit = runBlocking {
        val disk=Store(); disk.value=VisitorSession("access","refresh","visitor",3600)
        val c=client { respond("private error",HttpStatusCode.ServiceUnavailable) }
        val r=VisitorWhatsAppAuth(settings,c,true,disk,{100})
        r.restoreSession(); assertIs<VisitorAuthState.SignedIn>(r.state)
        assertTrue(runCatching { r.signOut() }.isFailure)
        assertIs<VisitorAuthState.SignedOut>(r.state); assertNull(disk.value)
    }
    @Test fun disabledRestoreMakesNoNetworkCalls(): Unit = runBlocking {
        var calls=0; val c=client { calls++; error("must not call") }
        val disk=Store(); disk.value=VisitorSession("access","refresh","visitor",50)
        val r=VisitorWhatsAppAuth(settings,c,false,disk,{100})
        r.restoreSession(); assertEquals(0,calls); assertIs<VisitorAuthState.SignedOut>(r.state)
    }
    @Test fun expiryRefreshRestoresIdentityWithoutSendingOtp(): Unit = runBlocking {
        var calls=0; val c=client { req ->
            assertEquals("/auth/v1/token",req.url.encodedPath); calls++
            respond(sessionJson,HttpStatusCode.OK,headersOf(HttpHeaders.ContentType,"application/json"))
        }
        val disk=Store(); disk.value=VisitorSession("old","old-refresh","visitor",50)
        val r=VisitorWhatsAppAuth(settings,c,true,disk,{100})
        assertTrue(r.restoreSession().isSuccess); assertIs<VisitorAuthState.SignedIn>(r.state)
        assertEquals(1,calls); assertEquals("refresh",disk.value?.refreshToken)
    }
}
