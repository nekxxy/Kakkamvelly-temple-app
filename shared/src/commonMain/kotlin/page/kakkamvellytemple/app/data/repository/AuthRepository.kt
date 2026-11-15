package page.kakkamvellytemple.app.data.repository

/** Admin sign-in states. The OTP channel (WhatsApp vs SMS vs email) is unresolved;
 *  this interface is channel-agnostic so the decision only changes configuration. */
sealed interface AdminAuthState {
    data object SignedOut : AdminAuthState
    /** An OTP was requested for this destination and the code is awaited. */
    data class AwaitingOtp(val destination: String) : AdminAuthState
    data class SignedIn(val userId: String) : AdminAuthState
    data class Failed(val message: String) : AdminAuthState
}

interface AuthRepository {
    val state: AdminAuthState
    /** Request a one-time code for a phone number or email, depending on the
     *  configured channel. Real sends stay disabled until sign-in is enabled. */
    suspend fun requestOtp(destination: String): Result<Unit>
    suspend fun verifyOtp(destination: String, code: String): Result<Unit>
    suspend fun signOut()
}
