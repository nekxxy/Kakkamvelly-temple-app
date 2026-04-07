package page.kakkamvellytemple.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WeatherResponse(
    val current: CurrentWeather
)

@Serializable
data class CurrentWeather(
    @SerialName("temperature_2m")       val temperature: Double,
    @SerialName("weather_code")          val weatherCode: Int,
    @SerialName("wind_speed_10m")        val windSpeed: Double,
    @SerialName("relative_humidity_2m")  val humidity: Int
)

data class WeatherData(
    val temperatureC: Int,
    val description: String,
    val icon: String,
    val windKmh: Int,
    val humidityPct: Int
)
