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
import page.kakkamvellytemple.app.data.repository.StaticData

/** Demo-mode admin state. All writes go to MockAdminRepository (on-device only) until
 *  the connected backend and auth scope are approved. */
class AdminViewModel(
    private val repo: AdminRepository = MockAdminRepository(),
    private val auth: AuthRepository? = null,
) {

    private val scope = CoroutineScope(Dispatchers.Main)

    var events by mutableStateOf(currentEvents()); private set
    var darshan by mutableStateOf(currentDarshan()); private set
    var morningPoojas by mutableStateOf(currentPoojas(true)); private set
    var eveningPoojas by mutableStateOf(currentPoojas(false)); private set
    var vazhipad by mutableStateOf(currentVazhipad()); private set
    var contacts by mutableStateOf(currentContacts()); private set
    var festivals by mutableStateOf(currentFestivals()); private set
    var gallery by mutableStateOf(currentGallery()); private set
    var status by mutableStateOf<String?>(null); private set

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

    /** Publish a bundle carrying only the edited sections; untouched sections
     *  stay null and fall back to packaged defaults. Applied on-device only. */
    fun publish() {
        scope.launch {
            val bundle = ContentBundle(
                version = (ContentStore.bundle?.version ?: 0) + 1,
                updatedAt = "demo-local",
                morningPoojas = morningPoojas,
                eveningPoojas = eveningPoojas,
                todayEvents = events,
                darshan = darshan,
                vazhipad = vazhipad,
                contacts = contacts,
                festivals = festivals,
                gallery = gallery
            )
            // Demo mode: local-only mock backend, no concurrent editors, so the
            // snapshot carries no server revisions to trace.
            repo.publishContent(AdminContentSnapshot(bundle, emptyMap()))
                .onSuccess { status = "Applied on this device (demo). Nothing published - backend not connected." }
                .onFailure { status = "Failed: ${it.message}" }
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
