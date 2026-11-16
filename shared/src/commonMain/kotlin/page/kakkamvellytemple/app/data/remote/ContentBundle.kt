package page.kakkamvellytemple.app.data.remote

import kotlinx.serialization.Serializable

/** Remote content bundle fetched from CONTENT_URL (JSON). Every section is optional;
 *  a missing/null section falls back to the built-in defaults in StaticData. */
@Serializable
data class ContentBundle(
    val version: Int = 0,
    val updatedAt: String = "",
    val morningPoojas: List<PoojaDto>? = null,
    val eveningPoojas: List<PoojaDto>? = null,
    val contacts: List<ContactDto>? = null,
    val vazhipad: List<VazhipadDto>? = null,
    val todayEvents: List<TodayEventDto>? = null,
    val festivals: List<FestivalDto>? = null,
    val gallery: List<GalleryDto>? = null,
    val uploadedGallery: List<UploadedGalleryDto>? = null,
    val announcements: List<AnnouncementDto>? = null,
    val darshan: DarshanDto? = null,
    /** Explicit committee-verification marker from the backend. Remote origin alone
     *  does NOT imply approval: until the backend declares verified=true, the app
     *  keeps the unverified-content notice up. */
    val verified: Boolean = false,
) {
    companion object {
        val ALL_SECTIONS = listOf(
            "darshan", "morningPoojas", "eveningPoojas", "contacts",
            "vazhipad", "todayEvents", "festivals", "gallery", "announcements"
        )
    }

    /** Names of sections this bundle actually supplies (non-null). */
    fun suppliedSections(): Set<String> = buildSet {
        if (darshan != null) add("darshan")
        if (morningPoojas != null) add("morningPoojas")
        if (eveningPoojas != null) add("eveningPoojas")
        if (contacts != null) add("contacts")
        if (vazhipad != null) add("vazhipad")
        if (todayEvents != null) add("todayEvents")
        if (festivals != null) add("festivals")
        if (gallery != null) add("gallery")
        if (announcements != null) add("announcements")
    }

    /** True when at least one real content section came from the backend. An empty
     *  backend bundle (all sections null) still leaves the app on packaged defaults. */
    fun hasAnySection(): Boolean = suppliedSections().isNotEmpty() || uploadedGallery?.isNotEmpty() == true
}

@Serializable data class PoojaDto(val nameMl: String, val nameEn: String, val time: String, val hour: Int, val minute: Int)
@Serializable data class ContactDto(val roleMl: String, val roleEn: String, val phone: String, val whatsapp: String? = null)
@Serializable data class VazhipadDto(val nameMl: String, val nameEn: String, val amount: Int, val description: String = "")
@Serializable data class TodayEventDto(val icon: String, val titleMl: String, val titleEn: String, val timeMl: String, val timeEn: String)
@Serializable data class FestivalDto(val nameMl: String, val nameEn: String, val icon: String, val dateUtcMs: Long, val noteMl: String = "", val isSpecial: Boolean = false)
@Serializable data class GalleryDto(val resName: String, val captionMl: String, val captionEn: String)
@Serializable data class AnnouncementDto(val titleMl: String, val titleEn: String, val bodyMl: String = "", val bodyEn: String = "", val active: Boolean = true)
@Serializable data class DarshanDto(val morningOpenMin: Int, val morningCloseMin: Int, val eveningOpenMin: Int, val eveningCloseMin: Int)

@Serializable data class UploadedGalleryDto(val assetId: String, val publicationId: String, val captionMl: String, val captionEn: String)
