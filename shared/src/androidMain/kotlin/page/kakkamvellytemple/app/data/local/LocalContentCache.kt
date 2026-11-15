package page.kakkamvellytemple.app.data.local

import android.content.Context
import java.io.File

actual object LocalContentCache {
    private var file: File? = null

    fun init(context: Context) {
        file = File(context.filesDir, "content-bundle.json")
    }

    actual fun load(): String? {
        if (!DemoAdmin.enabled) return null  // release builds never read local overrides
        return runCatching { file?.takeIf { it.exists() }?.readText() }.getOrNull()
    }

    actual fun save(json: String) {
        if (!DemoAdmin.enabled) return  // release builds never persist overrides
        runCatching { file?.writeText(json) }
    }

    actual fun clear() {
        runCatching { file?.delete() }
    }
}
