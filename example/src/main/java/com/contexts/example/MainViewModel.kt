package com.contexts.example

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.contexts.sniffles.Sniffles
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

data class UiState(
    val loading: Boolean = false,
    val response: String? = null,
)

class MainViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(UiState())
    val uiState = _uiState.asStateFlow()

    private val client = OkHttpClient.Builder()
        .addInterceptor(Sniffles.getInstance().getInterceptor())
        .build()

    fun makeRequest() {
        viewModelScope.launch {
            makeTestRequest()
        }
    }

    private suspend fun makeTestRequest() {
        _uiState.update { it.copy(loading = true, response = null) }
        val request = Request.Builder()
            .url("https://jsonplaceholder.typicode.com/posts/1")
            .build()
        withContext(Dispatchers.IO) {
            val result = client.newCall(request).execute()
            _uiState.update { it.copy(loading = false, response = result.toString()) }
            Log.d(TAG, "Request executed, $result")
        }
    }

    companion object {
        const val TAG = "MainViewModel"
    }
}