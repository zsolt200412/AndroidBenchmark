package com.example.androidbenchmark.benchmark

import android.content.Context
import android.os.Handler
import android.os.Looper
import java.util.concurrent.Executors
import kotlin.system.measureTimeMillis

data class BenchmarkResults(
    val cpuScore: Long,
    val memoryScore: Long,
    val gpuScore: Long,
    val gpuName: String,
    val screenResolution: String,
    val cpuCoreCount: Int,
    val cpuFrequency: String
)

class BenchmarkManager(private val context: Context) {
    private val executor = Executors.newSingleThreadExecutor()

    // Added: Run all benchmarks and return the results for UI consumption
    fun runBenchmarksAndGetResults(): BenchmarkResults {
        val cpuBenchmark = CPUBenchmark()
        cpuBenchmark.runTest()
        val cpuRes = cpuBenchmark.computeCpuScore()

        val memoryBenchmark = MemoryBenchmark()
        memoryBenchmark.runTest()
        val memoryRes = memoryBenchmark.computeMemoryScore()

        val gpuBenchmark = GPUBenchmark()
        val gpuRes = gpuBenchmark.runTest(context)

        val displayMetrics = context.resources.displayMetrics
        val screenResolution = "${displayMetrics.widthPixels}x${displayMetrics.heightPixels}"
        
        val cpuCoreCount = Runtime.getRuntime().availableProcessors()
        val cpuFrequency = getCpuFrequency()

        return BenchmarkResults(
            cpuScore = cpuRes,
            memoryScore = memoryRes,
            gpuScore = gpuRes.score.toLong(),
            gpuName = gpuRes.rendererName,
            screenResolution = screenResolution,
            cpuCoreCount = cpuCoreCount,
            cpuFrequency = cpuFrequency
        )
    }
    
    private fun getCpuFrequency(): String {
        return try {
            // Attempt to read max frequency of the first core
            val reader = java.io.BufferedReader(java.io.FileReader("/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_max_freq"))
            val freq = reader.readLine().trim().toLong() / 1000 // Convert to MHz
            reader.close()
            "$freq MHz"
        } catch (e: Exception) {
            "Unknown"
        }
    }
    
    /**
     * Shutdown internal resources used by the benchmark manager.
     */
    fun shutdown() {
        executor.shutdownNow()
    }
}
