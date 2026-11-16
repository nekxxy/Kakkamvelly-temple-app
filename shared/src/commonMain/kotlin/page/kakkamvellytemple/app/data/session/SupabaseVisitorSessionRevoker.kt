package page.kakkamvellytemple.app.data.session

import io.ktor.client.HttpClient
import io.ktor.client.request.*
import io.ktor.http.*
import page.kakkamvellytemple.app.data.remote.SupabaseContentSource

class SupabaseVisitorSessionRevoker(
    private val settings: SupabaseContentSource.Settings,
    private val client: HttpClient,
) : VisitorSessionRevoker {
    override suspend fun revoke(accessToken: String) {
        val response = client.post("${settings.url}/auth/v1/logout") {
            parameter("scope", "local") // never sign out unrelated devices
            header("apikey", settings.anonKey)
            bearerAuth(accessToken)
        }
        // An expired access JWT does not prove its refresh token was revoked.
        // Treat every non-success response as unconfirmed server logout.
        if (!response.status.isSuccess()) {
            throw Exception("Server logout could not be confirmed")
        }
    }
}
