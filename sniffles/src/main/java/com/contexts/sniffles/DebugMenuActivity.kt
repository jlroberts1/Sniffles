package com.contexts.sniffles

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.contexts.sniffles.model.FailureType
import com.contexts.sniffles.model.NetworkRequestEntry
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

internal class DebugMenuActivity : AppCompatActivity() {
    val viewModel: DebugMenuViewModel by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DebugMenuScreen(viewModel)
        }
    }
}

@Composable
private fun DebugMenuScreen(viewModel: DebugMenuViewModel) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val uiState by viewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Request History") }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Debug Controls") }
            )
        }

        when (selectedTab) {
            0 -> NetworkRequestList(uiState.requestHistory)
            1 -> DebugSettingsScreen(viewModel)
        }
    }
}

@Composable
private fun DebugSettingsScreen(viewModel: DebugMenuViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp)
            .verticalScroll(scrollState, true)
            .fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier)
        NetworkTimelineGraph(
            clearHistory = { viewModel.clearHistory() },
            requests = uiState.requestHistory
        )
        NetworkFailures(
            isFailureEnabled = uiState.isFailureEnabled,
            toggleFailure = { viewModel.toggleFailure(it) },
            selectedFailureType = uiState.selectedFailureType,
            updateFailureType = { viewModel.updateFailureType(it) },
        )
        NetworkDelayCard(
            currentDelay = uiState.delayMs,
            infiniteLoading = uiState.isInfiniteLoading,
            updateDelay = { viewModel.updateDelay(it) },
            toggleInfiniteLoading = { viewModel.toggleInfiniteLoading(it) }
        )
        LastRequestCard(
            lastRequestUrl = uiState.lastRequestUrl,
            lastRequestMethod = uiState.lastRequestMethod
        )
    }
}

@Composable
private fun NetworkDelayCard(
    currentDelay: Float,
    infiniteLoading: Boolean,
    updateDelay: (Float) -> Unit,
    toggleInfiniteLoading: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Card {
        Column(
            modifier = modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Text(
                "Network Delay",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text("Delay: ${currentDelay.toInt()} ms")

            Slider(
                value = currentDelay,
                onValueChange = { updateDelay(it) },
                valueRange = 0f..5000f,
                steps = 50
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                "Infinite Loading",
                style = MaterialTheme.typography.titleMedium
            )
            Switch(
                checked = infiniteLoading,
                onCheckedChange = { toggleInfiniteLoading(it) }
            )
        }
    }
}

@Composable
private fun LastRequestCard(
    lastRequestUrl: String?,
    lastRequestMethod: String?,
    modifier: Modifier = Modifier
) {
    Card {
        Column(
            modifier = modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Text(
                "Last Request",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (lastRequestUrl != null) {
                Text("URL: $lastRequestUrl")
                Text("Method: $lastRequestMethod")
            } else {
                Text("No requests captured yet")
            }
        }
    }
}

@Composable
private fun NetworkFailures(
    isFailureEnabled: Boolean,
    toggleFailure: (Boolean) -> Unit,
    selectedFailureType: FailureType,
    updateFailureType: (FailureType) -> Unit,
    modifier: Modifier = Modifier
) {
    Card {
        Column(
            modifier = modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Text(
                "Network Failures",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            Switch(
                checked = isFailureEnabled,
                onCheckedChange = { toggleFailure(it) },
                modifier = Modifier.align(Alignment.End)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Column {
                FailureType.entries.forEach { failureType ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedFailureType == failureType,
                            onClick = { updateFailureType(failureType) }
                        )
                        Text(
                            text = when (failureType) {
                                FailureType.NETWORK -> "Network Error"
                                FailureType.TIMEOUT -> "Timeout"
                                FailureType.SERVER_ERROR -> "Server Error (500)"
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NetworkRequestList(
    requests: List<NetworkRequestEntry>,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier.padding(horizontal = 8.dp)) {
        items(requests.reversed()) { request ->
            RequestListItem(request)
            HorizontalDivider()
        }
    }
}

@Composable
private fun RequestListItem(
    request: NetworkRequestEntry,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(
                    when (request.statusCode) {
                        in 200..299 -> Color.Green
                        -1 -> Color.Red
                        else -> Color.Yellow
                    },
                    shape = CircleShape
                )
        )

        Spacer(modifier = Modifier.width(8.dp))
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = request.url,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = request.method,
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "${request.duration}ms",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = if (request.statusCode == -1) "Failed" else "Status: ${request.statusCode}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun NetworkTimelineGraph(
    clearHistory: () -> Unit,
    requests: List<NetworkRequestEntry>,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var selectedRequest by remember { mutableStateOf<NetworkRequestEntry?>(null) }

    Card(
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Network Timeline",
                    style = MaterialTheme.typography.titleMedium
                )
                IconButton(onClick = { scope.launch { clearHistory() } }) {
                    Icon(Icons.Default.Clear, "Clear history")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .height(200.dp)
                    .fillMaxWidth()
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures { offset ->
                                selectedRequest = findRequestAtOffset(
                                    offset,
                                    requests,
                                    size
                                )
                            }
                        }
                ) {
                    val width = size.width
                    val height = size.height

                    if (requests.isEmpty()) return@Canvas

                    val timeRange = requests.let { reqs ->
                        val now = System.currentTimeMillis()
                        val oldest = reqs.minOf { it.startTime }
                        oldest..now
                    }

                    drawLine(
                        color = Color.Gray,
                        start = Offset(0f, height - 20),
                        end = Offset(width, height - 20),
                        strokeWidth = 1f
                    )

                    requests.forEach { request ->
                        val x = (request.startTime - timeRange.first).toFloat() /
                                (timeRange.last - timeRange.first) * width
                        val requestWidth = (request.duration.toFloat() /
                                (timeRange.last - timeRange.first) * width).coerceAtLeast(2f)

                        val color = when (request.statusCode) {
                            in 200..299 -> Color.Green
                            -1 -> Color.Red
                            else -> Color.Yellow
                        }

                        drawRect(
                            color = color,
                            topLeft = Offset(x, height - 80),
                            size = Size(requestWidth, 60f)
                        )
                    }
                }
            }

            selectedRequest?.let { request ->
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text("URL: ${request.url}")
                        Text("Method: ${request.method}")
                        Text("Duration: ${request.duration}ms")
                        Text("Status: ${request.statusCode}")
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                LegendItem(color = Color.Green, text = "Success")
                LegendItem(color = Color.Yellow, text = "Error")
                LegendItem(color = Color.Red, text = "Failed")
            }
        }
    }
}

@Composable
private fun LegendItem(color: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(color, RoundedCornerShape(2.dp))
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(text, style = MaterialTheme.typography.bodySmall)
    }
}

private fun findRequestAtOffset(
    offset: Offset,
    requests: List<NetworkRequestEntry>,
    size: IntSize
): NetworkRequestEntry? {
    // Implementation to find which request was clicked
    // Based on the x coordinate and the request's position in the timeline
    // Return the request if found, null otherwise

    return null
}
