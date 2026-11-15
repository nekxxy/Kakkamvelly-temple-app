package page.kakkamvellytemple.app.data.repository

import page.kakkamvellytemple.app.data.remote.ContentBundle

/** Admin write path. Only a local mock exists until connected backend setup
 *  (Supabase/FCM credentials, auth scope) is approved by the owner. */
interface AdminRepository {
    val isConnected: Boolean
    suspend fun publishContent(bundle: ContentBundle): Result<Unit>
    suspend fun sendNotification(title: String, body: String, topic: String): Result<String>
}

/** Local-only demo implementation: content edits apply on-device via ContentStore and
 *  persist across restart in a local file; notifications are recorded in memory and
 *  never sent anywhere. */
class MockAdminRepository : AdminRepository {
    override val isConnected: Boolean = false
    val sentLog = mutableListOf<String>()

    override suspend fun publishContent(bundle: ContentBundle): Result<Unit> {
        ContentStore.install(bundle, persist = true)  // on-device only; survives restart
        return Result.success(Unit)
    }

    override suspend fun sendNotification(title: String, body: String, topic: String): Result<String> {
        val entry = "[topic:$topic] $title - $body"
        sentLog += entry
        return Result.success(entry)
    }
}
