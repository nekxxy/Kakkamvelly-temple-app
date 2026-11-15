package page.kakkamvellytemple.app

import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData  // MockEngine respond type
import io.ktor.http.*
import io.ktor.http.content.OutgoingContent
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlin.test.*
import page.kakkamvellytemple.app.data.remote.ContentBundle
import page.kakkamvellytemple.app.data.remote.ContactDto
import page.kakkamvellytemple.app.data.remote.DarshanDto
import page.kakkamvellytemple.app.data.remote.FestivalDto
import page.kakkamvellytemple.app.data.remote.SupabaseAdminRepository
import page.kakkamvellytemple.app.data.remote.SupabaseContentSource
import page.kakkamvellytemple.app.data.remote.VazhipadDto
import page.kakkamvellytemple.app.data.repository.AdminContentSnapshot
import page.kakkamvellytemple.app.data.repository.AdminDeniedException
import page.kakkamvellytemple.app.data.repository.NotApprovedException
import page.kakkamvellytemple.app.data.repository.RevisionConflictException

/** Publish-path tests for the admin write repository against the transactional
 *  publish RPCs (migration 0004). Every backend response is mocked; these verify
 *  the app's handling contract: denied is never success, and a publish's expected
 *  revision traces to the snapshot the editor loaded (stale edits conflict, never
 *  silently overwrite). Live anon denial is verified separately against the
 *  project; authenticated authorization, rollback and concurrency behavior are
 *  NOT live-verified here - they rest on the SQL design plus these mocks until a
 *  real enrollment exists. */
class SupabaseAdminRepositoryTest {

    private val settings = SupabaseContentSource.Settings("https://test.supabase.co", "test-key")

