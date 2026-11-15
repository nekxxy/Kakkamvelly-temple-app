package page.kakkamvellytemple.app.data.repository

import page.kakkamvellytemple.app.data.remote.ContentBundle

/** Content plus the server revision of each section AT THE MOMENT it was loaded.
 *  Publishing hands these revisions back so the backend rejects a publish that
 *  would overwrite edits another admin made after the load (stale-edit
 *  protection); re-reading revisions at publish time would defeat it. */
class AdminContentSnapshot(
    val bundle: ContentBundle,
    val revisions: Map<String, Long>,
)

/** Admin write path. Only a local mock exists until connected backend setup
 *  (Supabase/FCM credentials, auth scope) is approved by the owner. */
interface AdminRepository {
    val isConnected: Boolean
    /** Load the current content with its per-section revisions for editing. */
    suspend fun loadSnapshot(): Result<AdminContentSnapshot>
    /** Publish the edited bundle; [AdminContentSnapshot.revisions] must be the
     *  revisions captured when the content being edited was loaded. */
    suspend fun publishContent(snapshot: AdminContentSnapshot): Result<Unit>
    suspend fun sendNotification(title: String, body: String, topic: String): Result<String>
}

/** Local-only demo implementation: content edits apply on-device via ContentStore and
 *  persist across restart in a local file; notifications are recorded in memory and
 *  never sent anywhere. */
class MockAdminRepository : AdminRepository {
    override val isConnected: Boolean = false
    val sentLog = mutableListOf<String>()

    override suspend fun loadSnapshot(): Result<AdminContentSnapshot> =
        Result.success(AdminContentSnapshot(ContentStore.bundle ?: ContentBundle(), emptyMap()))

    override suspend fun publishContent(snapshot: AdminContentSnapshot): Result<Unit> {
        ContentStore.install(snapshot.bundle, persist = true)  // on-device only; survives restart
        return Result.success(Unit)
    }

    override suspend fun sendNotification(title: String, body: String, topic: String): Result<String> {
        val entry = "[topic:$topic] $title - $body"
        sentLog += entry
        return Result.success(entry)
    }
}
