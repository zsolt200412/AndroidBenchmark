package com.example.androidbenchmark.benchmark

import kotlin.random.Random

/**
 * Placeholder for Memory benchmark implementation.
 * Implementation will be added later.
 */
val array_size = 10000

class MemoryBenchmark {
    fun runTest(): Int {
        val memoryIntArray: IntArray = IntArray(array_size) { it }
        val startTime = System.currentTimeMillis()
        // Simulate memory operations
        // Write
        for (i in memoryIntArray.indices) {
            memoryIntArray[i] = Random.nextInt()
        }
        // Read
        for (i in memoryIntArray.indices) {
            val temp = memoryIntArray[i]
        }
        val endTime = System.currentTimeMillis()
        val duration = endTime - startTime
        println("Memory operations took ${duration} ms")
        return duration.toInt()
    }

    fun computeMemoryScore(): Long{
        return array_size.toLong() / runTest().toLong()
    }
}
