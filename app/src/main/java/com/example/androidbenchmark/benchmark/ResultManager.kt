package com.example.androidbenchmark.benchmark

import android.os.Build

/**
 * Aggregating and persisting benchmark results with multiple test runs.
 */
class ResultManager {

    data class ProcessedResult(
        val cpuScore: Long,
        val memoryScore: Long,
        val gpuScore: Long,
        val totalScore: Long,
        val deviceModel: String,
        val manufacturer: String,
        val androidVersion: String,
        val cpuCoreCount: Int,
        val cpuFrequency: String,
        val gpuName: String,
        val screenResolution: String,
        val cpuIntegerTestResults: List<CPUTestResult> = emptyList(),
        val cpuFloatingPointTestResults: List<CPUTestResult> = emptyList(),
        val memoryTestResults: List<MemoryTestResult> = emptyList(),
        val memoryInfo: MemoryInfo? = null,
        val totalRamMB: Long? = null,
        val batteryLevelPercent: Int? = null,
        val batteryCapacityMah: Double? = null,
        val availableRamMB: Long? = null
    )

    fun processResults(results: BenchmarkResults): ProcessedResult {
        // Calculate total score. 
        val totalScore = results.cpuScore + results.memoryScore + results.gpuScore

        // Split CPU results into integer and floating-point tests
        // Integer tests use sizes up to ~15K, floating-point tests use 100K+
        val integerTests = results.cpuTestResults.filter { it.size < 100000 } // Array sizes for sorting
        val floatingPointTests = results.cpuTestResults.filter { it.size >= 100000 } // Iteration counts for FP

        return ProcessedResult(
            cpuScore = results.cpuScore,
            memoryScore = results.memoryScore,
            gpuScore = results.gpuScore,
            totalScore = totalScore,
            deviceModel = Build.MODEL,
            manufacturer = Build.MANUFACTURER,
            androidVersion = "Android ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})",
            cpuCoreCount = results.cpuCoreCount,
            cpuFrequency = results.cpuFrequency,
            gpuName = results.gpuName,
            screenResolution = results.screenResolution,
            cpuIntegerTestResults = integerTests,
            cpuFloatingPointTestResults = floatingPointTests,
            memoryTestResults = results.memoryTestResults,
            memoryInfo = results.memoryInfo,
            totalRamMB = results.totalRamMB,
            batteryLevelPercent = results.batteryLevelPercent,
            batteryCapacityMah = results.batteryCapacityMah,
            availableRamMB = results.availableRamMB
        )
    }
}
