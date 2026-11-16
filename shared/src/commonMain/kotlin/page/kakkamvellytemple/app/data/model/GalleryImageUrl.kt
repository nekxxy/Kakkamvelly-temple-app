package page.kakkamvellytemple.app.data.model

import page.kakkamvellytemple.app.data.remote.SupabaseContentSource

/** URLs are derived only from fixed origins and canonical IDs. Remote content
 * cannot supply an arbitrary image URL, path, query or credential. */
fun galleryImageUrl(photo: GalleryPhoto, settings: SupabaseContentSource.Settings? = SupabaseContentSource.settings): String? {
    if (photo.assetId == null && photo.publicationId == null) {
        if (!Regex("^[a-z0-9-]{1,64}$").matches(photo.resName)) return null
        return "https://kakkamvellytemple.page/images/${photo.resName}.jpg"
    }
    val config = settings?.takeIf { it.uploadedPhotosEnabled } ?: return null
    if (!Regex("^https://[a-z0-9-]+\\.supabase\\.co$").matches(config.url)) return null
    val uuid = Regex("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$")
    if (!uuid.matches(photo.assetId ?: "") || !uuid.matches(photo.publicationId ?: "")) return null
    return "${config.url}/functions/v1/temple-gallery/${photo.assetId}/${photo.publicationId}"
}
