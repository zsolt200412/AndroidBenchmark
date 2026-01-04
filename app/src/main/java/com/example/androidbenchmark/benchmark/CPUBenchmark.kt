package com.example.androidbenchmark.benchmark

import kotlin.random.Random
import kotlin.math.sin
import kotlin.math.cos
import kotlin.math.sqrt
import kotlin.math.pow
import kotlin.math.absoluteValue

data class CPUTestResult(
    val size: Int,
    val durationMs: Long,
    val operationsPerSecond: Long,
    val runs: Int = 1  // Number of runs averaged
)

class CPUBenchmark {
    private val testResults = mutableListOf<CPUTestResult>()
    private val numRuns = 3  // Run each test 3 times and average

    // Volatile to prevent compiler optimization
    @Volatile
    private var preventOptimization: Any? = null

    /**
     * Run CPU benchmark with multiple sizes to see how performance varies
     * Using larger sizes and nanosecond precision for better differentiation
     */
    fun runMultipleSizeTests(): List<CPUTestResult> {
        testResults.clear()
        // Reduced sizes to ensure tests complete in reasonable time
        // Still large enough to show differences between devices
        val sizes = listOf(5000, 10000, 15000, 20000, 25000)
        
        // Warmup: Run once to stabilize JIT compilation
        println("Warming up CPU benchmark...")
        runIntegerTestSingle(2000)
        
        for (size in sizes) {
            val result = runIntegerTestWithAverage(size, numRuns)
            testResults.add(result)
            println("Completed test for size $size - Total results so far: ${testResults.size}")
        }
        
        println("All CPU integer tests completed. Total results: ${testResults.size}")
        return testResults
    }

    /**
     * Run floating-point intensive operations with better precision
     */
    fun runFloatingPointTest(iterations: Int = 1000000): CPUTestResult {
        val start = System.nanoTime()
        var result = 0.0
        var piApprox = 0.0
        
        for (i in 0 until iterations) {
            val x = i.toDouble()
            result += sin(x) * cos(x) + sqrt(x.absoluteValue) + x.pow(1.5)
            
            // Add Pi calculation using Gregory-Leibniz series term per iteration
            val sign = if (i % 2 == 0) 1.0 else -1.0
            val denom = 2.0 * i + 1.0
            piApprox += 4.0 * sign / denom
        }
        
        val durationNs = System.nanoTime() - start
        val durationMs = durationNs / 1_000_000
        
        // Prevent optimization
        preventOptimization = result + piApprox
        
        // Calculate FLOPS (floating-point operations per second)
        // Ops per iteration: sin, cos, sqrt, pow, 2 multiplies, 1 add (from original) = 6
        // Plus Pi term: 2 multiplies (4*sign and 2*i), 1 add ( +1), 1 division, 1 add to accumulator = 5
        // Total ~11 FP ops per iteration
        val operations = iterations * 11L
        val operationsPerSecond = if (durationMs > 0) {
            (operations * 1000) / durationMs
        } else {
            (operations * 1_000_000_000) / durationNs
        }
        
        println("Floating-point test: $iterations iterations in $durationMs ms")
        println("Approximated π (Gregory–Leibniz, $iterations terms): $piApprox")
        
        return CPUTestResult(iterations, durationMs, operationsPerSecond, 1)
    }

    /**
     * Run multiple floating-point tests with varying complexity
     * INCREASED SIZES for better differentiation between devices
     */
    fun runMultipleFloatingPointTests(): List<CPUTestResult> {
        val results = mutableListOf<CPUTestResult>()
        // INCREASED: Now 500K to 5M iterations
        val iterationSizes = listOf(500000, 1000000, 1500000, 2000000, 2500000)
        
        // Warmup
        println("Warming up floating-point benchmark...")
        runFloatingPointTest(100000)
        
        for (size in iterationSizes) {
            results.add(runFloatingPointTestWithAverage(size, numRuns))
        }
        
        return results
    }

    private fun runIntegerTestSingle(size: Int): CPUTestResult {
        val randomInts = IntArray(size) { Random.nextInt() }
        val start = System.nanoTime()
        
        // Bubble sort
        for (i in 0 until randomInts.size - 1) {
            for (j in 0 until randomInts.size - i - 1) {
                if (randomInts[j] > randomInts[j + 1]) {
                    val tmp = randomInts[j]
                    randomInts[j] = randomInts[j + 1]
                    randomInts[j + 1] = tmp
                }
            }
        }
        
        val durationNs = System.nanoTime() - start
        val durationMs = durationNs / 1_000_000
        
        // Prevent optimization
        preventOptimization = randomInts[0]
        
        val operations = size.toLong() * size
        val operationsPerSecond = if (durationMs > 0) {
            (operations * 1000) / durationMs
        } else {
            (operations * 1_000_000_000) / durationNs
        }
        
        return CPUTestResult(size, durationMs, operationsPerSecond, 1)
    }

