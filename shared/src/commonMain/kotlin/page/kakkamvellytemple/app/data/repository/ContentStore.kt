package page.kakkamvellytemple.app.data.repository

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.serialization.json.Json
import page.kakkamvellytemple.app.data.local.LocalContentCache
import page.kakkamvellytemple.app.data.remote.ContentBundle

/** Latest content bundle (remote or locally published demo edits). Backed by Compose
 *  state so screens reading StaticData getters recompose when a bundle is installed. */
object ContentStore {

    /** Where the currently shown content comes from. PACKAGED_DEFAULTS means the
     *  unverified bundled April 2026 defaults - the UI must say so visibly. */
    enum class Source { PACKAGED_DEFAULTS, LOCAL_DEMO, REMOTE_BACKEND }

    var bundle: ContentBundle? by mutableStateOf(null)
        private set

    var source: Source by mutableStateOf(Source.PACKAGED_DEFAULTS)
        private set

    /** Sections actually supplied by the current remote bundle. A partial backend
     *  leaves the remaining sections on unverified packaged defaults. */
    var remoteSections: Set<String> by mutableStateOf(emptySet())
        private set

    /** The backend's explicit committee-verification marker. Never inferred from
     *  row presence. */
    var remoteVerified: Boolean by mutableStateOf(false)
        private set

    /** True only when every displayed temple fact came from the backend AND the
     *  backend explicitly marks the content committee-verified. Anything less keeps
     *  the user-visible unverified-content notice up. */
    val isShowingVerifiedRemote: Boolean
        get() = source == Source.REMOTE_BACKEND &&
                remoteVerified &&
                remoteSections.containsAll(ContentBundle.ALL_SECTIONS)

    private val json = Json { ignoreUnknownKeys = true }

    fun install(b: ContentBundle, persist: Boolean = false, source: Source = Source.LOCAL_DEMO) {
        bundle = b
        this.source = source
        if (source == Source.REMOTE_BACKEND) {
            remoteSections = b.suppliedSections()
            remoteVerified = b.verified
        } else {
            remoteSections = emptySet()
            remoteVerified = false
        }
        if (persist) persistLocal()
    }

    /** Load the locally persisted bundle (demo edits from a previous session). */
    fun loadLocal() {
        val raw = LocalContentCache.load() ?: return
        runCatching { json.decodeFromString<ContentBundle>(raw) }
            .getOrNull()
            ?.let {
                if (it.version >= (bundle?.version ?: Int.MIN_VALUE)) {
                    bundle = it
                    source = Source.LOCAL_DEMO
                }
            }
    }

    fun persistLocal() {
        bundle?.let { LocalContentCache.save(json.encodeToString(ContentBundle.serializer(), it)) }
    }
}
