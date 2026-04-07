package page.kakkamvellytemple.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import page.kakkamvellytemple.app.data.model.Festival
import page.kakkamvellytemple.app.data.repository.TempleRepository
import page.kakkamvellytemple.app.util.CountdownParts
import page.kakkamvellytemple.app.util.ISTClock

data class FestivalUiState(
    val nextFestival: Festival? = null,
    val countdown: CountdownParts = CountdownParts(0,0,0,0),
    val upcomingFestivals: List<Festival> = emptyList()
)

class FestivalViewModel(
    private val repo: TempleRepository = TempleRepository()
) : ViewModel() {
    private val _state = MutableStateFlow(FestivalUiState())
    val state: StateFlow<FestivalUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            while (true) {
                val next = repo.getNextFestival()
                val diffMs = (next.dateUtcMs - ISTClock.epochMs()).coerceAtLeast(0)
                _state.update {
                    it.copy(
                        nextFestival     = next,
                        countdown        = ISTClock.formatCountdown(diffMs),
                        upcomingFestivals = repo.getUpcomingFestivals()
                    )
                }
                delay(1_000)
            }
        }
    }
}
