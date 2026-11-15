package page.kakkamvellytemple.app

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlin.test.*
import page.kakkamvellytemple.app.data.remote.AnnouncementDto
import page.kakkamvellytemple.app.data.remote.ContactDto
import page.kakkamvellytemple.app.data.remote.ContentBundle
import page.kakkamvellytemple.app.data.remote.DarshanDto
import page.kakkamvellytemple.app.data.remote.GalleryDto
import page.kakkamvellytemple.app.data.repository.AdminAuthState
import page.kakkamvellytemple.app.data.repository.AdminContentSnapshot
import page.kakkamvellytemple.app.data.repository.AdminRepository
import page.kakkamvellytemple.app.data.repository.AuthRepository
import page.kakkamvellytemple.app.data.repository.PublishReport
import page.kakkamvellytemple.app.presentation.viewmodel.AdminViewModel

/** Connected-mode contract for the admin editor view model. The demo path stays
 *  local-only; the connected path must gate editing on a signed-in admin, anchor
 *  publishes to a loaded snapshot's revisions, and surface partial publishes as
 *  partial - never as whole-bundle success. Fakes only; no network. */
class AdminViewModelTest {

    private class FakeRepo(
        override val isConnected: Boolean = true,
        var snapshot: AdminContentSnapshot? = null,
        var loadError: Throwable? = null,
        var publishReport: PublishReport = PublishReport(published = listOf("contacts")),
        var publishError: Throwable? = null,
    ) : AdminRepository {
        val publishCalls = mutableListOf<Map<String, Long>>()
        var loadCalls = 0
        override suspend fun loadSnapshot(): Result<AdminContentSnapshot> {
            loadCalls++
            loadError?.let { return Result.failure(it) }
            return Result.success(snapshot ?: AdminContentSnapshot(ContentBundle(), emptyMap()))
        }
        override suspend fun publishContent(snapshot: AdminContentSnapshot): Result<PublishReport> {
            publishCalls += snapshot.revisions
            publishError?.let { return Result.failure(it) }
            return Result.success(publishReport)
        }
        override suspend fun sendNotification(title: String, body: String, topic: String): Result<String> =
            Result.success("recorded")
    }

    private class FakeAuth(var s: AdminAuthState) : AuthRepository {
        override val state: AdminAuthState get() = s
        override suspend fun requestOtp(destination: String) = Result.success(Unit)
        override suspend fun verifyOtp(destination: String, code: String) = Result.success(Unit)
        override suspend fun signOut() { s = AdminAuthState.SignedOut }
    }

    private fun vm(repo: AdminRepository, auth: AuthRepository? = null) =
        AdminViewModel(repo, auth, CoroutineScope(Dispatchers.Unconfined))

    @Test fun demoModeEditsWithoutSignIn() {
        val v = vm(page.kakkamvellytemple.app.data.repository.MockAdminRepository())
        assertTrue(v.canEdit, "demo mode needs no account")
        assertFalse(v.isConnected)
    }

    @Test fun connectedWithoutSignInCannotEditOrPublish() {
        val repo = FakeRepo()
        val v = vm(repo, FakeAuth(AdminAuthState.SignedOut))
        assertFalse(v.canEdit, "signed-out connected mode hides editing")
        v.publish()
        assertTrue(repo.publishCalls.isEmpty(), "no publish call may fire for a signed-out account")
        assertTrue(v.status.orEmpty().contains("Sign in"), "status says why: ${v.status}")
    }

    @Test fun connectedPublishRequiresLoadedSnapshot() {
        val repo = FakeRepo()
        val v = vm(repo, FakeAuth(AdminAuthState.SignedIn("u1")))
        assertTrue(v.canEdit)
        v.publish()
        assertTrue(repo.publishCalls.isEmpty(), "publishing unloaded content must be refused")
        assertTrue(v.status.orEmpty().contains("reload"), "status says why: ${v.status}")
    }

    @Test fun loadFromBackendReplacesEditorStateAndAnchorsPublish() {
        val repo = FakeRepo(
            snapshot = AdminContentSnapshot(
                ContentBundle(
                    contacts = listOf(ContactDto("സെക്രട്ടറി", "Secretary", "+919446844145")),
                    announcements = listOf(AnnouncementDto("മന്ദിരം", "Temple", "ബ", "B", true)),
                ),
                mapOf("contacts" to 5L, "announcements" to 7L),
            )
        )
        val v = vm(repo, FakeAuth(AdminAuthState.SignedIn("u1")))
        v.loadFromBackend()
        assertEquals(1, repo.loadCalls)
        assertEquals("Secretary", v.contacts.single().roleEn)
        assertEquals("Temple", v.announcements.single().titleEn)
        assertFalse(v.loading)

        v.publish()
        assertEquals(listOf(mapOf("contacts" to 5L, "announcements" to 7L)), repo.publishCalls,
            "the publish carries the loaded snapshot's revisions")
        // A complete publish re-anchors by reloading for the next edit round.
        assertEquals(2, repo.loadCalls)
    }

