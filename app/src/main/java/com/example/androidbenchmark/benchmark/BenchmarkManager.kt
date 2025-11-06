package com.example.androidbenchmark.benchmark

import android.content.Context
import android.os.Handler
import android.os.Looper
import java.util.concurrent.Executors
import kotlin.system.measureTimeMillis

data class BenchmarkResults(
    val cpuScore: Long,
    val memoryScore: Long,
    val gpuScore: Long
)

class BenchmarkManager(private val context: Context) {
    private val executor = Executors.newSingleThreadExecutor()

    // Added: Run all benchmarks and return the results for UI consumption
    fun runBenchmarksAndGetResults(): BenchmarkResults {
        val cpuBenchmark = CPUBenchmark()
        cpuBenchmark.runTest()
        val cpuRes = cpuBenchmark.computeArithmetic()

        val memoryBenchmark = MemoryBenchmark()
        memoryBenchmark.runTest()
        val memoryRes = memoryBenchmark.computeMemoryScore()

        val gpuBenchmark = GPUBenchmark()
        val gpuRes = gpuBenchmark.runTest(context)

        return BenchmarkResults(
            cpuScore = cpuRes,
            memoryScore = memoryRes,
            gpuScore = gpuRes.toLong()
        )
    }
    
    /**
     * Shutdown internal resources used by the benchmark manager.
     */
    fun shutdown() {
        executor.shutdownNow()
    }
}
