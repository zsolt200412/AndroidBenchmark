package com.example.androidbenchmark.benchmark

import android.content.Context
import kotlin.system.measureTimeMillis

data class BenchmarkResults(
    val cpuScore: Long,
    val memoryScore: Long,
    val gpuScore: Long,
    val gpuName: String,
    val screenResolution: String,
    val cpuCoreCount: Int,
    val cpuFrequency: String,
    val cpuTestResults: List<CPUTestResult> = emptyList(),
    val memoryTestResults: List<MemoryTestResult> = emptyList(),
    val memoryInfo: MemoryInfo? = null
)

class BenchmarkManager(private val context: Context) {

    // Added: Run all benchmarks and return the results for UI consumption
    fun runBenchmarksAndGetResults(): BenchmarkResults {
        val cpuBenchmark = CPUBenchmark()
        
        // Run multiple size tests for CPU
        val cpuMultipleTests = cpuBenchmark.runMultipleSizeTests()
        val cpuFloatingPointTests = cpuBenchmark.runMultipleFloatingPointTests()
        val allCpuTests = cpuMultipleTests + cpuFloatingPointTests
        
        // Calculate CPU score from all tests
        val cpuRes = cpuBenchmark.computeCpuScore(allCpuTests)

        val memoryBenchmark = MemoryBenchmark()
        
        // Run multiple size tests for Memory
        val memoryMultipleTests = memoryBenchmark.runMultipleSizeTests()
        
        // Calculate memory score from all tests
        val memoryRes = memoryBenchmark.computeMemoryScore(memoryMultipleTests)
        
        // Get memory info
        val memoryInfo = memoryBenchmark.getMemoryInfo()

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
            cpuFrequency = cpuFrequency,
            cpuTestResults = allCpuTests,
            memoryTestResults = memoryMultipleTests,
            memoryInfo = memoryInfo
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
}
