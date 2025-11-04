package com.example.androidbenchmark.benchmark

import android.content.Context
import android.os.Handler
import android.os.Looper
import java.util.concurrent.Executors
import kotlin.system.measureTimeMillis


class BenchmarkManager(private val context: Context) {
    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())
    fun startAllTests() {
        val cpuBenchmark = CPUBenchmark();
        cpuBenchmark.runTest();
        val cpuRes = cpuBenchmark.computeArithmetic();

        val memoryBenchmark = MemoryBenchmark();
        memoryBenchmark.runTest();
        val memoryRes = memoryBenchmark.computeMemoryScore();
    }
    /**
     * Shutdown internal resources used by the benchmark manager.
     */
    fun shutdown() {
        executor.shutdownNow()
    }
}
