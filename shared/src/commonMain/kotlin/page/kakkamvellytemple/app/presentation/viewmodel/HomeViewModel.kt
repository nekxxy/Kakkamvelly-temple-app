package page.kakkamvellytemple.app.presentation.viewmodel

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import page.kakkamvellytemple.app.data.model.*
import page.kakkamvellytemple.app.data.repository.TempleRepository
import page.kakkamvellytemple.app.data.repository.WeatherRepository
import page.kakkamvellytemple.app.util.CountdownParts
import page.kakkamvellytemple.app.util.ISTClock

data class HomeUiState(
    val darshanStatus: DarshanStatus? = null,
    val nextFestival: Festival? = null,
    val countdown: CountdownParts = CountdownParts(0, 0, 0, 0),
    val weather: WeatherData? = null,
    val weatherLoading: Boolean = true,
    val sunTimes: SunTimes? = null,
    val moonPhase: MoonPhase? = null,
    val istTimeStr: String = "",
    val annadhanam: AnnadhanamInfo? = null
)

class HomeViewModel(
    private val templeRepo: TempleRepository = TempleRepository(),
    private val weatherRepo: WeatherRepository = WeatherRepository()
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    init {
        startClock()
        loadWeather()
        loadStaticData()
    }

    private fun startClock() = scope.launch {
        while (true) {
            val ist = ISTClock.istTime()
            val h = if (ist.hour % 12 == 0) 12 else ist.hour % 12
            val ampm = if (ist.hour < 12) "AM" else "PM"
            val timeStr = "$h:${ist.minute.toString().padStart(2,'0')}:${ist.second.toString().padStart(2,'0')} $ampm IST"
            val nextFest = templeRepo.getNextFestival()
            val diffMs = (nextFest.dateUtcMs - ISTClock.epochMs()).coerceAtLeast(0)
            _state.update {
                it.copy(
                    istTimeStr    = timeStr,
                    darshanStatus = templeRepo.getDarshanStatus(),
                    nextFestival  = nextFest,
                    countdown     = ISTClock.formatCountdown(diffMs)
                )
            }
            delay(1_000)
        }
    }

    private fun loadWeather() = scope.launch {
        weatherRepo.getWeather()
            .onSuccess { w -> _state.update { it.copy(weather = w, weatherLoading = false) } }
            .onFailure {   _state.update { it.copy(weatherLoading = false) } }
    }

    private fun loadStaticData() {
        _state.update {
            it.copy(
                sunTimes   = templeRepo.getSunTimes(),
                moonPhase  = templeRepo.getMoonPhase(),
                annadhanam = templeRepo.getAnnadhanam()
            )
        }
    }

    fun refreshWeather() = loadWeather()
    fun dispose() = scope.cancel()
}
