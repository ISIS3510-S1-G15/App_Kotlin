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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RankingUiState(
    val criterion: RankingCriterion = RankingCriterion.OVERALL,
    val window: RankingWindow = RankingWindow.MONTH,
    val items: List<RankedRestaurant> = emptyList(),
    val loading: Boolean = true,
    val online: Boolean = true,
    val lastUpdated: Long? = null,
    val fromSnapshot: Boolean = false,
    val refreshQueued: Boolean = false
)

class RankingViewModel(
    private val repository: RankingRepository,
    connectivity: ConnectivityMonitor,
    private val scheduleDeferredRefresh: () -> Unit = {},
    deferredRefreshPending: Flow<Boolean> = emptyFlow()
) : ViewModel() {

    private val _state = MutableStateFlow(RankingUiState(online = connectivity.online.value))
    val state: StateFlow<RankingUiState> = _state.asStateFlow()

    private var job: Job? = null

    init {
        viewModelScope.launch {
            var wasOnline = _state.value.online
            connectivity.online.collect { online ->
                _state.update { it.copy(online = online) }
                if (online && !wasOnline && repository.isStale()) load(refresh = true)
                wasOnline = online
            }
        }
        viewModelScope.launch {
            var wasPending = false
            deferredRefreshPending.collect { pending ->
                _state.update { it.copy(refreshQueued = pending) }
                if (wasPending && !pending) load(refresh = false)
                wasPending = pending
            }
        }
        open()
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
        if (_state.value.online) load(refresh = true) else scheduleDeferredRefresh()
    }

    private fun open() {
        job?.cancel()
        val current = _state.value
        job = viewModelScope.launch {
            repository.snapshot(current.criterion, current.window)?.let { stored ->
                _state.update {
                    it.copy(items = stored.items, loading = false, lastUpdated = stored.computedAt, fromSnapshot = true)
                }
            }
            val local = repository.load(current.criterion, current.window, refresh = false)
            _state.update {
                it.copy(items = local.items, loading = false, lastUpdated = repository.lastRefreshAt() ?: local.computedAt, fromSnapshot = false)
            }
            if (_state.value.online && repository.isStale()) load(refresh = true)
        }
    }

    private fun load(refresh: Boolean) {
        job?.cancel()
        val current = _state.value
        _state.update { it.copy(loading = true) }
        job = viewModelScope.launch {
            val result = repository.load(current.criterion, current.window, refresh)
            _state.update {
                it.copy(
                    items = result.items,
                    loading = false,
                    lastUpdated = repository.lastRefreshAt() ?: result.computedAt,
                    fromSnapshot = false
                )
            }
        }
    }

    companion object {
        fun factory(services: AppServices): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                RankingViewModel(
                    repository = services.rankingRepository,
                    connectivity = services.connectivity,
                    scheduleDeferredRefresh = services.syncScheduler::scheduleRankingRefresh,
                    deferredRefreshPending = services.syncScheduler.rankingRefreshPending()
                )
            }
        }
    }
}
