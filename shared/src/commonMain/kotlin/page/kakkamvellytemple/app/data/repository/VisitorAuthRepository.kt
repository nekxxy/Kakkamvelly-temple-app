package page.kakkamvellytemple.app.data.repository

/** Visitor identity is separate from admin authorization. A successful OTP does
 * not enroll an admin. No visitor-facing interface supplies an admin token. */
sealed interface VisitorAuthState {
    data object SignedOut : VisitorAuthState
    data class AwaitingCode(val phone: String) : VisitorAuthState
    data class SignedIn(val userId: String) : VisitorAuthState
}

interface VisitorAuthRepository {
    val state: VisitorAuthState
    val deliveryEnabled: Boolean
    suspend fun requestCode(phone: String): Result<Unit>
    suspend fun verifyCode(phone: String, code: String): Result<Unit>
    suspend fun restoreSession(): Result<Unit> = Result.success(Unit)
    suspend fun signOut()
}