    /**
     * Run integer test multiple times and return average result
     * NOW USING NANOSECOND PRECISION
     */
    private fun runIntegerTestWithAverage(size: Int, runs: Int): CPUTestResult {
        var totalDurationNs = 0L
        var totalOps = 0L
        
        println("Running integer test (size=$size) $runs times...")
        
        for (run in 1..runs) {
            val randomInts = IntArray(size) { Random.nextInt() }
            val start = System.nanoTime()
            
            // Bubble sort
            for (i in 0 until randomInts.size - 1) {
                for (j in 0 until randomInts.size - i - 1) {
                    if (randomInts[j] > randomInts[j + 1]) {
                        val tmp = randomInts[j]
                        randomInts[j] = randomInts[j + 1]
                        randomInts[j + 1] = tmp
                    }
                }
            }
            
            val durationNs = System.nanoTime() - start
            
            // Prevent optimization
            preventOptimization = randomInts[0]
            
            totalDurationNs += durationNs
            
            val operations = size.toLong() * size
            val durationMs = durationNs / 1_000_000
            val operationsPerSecond = if (durationNs > 0) {
                (operations * 1_000_000_000) / durationNs
            } else {
                operations * 1000
            }
            totalOps += operationsPerSecond
            
            println("  Run $run: ${durationMs}ms (${durationNs}ns), $operationsPerSecond OPS")
        }
        
        val avgDurationNs = totalDurationNs / runs
        val avgDurationMs = avgDurationNs / 1_000_000
        val avgOps = totalOps / runs
        
        println("Average for size=$size: ${avgDurationMs}ms, $avgOps OPS (over $runs runs)")
        
        return CPUTestResult(size, avgDurationMs, avgOps, runs)
    }

    /**
     * Run floating-point test multiple times and return average result
     * NOW USING NANOSECOND PRECISION
     */
    private fun runFloatingPointTestWithAverage(iterations: Int, runs: Int): CPUTestResult {
        var totalDurationNs = 0L
        var totalOps = 0L
        
        println("Running floating-point test (iterations=$iterations) $runs times...")
        
        for (run in 1..runs) {
            val start = System.nanoTime()
            var result = 0.0
            var piApprox = 0.0
            
            for (i in 0 until iterations) {
                val x = i.toDouble()
                result += sin(x) * cos(x) + sqrt(x.absoluteValue) + x.pow(1.5)
                
                // Pi calculation term per iteration (Gregory-Leibniz)
                val sign = if (i % 2 == 0) 1.0 else -1.0
                val denom = 2.0 * i + 1.0
                piApprox += 4.0 * sign / denom
            }
            
            val durationNs = System.nanoTime() - start
            
            // Prevent optimization
            preventOptimization = result + piApprox
            
            totalDurationNs += durationNs
            
            // Ops per iteration ~11 (see above)
            val operations = iterations * 11L
            val durationMs = durationNs / 1_000_000
            val operationsPerSecond = if (durationNs > 0) {
                (operations * 1_000_000_000) / durationNs
            } else {
                operations * 1000
            }
            totalOps += operationsPerSecond
            
            println("  Run $run: ${durationMs}ms (${durationNs}ns), $operationsPerSecond OPS")
            println("  Run $run π approximation ($iterations terms): $piApprox")
        }
        
        val avgDurationNs = totalDurationNs / runs
        val avgDurationMs = avgDurationNs / 1_000_000
        val avgOps = totalOps / runs
        
        println("Average for iterations=$iterations: ${avgDurationMs}ms, $avgOps OPS (over $runs runs)")
        
        return CPUTestResult(iterations, avgDurationMs, avgOps, runs)
    }

    /**
     * Calculate overall CPU score from all test results
     */
    fun computeCpuScore(testResults: List<CPUTestResult>): Long {
        if (testResults.isEmpty()) return 0
        // Average OPS across all tests
        return testResults.map { it.operationsPerSecond }.average().toLong()
    }
}
