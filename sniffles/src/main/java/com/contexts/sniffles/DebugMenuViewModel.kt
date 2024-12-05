package com.contexts.sniffles

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.contexts.sniffles.model.FailureType
import com.contexts.sniffles.model.NetworkRequestEntry
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal data class DebugMenuUiState(
    val isFailureEnabled: Boolean = false,
    val selectedFailureType: FailureType = FailureType.NETWORK,
    val delayMs: Float = 0f,
    val lastRequestUrl: String? = null,
    val lastRequestMethod: String? = null,
    val isMockingEnabled: Boolean = false,
    val mockResponseBody: String = "",
    val mockResponseCode: Int = 200,
    val mockUrlPattern: String = "",
    val isInfiniteLoading: Boolean = false,
    val requestHistory: List<NetworkRequestEntry> = listOf()
)

internal class DebugMenuViewModel(
    private val sniffles: Sniffles = Sniffles.getInstance()
) : ViewModel() {

    private val _uiState = MutableStateFlow(DebugMenuUiState())
    val uiState: StateFlow<DebugMenuUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            while (true) {
                val history = sniffles.getInterceptor().getRequestHistory()
                val lastRequest = history.lastOrNull()
                _uiState.update {
                    it.copy(
                        requestHistory = history,
                        lastRequestUrl = lastRequest?.url,
                        lastRequestMethod = lastRequest?.method
                    )
                }
                delay(500)
            }
        }
    }

    fun clearHistory() {
        sniffles.getInterceptor().clearHistory()
    }

    fun toggleInfiniteLoading(enabled: Boolean) {
        _uiState.update { currentState ->
            currentState.copy(isInfiniteLoading = enabled)
        }
        sniffles.getInterceptor().setInfiniteLoading(enabled)
    }

    fun toggleFailure(enabled: Boolean) {
        _uiState.update { currentState ->
            currentState.copy(isFailureEnabled = enabled)
        }
        sniffles.getInterceptor().setFailure(enabled, _uiState.value.selectedFailureType)
    }

    fun updateFailureType(type: FailureType) {
        _uiState.update { currentState ->
            currentState.copy(selectedFailureType = type)
        }
        if (_uiState.value.isFailureEnabled) {
            sniffles.getInterceptor().setFailure(true, type)
        }
    }

    fun updateDelay(delayMs: Float) {
        _uiState.update { currentState ->
            currentState.copy(delayMs = delayMs)
        }
        sniffles.getInterceptor().setDelay(delayMs.toLong())
    }
}