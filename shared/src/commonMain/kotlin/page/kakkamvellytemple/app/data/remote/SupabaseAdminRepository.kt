package page.kakkamvellytemple.app.data.remote

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import page.kakkamvellytemple.app.data.repository.AdminAuthTokenProvider
import page.kakkamvellytemple.app.data.repository.AdminDeniedException
import page.kakkamvellytemple.app.data.repository.AdminRepository
import page.kakkamvellytemple.app.data.repository.NotApprovedException

/** Admin write path over PostgREST against the server-side default-deny RLS schema.
 *
 *  Role membership lives only in the backend `admin_users` table, managed through
 *  the service role; no client route can change it. Every write carries the signed-in
 *  admin's access token and the database itself denies anon and non-admin writes.
 *
 *  PostgREST returns success with an empty result set for UPDATE/DELETE that fail the
 *  RLS USING clause, so every write requests return=representation and an empty
 *  representation is treated as denial, never as success. */
class SupabaseAdminRepository(
    private val settings: SupabaseContentSource.Settings,
    private val tokenProvider: AdminAuthTokenProvider,
    private val client: HttpClient,
) : AdminRepository {

    override val isConnected: Boolean get() = settings.isConfigured

    private val json = Json { ignoreUnknownKeys = true }

    @Serializable private data class AdminUserRow(val user_id: String)
    @Serializable private data class DarshanWriteRow(
        val id: Int = 1,
        val morning_open_min: Int, val morning_close_min: Int,
        val evening_open_min: Int, val evening_close_min: Int,
    )
    @Serializable private data class PoojaWriteRow(
        val session: String, val name_ml: String, val name_en: String,
        val time_label: String, val hour: Int, val minute: Int,
    )
    @Serializable private data class ContactWriteRow(
        val role_ml: String, val role_en: String, val phone: String, val whatsapp: String? = null,
    )
    @Serializable private data class EventWriteRow(
        val icon: String?, val title_ml: String, val title_en: String,
        val time_ml: String, val time_en: String,
    )
    @Serializable private data class GalleryWriteRow(
        val photo_path: String, val caption_ml: String, val caption_en: String,
    )

    private fun HttpRequestBuilder.adminHeaders(token: String) {
        header("apikey", settings.anonKey)
        header("Authorization", "Bearer $token")
    }

    /** True only when the backend confirms the current user's admin membership row.
     *  Any doubt - no token, HTTP error, empty result - is false. */
    suspend fun currentUserIsAdmin(): Boolean {
        val token = tokenProvider.accessToken() ?: return false
        return try {
            val rows: List<AdminUserRow> = client.get("${settings.url}/rest/v1/admin_users?select=user_id") {
                adminHeaders(token)
            }.body()
            rows.isNotEmpty()
        } catch (t: Throwable) {
            false
        }
    }

    /** Upsert rows and require the affected rows back. 401/403 or an empty
     *  representation both map to AdminDeniedException. */
    private suspend fun writeRows(token: String, path: String, bodyJson: String) {
        val resp = client.post("${settings.url}/rest/v1/$path") {
            adminHeaders(token)
            contentType(ContentType.Application.Json)
            header("Prefer", "resolution=merge-duplicates,return=representation")
            setBody(bodyJson)
        }
        if (resp.status == HttpStatusCode.Unauthorized || resp.status == HttpStatusCode.Forbidden) {
            throw AdminDeniedException("Backend denied the write (HTTP ${resp.status.value})")
        }
        if (!resp.status.isSuccess()) {
            throw Exception("Backend write failed (HTTP ${resp.status.value})")
        }
        val rows: List<JsonElement> = resp.body()
        if (rows.isEmpty()) {
            throw AdminDeniedException("Backend applied zero rows; treated as denial")
        }
    }

    /** Publishes the supported sections of a bundle. Upsert semantics only: rows
     *  removed from the bundle are not deleted yet (tables are empty pre-launch;
     *  replace semantics land with real content). vazhipad and festivals have no
     *  backend tables and are rejected rather than silently dropped. */
    override suspend fun publishContent(bundle: ContentBundle): Result<Unit> = runCatching {
        val token = tokenProvider.accessToken()
            ?: throw AdminDeniedException("No signed-in admin session")
        if (!currentUserIsAdmin()) throw AdminDeniedException("Account is not a temple admin")

        val unsupported = buildList {
            if (bundle.vazhipad != null) add("vazhipad")
            if (bundle.festivals != null) add("festivals")
        }
        if (unsupported.isNotEmpty()) {
            throw UnsupportedOperationException("No backend tables for: ${unsupported.joinToString()}")
        }

        bundle.darshan?.let { d ->
            writeRows(token, "darshan_settings", json.encodeToString(listOf(
                DarshanWriteRow(1, d.morningOpenMin, d.morningCloseMin, d.eveningOpenMin, d.eveningCloseMin)
            )))
        }
        val poojas = (bundle.morningPoojas.orEmpty().map { p ->
            PoojaWriteRow("morning", p.nameMl, p.nameEn, p.time, p.hour, p.minute)
        } + bundle.eveningPoojas.orEmpty().map { p ->
            PoojaWriteRow("evening", p.nameMl, p.nameEn, p.time, p.hour, p.minute)
        })
        if (bundle.morningPoojas != null || bundle.eveningPoojas != null) {
            writeRows(token, "poojas", json.encodeToString(poojas))
        }
        bundle.contacts?.let { contacts ->
            writeRows(token, "contacts", json.encodeToString(contacts.map {
                ContactWriteRow(it.roleMl, it.roleEn, it.phone, it.whatsapp)
            }))
        }
        bundle.todayEvents?.let { events ->
            writeRows(token, "events", json.encodeToString(events.map {
                EventWriteRow(it.icon, it.titleMl, it.titleEn, it.timeMl, it.timeEn)
            }))
        }
        bundle.gallery?.let { gallery ->
            writeRows(token, "gallery", json.encodeToString(gallery.map {
                GalleryWriteRow(it.resName, it.captionMl, it.captionEn)
            }))
        }
    }

    /** Real notification sends stay disabled; this never touches a provider. */
    override suspend fun sendNotification(title: String, body: String, topic: String): Result<String> =
        Result.failure(NotApprovedException("Real notification sends are disabled"))
}
