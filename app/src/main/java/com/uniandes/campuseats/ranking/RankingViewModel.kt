package com.uniandes.campuseats.ranking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.uniandes.campuseats.AppServices
import com.uniandes.campuseats.data.ranking.RankedRestaurant
import com.uniandes.campuseats.data.ranking.RankingCriterion
import com.uniandes.campuseats.data.ranking.RankingRepository
import com.uniandes.campuseats.data.ranking.RankingWindow
import com.uniandes.campuseats.net.ConnectivityMonitor
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RankingUiState(
    val criterion: RankingCriterion = RankingCriterion.OVERALL,
    val window: RankingWindow = RankingWindow.WEEK,
    val items: List<RankedRestaurant> = emptyList(),
    val loading: Boolean = true,
    val online: Boolean = true,
    val lastUpdated: Long? = null
)

class RankingViewModel(
    private val repository: RankingRepository,
    connectivity: ConnectivityMonitor
) : ViewModel() {

    private val _state = MutableStateFlow(RankingUiState(online = connectivity.online.value))
    val state: StateFlow<RankingUiState> = _state.asStateFlow()

    private var job: Job? = null

    init {
        viewModelScope.launch {
            connectivity.online.collect { online -> _state.update { it.copy(online = online) } }
        }
        load(refresh = false)
    }

    fun selectCriterion(criterion: RankingCriterion) {
        if (criterion == _state.value.criterion) return
        _state.update { it.copy(criterion = criterion) }
        load(refresh = false)
    }

    fun selectWindow(window: RankingWindow) {
        if (window == _state.value.window) return
        _state.update { it.copy(window = window) }
        load(refresh = false)
    }

    fun refresh() {
        load(refresh = _state.value.online)
    }

    private fun load(refresh: Boolean) {
        job?.cancel()
        val current = _state.value
        _state.update { it.copy(loading = true) }
        job = viewModelScope.launch {
            val result = repository.load(current.criterion, current.window, refresh)
            _state.update {
                it.copy(items = result.items, loading = false, lastUpdated = result.computedAt)
            }
        }
    }

    companion object {
        fun factory(services: AppServices): ViewModelProvider.Factory = viewModelFactory {
            initializer { RankingViewModel(services.rankingRepository, services.connectivity) }
        }
    }
}
