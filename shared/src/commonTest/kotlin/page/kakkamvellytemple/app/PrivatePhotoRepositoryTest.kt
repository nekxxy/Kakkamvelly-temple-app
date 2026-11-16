package page.kakkamvellytemple.app

import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import io.ktor.http.content.TextContent
import kotlin.test.*
import page.kakkamvellytemple.app.data.remote.*
import page.kakkamvellytemple.app.data.repository.*

class PrivatePhotoRepositoryTest {
    private val settings=SupabaseContentSource.Settings("https://test.supabase.co","test-key")
    private val id="00000000-0000-0000-0000-000000000001"
    private fun slot()=SupabasePrivatePhotoRepository.UploadSlot(id,"temple-photo-quarantine","$id/$id/original")
    @Test fun disabledMakesNoRequests(): Unit = runBlocking {
        var calls=0; val c=HttpClient(MockEngine { calls++; error("no request") })
        val r=SupabasePrivatePhotoRepository(settings,{"admin-token"},c)
        assertIs<NotApprovedException>(r.createSlot().exceptionOrNull())
        assertIs<NotApprovedException>(r.uploadOriginal(slot(),byteArrayOf(1),"image/jpeg").exceptionOrNull())
        assertEquals(0,calls)
    }
    @Test fun visitorCannotSupplyAdminSession(): Unit = runBlocking {
        var calls=0; val c=HttpClient(MockEngine { calls++; error("no request") })
        val r=SupabasePrivatePhotoRepository(settings,{null},c,true)
        assertIs<AdminDeniedException>(r.createSlot().exceptionOrNull()); assertEquals(0,calls)
    }
    @Test fun canonicalSlotAndInsertOnlyPrivateUpload(): Unit = runBlocking {
        val paths=mutableListOf<String>()
        val c=HttpClient(MockEngine { req ->
            paths.add(req.url.encodedPath); assertEquals("Bearer admin-token",req.headers[HttpHeaders.Authorization])
            if(req.url.encodedPath.contains("/storage/")) assertEquals("false",req.headers["x-upsert"])
            respond(if(req.url.encodedPath.contains("/rpc/")) """{"id":"$id","bucket":"temple-photo-quarantine","path":"$id/$id/original"}""" else "{}",
                HttpStatusCode.OK,headersOf(HttpHeaders.ContentType,"application/json"))
        }) { install(ContentNegotiation) { json(Json { ignoreUnknownKeys=true }) } }
        val r=SupabasePrivatePhotoRepository(settings,{"admin-token"},c,true)
        assertTrue(r.createSlot().isSuccess); assertTrue(r.uploadOriginal(slot(),byteArrayOf(1),"image/jpeg").isSuccess)
        assertEquals(2,paths.size); assertTrue(paths.last().endsWith("/original"))
    }
    @Test fun arbitraryPathsAndSvgRejectedBeforeRequest(): Unit = runBlocking {
        var calls=0;val c=HttpClient(MockEngine { calls++; error("no request") })
        val r=SupabasePrivatePhotoRepository(settings,{"admin-token"},c,true)
        assertTrue(r.uploadOriginal(SupabasePrivatePhotoRepository.UploadSlot(id,"public","../secret"),byteArrayOf(1),"image/jpeg").isFailure)
        assertTrue(r.uploadOriginal(slot(),byteArrayOf(1),"image/svg+xml").isFailure);assertEquals(0,calls)
    }
    @Test fun deniedAndStorageConflictAreNeverSuccessful(): Unit = runBlocking {
        for(status in listOf(HttpStatusCode.Forbidden,HttpStatusCode.Conflict)) {
            var calls=0;val c=HttpClient(MockEngine { calls++; respond("private provider detail",status) })
            val r=SupabasePrivatePhotoRepository(settings,{"admin-token"},c,true)
            val result=r.uploadOriginal(slot(),byteArrayOf(1),"image/jpeg")
            assertTrue(result.isFailure);assertFalse(result.exceptionOrNull()!!.message!!.contains("provider detail"));assertEquals(1,calls)
        }
    }

