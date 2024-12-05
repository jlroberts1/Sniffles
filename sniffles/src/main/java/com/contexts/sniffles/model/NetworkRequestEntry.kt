package com.contexts.sniffles.model

data class NetworkRequestEntry(
    val url: String,
    val method: String,
    val startTime: Long,
    val duration: Long,
    val statusCode: Int,
    val isMocked: Boolean = false
) {
    val endTime: Long = startTime + duration
}