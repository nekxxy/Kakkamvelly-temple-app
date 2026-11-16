package page.kakkamvellytemple.app.data.session

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable

/** Never a data class: default toString/copy must not expose bearer credentials.
 * This session proves visitor identity only; server admin membership is separate. */
@Serializable
class VisitorSession(
    val accessToken: String,
    val refreshToken: String,
    val userId: String,
    val expiresAtSeconds: Long,
) {
    init {
        require(accessToken.isNotBlank() && refreshToken.isNotBlank() && userId.isNotBlank())
        require(expiresAtSeconds > 0)
    }
    override fun toString() = "VisitorSession(redacted)"
}

interface VisitorSessionStore {
    suspend fun load(): VisitorSession?
    suspend fun save(session: VisitorSession)
    suspend fun clear()
}

/** Rejected refresh (revoked/invalid token), distinct from an offline failure. */
class VisitorSessionRejected : Exception("Visitor session expired; sign in again")
fun interface VisitorSessionRefresher {
    suspend fun refresh(refreshToken: String): VisitorSession
}

/** Prepared session lifecycle, NOT connected to production login/navigation yet.
 * All operations share one mutex, including storage and rotation: concurrent
 * callers cannot redeem the same rotating refresh token, and a sign-out cannot
 * be undone by an in-flight refresh. Never return an expired access token.
 * No OTP, phone number or credentials are logged. */
class VisitorSessionManager(
    private val store: VisitorSessionStore,
    private val refresher: VisitorSessionRefresher,
    private val nowSeconds: () -> Long,
) {
    private val mutex = Mutex()
    private var loaded = false
    private var current: VisitorSession? = null

    suspend fun accept(session: VisitorSession) = mutex.withLock {
        require(session.expiresAtSeconds > nowSeconds()) { "Session already expired" }
        // Persist rotated tokens before exposing the new session. If storage
        // fails, no success is reported to the caller.
        store.save(session)
        current = session
        loaded = true
    }

    suspend fun validSession(): VisitorSession? = mutex.withLock {
        if (!loaded) {
            current = store.load()
            loaded = true
        }
        val old = current ?: return@withLock null
        if (old.expiresAtSeconds - nowSeconds() > 60) return@withLock old
        try {
            val next = refresher.refresh(old.refreshToken)
            if (next.userId != old.userId || next.expiresAtSeconds <= nowSeconds()) {
                throw VisitorSessionRejected()
            }
            store.save(next)
            current = next
            next
        } catch (e: VisitorSessionRejected) {
            current = null
            store.clear()
            throw e
        }
        // Network/server/storage failures propagate, preserving the persisted
        // refresh token for retry. They do not return a stale access token or
        // reinterpret "offline" as "signed out".
    }

    /** Local device only. Production logout must additionally revoke the server
     * session via the auth endpoint, with failure surfaced, before rollout. */
    suspend fun clearLocalSession() = mutex.withLock {
        current = null
        loaded = true
        store.clear()
    }
}
