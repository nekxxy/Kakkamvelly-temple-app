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
    var bundle: ContentBundle? by mutableStateOf(null)
        private set

    private val json = Json { ignoreUnknownKeys = true }

    fun install(b: ContentBundle, persist: Boolean = false) {
        bundle = b
        if (persist) persistLocal()
    }

    /** Load the locally persisted bundle (demo edits from a previous session). */
    fun loadLocal() {
        val raw = LocalContentCache.load() ?: return
        runCatching { json.decodeFromString<ContentBundle>(raw) }
            .getOrNull()
            ?.let { if (it.version >= (bundle?.version ?: Int.MIN_VALUE)) bundle = it }
    }

    fun persistLocal() {
        bundle?.let { LocalContentCache.save(json.encodeToString(ContentBundle.serializer(), it)) }
    }
}
