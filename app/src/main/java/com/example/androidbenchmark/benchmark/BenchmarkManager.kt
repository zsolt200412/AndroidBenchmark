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
    val memoryInfo: MemoryInfo? = null,
    val totalRamMB: Long? = null,
    val batteryLevelPercent: Int? = null,
    val batteryCapacityMah: Double? = null,
    val availableRamMB: Long? = null
)

class BenchmarkManager(private val context: Context) {

    fun runBenchmarksAndGetResults(): BenchmarkResults {
        val cpuBenchmark = CPUBenchmark()
        
        val cpuMultipleTests = cpuBenchmark.runMultipleSizeTests()
        val cpuFloatingPointTests = cpuBenchmark.runMultipleFloatingPointTests()
        val allCpuTests = cpuMultipleTests + cpuFloatingPointTests
        
        val cpuRes = cpuBenchmark.computeCpuScore(allCpuTests)

        val memoryBenchmark = MemoryBenchmark()
        
        val memoryMultipleTests = memoryBenchmark.runMultipleSizeTests()
        
        val memoryRes = memoryBenchmark.computeMemoryScore(memoryMultipleTests)
        
        val memoryInfo = memoryBenchmark.getMemoryInfo()

        val gpuBenchmark = GPUBenchmark()
        val gpuRes = gpuBenchmark.runTest(context)

        val displayMetrics = context.resources.displayMetrics
        val screenResolution = "${displayMetrics.widthPixels}x${displayMetrics.heightPixels}"
        
        val cpuCoreCount = Runtime.getRuntime().availableProcessors()
        val cpuFrequency = getCpuFrequency()

        val totalRamMB = getTotalRamMB()
        val batteryLevel = getBatteryLevelPercent()
        val batteryCapacity = getBatteryCapacityMah()
        val availableRamMB = getAvailableRamMB()

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
            memoryInfo = memoryInfo,
            totalRamMB = totalRamMB,
            batteryLevelPercent = batteryLevel,
            batteryCapacityMah = batteryCapacity,
            availableRamMB = availableRamMB
        )
    }
    
    private fun getCpuFrequency(): String {
        return try {
            val reader = java.io.BufferedReader(java.io.FileReader("/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_max_freq"))
            val freq = reader.readLine().trim().toLong() / 1000
            reader.close()
            "$freq MHz"
        } catch (e: Exception) {
            "Unknown"
        }
    }

    private fun getTotalRamMB(): Long? {
        return try {
            val am = context.getSystemService(android.content.Context.ACTIVITY_SERVICE) as android.app.ActivityManager
            val mi = android.app.ActivityManager.MemoryInfo()
            am.getMemoryInfo(mi)
            mi.totalMem / (1024 * 1024)
        } catch (e: Exception) {
            null
        }
    }

    private fun getAvailableRamMB(): Long? {
        return try {
            val am = context.getSystemService(android.content.Context.ACTIVITY_SERVICE) as android.app.ActivityManager
            val mi = android.app.ActivityManager.MemoryInfo()
            am.getMemoryInfo(mi)
            mi.availMem / (1024 * 1024)
        } catch (e: Exception) {
            null
        }
    }

    private fun getBatteryLevelPercent(): Int? {
        return try {
            val bm = context.getSystemService(android.content.Context.BATTERY_SERVICE) as android.os.BatteryManager
            val level = bm.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY)
            if (level >= 0) level else null
        } catch (e: Exception) {
            null
        }
    }

    private fun getBatteryCapacityMah(): Double? {
        return try {
            val powerProfileClass = Class.forName("com.android.internal.os.PowerProfile")
            val ctor = powerProfileClass.getConstructor(android.content.Context::class.java)
            val powerProfile = ctor.newInstance(context)
            val method = powerProfileClass.getMethod("getBatteryCapacity")
            val capacity = method.invoke(powerProfile) as Double
            capacity
        } catch (e: Exception) {
            null
        }
    }
}
