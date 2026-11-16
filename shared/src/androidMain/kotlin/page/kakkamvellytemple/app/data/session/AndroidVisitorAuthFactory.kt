package page.kakkamvellytemple.app.data.session

import android.content.Context
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import page.kakkamvellytemple.app.data.remote.*
import page.kakkamvellytemple.app.data.repository.VisitorAuthRepository

/** Explicit composition, still disabled by default. Lifecycle owner must close
 * the client on final disposal. No Logging plugin, insecure store fallback,
 * admin dependency, or provider credential is installed. */
class AndroidVisitorAuthFactory(context: Context, settings: SupabaseContentSource.Settings,
    deliveryEnabled: Boolean = false) {
    private val client = HttpClient(Android) {
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
    }
    val auth: VisitorAuthRepository = VisitorWhatsAppAuth(settings, client,
        deliveryEnabled, AndroidVisitorSessionStore(context.applicationContext))
    fun close() = client.close()
}
