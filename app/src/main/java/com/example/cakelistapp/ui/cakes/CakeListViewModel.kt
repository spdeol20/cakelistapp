package com.example.cakelistapp.ui.cakes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.cakelistapp.data.remote.network.CakeApiLog
import com.example.cakelistapp.domain.model.Cake
import com.example.cakelistapp.domain.repository.CakeRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CakeListViewModel(
    private val repository: CakeRepository,
    private val loadErrorMessage: String,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CakeListUiState())
    val uiState: StateFlow<CakeListUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    init {
        loadCakes()
    }

    fun loadCakes(isRefresh: Boolean = false) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.update { state ->
                state.copy(
                    isInitialLoading = !isRefresh || state.cakes.isEmpty(),
                    isRefreshing = isRefresh && state.cakes.isNotEmpty(),
                    errorMessage = null,
                )
            }

            try {
                CakeApiLog.debug("Loading cakes isRefresh=$isRefresh")
                val cakes = repository.getCakes()
                CakeApiLog.debug("Loaded ${cakes.size} cakes")
                _uiState.update { state ->
                    state.copy(
                        cakes = cakes,
                        isInitialLoading = false,
                        isRefreshing = false,
                        errorMessage = null,
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                // TODO: Map failures to distinct causes (no connectivity, HTTP status,
                //  deserialisation) so the message is actionable instead of one generic string.
                CakeApiLog.debug("Cakes load failed")
                _uiState.update { state ->
                    state.copy(
                        isInitialLoading = false,
                        isRefreshing = false,
                        errorMessage = loadErrorMessage,
                    )
                }
            }
        }
    }

    fun onCakeSelected(cake: Cake) {
        _uiState.update { it.copy(selectedCake = cake) }
    }

    fun onDialogDismissed() {
        _uiState.update { it.copy(selectedCake = null) }
    }

    fun errorMessageShown() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    companion object {
        fun factory(
            repository: CakeRepository,
            loadErrorMessage: String,
        ): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return CakeListViewModel(repository, loadErrorMessage) as T
                }
            }
        }
    }
}