    private fun clientFor(responder: MockRequestHandleScope.(HttpRequestData) -> HttpResponseData) =
        HttpClient(MockEngine(responder)) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }

    private fun MockRequestHandleScope.okJson(body: String) =
        respond(body, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))

    private fun MockRequestHandleScope.errJson(code: String, message: String, status: HttpStatusCode = HttpStatusCode.BadRequest) =
        respond("""{"code":"$code","message":"$message"}""", status,
            headersOf(HttpHeaders.ContentType, "application/json"))

    private val sampleBundle = ContentBundle(
        darshan = DarshanDto(330, 540, 1065, 1125),
        contacts = listOf(ContactDto("സെക്രട്ടറി", "Secretary", "+919446844145")),
    )
    private val sampleRevisions = mapOf(
        "darshan_settings" to 3L, "contacts" to 5L, "vazhipad" to 2L, "festivals" to 7L,
    )
    private fun snapshotOf(bundle: ContentBundle, revisions: Map<String, Long> = sampleRevisions) =
        AdminContentSnapshot(bundle, revisions)

    /** Mock that accepts the admin-membership check and lets the caller decide
     *  each RPC response; no content_meta read happens on the publish path. */
    private fun rpcClient(rpcHandler: MockRequestHandleScope.(HttpRequestData) -> HttpResponseData) =
        clientFor { req ->
            when {
                req.url.encodedPath.endsWith("/admin_users") -> okJson("""[{"user_id":"u1"}]""")
                else -> rpcHandler(req)
            }
        }

    @Test fun publishWithoutSessionIsDeniedAndMakesNoHttpCalls() = runBlocking {
        var calls = 0
        val repo = SupabaseAdminRepository(settings, { null }, clientFor { calls++; okJson("[]") })
        val result = repo.publishContent(snapshotOf(sampleBundle))
        assertTrue(result.isFailure)
        assertIs<AdminDeniedException>(result.exceptionOrNull())
        assertEquals(0, calls)
    }

    @Test fun nonAdminMembershipIsDeniedBeforeAnyWrite() = runBlocking {
        val posts = mutableListOf<String>()
        val repo = SupabaseAdminRepository(settings, { "fake-token" }, clientFor { req ->
            when {
                req.url.encodedPath.endsWith("/admin_users") -> okJson("[]")  // not a member
                else -> { posts += req.url.encodedPath; okJson("[]") }
            }
        })
        val result = repo.publishContent(snapshotOf(sampleBundle))
        assertTrue(result.isFailure)
        assertIs<AdminDeniedException>(result.exceptionOrNull())
        assertTrue(posts.isEmpty(), "no content write may happen for a non-admin")
    }

    @Test fun rpcDeniedMapsToDenied(): Unit = runBlocking {
        val repo = SupabaseAdminRepository(settings, { "fake-token" }, rpcClient { req ->
            errJson("42501", "admin_required", HttpStatusCode.Forbidden)
        })
        val result = repo.publishContent(snapshotOf(sampleBundle))
        assertTrue(result.isFailure)
        assertIs<AdminDeniedException>(result.exceptionOrNull())
    }

    @Test fun revisionConflictMapsToRetryableConflict(): Unit = runBlocking {
        val repo = SupabaseAdminRepository(settings, { "fake-token" }, rpcClient { req ->
            errJson("40001", "revision_conflict: section darshan_settings is at revision 4, caller expected 3")
        })
        val result = repo.publishContent(snapshotOf(sampleBundle))
        assertTrue(result.isFailure)
        assertIs<RevisionConflictException>(result.exceptionOrNull())
    }

    @Test fun unknownSectionIsSetupErrorNotDenial(): Unit = runBlocking {
        val repo = SupabaseAdminRepository(settings, { "fake-token" }, rpcClient { req ->
            errJson("22023", "unknown_section:darshan_settings")
        })
        val result = repo.publishContent(snapshotOf(sampleBundle))
        assertTrue(result.isFailure)
        assertIs<IllegalStateException>(result.exceptionOrNull())
    }

    @Test fun missingSnapshotRevisionFailsBeforeRpcCall(): Unit = runBlocking {
        val rpcCalls = mutableListOf<String>()
        val repo = SupabaseAdminRepository(settings, { "fake-token" }, rpcClient { req ->
            rpcCalls += req.url.encodedPath; okJson("4")
        })
        // Snapshot carries no revision for the sections being published.
        val result = repo.publishContent(snapshotOf(sampleBundle, emptyMap()))
        assertTrue(result.isFailure)
        assertIs<IllegalStateException>(result.exceptionOrNull())
        assertTrue(rpcCalls.isEmpty(), "no publish RPC may fire without a snapshot revision")
    }

    @Test fun publishSendsTheSnapshotRevisionNotAFreshRead() = runBlocking {
        val rpcBodies = mutableMapOf<String, String>()
        val repo = SupabaseAdminRepository(settings, { "fake-token" }, rpcClient { req ->
            val fn = req.url.encodedPath.substringAfterLast('/')
            rpcBodies[fn] = (req.body as OutgoingContent.ByteArrayContent).bytes().decodeToString()
            okJson("4")  // RPC returns the new revision scalar
        })
        // The server has meanwhile moved to revision 99; the publish must still
        // send the SNAPSHOT revisions (3 and 5), so the backend can conflict.
        val result = repo.publishContent(snapshotOf(sampleBundle))
        assertTrue(result.isSuccess, "expected success, got ${result.exceptionOrNull()}")
        assertEquals(setOf("publish_darshan_settings", "publish_contacts"), rpcBodies.keys)
        assertTrue(rpcBodies.getValue("publish_darshan_settings").contains(""""p_expected_revision":3"""),
            "darshan publish must carry the snapshot revision: ${rpcBodies["publish_darshan_settings"]}")
        assertTrue(rpcBodies.getValue("publish_contacts").contains(""""p_expected_revision":5"""),
            "contacts publish must carry the snapshot revision: ${rpcBodies["publish_contacts"]}")
        assertTrue(rpcBodies.getValue("publish_contacts").contains(""""role_ml":"സെക്രട്ടറി""""),
            "row payload must reach the RPC: ${rpcBodies["publish_contacts"]}")
    }

    @Test fun twoEditorsStaleSnapshotConflictsAndDoesNotOverwrite() = runBlocking {
        // One mock "server": revision state advances only on a successful publish.
        var contactsRev = 5L
        val engine = clientFor { req ->
            when {
                req.url.encodedPath.endsWith("/admin_users") -> okJson("""[{"user_id":"u1"}]""")
                req.url.encodedPath.endsWith("/content_meta") ->
                    okJson("""[{"section":"contacts","revision":$contactsRev,"verified":false}]""")
                req.url.encodedPath.endsWith("/rpc/publish_contacts") -> {
                    val body = (req.body as OutgoingContent.ByteArrayContent).bytes().decodeToString()
                    val expected = """"p_expected_revision":(\d+)""".toRegex().find(body)?.groupValues?.get(1)?.toLong()
                    if (expected != contactsRev) {
                        errJson("40001", "revision_conflict: section contacts is at revision $contactsRev, caller expected $expected")
                    } else {
                        contactsRev += 1
                        okJson("$contactsRev")
                    }
                }
                else -> okJson("[]")  // content table reads return empty sections
            }
        }
        val editorA = SupabaseAdminRepository(settings, { "fake-token" }, engine)
        val editorB = SupabaseAdminRepository(settings, { "fake-token" }, engine)

        // Both editors load the same snapshot (revision 5).
        val snapA = editorA.loadSnapshot().getOrThrow()
        val snapB = editorB.loadSnapshot().getOrThrow()
        assertEquals(5L, snapA.revisions["contacts"])
        assertEquals(5L, snapB.revisions["contacts"])

        // Editor B publishes first: succeeds and advances the server revision.
        val editB = AdminContentSnapshot(
            ContentBundle(contacts = listOf(ContactDto("സെക്രട്ടറി", "Secretary", "+919446844145"))),
            snapB.revisions)
        assertTrue(editorB.publishContent(editB).isSuccess)
        assertEquals(6L, contactsRev)

        // Editor A publishes its stale snapshot: conflicts, revision untouched.
        val editA = AdminContentSnapshot(
            ContentBundle(contacts = listOf(ContactDto("തിരുമേനി", "Thirumeni", "+919961292222"))),
            snapA.revisions)
        val resultA = editorA.publishContent(editA)
        assertTrue(resultA.isFailure)
        assertIs<RevisionConflictException>(resultA.exceptionOrNull())
        assertEquals(6L, contactsRev, "a conflicted publish must not advance the revision")
    }

    @Test fun midLoadPublishRetriesAndLoadsConsistentSnapshot() = runBlocking {
        // Simulated server: revision starts at 5; a competing admin publishes DURING
        // our first load (between the before and after revision reads).
        var rev = 5L
        var metaReads = 0
        var contentReads = 0
        val repo = SupabaseAdminRepository(settings, { "fake-token" }, clientFor { req ->
            when {
                req.url.encodedPath.endsWith("/admin_users") -> okJson("""[{"user_id":"u1"}]""")
                req.url.encodedPath.endsWith("/content_meta") -> {
                    metaReads += 1
                    if (metaReads == 2) rev = 6L  // competing publish lands mid-first-load
                    okJson("""[{"section":"contacts","revision":$rev,"verified":false}]""")
                }
                else -> { contentReads += 1; okJson("[]") }
            }
        })
        val snap = repo.loadSnapshot().getOrThrow()
        assertEquals(6L, snap.revisions["contacts"],
            "the snapshot must carry the post-race revision, not the stale pre-race one")
        assertTrue(metaReads >= 4, "first load raced (2 meta reads), retry needed 2 more: got $metaReads")
        assertTrue(contentReads >= 2, "content must be re-fetched after the race: got $contentReads")
    }

    @Test fun persistentMidLoadRacesFailAsRetryableConflict(): Unit = runBlocking {
        var rev = 5L
        var metaReads = 0
        val repo = SupabaseAdminRepository(settings, { "fake-token" }, clientFor { req ->
            when {
                req.url.encodedPath.endsWith("/admin_users") -> okJson("""[{"user_id":"u1"}]""")
                req.url.encodedPath.endsWith("/content_meta") -> {
                    metaReads += 1
                    if (metaReads % 2 == 0) rev += 1  // every load races
                    okJson("""[{"section":"contacts","revision":$rev,"verified":false}]""")
                }
                else -> okJson("[]")
            }
        })
        val result = repo.loadSnapshot()
        assertTrue(result.isFailure)
        assertIs<RevisionConflictException>(result.exceptionOrNull())
    }

    @Test fun vazhipadAndFestivalsPublishThroughTheirRpcs() = runBlocking {
        val rpcCalls = mutableListOf<String>()
        val repo = SupabaseAdminRepository(settings, { "fake-token" }, rpcClient { req ->
            rpcCalls += req.url.encodedPath.substringAfterLast('/')
            okJson("8")
        })
        val bundle = ContentBundle(
            vazhipad = listOf(VazhipadDto("പുഷ്പാഞ്ജലി", "Pushpanjali", 150, "offering")),
            festivals = listOf(FestivalDto("ഉത്സവം", "Festival", "🎉", 0L)),
        )
        val result = repo.publishContent(snapshotOf(bundle))
        assertTrue(result.isSuccess, "expected success, got ${result.exceptionOrNull()}")
        assertEquals(listOf("publish_vazhipad", "publish_festivals"), rpcCalls)
    }

    @Test fun midBundleFailureLeavesLaterSectionsUntouched(): Unit = runBlocking {
        val rpcCalls = mutableListOf<String>()
        val repo = SupabaseAdminRepository(settings, { "fake-token" }, rpcClient { req ->
            val fn = req.url.encodedPath.substringAfterLast('/')
            rpcCalls += fn
            if (fn == "publish_darshan_settings") okJson("4")
            else errJson("40001", "revision_conflict: section contacts is at revision 6, caller expected 5")
        })
        // darshan publishes first (own committed transaction), contacts conflicts.
        val result = repo.publishContent(snapshotOf(sampleBundle))
        assertTrue(result.isFailure)
        assertIs<RevisionConflictException>(result.exceptionOrNull())
        assertEquals(listOf("publish_darshan_settings", "publish_contacts"), rpcCalls)
    }

    @Test fun notificationsStayUnsent(): Unit = runBlocking {
        val repo = SupabaseAdminRepository(settings, { "fake-token" }, clientFor { okJson("[]") })
        val result = repo.sendNotification("t", "b", "topic")
        assertTrue(result.isFailure)
        assertIs<NotApprovedException>(result.exceptionOrNull())
    }
                   }
