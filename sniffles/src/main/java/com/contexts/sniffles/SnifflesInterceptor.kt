package com.contexts.sniffles

import com.contexts.sniffles.model.FailureType
import com.contexts.sniffles.model.NetworkRequestEntry
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.IOException
import java.net.SocketTimeoutException
import java.util.Collections
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

class SnifflesInterceptor : Interceptor {
    private val shouldFail = AtomicBoolean(false)
    private val delayMillis = AtomicLong(0)
    private val lastRequest = AtomicReference<Request>()
    private val failureType = AtomicReference(FailureType.NETWORK)
    private val isInfiniteLoading = AtomicBoolean(false)
    private val requestHistory = Collections.synchronizedList(mutableListOf<NetworkRequestEntry>())

    fun getRequestHistory(): List<NetworkRequestEntry> = requestHistory.toList()

    fun clearHistory() {
        requestHistory.clear()
    }

    fun setInfiniteLoading(enabled: Boolean) {
        isInfiniteLoading.set(enabled)
    }

    fun setFailure(should: Boolean, type: FailureType = FailureType.NETWORK) {
        shouldFail.set(should)
        failureType.set(type)
    }

    fun setDelay(millis: Long) {
        delayMillis.set(millis)
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val startTime = System.currentTimeMillis()
        lastRequest.set(request)
        try {
            if (isInfiniteLoading.get()) {
                runBlocking {
                    while (true) {
                        delay(1000)
                    }
                }
            }

            val currentDelay = delayMillis.get()
            if (currentDelay > 0) {
                runBlocking {
                    delay(currentDelay)
                }
            }

            if (shouldFail.get()) {
                when (failureType.get()) {
                    FailureType.NETWORK -> {
                        addToHistory(
                            NetworkRequestEntry(
                                url = request.url.toString(),
                                method = request.method,
                                startTime = startTime,
                                duration = System.currentTimeMillis() - startTime,
                                statusCode = -1,
                                isMocked = false
                            )
                        )
                        throw IOException("Simulated network failure")
                    }

                    FailureType.TIMEOUT -> {
                        addToHistory(
                            NetworkRequestEntry(
                                url = request.url.toString(),
                                method = request.method,
                                startTime = startTime,
                                duration = System.currentTimeMillis() - startTime,
                                statusCode = -1,
                                isMocked = false
                            )
                        )
                        throw SocketTimeoutException("Simulated timeout")
                    }

                    FailureType.SERVER_ERROR -> {
                        val response = Response.Builder()
                            .request(request)
                            .protocol(Protocol.HTTP_1_1)
                            .code(500)
                            .message("Internal Server Error")
                            .body("".toResponseBody(null))
                            .build()

                        addToHistory(
                            NetworkRequestEntry(
                                url = request.url.toString(),
                                method = request.method,
                                startTime = startTime,
                                duration = System.currentTimeMillis() - startTime,
                                statusCode = 500,
                                isMocked = false
                            )
                        )
                        return response
                    }

                    else -> Unit
                }
            }

            val response = chain.proceed(request)

            addToHistory(
                NetworkRequestEntry(
                    url = request.url.toString(),
                    method = request.method,
                    startTime = startTime,
                    duration = System.currentTimeMillis() - startTime,
                    statusCode = response.code,
                    isMocked = false
                )
            )

            return response
        } catch (e: Exception) {
            addToHistory(
                NetworkRequestEntry(
                    url = request.url.toString(),
                    method = request.method,
                    startTime = startTime,
                    duration = System.currentTimeMillis() - startTime,
                    statusCode = -1,
                    isMocked = false
                )
            )
            throw e
        }
    }

    private fun addToHistory(entry: NetworkRequestEntry) {
        requestHistory.add(entry)
        if (requestHistory.size > MAX_HISTORY_SIZE) {
            requestHistory.removeAt(0)
        }
    }

    companion object {
        const val MAX_HISTORY_SIZE = 50
    }
}