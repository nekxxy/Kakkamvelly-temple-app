package page.kakkamvellytemple.app.data.model

import page.kakkamvellytemple.app.data.remote.GalleryDto

/** Validation rules for gallery photo entries in the admin editor.
 *
 *  Photo FILES live on the temple website host (kakkamvellytemple.page/images/<slug>.jpg);
 *  the app and backend carry only the slug plus captions. The visitor screen
 *  (GalleryScreen.imageUrl) silently coerces a bad slug to a safe value, which
 *  would render the WRONG photo without any error - so the admin side rejects
 *  bad entries before they can be published. The slug rule here mirrors that
 *  filter exactly; keep the two in sync. */
object GalleryValidation {
    const val MAX_PHOTOS = 24
    const val MAX_SLUG_LEN = 40
    const val MAX_CAPTION_LEN = 80

    private val SLUG_CHARS = Regex("^[a-z0-9-]+$")

    /** Null when [slug] is a usable photo id; otherwise the reason it is not. */
    fun slugError(slug: String): String? = when {
        slug.isBlank() -> "Photo id is empty"
        slug.length > MAX_SLUG_LEN -> "Photo id is longer than $MAX_SLUG_LEN characters"
        !SLUG_CHARS.matches(slug) -> "Photo id may only contain a-z, 0-9 and '-' (it becomes .../images/<id>.jpg)"
        else -> null
    }

    fun captionError(caption: String): String? =
        if (caption.length > MAX_CAPTION_LEN) "Caption is longer than $MAX_CAPTION_LEN characters" else null

    /** Per-row errors for the whole gallery: row index -> first problem found.
     *  An empty map means the gallery is publishable. */
    fun rowErrors(entries: List<GalleryDto>): Map<Int, String> {
        val errors = linkedMapOf<Int, String>()
        val seen = mutableSetOf<String>()
        entries.forEachIndexed { i, g ->
            val err = slugError(g.resName)
                ?: captionError(g.captionMl)
                ?: captionError(g.captionEn)
                ?: if (seen.add(g.resName)) null else "Duplicate photo id"
            if (err != null) errors[i] = err
        }
        return errors
    }

    /** Null when one more photo may be added. */
    fun fullError(entries: List<GalleryDto>): String? =
        if (entries.size >= MAX_PHOTOS) "Gallery is full ($MAX_PHOTOS photos max)" else null
}
