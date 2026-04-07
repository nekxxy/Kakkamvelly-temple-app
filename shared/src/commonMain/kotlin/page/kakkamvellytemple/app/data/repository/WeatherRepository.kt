package page.kakkamvellytemple.app.data.repository

import page.kakkamvellytemple.app.data.model.WeatherData
import page.kakkamvellytemple.app.data.remote.WeatherApi
import page.kakkamvellytemple.app.util.WmoDescriptions

class WeatherRepository(private val api: WeatherApi = WeatherApi()) {
    suspend fun getWeather(): Result<WeatherData> {
        return api.fetchWeather().map { response ->
            val c = response.current
            val (icon, desc) = WmoDescriptions.get(c.weatherCode)
            WeatherData(
                temperatureC = c.temperature.toInt(),
                description  = desc,
                icon         = icon,
                windKmh      = c.windSpeed.toInt(),
                humidityPct  = c.humidity
            )
        }
    }
}
