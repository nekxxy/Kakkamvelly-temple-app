package page.kakkamvellytemple.app.data.remote

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.ktor.client.HttpClient
import page.kakkamvellytemple.app.data.repository.*

/** Prepared visitor WhatsApp transport, DISABLED by default. Sender registration,
 * template approval, cost approval, abuse controls and live delivery verification
 * must be completed before a production entry point can enable it. No SMS fallback.
 *
 * Supabase issues/verifies the OTP; this class never generates a code. Built-in
 * WhatsApp delivery requires Twilio/Twilio Verify. A custom Send SMS Hook can use
 * Meta directly, but its channel dispatch must be integration-tested before use.
 * Session persistence/refresh and the production navigation gate remain unwired.
 * The private delegate is NOT the admin screen's auth dependency. */
class VisitorWhatsAppAuth(
    settings: SupabaseContentSource.Settings,
    client: HttpClient,
    override val deliveryEnabled: Boolean = false,
) : VisitorAuthRepository {
    private val delegate = SupabaseAuthRepository(
        settings, client, SupabaseAuthRepository.Channel.WHATSAPP, deliveryEnabled,
    )

    private var pendingPhone by mutableStateOf<String?>(null)

    override val state: VisitorAuthState
        get() = when (val s = delegate.state) {
            is AdminAuthState.SignedIn -> VisitorAuthState.SignedIn(s.userId)
            else -> pendingPhone?.let { VisitorAuthState.AwaitingCode(it) } ?: VisitorAuthState.SignedOut
        }

    private fun validPhone(phone: String) = Regex("^\\+[1-9][0-9]{6,14}$").matches(phone)

    override suspend fun requestCode(phone: String): Result<Unit> {
        if (!deliveryEnabled) return Result.failure(NotApprovedException("Visitor WhatsApp delivery is not enabled yet"))
        if (!validPhone(phone)) return Result.failure(IllegalArgumentException("Enter a number with its country code, such as +91"))
        return delegate.requestOtp(phone).onSuccess { pendingPhone = phone }
    }

    override suspend fun verifyCode(phone: String, code: String): Result<Unit> {
        if (!deliveryEnabled) return Result.failure(NotApprovedException("Visitor WhatsApp delivery is not enabled yet"))
        val pending = state as? VisitorAuthState.AwaitingCode
            ?: return Result.failure(IllegalStateException("Request a WhatsApp code first"))
        if (pending.phone != phone) return Result.failure(IllegalArgumentException("Request a new code for the changed number"))
        if (!Regex("^[0-9]{6}$").matches(code)) return Result.failure(IllegalArgumentException("Enter the six-digit code"))
        return delegate.verifyOtp(phone, code).onSuccess { pendingPhone = null }
    }

    override suspend fun signOut() {
        pendingPhone = null
        delegate.signOut()
    }
}
