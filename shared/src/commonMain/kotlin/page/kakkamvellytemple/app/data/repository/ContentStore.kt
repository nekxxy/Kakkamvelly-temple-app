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

    /** True only when the shown content actually came from the backend. An empty
     *  backend response (all sections null) means packaged defaults are still on
     *  screen, so the unverified-content notice must stay. */
    val isShowingVerifiedRemote: Boolean
        get() = source == Source.REMOTE_BACKEND && bundle?.hasAnySection() == true

    private val json = Json { ignoreUnknownKeys = true }

    fun install(b: ContentBundle, persist: Boolean = false, source: Source = Source.LOCAL_DEMO) {
        bundle = b
        this.source = source
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
