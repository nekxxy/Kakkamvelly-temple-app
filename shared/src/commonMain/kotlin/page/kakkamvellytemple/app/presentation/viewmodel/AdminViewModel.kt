package page.kakkamvellytemple.app.presentation.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import page.kakkamvellytemple.app.data.remote.*
import page.kakkamvellytemple.app.data.repository.AdminAuthState
import page.kakkamvellytemple.app.data.repository.AdminContentSnapshot
import page.kakkamvellytemple.app.data.repository.AdminRepository
import page.kakkamvellytemple.app.data.repository.AuthRepository
import page.kakkamvellytemple.app.data.repository.ContentStore
import page.kakkamvellytemple.app.data.repository.MockAdminRepository
import page.kakkamvellytemple.app.data.repository.PublishReport
import page.kakkamvellytemple.app.data.repository.StaticData

/** Admin editor state. Two isolated modes:
 *  - DEMO (repo not connected, e.g. MockAdminRepository): everything edits and
 *    "publishes" on-device only; no account needed; the UI labels it demo.
 *  - CONNECTED (repo.isConnected): editing requires a signed-in admin
 *    ([canEdit]); content is loaded via loadSnapshot and published with that
 *    snapshot's revisions, and per-section results are shown - never a
 *    whole-bundle success for a partial publish. The demo entry point shares
 *    no state with this path, so the unauthenticated demo cannot become a
 *    production admin route when a real repo is injected: without a signed-in
 *    admin the editors stay hidden behind the sign-in card. */
