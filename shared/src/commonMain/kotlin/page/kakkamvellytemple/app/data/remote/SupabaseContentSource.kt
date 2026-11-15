package page.kakkamvellytemple.app.data.remote

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** PostgREST content source for the editable temple content bundle.
 *
 *  v1 maps the tables provisioned in backend schema v1+v3 (project yaizrrccoeiagvvaywsx):
 *  darshan_settings, poojas, contacts, events, gallery, vazhipad, festivals.
 *  `announcements` exists in the schema but has no bundle section yet.
 *
 *  Every table that returns zero rows maps to a null section, so an empty backend
 *  (content pending committee verification) never overrides packaged defaults.
 *
 *  Credentials are injected at app start (BuildConfig <- env/backend.properties);
 *  nothing is committed to the repo. All access is select-only under public-read RLS. */
object SupabaseContentSource {

    data class Settings(val url: String, val anonKey: String) {
        val isConfigured: Boolean get() = url.isNotBlank() && anonKey.isNotBlank()
    }

    var settings: Settings? = null
        private set

    fun configure(url: String, anonKey: String) {
        settings = Settings(url.trimEnd('/'), anonKey).takeIf { it.isConfigured }
    }

    @Serializable
    private data class DarshanRow(
        @SerialName("morning_open_min") val morningOpenMin: Int,
        @SerialName("morning_close_min") val morningCloseMin: Int,
        @SerialName("evening_open_min") val eveningOpenMin: Int,
        @SerialName("evening_close_min") val eveningCloseMin: Int,
        @SerialName("updated_at") val updatedAt: String? = null,
    )

    @Serializable
    private data class PoojaRow(
        val session: String,
        @SerialName("name_ml") val nameMl: String,
        @SerialName("name_en") val nameEn: String,
        @SerialName("time_label") val timeLabel: String? = null,
        val hour: Int? = null,
        val minute: Int? = null,
    )

    @Serializable
    private data class ContactRow(
        @SerialName("role_ml") val roleMl: String,
        @SerialName("role_en") val roleEn: String,
        val phone: String? = null,
        val whatsapp: String? = null,
    )

    @Serializable
    private data class EventRow(
        val icon: String? = null,
        @SerialName("title_ml") val titleMl: String,
        @SerialName("title_en") val titleEn: String,
        @SerialName("time_ml") val timeMl: String? = null,
        @SerialName("time_en") val timeEn: String? = null,
    )

    @Serializable
    private data class GalleryRow(
        @SerialName("photo_path") val photoPath: String? = null,
        @SerialName("caption_ml") val captionMl: String? = null,
        @SerialName("caption_en") val captionEn: String? = null,
    )

    @Serializable
    private data class VazhipadRow(
        @SerialName("name_ml") val nameMl: String,
        @SerialName("name_en") val nameEn: String,
        @SerialName("amount_inr") val amountInr: Double,
        @SerialName("description_ml") val descriptionMl: String? = null,
    )

    @Serializable
    private data class FestivalRow(
        @SerialName("name_ml") val nameMl: String,
        @SerialName("name_en") val nameEn: String,
        val icon: String? = null,
        @SerialName("date_utc_ms") val dateUtcMs: Long,
        @SerialName("note_ml") val noteMl: String? = null,
        @SerialName("is_special") val isSpecial: Boolean = false,
    )

    @Serializable
    private data class MetaRow(val section: String, val verified: Boolean = false)

    /** Markers that must ALL be present and verified=true before the bundle itself
     *  counts as verified. An absent, false or unreadable marker keeps the whole bundle
     *  unverified, so the app notice stays up until every displayed section is
     *  explicitly committee-verified in the backend. */
    private val REQUIRED_META_SECTIONS = listOf(
        "darshan_settings", "poojas", "contacts", "events", "gallery", "vazhipad", "festivals",
    )

    private suspend inline fun <reified T> getRows(client: HttpClient, s: Settings, path: String): T =
        client.get("${s.url}/rest/v1/$path") {
            header("apikey", s.anonKey)
            header("Authorization", "Bearer ${s.anonKey}")
        }.body()

