package page.kakkamvellytemple.app.data.remote

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import page.kakkamvellytemple.app.data.repository.ContentStore

/** Fetches the editable content bundle (pooja timings, contacts, vazhipad rates,
 *  today's events, festivals, gallery, darshan hours) from a plain JSON endpoint.
 *  Endpoint can later point at Supabase REST without app changes. */
object ContentSync {
    const val CONTENT_URL = "https://kakkamvellytemple.page/app-content.json"

    /** Connected backend setup is NOT approved yet. While false, refresh() performs
     *  no network access and the app runs entirely on packaged defaults. */
    const val CONNECTED_SETUP_APPROVED = false

    private val client = HttpClient {
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        install(Logging) { level = LogLevel.NONE }
    }

    var lastError: String? = null
        private set

    /** Inject Supabase credentials at app start (from BuildConfig <- env/backend.properties).
     *  Blank values leave the source unconfigured and refresh() uses the JSON endpoint. */
    fun configureSupabase(url: String, anonKey: String) {
        SupabaseContentSource.configure(url, anonKey)
    }

    suspend fun refresh(url: String = CONTENT_URL): Boolean {
        if (!CONNECTED_SETUP_APPROVED) return false  // inert until connected setup is approved
        return try {
        val fresh: ContentBundle =
            if (SupabaseContentSource.settings != null) SupabaseContentSource.fetchBundle(client)
            else client.get(url).body()
        if (fresh.version >= (ContentStore.bundle?.version ?: Int.MIN_VALUE)) {
            ContentStore.install(fresh)
        }
        lastError = null
        true
        } catch (t: Throwable) {
            lastError = t.message
            false
        }
    }
}
