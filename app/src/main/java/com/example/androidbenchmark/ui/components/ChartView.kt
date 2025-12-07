package com.example.androidbenchmark.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.androidbenchmark.benchmark.ResultManager
import kotlin.math.max

/**
 * Composable for rendering benchmark charts with performance graphs.
 */
@Composable
fun ChartView(result: ResultManager.ProcessedResult?) {
    if (result == null) {
        Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
            Text("No results yet")
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "Total Score: ${result.totalScore}",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        BenchmarkBar("CPU", "${result.cpuScore} OPS")
        Spacer(modifier = Modifier.height(8.dp))
        BenchmarkBar("Memory", "${result.memoryScore} OPS")
        Spacer(modifier = Modifier.height(8.dp))
        BenchmarkBar("GPU", "${result.gpuScore} FPS")

        Spacer(modifier = Modifier.height(24.dp))

        // CPU Integer Performance Graph
        if (result.cpuIntegerTestResults.isNotEmpty()) {
            PerformanceGraphCard(
                title = "CPU Integer Performance (Bubble Sort)",
                testResults = result.cpuIntegerTestResults,
                yAxisLabel = "Operations/Second",
                color = Color(0xFF2196F3)
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // CPU Floating-Point Performance Graph
        if (result.cpuFloatingPointTestResults.isNotEmpty()) {
            PerformanceGraphCard(
                title = "CPU Floating-Point Performance (FLOPS)",
                testResults = result.cpuFloatingPointTestResults,
                yAxisLabel = "Operations/Second",
                color = Color(0xFFFF9800)
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Memory Performance Graph
        if (result.memoryTestResults.isNotEmpty()) {
            MemoryPerformanceGraphCard(
                title = "Memory Performance (Matrix Multiplication)",
                testResults = result.memoryTestResults,
                color = Color(0xFF4CAF50)
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Memory Information Card
        result.memoryInfo?.let { memInfo ->
            MemoryInfoCard(memoryInfo = memInfo)
            Spacer(modifier = Modifier.height(16.dp))
        }

        Text(
            text = "Device Info",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        DeviceInfoRow("Model", "${result.manufacturer} ${result.deviceModel}")
        DeviceInfoRow("OS", result.androidVersion)
        DeviceInfoRow("CPU", "${result.cpuCoreCount} Cores @ ${result.cpuFrequency}")
        DeviceInfoRow("GPU", result.gpuName)
        DeviceInfoRow("Resolution", result.screenResolution)
    }
}

@Composable
fun MemoryInfoCard(memoryInfo: com.example.androidbenchmark.benchmark.MemoryInfo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Memory Information",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            MemoryInfoRow("Max Memory", "${memoryInfo.maxMB} MB")
            MemoryInfoRow("Total Memory", "${memoryInfo.totalMB} MB")
            MemoryInfoRow("Used Memory", "${memoryInfo.usedMB} MB")
            MemoryInfoRow("Free Memory", "${memoryInfo.freeMB} MB")
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Memory usage bar
            val usagePercent = if (memoryInfo.maxMB > 0) {
                (memoryInfo.usedMB.toFloat() / memoryInfo.maxMB.toFloat() * 100).toInt()
            } else 0
            
            Text(
                text = "Memory Usage: $usagePercent%",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@Composable
fun MemoryInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun PerformanceGraphCard(
    title: String,
    testResults: List<com.example.androidbenchmark.benchmark.CPUTestResult>,
    yAxisLabel: String,
    color: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            
            // Debug info
            Text(
                text = "Data points: ${testResults.size}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            
            // Show averaging info if applicable
            if (testResults.isNotEmpty() && testResults.first().runs > 1) {
                Text(
                    text = "Averaged over ${testResults.first().runs} runs",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            
            // Graph
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .padding(top = 8.dp, bottom = 8.dp)
            ) {
                if (testResults.isEmpty()) return@Canvas
                
                val padding = 40f
                val graphWidth = size.width - padding * 2
                val graphHeight = size.height - padding * 2
                
                // Find max values
                val maxOps = testResults.maxOfOrNull { it.operationsPerSecond }?.toFloat() ?: 1f
                val maxSize = testResults.maxOfOrNull { it.size }?.toFloat() ?: 1f
                
                // Draw axes
                drawLine(
                    color = Color.Gray,
                    start = Offset(padding, padding),
                    end = Offset(padding, size.height - padding),
                    strokeWidth = 2f
                )
                drawLine(
                    color = Color.Gray,
                    start = Offset(padding, size.height - padding),
                    end = Offset(size.width - padding, size.height - padding),
                    strokeWidth = 2f
                )
                
                // Draw line graph
                val path = Path()
                testResults.forEachIndexed { index, result ->
                    // Safe division - handle single point case
                    val xFactor = if (testResults.size > 1) {
                        index.toFloat() / (testResults.size - 1).toFloat()
                    } else {
                        0.5f // Center single point
                    }
                    val x = padding + xFactor * graphWidth
                    val y = size.height - padding - (result.operationsPerSecond.toFloat() / maxOps) * graphHeight
                    
                    if (index == 0) {
                        path.moveTo(x, y)
                    } else {
                        path.lineTo(x, y)
                    }
                    
                    // Draw point
                    drawCircle(
                        color = color,
                        radius = 6f,
                        center = Offset(x, y)
                    )
                }
                
                drawPath(
                    path = path,
                    color = color,
                    style = Stroke(width = 3f)
                )
            }
            
            // Legend
            Column(modifier = Modifier.padding(top = 8.dp)) {
                testResults.forEach { result ->
                    Text(
                        text = "N=${result.size}: ${formatLargeNumber(result.operationsPerSecond)} OPS (${result.durationMs}ms avg)",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
fun MemoryPerformanceGraphCard(
    title: String,
    testResults: List<com.example.androidbenchmark.benchmark.MemoryTestResult>,
    color: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            
            // Show averaging info if applicable
            if (testResults.isNotEmpty() && testResults.first().runs > 1) {
                Text(
                    text = "Averaged over ${testResults.first().runs} runs",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            
            // Graph
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .padding(top = 8.dp, bottom = 8.dp)
            ) {
                if (testResults.isEmpty()) return@Canvas
                
                val padding = 40f
                val graphWidth = size.width - padding * 2
                val graphHeight = size.height - padding * 2
                
                // Find max values
                val maxOps = testResults.maxOfOrNull { it.operationsPerSecond }?.toFloat() ?: 1f
                
                // Draw axes
                drawLine(
                    color = Color.Gray,
                    start = Offset(padding, padding),
                    end = Offset(padding, size.height - padding),
                    strokeWidth = 2f
                )
                drawLine(
                    color = Color.Gray,
                    start = Offset(padding, size.height - padding),
                    end = Offset(size.width - padding, size.height - padding),
                    strokeWidth = 2f
                )
                
                // Draw line graph
                val path = Path()
                testResults.forEachIndexed { index, result ->
                    val x = padding + (index.toFloat() / (testResults.size - 1).toFloat()) * graphWidth
                    val y = size.height - padding - (result.operationsPerSecond.toFloat() / maxOps) * graphHeight
                    
                    if (index == 0) {
                        path.moveTo(x, y)
                    } else {
                        path.lineTo(x, y)
                    }
                    
                    // Draw point
                    drawCircle(
                        color = color,
                        radius = 6f,
                        center = Offset(x, y)
                    )
                }
                
                drawPath(
                    path = path,
                    color = color,
                    style = Stroke(width = 3f)
                )
            }
            
            // Legend
            Column(modifier = Modifier.padding(top = 8.dp)) {
                testResults.forEach { result ->
                    Text(
                        text = "N=${result.size}: ${formatLargeNumber(result.operationsPerSecond)} OPS, ${result.bandwidthMBps.toInt()} MB/s (${result.durationMs}ms avg)",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

fun formatLargeNumber(number: Long): String {
    return when {
        number >= 1_000_000_000 -> String.format("%.2fG", number / 1_000_000_000.0)
        number >= 1_000_000 -> String.format("%.2fM", number / 1_000_000.0)
        number >= 1_000 -> String.format("%.2fK", number / 1_000.0)
        else -> number.toString()
    }
}

@Composable
fun DeviceInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(start = 16.dp)
        )
    }
}

@Composable
fun BenchmarkBar(label: String, valueText: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, style = MaterialTheme.typography.bodyMedium)
            Text(text = valueText, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
        }
    }
}
