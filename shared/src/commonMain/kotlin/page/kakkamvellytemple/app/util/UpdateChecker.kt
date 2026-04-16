package page.kakkamvellytemple.app.util

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.serialization.json.*

data class UpdateInfo(
    val available: Boolean,
    val latestVersion: String,
    val downloadUrl: String,
    val releaseNotes: String
)

object UpdateChecker {
    private const val RELEASES_API = "https://api.github.com/repos/nekxxy/Kakkamvelly-temple-app/releases/latest"
    const val CURRENT_VERSION = "1.0.0"
    const val CURRENT_BUILD   = 1  // increment with each release
    const val APK_URL = "https://github.com/nekxxy/Kakkamvelly-temple-app/releases/latest/download/kakkamvelly-temple.apk"

    suspend fun check(): UpdateInfo {
        return try {
            val client = HttpClient()
            val response = client.get(RELEASES_API) {
                header("Accept", "application/vnd.github.v3+json")
                header("User-Agent", "KVT-App")
            }
            val json = Json { ignoreUnknownKeys = true }
            val body = response.bodyAsText()
            val obj = json.parseToJsonElement(body).jsonObject
            val tagName = obj["tag_name"]?.jsonPrimitive?.content ?: ""
            val name    = obj["name"]?.jsonPrimitive?.content ?: ""
            val notes   = obj["body"]?.jsonPrimitive?.content ?: ""

            // Extract build number from release name "v1.0.X"
            val remoteBuild = tagName.removePrefix("v").split(".").lastOrNull()?.toIntOrNull() ?: 0
            val available = remoteBuild > CURRENT_BUILD && tagName != "latest"

            client.close()
            UpdateInfo(
                available     = available,
                latestVersion = name.substringBefore(" —").trim(),
                downloadUrl   = APK_URL,
                releaseNotes  = notes
            )
        } catch (e: Exception) {
            UpdateInfo(false, CURRENT_VERSION, APK_URL, "")
        }
    }
}