    @Test fun loadFailureKeepsStateAndRefusesPublish() {
        val repo = FakeRepo(loadError = Exception("permission denied for function"))
        val v = vm(repo, FakeAuth(AdminAuthState.SignedIn("u1")))
        val before = v.contacts
        v.loadFromBackend()
        assertEquals(before, v.contacts, "a failed load never replaces editor state")
        assertTrue(v.status.orEmpty().contains("Could not load"), "status carries the denial: ${v.status}")
        v.publish()
        assertTrue(repo.publishCalls.isEmpty())
    }

    @Test fun galleryAddRemoveReorderAndValidation() {
        val v = vm(page.kakkamvellytemple.app.data.repository.MockAdminRepository())
        val initial = v.gallery.size
        assertTrue(initial > 0, "packaged gallery is non-empty")
        assertTrue(v.galleryErrors.isEmpty(), "packaged gallery is valid")
        v.addGalleryPhoto()
        assertEquals(initial + 1, v.gallery.size)
        assertTrue(v.galleryErrors.isNotEmpty(), "a blank photo id is an error until edited")
        v.updateGalleryPhoto(initial, GalleryDto("new-photo", "അ", "New"))
        assertTrue(v.galleryErrors.isEmpty(), "fixing the row clears its error: ${v.galleryErrors}")
        v.moveGalleryPhoto(initial, up = true)
        assertEquals("new-photo", v.gallery[initial - 1].resName)
        v.moveGalleryPhoto(initial - 1, up = true)  // already at top: no-op
        assertEquals("new-photo", v.gallery[initial - 1].resName)
        v.removeGalleryPhoto(initial - 1)
        assertEquals(initial, v.gallery.size)
    }

    @Test fun galleryCapStopsAdds() {
        val v = vm(page.kakkamvellytemple.app.data.repository.MockAdminRepository())
        val initial = v.gallery.size
        var guard = 0
        while (v.gallery.size < page.kakkamvellytemple.app.data.model.GalleryValidation.MAX_PHOTOS && guard++ < 100) {
            val i = v.gallery.size
            v.addGalleryPhoto()
            v.updateGalleryPhoto(i, GalleryDto("p$guard", "അ", "p$guard"))
        }
        val before = v.gallery.size
        v.addGalleryPhoto()
        assertEquals(before, v.gallery.size, "add beyond the cap is refused")
        assertTrue(v.status.orEmpty().contains("full"), "status says why: ${v.status}")
    }

    @Test fun galleryErrorsBlockDemoPublish() {
        val v = vm(page.kakkamvellytemple.app.data.repository.MockAdminRepository())
        v.addGalleryPhoto()  // blank photo id -> invalid
        v.publish()
        assertTrue(v.status.orEmpty().contains("gallery"), "status names the blocker: ${v.status}")
    }

    @Test fun galleryErrorsBlockConnectedPublishBeforeAnyCall() {
        val repo = FakeRepo(snapshot = AdminContentSnapshot(ContentBundle(), mapOf("contacts" to 5L)))
        val v = vm(repo, FakeAuth(AdminAuthState.SignedIn("u1")))
        v.loadFromBackend()
        v.addGalleryPhoto()
        v.publish()
        assertTrue(repo.publishCalls.isEmpty(), "invalid gallery must not reach the backend")
        assertTrue(v.status.orEmpty().contains("gallery"), "status names the blocker: ${v.status}")
    }

    @Test fun partialPublishIsSurfacedAsPartialNeverSuccess() {
        val repo = FakeRepo(
            snapshot = AdminContentSnapshot(ContentBundle(), mapOf("contacts" to 5L)),
            publishReport = PublishReport(
                published = listOf("contacts"),
                failed = "vazhipad",
                error = "revision_conflict: section vazhipad is at revision 4, caller expected 2",
                notAttempted = listOf("festivals"),
            ),
        )
        val v = vm(repo, FakeAuth(AdminAuthState.SignedIn("u1")))
        v.loadFromBackend()
        v.publish()
        val s = v.status.orEmpty()
        assertTrue(s.contains("PARTIAL"), "partial publish says PARTIAL: $s")
        assertTrue(s.contains("contacts"), "succeeded sections named: $s")
        assertTrue(s.contains("vazhipad"), "failed section named: $s")
        assertTrue(s.contains("festivals"), "unattempted sections named: $s")
        assertFalse(s.contains("Awaiting committee verification"), "no whole-success wording on a partial: $s")
    }
}
