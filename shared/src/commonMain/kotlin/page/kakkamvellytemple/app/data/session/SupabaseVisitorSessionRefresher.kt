package page.kakkamvellytemple.app.data.session

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import page.kakkamvellytemple.app.data.remote.SupabaseContentSource

/** Server-side OTP provider selection is unrelated to session refresh. Publishable
 * key only; no Meta credential/service-role key is ever an app dependency. */
class SupabaseVisitorSessionRefresher(
    private val settings: SupabaseContentSource.Settings,
    private val client: HttpClient,
    private val nowSeconds: () -> Long,
) : VisitorSessionRefresher {
    @Serializable private class Request(val refresh_token: String)
    @Serializable private class User(val id: String)
    @Serializable private class Response(
        val access_token: String,
        val refresh_token: String,
        val expires_in: Long,
        val user: User,
    )
    override suspend fun refresh(refreshToken: String): VisitorSession {
        val response = client.post("${settings.url}/auth/v1/token") {
            parameter("grant_type", "refresh_token")
            header("apikey", settings.anonKey)
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(Request(refreshToken)))
        }
        if (response.status.value in listOf(400, 401, 403)) throw VisitorSessionRejected()
        if (!response.status.isSuccess()) throw Exception("Session refresh temporarily unavailable")
        val body: Response = response.body()
        if (body.expires_in !in 1..604800) throw Exception("Invalid session lifetime")
        return VisitorSession(body.access_token, body.refresh_token, body.user.id, nowSeconds() + body.expires_in)
    }
}
