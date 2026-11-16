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
        // Supabase clients treat nonexistent/expired sessions as already out.
        if (!response.status.isSuccess() && response.status.value !in listOf(401, 403, 404)) {
            throw Exception("Signed out on this device; server logout could not be confirmed")
        }
    }
}
