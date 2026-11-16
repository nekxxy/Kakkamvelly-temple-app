package page.kakkamvellytemple.app.presentation.viewmodel

import androidx.compose.runtime.*
import kotlinx.coroutines.*
import page.kakkamvellytemple.app.data.remote.SupabasePrivatePhotoRepository

/** Prepared private-management state, no production caller/picker yet. Never
 * adds a photo to public gallery. All remote work is serialized by busy. */
class PrivatePhotoViewModel(
    private val repository: SupabasePrivatePhotoRepository,
    private val scope: CoroutineScope,
) {
    var photos by mutableStateOf<List<SupabasePrivatePhotoRepository.PrivatePhoto>>(emptyList()); private set
    var galleryRevision by mutableStateOf<Long?>(null); private set
    var deliveryEnabled by mutableStateOf(false); private set
    var busy by mutableStateOf(false); private set
    var error by mutableStateOf<String?>(null); private set
    var notice by mutableStateOf<String?>(null); private set
    var preview by mutableStateOf<ByteArray?>(null); private set
    private var job: Job? = null
    fun closePreview() { preview?.fill(0); preview = null }
    private fun run(block: suspend () -> Unit) {
        if (busy) return
        busy=true; error=null; notice=null
        job=scope.launch { try { block() } catch(e: CancellationException) { throw e }
            catch(_: Exception) { error="Private photo operation could not be confirmed. No photo was published." }
            finally { busy=false } }
    }
    private suspend fun reload() {
        val snapshot=repository.managementSnapshot().getOrThrow()
        photos=snapshot.photos; galleryRevision=snapshot.gallery_revision; deliveryEnabled=snapshot.delivery_enabled
    }
    fun refresh() = run { reload() }
    fun showPreview(id: String) = run {
        val path=photos.firstOrNull { it.id==id }?.preview_path ?: error("No private preview")
        closePreview(); preview=repository.previewProcessed(id,path).getOrThrow()
    }
    fun upload(bytes: ByteArray, mime: String) {
        if (busy) return
        if (bytes.isEmpty() || bytes.size > 8 * 1024 * 1024 || mime !in listOf("image/jpeg", "image/png", "image/webp")) {
            error="Choose a JPEG, PNG or WebP photo up to 8 MiB."; return
        }
        // Own a short-lived copy; never persist originals or retain picker bytes.
        val owned=bytes.copyOf()
        run {
            try {
                val slot=repository.createSlot().getOrThrow()
                repository.uploadOriginal(slot,owned,mime).getOrThrow()
                notice="Private original uploaded. Processing and review are still required. Nothing was published."
                reload()
            } finally { owned.fill(0) }
        }
    }
    fun review(id: String) = run {
        repository.reviewProcessed(id).getOrThrow()
        notice="Review recorded privately. Nothing was published."
        reload()
    }
    /** Call when leaving admin UI; cancellation is not a server rollback. */
    fun clear() { job?.cancel(); closePreview(); photos=emptyList(); galleryRevision=null; deliveryEnabled=false; notice=null; error=null }
}
