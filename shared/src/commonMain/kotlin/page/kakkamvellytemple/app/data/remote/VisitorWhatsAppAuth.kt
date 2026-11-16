package page.kakkamvellytemple.app.data.remote

import androidx.compose.runtime.*
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import page.kakkamvellytemple.app.data.repository.*
import page.kakkamvellytemple.app.data.session.*

/** Visitor identity only, never an admin AuthRepository or token provider.
 * Delivery stays disabled until sender/template/cost/abuse-control approval and
 * live integration verification. No SMS fallback or sender credentials in app.
 * Android production composition must supply AndroidVisitorSessionStore and a
 * dedicated HTTP client WITHOUT Logging. Default store is ephemeral for tests
 * and previews; it is not a silent fallback for a failed encrypted store. */
class VisitorWhatsAppAuth(
    private val settings: SupabaseContentSource.Settings,
    private val client: HttpClient,
    override val deliveryEnabled: Boolean = false,
    store: VisitorSessionStore = MemoryVisitorSessionStore(),
    private val nowSeconds: () -> Long = { Clock.System.now().toEpochMilliseconds() / 1000 },
) : VisitorAuthRepository {
    private val mutex = Mutex()
    private val json = Json { ignoreUnknownKeys = true }
    private val sessions = VisitorSessionManager(store,
        SupabaseVisitorSessionRefresher(settings, client, nowSeconds), nowSeconds,
        SupabaseVisitorSessionRevoker(settings, client))
    private var pendingPhone: String? = null
    override var state: VisitorAuthState by mutableStateOf(VisitorAuthState.SignedOut)
        private set

    @Serializable private class OtpRequest(val phone: String, val channel: String = "whatsapp")
    @Serializable private class VerifyRequest(val phone: String, val token: String, val type: String = "sms")
    @Serializable private class User(val id: String)
    @Serializable private class SessionResponse(
        val access_token: String, val refresh_token: String, val expires_in: Long, val user: User,
    )
    private suspend fun attempt(block: suspend () -> Unit): Result<Unit> = try {
        block(); Result.success(Unit)
    } catch (e: CancellationException) { throw e }
      catch (_: Exception) { Result.failure(Exception("Visitor login is temporarily unavailable or the code is invalid")) }

    override suspend fun requestCode(phone: String): Result<Unit> = mutex.withLock {
        if (!deliveryEnabled) return@withLock Result.failure(NotApprovedException("Visitor WhatsApp delivery is not enabled yet"))
        if (!Regex("^\\+[1-9][0-9]{6,14}$").matches(phone)) {
            return@withLock Result.failure(IllegalArgumentException("Enter a number with its country code"))
        }
        attempt {
            val response = client.post("${settings.url}/auth/v1/otp") {
                header("apikey", settings.anonKey); contentType(ContentType.Application.Json)
                setBody(Json { encodeDefaults = true }.encodeToString(OtpRequest(phone)))
            }
            check(response.status.isSuccess()) { "Code request failed" }
            pendingPhone = phone
            state = VisitorAuthState.AwaitingCode(phone)
        }
    }

    override suspend fun verifyCode(phone: String, code: String): Result<Unit> = mutex.withLock {
        if (!deliveryEnabled) return@withLock Result.failure(NotApprovedException("Visitor WhatsApp delivery is not enabled yet"))
        if (pendingPhone != phone || !Regex("^[0-9]{6}$").matches(code)) {
            return@withLock Result.failure(IllegalArgumentException("Request a code for this number and enter six digits"))
        }
        attempt {
            val response = client.post("${settings.url}/auth/v1/verify") {
                header("apikey", settings.anonKey); contentType(ContentType.Application.Json)
                setBody(Json { encodeDefaults = true }.encodeToString(VerifyRequest(phone, code)))
            }
            check(response.status.isSuccess()) { "Code verification failed" }
            val body: SessionResponse = response.body()
            check(body.expires_in in 1..604800)
            // Durable persistence succeeds BEFORE a signed-in UI state is exposed.
            sessions.accept(VisitorSession(body.access_token, body.refresh_token, body.user.id, nowSeconds() + body.expires_in))
            pendingPhone = null
            state = VisitorAuthState.SignedIn(body.user.id)
        }
    }

    override suspend fun restoreSession(): Result<Unit> = mutex.withLock {
        if (!deliveryEnabled) return@withLock Result.success(Unit) // inert while setup/rollout held
        try {
            val restored = sessions.validSession()
            if (restored != null) state = VisitorAuthState.SignedIn(restored.userId)
            else if (pendingPhone == null) state = VisitorAuthState.SignedOut
            Result.success(Unit)
        } catch (e: CancellationException) { throw e }
          catch (_: Exception) {
            // Never leave stale authenticated navigation open after an offline or
            // expired-session recheck. Disk refresh token is retained for retry.
            state = VisitorAuthState.SignedOut
            Result.failure(Exception("Could not restore visitor session"))
        }
    }

    override suspend fun signOut() = mutex.withLock {
        pendingPhone = null
        state = VisitorAuthState.SignedOut
        try { sessions.signOut() } catch (e: CancellationException) { throw e }
        catch (_: Exception) { throw Exception("Sign-out could not be fully confirmed. Avoid using this app on a shared device.") }
    }
}
