package page.kakkamvellytemple.app.data.remote

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import page.kakkamvellytemple.app.data.model.WeatherResponse
import page.kakkamvellytemple.app.data.repository.StaticData

class WeatherApi {
    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
        install(Logging) { level = LogLevel.NONE }
    }

    suspend fun fetchWeather(): Result<WeatherResponse> = runCatching {
        client.get(StaticData.WEATHER_URL).body()
    }
}
