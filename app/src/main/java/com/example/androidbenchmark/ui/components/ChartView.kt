package com.example.androidbenchmark.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.androidbenchmark.benchmark.ResultManager

/**
 * Composable for rendering benchmark charts.
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

        BenchmarkBar("CPU", result.cpuScore.toString())
        Spacer(modifier = Modifier.height(8.dp))
        BenchmarkBar("Memory", result.memoryScore.toString())
        Spacer(modifier = Modifier.height(8.dp))
        BenchmarkBar("GPU", "${result.gpuScore} FPS")

        Spacer(modifier = Modifier.height(24.dp))

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
