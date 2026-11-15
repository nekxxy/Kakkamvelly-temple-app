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
import page.kakkamvellytemple.app.data.repository.AdminContentSnapshot
import page.kakkamvellytemple.app.data.repository.AdminDeniedException
import page.kakkamvellytemple.app.data.repository.AdminRepository
import page.kakkamvellytemple.app.data.repository.NotApprovedException
import page.kakkamvellytemple.app.data.repository.RevisionConflictException

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
    @Serializable private data class VazhipadWriteRow(
        val name_ml: String, val name_en: String, val amount_inr: Int, val description_ml: String,
    )
    @Serializable private data class FestivalWriteRow(
        val name_ml: String, val name_en: String, val icon: String,
        val date_utc_ms: Long, val note_ml: String, val is_special: Boolean,
    )
    @Serializable private data class ContentMetaRow(val section: String, val revision: Long)

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

    /** PostgREST error envelope for RPC failures. */
    @Serializable private data class RpcError(val code: String? = null, val message: String? = null)

    /** Loads the current content AND its per-section revisions as one snapshot for
     *  the admin editor. The revisions captured here are the ones publishContent
     *  sends back as expectations, so a competing publish that landed after this
     *  load is detected as a conflict instead of silently overwritten. */
    override suspend fun loadSnapshot(): Result<AdminContentSnapshot> = runCatching {
        val token = tokenProvider.accessToken()
            ?: throw AdminDeniedException("No signed-in admin session")
        if (!currentUserIsAdmin()) throw AdminDeniedException("Account is not a temple admin")
        // Bracket the content fetch with two revision reads. Revisions only
        // increment on publish, so identical before/after revisions prove the
        // content fetched between them is one consistent snapshot. A publish
        // landing mid-load bumps a revision and the load retries; a load that
        // keeps racing fails as a retryable conflict instead of returning
        // mixed old/new content labeled with a single revision set.
        val maxAttempts = 3
        var attempt = 0
        var snapshot: AdminContentSnapshot? = null
        while (snapshot == null) {
            attempt++
            val before = currentRevisions(token)
            val bundle = SupabaseContentSource.fetchBundle(client, settings)
            val after = currentRevisions(token)
            if (before == after) {
                snapshot = AdminContentSnapshot(bundle, before)
            } else if (attempt >= maxAttempts) {
                throw RevisionConflictException("Content changed repeatedly while loading; please try again")
            }
        }
        snapshot
    }

    /** Current per-section revisions. Used by loadSnapshot so the expected revision
     *  of a publish traces to the content snapshot the admin actually loaded and
     *  edited - never re-read at publish time, which would rubber-stamp
     *  intervening edits. content_meta is public-read; the admin token is sent
     *  anyway for a consistent audit trail. */
    private suspend fun currentRevisions(token: String): Map<String, Long> {
        val resp = client.get("${settings.url}/rest/v1/content_meta?select=section,revision") {
            adminHeaders(token)
        }
        if (!resp.status.isSuccess()) {
            throw Exception("Could not read section revisions (HTTP ${resp.status.value})")
        }
        val rows: List<ContentMetaRow> = resp.body()
        return rows.associate { it.section to it.revision }
    }

    /** One transactional publish RPC call. The database verifies caller admin
     *  membership again inside the definer function, locks the section row, and
     *  rejects stale overwrites; any failure rolls the section's replace back, so
     *  existing rows survive an error. */
    private suspend fun publishRpc(token: String, fn: String, expectedRevision: Long, rowsJsonElement: String) {
        val body = """{"p_rows":$rowsJsonElement,"p_expected_revision":$expectedRevision}"""
        callRpc(token, fn, body)
    }

    private suspend fun callRpc(token: String, fn: String, bodyJson: String) {
        val resp = client.post("${settings.url}/rest/v1/rpc/$fn") {
            adminHeaders(token)
            contentType(ContentType.Application.Json)
            setBody(bodyJson)
        }
        if (resp.status.isSuccess()) return
        val errText = resp.bodyAsText()
        val err = try { json.decodeFromString(RpcError.serializer(), errText) } catch (t: Throwable) { RpcError() }
        when {
            resp.status == HttpStatusCode.Unauthorized || resp.status == HttpStatusCode.Forbidden || err.code == "42501" ->
                throw AdminDeniedException("Backend denied the publish (HTTP ${resp.status.value} ${err.code ?: ""})".trim())
            err.code == "40001" ->
                throw RevisionConflictException(err.message ?: "Section was changed by another publish; re-read and retry")
            err.code == "22023" ->
                throw IllegalStateException("Publish rejected: ${err.message ?: "invalid payload"}")
            resp.status == HttpStatusCode.NotFound ->
                throw IllegalStateException("Publish RPC $fn missing on backend (migration 0004 not applied?)")
            else ->
                throw Exception("Backend publish failed (HTTP ${resp.status.value}): ${err.message ?: errText.take(200)}")
        }
    }

    /** Publishes the supplied sections of a snapshot's bundle through the
     *  per-section transactional publish RPCs (migration 0004). Replace semantics
     *  per section: the bundle is the whole new state of each supplied section.
     *  Each section is its own transaction - a mid-bundle failure leaves earlier
     *  sections published and later sections untouched; it never leaves one
     *  section half-written. Sections absent from the bundle are left alone. */
    override suspend fun publishContent(snapshot: AdminContentSnapshot): Result<Unit> = runCatching {
        val bundle = snapshot.bundle
        val token = tokenProvider.accessToken()
            ?: throw AdminDeniedException("No signed-in admin session")
        if (!currentUserIsAdmin()) throw AdminDeniedException("Account is not a temple admin")

        // Expected revisions come from the loaded snapshot, not a fresh read:
        // re-reading here would silently bless edits made after the admin loaded.
        val revisions = snapshot.revisions
        fun revisionFor(section: String): Long = revisions[section]
            ?: throw IllegalStateException(
                "Snapshot has no revision for section $section; reload before publishing")

        bundle.darshan?.let { d ->
            val rowJson = json.encodeToString(
                DarshanWriteRow(1, d.morningOpenMin, d.morningCloseMin, d.eveningOpenMin, d.eveningCloseMin))
            callRpc(token, "publish_darshan_settings",
                """{"p_row":$rowJson,"p_expected_revision":${revisionFor("darshan_settings")}}""")
        }
        if (bundle.morningPoojas != null || bundle.eveningPoojas != null) {
            // Replace semantics cover BOTH sessions: a publish with only one side
            // supplied intentionally empties the other.
            val rows = (bundle.morningPoojas.orEmpty().map { p ->
                PoojaWriteRow("morning", p.nameMl, p.nameEn, p.time, p.hour, p.minute)
            } + bundle.eveningPoojas.orEmpty().map { p ->
                PoojaWriteRow("evening", p.nameMl, p.nameEn, p.time, p.hour, p.minute)
            })
            publishRpc(token, "publish_poojas", revisionFor("poojas"), json.encodeToString(rows))
        }
        bundle.contacts?.let { contacts ->
            publishRpc(token, "publish_contacts", revisionFor("contacts"), json.encodeToString(contacts.map {
                ContactWriteRow(it.roleMl, it.roleEn, it.phone, it.whatsapp)
            }))
        }
        bundle.todayEvents?.let { events ->
            publishRpc(token, "publish_events", revisionFor("events"), json.encodeToString(events.map {
                EventWriteRow(it.icon, it.titleMl, it.titleEn, it.timeMl, it.timeEn)
            }))
        }
        bundle.gallery?.let { gallery ->
            publishRpc(token, "publish_gallery", revisionFor("gallery"), json.encodeToString(gallery.map {
                GalleryWriteRow(it.resName, it.captionMl, it.captionEn)
            }))
        }
        bundle.vazhipad?.let { vazhipad ->
            publishRpc(token, "publish_vazhipad", revisionFor("vazhipad"), json.encodeToString(vazhipad.map {
                VazhipadWriteRow(it.nameMl, it.nameEn, it.amount, it.description)
            }))
        }
        bundle.festivals?.let { festivals ->
            publishRpc(token, "publish_festivals", revisionFor("festivals"), json.encodeToString(festivals.map {
                FestivalWriteRow(it.nameMl, it.nameEn, it.icon, it.dateUtcMs, it.noteMl, it.isSpecial)
            }))
        }
    }

    /** Real notification sends stay disabled; this never touches a provider. */
    override suspend fun sendNotification(title: String, body: String, topic: String): Result<String> =
        Result.failure(NotApprovedException("Real notification sends are disabled"))
}