    /** Fetch and assemble the content bundle. Throws on HTTP/parse errors; caller decides fallback.
     *  [forSettings] overrides the global configuration (used by the admin snapshot load,
     *  which must not mutate the app-wide singleton). */
    suspend fun fetchBundle(client: HttpClient, forSettings: Settings? = null): ContentBundle {
        val s = forSettings ?: settings ?: error("SupabaseContentSource not configured")

        val darshanRows: List<DarshanRow> = getRows(client, s, "darshan_settings?select=*")
        val poojaRows: List<PoojaRow> = getRows(client, s, "poojas?active=eq.true&order=sort_order.asc")
        val contactRows: List<ContactRow> = getRows(client, s, "contacts?order=sort_order.asc")
        val eventRows: List<EventRow> = getRows(client, s, "events?active=eq.true")
        val galleryRows: List<GalleryRow> = getRows(client, s, "gallery?active=eq.true&order=sort_order.asc")
        val vazhipadRows: List<VazhipadRow> = getRows(client, s, "vazhipad?active=eq.true&order=sort_order.asc")
        val festivalRows: List<FestivalRow> = getRows(client, s, "festivals?active=eq.true&order=date_utc_ms.asc")

        // Verification markers are authoritative and conservative: any fetch failure,
        // absent marker or false value means unverified.
        val metaVerified: Boolean = try {
            val metaRows: List<MetaRow> = getRows(client, s, "content_meta?select=section,verified")
            val bySection = metaRows.associate { it.section to it.verified }
            REQUIRED_META_SECTIONS.all { bySection[it] == true }
        } catch (t: Throwable) {
            false
        }

        val darshanRow = darshanRows.firstOrNull()

        fun List<PoojaRow>.toDtos(): List<PoojaDto> = mapNotNull { r ->
            val h = r.hour; val m = r.minute
            if (h == null || m == null) null
            else PoojaDto(r.nameMl, r.nameEn, r.timeLabel ?: "", h, m)
        }

        return ContentBundle(
            // darshan_settings.updated_at is the backend's content version marker; fall back to fetch time.
            version = darshanRow?.updatedAt
                ?.let { runCatching { Instant.parse(it).epochSeconds.toInt() }.getOrNull() }
                ?: Clock.System.now().epochSeconds.toInt(),
            updatedAt = darshanRow?.updatedAt ?: "",
            morningPoojas = poojaRows.filter { it.session == "morning" }.toDtos().ifEmpty { null },
            eveningPoojas = poojaRows.filter { it.session == "evening" }.toDtos().ifEmpty { null },
            contacts = contactRows.mapNotNull { r ->
                r.phone?.let { ContactDto(r.roleMl, r.roleEn, it, r.whatsapp) }
            }.ifEmpty { null },
            todayEvents = eventRows.map {
                TodayEventDto(it.icon ?: "\uD83D\uDE4F", it.titleMl, it.titleEn, it.timeMl ?: "", it.timeEn ?: "")
            }.ifEmpty { null },
            gallery = galleryRows.mapNotNull { r ->
                r.photoPath?.let { GalleryDto(it, r.captionMl ?: "", r.captionEn ?: "") }
            }.ifEmpty { null },
            darshan = darshanRow?.let {
                DarshanDto(it.morningOpenMin, it.morningCloseMin, it.eveningOpenMin, it.eveningCloseMin)
            },
            vazhipad = vazhipadRows.map {
                VazhipadDto(it.nameMl, it.nameEn, it.amountInr.toInt(), it.descriptionMl ?: "")
            }.ifEmpty { null },
            festivals = festivalRows.map {
                FestivalDto(it.nameMl, it.nameEn, it.icon ?: "\uD83C\uDF38", it.dateUtcMs, it.noteMl ?: "", it.isSpecial)
            }.ifEmpty { null },
            verified = metaVerified,
        )
    }
}
