package com.example.androidbenchmark.benchmark

import kotlin.random.Random

data class MemoryTestResult(
    val size: Int,
    val durationMs: Long,
    val operationsPerSecond: Long,
    val bandwidthMBps: Double,
    val runs: Int = 1  // Number of runs averaged
)

/**
 * Memory benchmark with multiple size tests
 * IMPROVED: Larger sizes and nanosecond precision for better device differentiation
 */
class MemoryBenchmark {
    private val testResults = mutableListOf<MemoryTestResult>()
    private val numRuns = 3  // Run each test 3 times and average
    
    // Volatile to prevent compiler optimization
    @Volatile
    private var preventOptimization: Any? = null

    /**
     * Run memory benchmark with multiple matrix sizes
     * INCREASED SIZES: 200-800 for better differentiation
     */
    fun runMultipleSizeTests(): List<MemoryTestResult> {
        testResults.clear()
        // INCREASED: Now using 200x200 to 800x800 matrices
        val sizes = listOf(200, 400, 600, 800, 1000)
        
        // Warmup
        println("Warming up memory benchmark...")
        runMatrixTestSingle(100)
        
        for (size in sizes) {
            val result = runMatrixTestWithAverage(size, numRuns)
            testResults.add(result)
        }
        
        return testResults
    }

    private fun runMatrixTestSingle(size: Int): MemoryTestResult {
        val matrixA = Array(size) { IntArray(size) { Random.nextInt(100) } }
        val matrixB = Array(size) { IntArray(size) { Random.nextInt(100) } }
        val result = Array(size) { IntArray(size) }

        val startTime = System.nanoTime()
        
        // Perform Matrix Multiplication
        for (i in 0 until size) {
            for (j in 0 until size) {
                var sum = 0
                for (k in 0 until size) {
                    sum += matrixA[i][k] * matrixB[k][j]
                }
                result[i][j] = sum
            }
        }

        val durationNs = System.nanoTime() - startTime
        val durationMs = durationNs / 1_000_000
        
        // Prevent optimization
        preventOptimization = result[0][0]
        
        // Calculate operations
        val operations = size.toLong() * size * size
        val operationsPerSecond = if (durationNs > 0) {
            (operations * 1_000_000_000) / durationNs
        } else {
            operations * 1000
        }
        
        // Calculate memory bandwidth
        val bytesTransferred = operations * 12.0
        val bandwidthMBps = (bytesTransferred / durationMs).coerceAtLeast(1.0) / 1000.0
        
        return MemoryTestResult(size, durationMs, operationsPerSecond, bandwidthMBps, 1)
    }

    /**
     * Run matrix test multiple times and return average result
     * NOW USING NANOSECOND PRECISION
     */
    private fun runMatrixTestWithAverage(size: Int, runs: Int): MemoryTestResult {
        var totalDurationNs = 0L
        var totalOps = 0L
        var totalBandwidth = 0.0
        
        println("Running matrix test (size=$size) $runs times...")
        
        for (run in 1..runs) {
            val matrixA = Array(size) { IntArray(size) { Random.nextInt(100) } }
            val matrixB = Array(size) { IntArray(size) { Random.nextInt(100) } }
            val result = Array(size) { IntArray(size) }

            val startTime = System.nanoTime()
            
            // Perform Matrix Multiplication
            for (i in 0 until size) {
                for (j in 0 until size) {
                    var sum = 0
                    for (k in 0 until size) {
                        sum += matrixA[i][k] * matrixB[k][j]
                    }
                    result[i][j] = sum
                }
            }

            val durationNs = System.nanoTime() - startTime
            
            // Prevent optimization
            preventOptimization = result[0][0]
            
            totalDurationNs += durationNs
            
            // Calculate operations
            val operations = size.toLong() * size * size
            val durationMs = durationNs / 1_000_000
            val operationsPerSecond = if (durationNs > 0) {
                (operations * 1_000_000_000) / durationNs
            } else {
                operations * 1000
            }
            totalOps += operationsPerSecond
            
            // Calculate memory bandwidth (bytes per second, then convert to MB/s)
            val bytesTransferred = operations * 12.0  // 2 reads + 1 write = 12 bytes
            val bandwidthBytesPerSec = if (durationNs > 0) {
                (bytesTransferred * 1_000_000_000.0) / durationNs
            } else {
                bytesTransferred * 1000.0
            }
            val bandwidthMBps = bandwidthBytesPerSec / (1024.0 * 1024.0)
            totalBandwidth += bandwidthMBps
            
            println("  Run $run: ${durationMs}ms (${durationNs}ns), $operationsPerSecond OPS, ${bandwidthMBps.toInt()} MB/s")
        }
        
        val avgDurationNs = totalDurationNs / runs
        val avgDurationMs = avgDurationNs / 1_000_000
        val avgOps = totalOps / runs
        val avgBandwidth = totalBandwidth / runs
        
        println("Average for size=$size: ${avgDurationMs}ms, $avgOps OPS, ${avgBandwidth.toInt()} MB/s (over $runs runs)")
        
        return MemoryTestResult(size, avgDurationMs, avgOps, avgBandwidth, runs)
    }

    /**
     * Calculate overall memory score from all test results
     */
    fun computeMemoryScore(testResults: List<MemoryTestResult>): Long {
        if (testResults.isEmpty()) return 0
        // Average OPS across all tests
        return testResults.map { it.operationsPerSecond }.average().toLong()
    }
    
    /**
     * Get system memory information
     */
    fun getMemoryInfo(): MemoryInfo {
        val runtime = Runtime.getRuntime()
        val maxMemory = runtime.maxMemory() / (1024 * 1024) // Convert to MB
        val totalMemory = runtime.totalMemory() / (1024 * 1024)
        val freeMemory = runtime.freeMemory() / (1024 * 1024)
        val usedMemory = totalMemory - freeMemory
        
        return MemoryInfo(
            totalMB = totalMemory,
            usedMB = usedMemory,
            freeMB = freeMemory,
            maxMB = maxMemory
        )
    }
}

data class MemoryInfo(
    val totalMB: Long,
    val usedMB: Long,
    val freeMB: Long,
    val maxMB: Long
)
