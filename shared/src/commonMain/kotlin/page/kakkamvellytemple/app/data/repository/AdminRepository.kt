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

/** Per-section outcome of a publish. Publishing is section-atomic but NOT
 *  bundle-atomic: when a section fails, earlier sections are already published
 *  and later sections were never attempted. Whole-bundle success is only
 *  [isComplete]; anything else must be presented as a partial result, never as
 *  "content published". */
class PublishReport(
    /** Sections committed, in publish order. */
    val published: List<String>,
    /** Section whose publish failed, or null on complete success. */
    val failed: String? = null,
    /** Failure detail for [failed]. */
    val error: String? = null,
    /** Sections after [failed] that were never attempted. */
    val notAttempted: List<String> = emptyList(),
) {
    val isComplete: Boolean get() = failed == null
}

/** Admin write path. Only a local mock exists until connected backend setup
 *  (Supabase/FCM credentials, auth scope) is approved by the owner. */
interface AdminRepository {
    val isConnected: Boolean
    /** Load the current content with its per-section revisions for editing. */
    suspend fun loadSnapshot(): Result<AdminContentSnapshot>
    /** Publish the edited bundle; [AdminContentSnapshot.revisions] must be the
     *  revisions captured when the content being edited was loaded. */
    suspend fun publishContent(snapshot: AdminContentSnapshot): Result<PublishReport>
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

    override suspend fun publishContent(snapshot: AdminContentSnapshot): Result<PublishReport> {
        ContentStore.install(snapshot.bundle, persist = true)  // on-device only; survives restart
        val sections = ContentBundle.ALL_SECTIONS.filter { it in snapshot.bundle.suppliedSections() }
        return Result.success(PublishReport(published = sections))
    }

    override suspend fun sendNotification(title: String, body: String, topic: String): Result<String> {
        val entry = "[topic:$topic] $title - $body"
        sentLog += entry
        return Result.success(entry)
    }
}
