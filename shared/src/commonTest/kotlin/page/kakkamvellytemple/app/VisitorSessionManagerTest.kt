package page.kakkamvellytemple.app

import kotlinx.coroutines.*
import kotlin.test.*
import page.kakkamvellytemple.app.data.session.*

class VisitorSessionManagerTest {
    private fun session(expiry: Long = 3600, refresh: String = "refresh-1", user: String = "visitor") =
        VisitorSession("access", refresh, user, expiry)
    private class Store(var value: VisitorSession? = null) : VisitorSessionStore {
        var failSave = false
        override suspend fun load() = value
        override suspend fun save(session: VisitorSession) { if (failSave) error("disk failed"); value = session }
        override suspend fun clear() { value = null }
    }
    @Test fun validStoredSessionRestoresWithoutRefresh(): Unit = runBlocking {
        val s = Store(session()); var calls = 0
        val m = VisitorSessionManager(s, { calls++; session() }, { 100 })
        assertEquals("visitor", m.validSession()?.userId); assertEquals(0, calls)
    }
    @Test fun concurrentRefreshesRotateOnlyOnce(): Unit = runBlocking {
        val s = Store(session(120)); var calls = 0
        val m = VisitorSessionManager(s, { token ->
            assertEquals("refresh-1", token); calls++; delay(10); session(4000, "refresh-2")
        }, { 100 })
        coroutineScope { List(10) { async { m.validSession() } }.awaitAll() }
        assertEquals(1, calls); assertEquals("refresh-2", s.value?.refreshToken)
    }
    @Test fun revokedSessionClearsDiskAndMemory(): Unit = runBlocking {
        val s = Store(session(120))
        val m = VisitorSessionManager(s, { throw VisitorSessionRejected() }, { 100 })
        assertFailsWith<VisitorSessionRejected> { m.validSession() }
        assertNull(s.value); assertNull(m.validSession())
    }
    @Test fun offlineFailureDoesNotReturnExpiredTokenOrEraseRefresh(): Unit = runBlocking {
        val s = Store(session(50)); val m = VisitorSessionManager(s, { error("offline") }, { 100 })
        assertFailsWith<IllegalStateException> { m.validSession() }
        assertEquals("refresh-1", s.value?.refreshToken)
    }
    @Test fun storageFailureDoesNotExposeNewSession(): Unit = runBlocking {
        val s = Store(); s.failSave = true
        val m = VisitorSessionManager(s, { error("not used") }, { 100 })
        assertFailsWith<IllegalStateException> { m.accept(session()) }
        assertNull(m.validSession())
    }
    @Test fun signOutWaitsForRefreshThenClearsIt(): Unit = runBlocking {
        val start = CompletableDeferred<Unit>(); val finish = CompletableDeferred<Unit>()
        val s = Store(session(120))
        val m = VisitorSessionManager(s, { start.complete(Unit); finish.await(); session(4000) }, { 100 })
        val refreshing = async { m.validSession() }; start.await()
        val signingOut = launch { m.clearLocalSession() }; yield(); finish.complete(Unit)
        refreshing.await(); signingOut.join(); assertNull(s.value); assertNull(m.validSession())
    }
    @Test fun userSwitchInRefreshFailsClosed(): Unit = runBlocking {
        val s = Store(session(120)); val m = VisitorSessionManager(s, { session(user = "other") }, { 100 })
        assertFailsWith<VisitorSessionRejected> { m.validSession() }; assertNull(s.value)
    }
    @Test fun credentialsNeverAppearInToString() {
        val s = session(); assertEquals("VisitorSession(redacted)", s.toString())
    }
}
