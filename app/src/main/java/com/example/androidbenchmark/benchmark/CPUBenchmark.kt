package com.example.androidbenchmark.benchmark

import kotlin.random.Random

class CPUBenchmark {
    private var duration: Long = -1L
    private val arraySize = 10000

    fun runTest(): Int {
        val randomInts = IntArray(arraySize) { Random.nextInt() }
        val start = System.currentTimeMillis()
        for (i in 0 until randomInts.size - 1) {
            for (j in 0 until randomInts.size - i - 1) {
                if (randomInts[j] > randomInts[j + 1]) {
                    val tmp = randomInts[j]
                    randomInts[j] = randomInts[j + 1]
                    randomInts[j + 1] = tmp
                }
            }
        }
        duration = System.currentTimeMillis() - start

        // Ensure duration is at least 1ms to avoid division by zero
        if (duration <= 0) duration = 1

        println("Bubble sort took ${duration} ms")
        return duration.toInt()
    }

    fun computeCpuScore(): Long {
        // Calculate score based on complexity O(N^2)
        val operations = arraySize.toLong() * arraySize
        return operations / duration
    }
}
