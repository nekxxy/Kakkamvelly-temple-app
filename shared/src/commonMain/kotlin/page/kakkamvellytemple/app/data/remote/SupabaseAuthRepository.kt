package page.kakkamvellytemple.app.data.remote

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import page.kakkamvellytemple.app.data.repository.AdminAuthState
import page.kakkamvellytemple.app.data.repository.AdminAuthTokenProvider
import page.kakkamvellytemple.app.data.repository.AuthRepository
import page.kakkamvellytemple.app.data.repository.NotApprovedException

/** GoTrue (Supabase Auth) OTP sign-in. Channel-agnostic: [channel] selects phone or
 *  email payloads; which channel ships is an open owner decision, so the repository
 *  is constructed disabled by default - requestOtp/verifyOtp then fail without any
 *  network call, and nothing can send a real OTP by accident.
 *
 * Also serves as the [AdminAuthTokenProvider] for SupabaseAdminRepository: the
 *  verified session's access token authorizes admin writes under server-side RLS. */
class SupabaseAuthRepository(
    private val settings: SupabaseContentSource.Settings,
    private val client: HttpClient,
    private val channel: Channel = Channel.EMAIL,
    private val enabled: Boolean = false,
) : AuthRepository, AdminAuthTokenProvider {

    enum class Channel { EMAIL, PHONE, WHATSAPP }

    private val json = Json { ignoreUnknownKeys = true }

    override var state: AdminAuthState by mutableStateOf(AdminAuthState.SignedOut)
        private set

    private var accessToken: String? = null
    override suspend fun accessToken(): String? = accessToken

    @Serializable private data class OtpRequest(
        val email: String? = null, val phone: String? = null, val channel: String? = null,
    )
    @Serializable private data class VerifyRequest(
        val type: String, val token: String,
        val email: String? = null, val phone: String? = null,
    )
    @Serializable private data class SessionResponse(
        val access_token: String? = null,
        val user: UserHolder? = null,
    )
    @Serializable private data class UserHolder(val id: String = "")

    private fun disabled(): Result<Nothing> =
        Result.failure(NotApprovedException("Admin sign-in is not enabled yet"))

    override suspend fun requestOtp(destination: String): Result<Unit> {
        if (!enabled) return disabled()
        return runCatching {
            val body = when (channel) {
                Channel.EMAIL -> json.encodeToString(OtpRequest(email = destination))
                Channel.PHONE -> json.encodeToString(OtpRequest(phone = destination))
                Channel.WHATSAPP -> json.encodeToString(OtpRequest(phone = destination, channel = "whatsapp"))
            }
            val resp = client.post("${settings.url}/auth/v1/otp") {
                header("apikey", settings.anonKey)
                contentType(ContentType.Application.Json)
                setBody(body)
            }
            if (!resp.status.isSuccess()) {
                throw Exception("OTP request failed (HTTP ${resp.status.value}): ${resp.bodyAsText().take(160)}")
            }
            state = AdminAuthState.AwaitingOtp(destination)
        }
    }

    override suspend fun verifyOtp(destination: String, code: String): Result<Unit> {
        if (!enabled) return disabled()
        return runCatching {
            val body = when (channel) {
                Channel.EMAIL -> json.encodeToString(VerifyRequest("email", code, email = destination))
                Channel.PHONE, Channel.WHATSAPP -> json.encodeToString(VerifyRequest("sms", code, phone = destination))
            }
            val resp = client.post("${settings.url}/auth/v1/verify") {
                header("apikey", settings.anonKey)
                contentType(ContentType.Application.Json)
                setBody(body)
            }
            if (!resp.status.isSuccess()) {
                state = AdminAuthState.Failed("Verification failed (HTTP ${resp.status.value})")
                throw Exception("Verification failed (HTTP ${resp.status.value})")
            }
            val session: SessionResponse = resp.body()
            val token = session.access_token ?: throw Exception("No access token in response")
            accessToken = token
            state = AdminAuthState.SignedIn(session.user?.id ?: "")
        }
    }

    override suspend fun signOut() {
        accessToken = null
        state = AdminAuthState.SignedOut
    }
}
