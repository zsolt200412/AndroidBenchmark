package com.example.androidbenchmark.benchmark

import kotlin.random.Random

val randomInts: IntArray = IntArray(1000) { Random.nextInt() }
val arraySize = 1000;
var duration: Long = -1L;
class CPUBenchmark {
    fun runTest(): Int {
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
        println("Bubble sort took ${duration} ms")
        return duration.toInt()
    }

    fun computeArithmetic(): Long {
        return runTest() / arraySize.toLong()
    }
}