class AdminViewModel(
    private val repo: AdminRepository = MockAdminRepository(),
    private val auth: AuthRepository? = null,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main),
) {

    var events by mutableStateOf(currentEvents()); private set
    var darshan by mutableStateOf(currentDarshan()); private set
    var morningPoojas by mutableStateOf(currentPoojas(true)); private set
    var eveningPoojas by mutableStateOf(currentPoojas(false)); private set
    var vazhipad by mutableStateOf(currentVazhipad()); private set
    var contacts by mutableStateOf(currentContacts()); private set
    var festivals by mutableStateOf(currentFestivals()); private set
    var gallery by mutableStateOf(currentGallery()); private set
    var announcements by mutableStateOf(currentAnnouncements()); private set
    var status by mutableStateOf<String?>(null); private set
    var loading by mutableStateOf(false); private set

    /** The snapshot loaded from the connected backend; null in demo mode and
     *  whenever a connected load has not completed (or failed). Connected
     *  publishing is refused while this is null - publishing packaged defaults
     *  over live content with no traced revisions would be exactly the stale
     *  overwrite the revision protocol exists to prevent. */
    private var loadedSnapshot: AdminContentSnapshot? = null

    /** Editing is open in demo mode, or in connected mode only for a signed-in
     *  admin. The backend re-verifies membership on every write; this gate keeps
     *  the UI honest about who can even attempt it. */
    val canEdit: Boolean
        get() = !repo.isConnected || auth?.state is AdminAuthState.SignedIn

    val isConnected get() = repo.isConnected

    /** Null when no auth repository is wired: the sign-in UI stays hidden. */
    val authState: AdminAuthState? get() = auth?.state

    fun requestOtp(destination: String) {
        val a = auth ?: return
        scope.launch {
            a.requestOtp(destination)
                .onFailure { status = "Sign-in: ${it.message}" }
        }
    }

    fun verifyOtp(destination: String, code: String) {
        val a = auth ?: return
        scope.launch {
            a.verifyOtp(destination, code)
                .onFailure { status = "Sign-in: ${it.message}" }
        }
    }

    fun signOut() {
        val a = auth ?: return
        scope.launch { a.signOut() }
    }

    private fun currentEvents() = StaticData.TODAY_EVENTS.map {
        TodayEventDto(it.icon, it.titleMl, it.titleEn, it.timeMl, it.timeEn)
    }
    private fun currentDarshan() = DarshanDto(
        StaticData.MORNING_OPEN, StaticData.MORNING_CLOSE,
        StaticData.EVENING_OPEN, StaticData.EVENING_CLOSE
    )
    private fun currentPoojas(morning: Boolean) =
        (if (morning) StaticData.MORNING_POOJAS else StaticData.EVENING_POOJAS).map {
            PoojaDto(it.nameMl, it.nameEn, it.time, it.hour, it.minute)
        }
    private fun currentVazhipad() = StaticData.VAZHIPAD_ITEMS.map {
        VazhipadDto(it.nameMl, it.nameEn, it.amount, it.description)
    }
    private fun currentContacts() = StaticData.CONTACTS.map {
        ContactDto(it.roleMl, it.roleEn, it.phone, it.whatsapp)
    }
    private fun currentFestivals() = StaticData.FESTIVALS.map {
        FestivalDto(it.nameMl, it.nameEn, it.icon, it.dateUtcMs, it.noteMl, it.isSpecial)
    }
    private fun currentAnnouncements(): List<AnnouncementDto> =
        ContentStore.bundle?.announcements ?: emptyList()

    private fun currentGallery() = StaticData.GALLERY_PHOTOS.map {
        GalleryDto(it.resName, it.captionMl, it.captionEn)
    }

    fun addEvent(e: TodayEventDto) { events = events + e }
    fun removeEvent(index: Int) { events = events.filterIndexed { i, _ -> i != index } }
    fun updateDarshan(d: DarshanDto) { darshan = d }
    fun updatePooja(morning: Boolean, index: Int, p: PoojaDto) {
        if (morning) morningPoojas = morningPoojas.mapIndexed { i, x -> if (i == index) p else x }
        else eveningPoojas = eveningPoojas.mapIndexed { i, x -> if (i == index) p else x }
    }
    fun updateVazhipad(index: Int, v: VazhipadDto) {
        vazhipad = vazhipad.mapIndexed { i, x -> if (i == index) v else x }
    }
    fun updateContact(index: Int, c: ContactDto) {
        contacts = contacts.mapIndexed { i, x -> if (i == index) c else x }
    }
    fun updateFestival(index: Int, f: FestivalDto) {
        festivals = festivals.mapIndexed { i, x -> if (i == index) f else x }
    }
    fun updateGalleryCaption(index: Int, g: GalleryDto) {
        gallery = gallery.mapIndexed { i, x -> if (i == index) g else x }
    }
    fun addAnnouncement(a: AnnouncementDto) { announcements = announcements + a }
    fun removeAnnouncement(index: Int) { announcements = announcements.filterIndexed { i, _ -> i != index } }
    fun updateAnnouncement(index: Int, a: AnnouncementDto) {
        announcements = announcements.mapIndexed { i, x -> if (i == index) a else x }
    }

    /** Connected mode only: load the live content with its traced revisions.
     *  Success replaces every editor's state with the backend's content;
     *  sections the backend does not supply keep their current (default) state.
     *  Failure (including admin denial) leaves state untouched and surfaces the
     *  error; publishing stays refused until a load succeeds. */
    fun loadFromBackend() {
        if (!repo.isConnected || loading) return
        scope.launch {
            loading = true
            status = null
            repo.loadSnapshot()
                .onSuccess { snap ->
                    loadedSnapshot = snap
                    snap.bundle.let { b ->
                        b.todayEvents?.let { events = it }
                        b.darshan?.let { darshan = it }
                        b.morningPoojas?.let { morningPoojas = it }
                        b.eveningPoojas?.let { eveningPoojas = it }
                        b.vazhipad?.let { vazhipad = it }
                        b.contacts?.let { contacts = it }
                        b.festivals?.let { festivals = it }
                        b.gallery?.let { gallery = it }
                        b.announcements?.let { announcements = it }
                    }
                    status = "Loaded current content from backend"
                }
                .onFailure {
                    loadedSnapshot = null
                    status = "Could not load content: ${it.message}"
                }
            loading = false
        }
    }

    /** Publish the edited sections. Demo mode applies on-device only with no
     *  revisions to trace. Connected mode requires a signed-in admin and a
     *  previously loaded snapshot, and reports the per-section outcome - a
     *  partial publish is surfaced as partial, never as success. */
    fun publish() {
        if (repo.isConnected && !canEdit) {
            status = "Sign in as a temple admin before publishing"
            return
        }
        scope.launch {
            val bundle = ContentBundle(
                version = (ContentStore.bundle?.version ?: 0) + 1,
                updatedAt = if (repo.isConnected) "" else "demo-local",
                morningPoojas = morningPoojas,
                eveningPoojas = eveningPoojas,
                todayEvents = events,
                darshan = darshan,
                vazhipad = vazhipad,
                contacts = contacts,
                festivals = festivals,
                gallery = gallery,
                announcements = announcements
            )
            if (!repo.isConnected) {
                // Demo mode: local-only mock backend, no concurrent editors, so the
                // snapshot carries no server revisions to trace.
                repo.publishContent(AdminContentSnapshot(bundle, emptyMap()))
                    .onSuccess { status = "Applied on this device (demo). Nothing published - backend not connected." }
                    .onFailure { status = "Failed: ${it.message}" }
                return@launch
            }
            val snap = loadedSnapshot
            if (snap == null) {
                status = "Content not loaded from the backend yet - reload before publishing"
                return@launch
            }
            repo.publishContent(AdminContentSnapshot(bundle, snap.revisions))
                .onSuccess { report ->
                    status = renderReport(report)
                    if (report.isComplete) loadFromBackend()  // re-anchor revisions for the next edit
                }
                .onFailure { status = "Publish failed: ${it.message}" }
        }
    }

    private fun renderReport(r: PublishReport): String = buildString {
        if (r.isComplete) {
            append("Published: ")
            append(r.published.joinToString(", "))
            append(". Awaiting committee verification before visitors see changes.")
        } else {
            if (r.published.isNotEmpty()) {
                append("PARTIAL publish. Succeeded: ")
                append(r.published.joinToString(", "))
                append(". ")
            } else {
                append("Publish failed before any section was written. ")
            }
            append("Failed at ")
            append(r.failed)
            append(": ")
            append(r.error)
            if (r.notAttempted.isNotEmpty()) {
                append(". Not attempted: ")
                append(r.notAttempted.joinToString(", "))
            }
            append(".")
        }
    }

    fun sendNotification(title: String, body: String, topic: String) {
        scope.launch {
            repo.sendNotification(title, body, topic)
                .onSuccess { status = "Recorded only (demo). No notification sent - backend not connected." }
                .onFailure { status = "Failed: ${it.message}" }
        }
    }
}
