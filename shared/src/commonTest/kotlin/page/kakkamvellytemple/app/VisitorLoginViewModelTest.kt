package page.kakkamvellytemple.app

import kotlinx.coroutines.*
import kotlin.test.*
import page.kakkamvellytemple.app.data.repository.*
import page.kakkamvellytemple.app.presentation.viewmodel.VisitorLoginViewModel

class VisitorLoginViewModelTest {
    private class Fake(override val deliveryEnabled: Boolean = true) : VisitorAuthRepository {
        override var state: VisitorAuthState = VisitorAuthState.SignedOut
        var requests = 0; var verifies = 0; var failVerify = false
        override suspend fun requestCode(phone: String): Result<Unit> {
            requests++; state = VisitorAuthState.AwaitingCode(phone); return Result.success(Unit)
        }
        override suspend fun verifyCode(phone: String, code: String): Result<Unit> {
            verifies++
            if (failVerify) return Result.failure(Exception("private provider detail"))
            state = VisitorAuthState.SignedIn("visitor"); return Result.success(Unit)
        }
        override suspend fun signOut() { state = VisitorAuthState.SignedOut }
    }
    private val phone = "+15555550123"
    private fun vm(auth: Fake, clock: () -> Long = { 0L }) =
        VisitorLoginViewModel(auth, CoroutineScope(Dispatchers.Unconfined), clock)

    @Test fun disabledAndMalformedRequestsDoNotReachTransport() {
        val auth = Fake(false); val v = vm(auth); v.updatePhone(phone); v.requestCode()
        assertEquals(0, auth.requests); assertNotNull(v.error)
        val enabled = Fake(); val w = vm(enabled); w.updatePhone("555"); w.requestCode()
        assertEquals(0, enabled.requests)
    }
    @Test fun cooldownAndNumberChangesCannotTriggerImmediateResend() {
        var now = 0L; val auth = Fake(); val v = vm(auth) { now }
        v.updatePhone(phone); v.requestCode(); assertTrue(v.awaitingCode)
        v.requestCode(); assertEquals(1, auth.requests)
        v.changeNumber(); v.updatePhone("+15555550124"); v.requestCode()
        assertEquals(1, auth.requests)
        now = 60_000; v.tick(); v.requestCode(); assertEquals(2, auth.requests)
    }
    @Test fun failedVerificationClearsCodeAndHidesProviderDetail() {
        val auth = Fake(); auth.failVerify = true
        val v = vm(auth); v.updatePhone(phone); v.requestCode(); v.updateCode("123456"); v.verifyCode()
        assertEquals("", v.code); assertTrue(v.awaitingCode)
        assertFalse(v.error.orEmpty().contains("private")); assertFalse(v.busy)
        auth.failVerify = false; v.updateCode("123456"); v.verifyCode(); assertTrue(v.signedIn)
    }
    @Test fun cannotVerifyBeforeRequestOrWithMalformedCode() {
        val auth = Fake(); val v = vm(auth); v.verifyCode(); assertEquals(0, auth.verifies)
        v.updatePhone(phone); v.requestCode(); v.updateCode("12a"); v.verifyCode(); assertEquals(0, auth.verifies)
    }
    @Test fun successfulVerificationClearsEphemeralCode() {
        val auth = Fake(); val v = vm(auth); v.updatePhone(phone); v.requestCode()
        v.updateCode("123456"); v.verifyCode(); assertTrue(v.signedIn); assertEquals("", v.code)
    }
}
