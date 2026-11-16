package page.kakkamvellytemple.app.presentation.viewmodel

import androidx.compose.runtime.*
import kotlinx.coroutines.*
import kotlinx.datetime.Clock
import page.kakkamvellytemple.app.data.repository.*

/** Isolated visitor flow. No admin state or token access. Client cooldown is UX
 * only, NOT an abuse boundary: production needs server throttling and CAPTCHA.
 * No code/phone persistence, analytics, or raw provider error display. */
class VisitorLoginViewModel(
    private val auth: VisitorAuthRepository,
    private val scope: CoroutineScope,
    private val nowMs: () -> Long = { Clock.System.now().toEpochMilliseconds() },
) {
    var phone by mutableStateOf(""); private set
    var code by mutableStateOf(""); private set
    var busy by mutableStateOf(false); private set
    var error by mutableStateOf<String?>(null); private set
    var secondsUntilResend by mutableStateOf(0); private set
    private var resendAt = 0L
    val deliveryEnabled get() = auth.deliveryEnabled
    val awaitingCode get() = auth.state is VisitorAuthState.AwaitingCode
    val signedIn get() = auth.state is VisitorAuthState.SignedIn

    fun updatePhone(value: String) { if (!busy && !awaitingCode) { phone = value; error = null } }
    fun updateCode(value: String) { if (!busy) { code = value; error = null } }
    fun tick() { secondsUntilResend = ((resendAt - nowMs()).coerceAtLeast(0) + 999).div(1000).toInt() }

    fun requestCode() {
        tick()
        if (busy || secondsUntilResend > 0) return
        if (!deliveryEnabled) { error = "WhatsApp login is not enabled yet."; return }
        val destination = phone.trim()
        if (!Regex("^\\+[1-9][0-9]{6,14}$").matches(destination)) {
            error = "Enter your WhatsApp number with country code (for example +91)."; return
        }
        busy = true; error = null
        scope.launch {
            try {
                auth.requestCode(destination)
                    .onSuccess { phone = destination; code = ""; resendAt = nowMs() + 60_000; tick() }
                    .onFailure { error = "Could not send a code. Please try again later." }
            } finally { busy = false }
        }
    }

    fun verifyCode() {
        if (busy) return
        if (!awaitingCode) { error = "Request a WhatsApp code first."; return }
        if (!Regex("^[0-9]{6}$").matches(code)) { error = "Enter the six-digit code from WhatsApp."; return }
        busy = true; error = null
        scope.launch {
            try {
                auth.verifyCode(phone, code)
                    .onSuccess { code = "" }
                    .onFailure { code = ""; error = "That code could not be verified. Try again or request a new code." }
            } finally { busy = false }
        }
    }

    fun changeNumber() {
        if (busy) return
        busy = true
        scope.launch {
            try { auth.signOut(); code = ""; error = null } finally { busy = false }
            // Keep cooldown across number changes. Changing a field is not a way
            // around even the client cooldown; server controls remain required.
        }
    }
}
