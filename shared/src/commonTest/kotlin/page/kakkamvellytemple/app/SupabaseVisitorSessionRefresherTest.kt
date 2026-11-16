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
import page.kakkamvellytemple.app.data.remote.SupabaseContentSource
import page.kakkamvellytemple.app.data.session.*

class SupabaseVisitorSessionRefresherTest {
    private val settings = SupabaseContentSource.Settings("https://test.supabase.co", "publishable-test")
    private fun refresh(status: HttpStatusCode, body: String) = SupabaseVisitorSessionRefresher(settings,
        HttpClient(MockEngine { req ->
            assertEquals("/auth/v1/token", req.url.encodedPath)
            assertEquals("refresh_token", req.url.parameters["grant_type"])
            assertEquals("publishable-test", req.headers["apikey"])
            val request = (req.body as OutgoingContent.ByteArrayContent).bytes().decodeToString()
            assertEquals("old-refresh", Json.parseToJsonElement(request).jsonObject["refresh_token"]?.jsonPrimitive?.content)
            respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))
        }) { install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) } }, { 100 })
    @Test fun successfulRefreshKeepsRotatedTokenAndExpiry(): Unit = runBlocking {
        val next = refresh(HttpStatusCode.OK,
            """{"access_token":"new-access","refresh_token":"new-refresh","expires_in":3600,"user":{"id":"v"},"ignored":true}""").refresh("old-refresh")
        assertEquals("new-refresh", next.refreshToken); assertEquals(3700, next.expiresAtSeconds)
    }
    @Test fun rejectedRefreshNeedsNewLogin(): Unit = runBlocking {
        assertFailsWith<VisitorSessionRejected> { refresh(HttpStatusCode.BadRequest, "{}").refresh("old-refresh") }
    }
    @Test fun rateLimitIsNotTreatedAsRevocation(): Unit = runBlocking {
        val failure = runCatching { refresh(HttpStatusCode.TooManyRequests, "private provider detail").refresh("old-refresh") }.exceptionOrNull()
        assertNotNull(failure); assertFalse(failure is VisitorSessionRejected)
        assertFalse(failure.message.orEmpty().contains("private"))
    }
    @Test fun missingRotatedTokenCannotCreateSession(): Unit = runBlocking {
        assertTrue(runCatching { refresh(HttpStatusCode.OK,
            """{"access_token":"access","expires_in":3600,"user":{"id":"v"}}""").refresh("old-refresh") }.isFailure)
    }
    @Test fun nonpositiveLifetimeCannotCreateSession(): Unit = runBlocking {
        assertTrue(runCatching { refresh(HttpStatusCode.OK,
            """{"access_token":"access","refresh_token":"r","expires_in":0,"user":{"id":"v"}}""").refresh("old-refresh") }.isFailure)
    }
}
