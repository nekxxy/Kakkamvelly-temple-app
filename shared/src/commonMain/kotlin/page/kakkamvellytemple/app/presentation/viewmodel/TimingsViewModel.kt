package page.kakkamvellytemple.app.presentation.viewmodel

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import page.kakkamvellytemple.app.data.model.PoojaItem
import page.kakkamvellytemple.app.data.repository.StaticData
import page.kakkamvellytemple.app.data.repository.TempleRepository

data class TimingsUiState(
    val morningPoojas: List<PoojaItem> = StaticData.MORNING_POOJAS,
    val eveningPoojas: List<PoojaItem> = StaticData.EVENING_POOJAS,
    val activePoojaIndex: Int = -1
)

class TimingsViewModel(private val repo: TempleRepository = TempleRepository()) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val _state = MutableStateFlow(TimingsUiState())
    val state: StateFlow<TimingsUiState> = _state.asStateFlow()

    init {
        scope.launch {
            while (true) {
                _state.update { it.copy(activePoojaIndex = repo.getCurrentPoojaIndex()) }
                delay(30_000)
            }
        }
    }

    fun dispose() = scope.cancel()
}
