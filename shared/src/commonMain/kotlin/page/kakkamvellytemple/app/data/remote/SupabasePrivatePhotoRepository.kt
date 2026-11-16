package page.kakkamvellytemple.app.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsChannel
import kotlinx.io.readByteArray
import io.ktor.utils.io.readRemaining
import io.ktor.http.*
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import page.kakkamvellytemple.app.data.repository.*

/** Private upload only. No public URL, publish RPC, service key or visitor token
 * provider. No app caller enables this yet. Server membership and slot RLS are
 * authoritative; client validation is UX defense, never the security boundary.
 * Production composition requires a dedicated non-logging client with timeouts. */
class SupabasePrivatePhotoRepository(
    private val settings: SupabaseContentSource.Settings,
    private val tokens: AdminAuthTokenProvider,
    private val client: HttpClient,
    private val enabled: Boolean = false,
    private val publicationRequestsEnabled: Boolean = false,
) {
    @Serializable class UploadSlot(val id: String, val bucket: String, val path: String)
    @Serializable class PrivatePhoto(
        val id: String, val state: String, val created_at: String,
        val expires_at: String, val preview_path: String? = null,
        val publication_id: String? = null, val publication_state: String? = null,
        val caption_ml: String = "", val caption_en: String = "",

    )
    @Serializable class ManagementSnapshot(
        val gallery_revision: Long, val requests_enabled: Boolean, val delivery_enabled: Boolean,
        val photos: List<PrivatePhoto>,
    )
    @Serializable class PublicationReceipt(val id: String, val state: String)
    @Serializable private class PublicationRequest(
        val p_request_id: String, val p_asset_id: String, val p_expected_revision: Long,
        val p_caption_ml: String, val p_caption_en: String,
    )
    @Serializable private class CancelPublicationRequest(val p_request_id: String)
    @Serializable private class CaptionEditRequest(val p_id: String, val p_expected_revision: Long, val p_caption_ml: String, val p_caption_en: String)
    @Serializable private class RetireRequest(val p_id: String, val p_expected_revision: Long)
    @Serializable private class ReviewRequest(val p_id: String)
    private val uuid = "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"
    private fun valid(slot: UploadSlot): Boolean =
        Regex("^$uuid$").matches(slot.id) && slot.bucket == "temple-photo-quarantine" &&
        Regex("^$uuid/${slot.id}/original$").matches(slot.path)
    private suspend fun token(): String {
        if (!enabled) throw NotApprovedException("Private photo uploads are not enabled")
        return tokens.accessToken() ?: throw AdminDeniedException("No signed-in admin session")
    }
    private fun HttpRequestBuilder.headersFor(token: String) {
        header("apikey", settings.anonKey); bearerAuth(token)
    }
    private suspend fun <T> attempt(block: suspend () -> T): Result<T> = try { Result.success(block()) }
    catch (e: CancellationException) { throw e }
    catch (e: NotApprovedException) { Result.failure(e) }
    catch (e: AdminDeniedException) { Result.failure(e) }
    catch (_: Exception) { Result.failure(Exception("Private photo operation could not be confirmed")) }
    private fun checkStatus(status: HttpStatusCode) {
        if (status.value in listOf(401, 403)) throw AdminDeniedException()
        check(status.isSuccess()) { "Photo operation failed" }
    }
    suspend fun createSlot(): Result<UploadSlot> = attempt {
        val t = token()
        val response = client.post("${settings.url}/rest/v1/rpc/create_photo_upload_slot") {
            headersFor(t); contentType(ContentType.Application.Json); setBody("{}")
        }
        checkStatus(response.status)
        val slot: UploadSlot = response.body()
        check(valid(slot)) { "Invalid private slot" }
        slot
    }
    suspend fun listPrivate(): Result<List<PrivatePhoto>> = attempt {
        val t = token()
        val response = client.post("${settings.url}/rest/v1/rpc/list_private_temple_photos") {
            headersFor(t); contentType(ContentType.Application.Json); setBody("{}")
        }
        checkStatus(response.status)
        val rows: List<PrivatePhoto> = response.body()
        check(validPhotos(rows))
        rows
    }
    private fun validPhotos(rows: List<PrivatePhoto>): Boolean = rows.size <= 24 && rows.map { it.id }.toSet().size == rows.size && rows.all {
        Regex("^$uuid$").matches(it.id) && it.state in listOf("pending", "processed", "reviewed") &&
        (it.preview_path == null || Regex("^processed/${it.id}(/$uuid)?\\.jpg$").matches(it.preview_path)) &&
        ((it.publication_id == null && it.publication_state == null) ||
            (it.publication_id?.let { p -> Regex("^$uuid$").matches(p) } == true &&
                it.publication_state in listOf("queued", "canceled", "published", "retired"))) &&
        it.caption_ml.length <= 2000 && it.caption_en.length <= 2000
    }
    /** One database snapshot avoids separately fetched revision/status mismatches.
     * Server booleans describe status only; they NEVER enable a client feature. */
    suspend fun managementSnapshot(): Result<ManagementSnapshot> = attempt {
        val t = token()
        val response = client.post("${settings.url}/rest/v1/rpc/get_temple_photo_management") {
            headersFor(t); contentType(ContentType.Application.Json); setBody("{}")
        }
        checkStatus(response.status)
        val snapshot: ManagementSnapshot = response.body()
        check(snapshot.gallery_revision >= 0 && validPhotos(snapshot.photos))
        snapshot
    }
    /** Authenticated admin read; bytes are never a public/signed URL. RLS denies
     * visitor sessions and original files. Keep previews in private memory only. */
    suspend fun previewProcessed(id: String, path: String = "processed/$id.jpg"): Result<ByteArray> = attempt {
        val t = token(); require(Regex("^$uuid$").matches(id))
        require(Regex("^processed/$id(/$uuid)?\\.jpg$").matches(path))
        val response = client.get("${settings.url}/storage/v1/object/authenticated/temple-photo-quarantine/$path") {
            headersFor(t)
        }
        checkStatus(response.status)
        check(response.headers[HttpHeaders.ContentType]?.substringBefore(';') == "image/jpeg")
        val channel = response.bodyAsChannel()
        val bytes = try { channel.readRemaining(4L * 1024 * 1024 + 1).readByteArray() }
            finally { channel.cancel(null) }
        check(bytes.isNotEmpty() && bytes.size <= 4 * 1024 * 1024)
        bytes
    }
    /** Successful storage upload means PRIVATE original only. It does not mean
     * processed, reviewed, verified, published, or visible to visitors. No retry
     * after an uncertain upload: reuse must be reconciled, never use upsert. */
    suspend fun uploadOriginal(slot: UploadSlot, bytes: ByteArray, mime: String): Result<Unit> = attempt {
        val t = token()
        require(valid(slot) && bytes.isNotEmpty() && bytes.size <= 8 * 1024 * 1024)
        require(mime in listOf("image/jpeg", "image/png", "image/webp"))
        val response = client.post("${settings.url}/storage/v1/object/${slot.bucket}/${slot.path}") {
            headersFor(t); header("x-upsert", "false"); contentType(ContentType.parse(mime)); setBody(bytes)
        }
        checkStatus(response.status)
    }
    /** Server refuses review until isolated normalization has completed.
     * Review is attributed to the signed-in admin, never publication. */
    suspend fun reviewProcessed(id: String): Result<Unit> = attempt {
        val t = token(); require(Regex("^$uuid$").matches(id))
        val response = client.post("${settings.url}/rest/v1/rpc/review_temple_photo") {
            headersFor(t); contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(ReviewRequest(id)))
        }
        checkStatus(response.status)
    }
    /** Queues a private intent only. Reuse requestId with EXACTLY the same
     * payload after an uncertain response; never invent a replacement retry.
     * No public gallery, visitor visibility or verification state is changed. */
    suspend fun queuePublicationIntent(
        requestId: String, assetId: String, expectedRevision: Long,
        captionMl: String, captionEn: String,
    ): Result<PublicationReceipt> = attempt {
        if (!publicationRequestsEnabled) throw NotApprovedException("Photo publication requests are not enabled")
        val t = token()
        require(Regex("^$uuid$").matches(requestId) && Regex("^$uuid$").matches(assetId))
        require(expectedRevision >= 0 && captionMl.length <= 2000 && captionEn.length <= 2000)
        val response = client.post("${settings.url}/rest/v1/rpc/queue_reviewed_temple_photo") {
            headersFor(t); contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(PublicationRequest(requestId,assetId,expectedRevision,captionMl,captionEn)))
        }
        checkStatus(response.status)
        val receipt: PublicationReceipt = response.body()
        check(receipt.id == requestId && receipt.state in listOf("queued", "canceled", "published"))
        receipt
    }
    /** Cancellation remains available if new publication requests are disabled.
     * It cancels an intent, not a published image or a running remote operation. */
    suspend fun cancelPublicationIntent(requestId: String): Result<PublicationReceipt> = attempt {
        val t = token(); require(Regex("^$uuid$").matches(requestId))
        val response = client.post("${settings.url}/rest/v1/rpc/cancel_temple_photo_publication") {
            headersFor(t); contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(CancelPublicationRequest(requestId)))
        }
        checkStatus(response.status)
        val receipt: PublicationReceipt = response.body()
        check(receipt.id == requestId && receipt.state == "canceled")
        receipt
    }
    /** Caption edits are new content. The backend resets committee verification.
     * An uncertain response must be reconciled, not blindly resent with a new revision. */
    suspend fun editPublishedCaptions(publicationId: String, expectedRevision: Long, captionMl: String, captionEn: String): Result<Long> = attempt {
        if (!publicationRequestsEnabled) throw NotApprovedException("Photo publication requests are not enabled")
        val t = token(); require(Regex("^$uuid$").matches(publicationId) && expectedRevision >= 0)
        require(captionMl.length <= 2000 && captionEn.length <= 2000)
        val response = client.post("${settings.url}/rest/v1/rpc/edit_temple_photo_captions") {
            headersFor(t); contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(CaptionEditRequest(publicationId,expectedRevision,captionMl,captionEn)))
        }
        checkStatus(response.status)
        val revision: Long = response.body(); check(revision >= expectedRevision); revision
    }
    /** Retirement is monotonic, not deletion. Allowed with new requests disabled;
     * old downloaded copies cannot be recalled. No object slots are reopened. */
    suspend fun retirePublishedPhoto(publicationId: String, expectedRevision: Long): Result<Long> = attempt {
        val t = token(); require(Regex("^$uuid$").matches(publicationId) && expectedRevision >= 0)
        val response = client.post("${settings.url}/rest/v1/rpc/retire_temple_gallery_photo") {
            headersFor(t); contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(RetireRequest(publicationId,expectedRevision)))
        }
        checkStatus(response.status)
        val revision: Long = response.body(); check(revision >= expectedRevision); revision
    }
}
