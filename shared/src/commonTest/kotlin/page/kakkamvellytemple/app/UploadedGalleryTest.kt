package page.kakkamvellytemple.app

import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.runBlocking
import kotlin.test.*
import page.kakkamvellytemple.app.data.model.*
import page.kakkamvellytemple.app.data.remote.*

class UploadedGalleryTest {
    private val asset="00000000-0000-0000-0000-000000000001"
    private val publication="00000000-0000-0000-0000-000000000002"
    private val settings=SupabaseContentSource.Settings("https://test.supabase.co","test",true)
    @Test fun validLegacyUrlRemainsUnchangedAndInvalidSlugIsNotSanitizedIntoAnotherPhoto() {
        assertEquals("https://kakkamvellytemple.page/images/temple-gate.jpg",galleryImageUrl(GalleryPhoto("temple-gate","","")))
        assertNull(galleryImageUrl(GalleryPhoto("temple/gate","","")))
    }
    @Test fun uploadedReferencesNeedIndependentEnableAndCanonicalFixedOriginIds() {
        val p=GalleryPhoto("","", "",asset,publication)
        assertNull(galleryImageUrl(p,SupabaseContentSource.Settings(settings.url,"test")))
        assertEquals("https://test.supabase.co/functions/v1/temple-gallery/$asset/$publication",galleryImageUrl(p,settings))
        assertNull(galleryImageUrl(p,settings.copy(url="https://evil.invalid")))
        assertNull(galleryImageUrl(p.copy(assetId="../original"),settings))
        assertNull(galleryImageUrl(p.copy(publicationId=null),settings))
    }
    @Test fun defaultContentFetchDoesNotContactUploadedManifestRpc(): Unit = runBlocking {
        val paths=mutableListOf<String>()
        val client=HttpClient(MockEngine { req -> paths+=req.url.encodedPath;respond("[]",HttpStatusCode.OK,headersOf(HttpHeaders.ContentType,"application/json")) }) { install(ContentNegotiation) { json() } }
        val bundle=SupabaseContentSource.fetchBundle(client,settings.copy(uploadedPhotosEnabled=false))
        assertNull(bundle.uploadedGallery);assertFalse(paths.any { it.contains("get_uploaded_temple_gallery") })
    }
    @Test fun enabledFetchKeepsUploadedReferencesSeparateFromLegacyAdminRows(): Unit = runBlocking {
        val client=HttpClient(MockEngine { req ->
            respond(if(req.url.encodedPath.endsWith("get_uploaded_temple_gallery")) """[{"asset_id":"$asset","publication_id":"$publication","caption_ml":"മലയാളം","caption_en":"English"}]""" else "[]",
                HttpStatusCode.OK,headersOf(HttpHeaders.ContentType,"application/json"))
        }) { install(ContentNegotiation) { json() } }
        val bundle=SupabaseContentSource.fetchBundle(client,settings)
        assertNull(bundle.gallery);assertEquals(asset,bundle.uploadedGallery!!.single().assetId)
        assertTrue(bundle.hasAnySection());assertFalse(bundle.verified)
        // Uploaded rows cannot claim the packaged legacy gallery was verified.
        assertFalse(bundle.suppliedSections().contains("gallery"))
    }
    @Test fun malformedManifestIdentifiersFailWholeRefreshRatherThanFetchArbitraryUrl(): Unit = runBlocking {
        val client=HttpClient(MockEngine { req ->
            respond(if(req.url.encodedPath.endsWith("get_uploaded_temple_gallery")) """[{"asset_id":"https://evil.invalid","publication_id":"$publication","caption_ml":"","caption_en":""}]""" else "[]",
                HttpStatusCode.OK,headersOf(HttpHeaders.ContentType,"application/json"))
        }) { install(ContentNegotiation) { json() } }
        assertFailsWith<IllegalStateException> { SupabaseContentSource.fetchBundle(client,settings) }
    }
}
