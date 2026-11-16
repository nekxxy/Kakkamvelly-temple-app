package page.kakkamvellytemple.app

import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
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
}