    @Test fun privatePreviewRequiresAuthAndNeverUsesPublicRoute(): Unit = runBlocking {
        val c=HttpClient(MockEngine { req ->
            assertTrue(req.url.encodedPath.contains("/object/authenticated/temple-photo-quarantine/processed/"))
            assertEquals("Bearer admin-token",req.headers[HttpHeaders.Authorization])
            respond(byteArrayOf(1,2,3),HttpStatusCode.OK,headersOf(HttpHeaders.ContentType,"image/jpeg"))
        })
        val r=SupabasePrivatePhotoRepository(settings,{"admin-token"},c,true)
        assertContentEquals(byteArrayOf(1,2,3),r.previewProcessed(id).getOrThrow())
    }
    @Test fun privateListRejectsUnexpectedExternalPreviewPath(): Unit = runBlocking {
        val c=HttpClient(MockEngine {
            respond("""[{"id":"$id","state":"processed","created_at":"t","expires_at":"t","preview_path":"https://evil.invalid/image"}]""",
                HttpStatusCode.OK,headersOf(HttpHeaders.ContentType,"application/json"))
        }) { install(ContentNegotiation) { json(Json { ignoreUnknownKeys=true }) } }
        assertTrue(SupabasePrivatePhotoRepository(settings,{"admin-token"},c,true).listPrivate().isFailure)
    }
    @Test fun publicationIntentHasIndependentDefaultOffGate(): Unit = runBlocking {
        var calls=0; val c=HttpClient(MockEngine { calls++; error("no request") })
        val r=SupabasePrivatePhotoRepository(settings,{"admin-token"},c,true)
        assertIs<NotApprovedException>(r.queuePublicationIntent(id,id,0,"ml","en").exceptionOrNull())
        assertEquals(0,calls)
    }
    @Test fun publicationIntentPreservesExactCaptionsAndRequestId(): Unit = runBlocking {
        var calls=0
        val c=HttpClient(MockEngine { req ->
            calls++; assertEquals("/rest/v1/rpc/queue_reviewed_temple_photo",req.url.encodedPath)
            assertEquals("Bearer admin-token",req.headers[HttpHeaders.Authorization])
            val body=Json.parseToJsonElement((req.body as TextContent).text).jsonObject
            assertEquals(id,body["p_request_id"]!!.jsonPrimitive.content)
            assertEquals("മലയാളം",body["p_caption_ml"]!!.jsonPrimitive.content)
            assertEquals("An exact caption",body["p_caption_en"]!!.jsonPrimitive.content)
            respond("""{"id":"$id","state":"queued"}""",HttpStatusCode.OK,headersOf(HttpHeaders.ContentType,"application/json"))
        }) { install(ContentNegotiation) { json() } }
        val r=SupabasePrivatePhotoRepository(settings,{"admin-token"},c,true,true)
        assertEquals("queued",r.queuePublicationIntent(id,id,0,"മലയാളം","An exact caption").getOrThrow().state)
        assertEquals(1,calls)
    }
    @Test fun cancellationWorksWithNewPublicationRequestsDisabled(): Unit = runBlocking {
        val c=HttpClient(MockEngine { req ->
            assertEquals("/rest/v1/rpc/cancel_temple_photo_publication",req.url.encodedPath)
            respond("""{"id":"$id","state":"canceled"}""",HttpStatusCode.OK,headersOf(HttpHeaders.ContentType,"application/json"))
        }) { install(ContentNegotiation) { json() } }
        assertEquals("canceled",SupabasePrivatePhotoRepository(settings,{"admin-token"},c,true).cancelPublicationIntent(id).getOrThrow().state)
    }
    @Test fun publishedCaptionEditUsesExactWordsAndRevision(): Unit = runBlocking {
        var calls=0
        val c=HttpClient(MockEngine { req ->
            calls++; assertEquals("/rest/v1/rpc/edit_temple_photo_captions",req.url.encodedPath)
            val body=Json.parseToJsonElement((req.body as TextContent).text).jsonObject
            assertEquals(4L,body["p_expected_revision"]!!.jsonPrimitive.long)
            assertEquals("Exact words",body["p_caption_en"]!!.jsonPrimitive.content)
            respond("5",HttpStatusCode.OK,headersOf(HttpHeaders.ContentType,"application/json"))
        }) { install(ContentNegotiation) { json() } }
        val r=SupabasePrivatePhotoRepository(settings,{"admin-token"},c,true,true)
        assertEquals(5L,r.editPublishedCaptions(id,4,"മലയാളം","Exact words").getOrThrow());assertEquals(1,calls)
    }
    @Test fun retirementDoesNotRequireNewPublicationRequestsEnabled(): Unit = runBlocking {
        val c=HttpClient(MockEngine { req ->
            assertEquals("/rest/v1/rpc/retire_temple_gallery_photo",req.url.encodedPath)
            respond("5",HttpStatusCode.OK,headersOf(HttpHeaders.ContentType,"application/json"))
        }) { install(ContentNegotiation) { json() } }
        assertEquals(5L,SupabasePrivatePhotoRepository(settings,{"admin-token"},c,true).retirePublishedPhoto(id,4).getOrThrow())
    }
    @Test fun managementSnapshotKeepsPublicationDistinctFromPrivateProcessing(): Unit = runBlocking {
        val c=HttpClient(MockEngine { req ->
            assertEquals("/rest/v1/rpc/get_temple_photo_management",req.url.encodedPath)
            assertEquals("Bearer admin-token",req.headers[HttpHeaders.Authorization])
            respond("""{"gallery_revision":8,"requests_enabled":false,"delivery_enabled":false,"photos":[{"id":"$id","state":"reviewed","created_at":"t","expires_at":"t","preview_path":"processed/$id.jpg","publication_id":"$id","publication_state":"published","caption_ml":"മലയാളം","caption_en":"Exact caption"}]}""",
                HttpStatusCode.OK,headersOf(HttpHeaders.ContentType,"application/json"))
        }) { install(ContentNegotiation) { json() } }
        val snapshot=SupabasePrivatePhotoRepository(settings,{"admin-token"},c,true).managementSnapshot().getOrThrow()
        assertEquals(8L,snapshot.gallery_revision);assertFalse(snapshot.delivery_enabled)
        assertEquals("reviewed",snapshot.photos.single().state);assertEquals("published",snapshot.photos.single().publication_state)
    }
    @Test fun managementRejectsUnknownPublicationStateAndInvalidIdentity(): Unit = runBlocking {
        for(state in listOf("unknown","published")) {
            val c=HttpClient(MockEngine {
                respond("""{"gallery_revision":8,"requests_enabled":false,"delivery_enabled":false,"photos":[{"id":"$id","state":"reviewed","created_at":"t","expires_at":"t","publication_id":"../x","publication_state":"$state"}]}""",
                    HttpStatusCode.OK,headersOf(HttpHeaders.ContentType,"application/json"))
            }) { install(ContentNegotiation) { json() } }
            assertTrue(SupabasePrivatePhotoRepository(settings,{"admin-token"},c,true).managementSnapshot().isFailure)
        }
    }
}
