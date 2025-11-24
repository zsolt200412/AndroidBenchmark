package com.example.androidbenchmark.benchmark

import android.os.Build

/**
 * Placeholder for aggregating and persisting benchmark results.
 * Implementation and data model will be added later.
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
        val screenResolution: String
    ){}

        fun processResults(results: BenchmarkResults): ProcessedResult {
        // Calculate total score. 
        val totalScore = results.cpuScore + results.memoryScore + results.gpuScore

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
            screenResolution = results.screenResolution
        )
    }
}
