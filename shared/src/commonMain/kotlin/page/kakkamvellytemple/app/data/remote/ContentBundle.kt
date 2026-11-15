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
    val darshan: DarshanDto? = null,
) {
    /** True when at least one real content section came from the backend. An empty
     *  backend bundle (all sections null) still leaves the app on packaged defaults. */
    fun hasAnySection(): Boolean =
        morningPoojas != null || eveningPoojas != null || contacts != null ||
        vazhipad != null || todayEvents != null || festivals != null ||
        gallery != null || darshan != null
}

@Serializable data class PoojaDto(val nameMl: String, val nameEn: String, val time: String, val hour: Int, val minute: Int)
@Serializable data class ContactDto(val roleMl: String, val roleEn: String, val phone: String, val whatsapp: String? = null)
@Serializable data class VazhipadDto(val nameMl: String, val nameEn: String, val amount: Int, val description: String = "")
@Serializable data class TodayEventDto(val icon: String, val titleMl: String, val titleEn: String, val timeMl: String, val timeEn: String)
@Serializable data class FestivalDto(val nameMl: String, val nameEn: String, val icon: String, val dateUtcMs: Long, val noteMl: String = "", val isSpecial: Boolean = false)
@Serializable data class GalleryDto(val resName: String, val captionMl: String, val captionEn: String)
@Serializable data class DarshanDto(val morningOpenMin: Int, val morningCloseMin: Int, val eveningOpenMin: Int, val eveningCloseMin: Int)
