package com.example.androidbenchmark

import android.opengl.GLSurfaceView
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.lifecycleScope
import com.example.androidbenchmark.benchmark.BenchmarkManager
import com.example.androidbenchmark.benchmark.GPUBenchmark
import com.example.androidbenchmark.benchmark.ResultManager
import com.example.androidbenchmark.ui.components.ChartView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    private lateinit var benchmarkManager: BenchmarkManager
    private lateinit var resultManager: ResultManager
    private lateinit var cpuScoreText: TextView
    private lateinit var memoryScoreText: TextView
    private lateinit var gpuScoreText: TextView
    private lateinit var gpuDisplayContainer: FrameLayout
    private lateinit var gpuStatusText: TextView
    private lateinit var runBenchmarkButton: Button
    private lateinit var showGpuButton: Button
    private lateinit var chartComposeView: ComposeView

    private var glSurfaceView: GLSurfaceView? = null
    private var isGpuDisplayVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        initViews()
        benchmarkManager = BenchmarkManager(this)
        resultManager = ResultManager()
        
        // Set up button listeners
        runBenchmarkButton.setOnClickListener {
            runAllBenchmarks()
        }
        
        showGpuButton.setOnClickListener {
            toggleGpuDisplay()
        }
        
        // Auto-run benchmarks on start
        runAllBenchmarks()
    }
    
    private fun initViews() {
        cpuScoreText = findViewById(R.id.cpuScoreText)
        memoryScoreText = findViewById(R.id.memoryScoreText)
        gpuScoreText = findViewById(R.id.gpuScoreText)
        gpuDisplayContainer = findViewById(R.id.gpuDisplayContainer)
        gpuStatusText = findViewById(R.id.gpuStatusText)
        runBenchmarkButton = findViewById(R.id.runBenchmarkButton)
        showGpuButton = findViewById(R.id.showGpuButton)
        chartComposeView = findViewById(R.id.chartComposeView)

        chartComposeView.setContent {
            ChartView(null)
        }
    }
    
    private fun runAllBenchmarks() {
        // Reset UI
        cpuScoreText.text = "Score: Running..."
        memoryScoreText.text = "Score: Running..."
        gpuScoreText.text = "Score: Running..."
        runBenchmarkButton.isEnabled = false
        
        // Clear previous chart results
        chartComposeView.setContent {
            ChartView(null)
        }
        
        lifecycleScope.launch {
            try {
                // Update status: Starting CPU tests
                withContext(Dispatchers.Main) {
                    cpuScoreText.text = "Running integer tests (1/3)..."
                }
                
                // Run benchmarks in background thread with progress updates
                val results = withContext(Dispatchers.Default) {
                    // CPU Integer tests
                    withContext(Dispatchers.Main) {
                        cpuScoreText.text = "Running integer tests (bubble sort)..."
                    }
                    
                    val cpuBenchmark = com.example.androidbenchmark.benchmark.CPUBenchmark()
                    val cpuMultipleTests = cpuBenchmark.runMultipleSizeTests()
                    
                    // CPU Floating-Point tests
                    withContext(Dispatchers.Main) {
                        cpuScoreText.text = "Running floating-point tests..."
                    }
                    
                    val cpuFloatingPointTests = cpuBenchmark.runMultipleFloatingPointTests()
                    val allCpuTests = cpuMultipleTests + cpuFloatingPointTests
                    
                    val cpuRes = cpuBenchmark.computeCpuScore(allCpuTests)
                    
                    // Memory tests
                    withContext(Dispatchers.Main) {
                        cpuScoreText.text = "Score: ${cpuRes} OPS"
                        memoryScoreText.text = "Running matrix tests (2/3)..."
                    }
                    
                    val memoryBenchmark = com.example.androidbenchmark.benchmark.MemoryBenchmark()
                    val memoryMultipleTests = memoryBenchmark.runMultipleSizeTests()
                    val memoryRes = memoryBenchmark.computeMemoryScore(memoryMultipleTests)
                    val memoryInfo = memoryBenchmark.getMemoryInfo()
                    
                    // GPU test
                    withContext(Dispatchers.Main) {
                        memoryScoreText.text = "Score: ${memoryRes} OPS"
                        gpuScoreText.text = "Running GPU test (3/3)..."
                    }
                    
                    val gpuBenchmark = com.example.androidbenchmark.benchmark.GPUBenchmark()
                    val gpuRes = gpuBenchmark.runTest(this@MainActivity)
                    
                    val displayMetrics = resources.displayMetrics
                    val screenResolution = "${displayMetrics.widthPixels}x${displayMetrics.heightPixels}"
                    val cpuCoreCount = Runtime.getRuntime().availableProcessors()
                    
                    com.example.androidbenchmark.benchmark.BenchmarkResults(
                        cpuScore = cpuRes,
                        memoryScore = memoryRes,
                        gpuScore = gpuRes.score.toLong(),
                        gpuName = gpuRes.rendererName,
                        screenResolution = screenResolution,
                        cpuCoreCount = cpuCoreCount,
                        cpuFrequency = getCpuFrequency(),
                        cpuTestResults = allCpuTests,
                        memoryTestResults = memoryMultipleTests,
                        memoryInfo = memoryInfo
                    )
                }

                val processedResults = resultManager.processResults(results)
                
                // Update UI on main thread
                cpuScoreText.text = "Score: ${results.cpuScore} OPS"
                memoryScoreText.text = "Score: ${results.memoryScore} OPS"
                gpuScoreText.text = "Score: ${results.gpuScore} FPS"

                chartComposeView.setContent {
                    ChartView(processedResults)
                }
                
                runBenchmarkButton.isEnabled = true
                
            } catch (e: Exception) {
                cpuScoreText.text = "Score: Error"
                memoryScoreText.text = "Score: Error"
                gpuScoreText.text = "Score: Error"
                runBenchmarkButton.isEnabled = true
            }
        }
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
    
    private fun toggleGpuDisplay() {
        if (isGpuDisplayVisible) {
            hideGpuDisplay()
        } else {
            showGpuDisplay()
        }
    }
    
    private fun showGpuDisplay() {
        gpuDisplayContainer.visibility = View.VISIBLE
        gpuStatusText.text = "Initializing GPU benchmark..."
        showGpuButton.text = "Hide GPU Test"
        isGpuDisplayVisible = true
        
        // Create and add GLSurfaceView for live GPU benchmark visualization
        glSurfaceView = GLSurfaceView(this).apply {
            setEGLContextClientVersion(2)
            preserveEGLContextOnPause = true
            
            // Use the visual renderer from GPUBenchmark
            val gpuBenchmark = GPUBenchmark()
            setRenderer(gpuBenchmark.createVisualRenderer())
            renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
        }
        
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.Main) {
                    gpuStatusText.text = "Starting GPU visualization..."
                    
                    // Add GLSurfaceView to container
                    gpuDisplayContainer.removeAllViews()
                    gpuDisplayContainer.addView(glSurfaceView)
                }
                
                // Run a separate benchmark for scoring in background
                val result = withContext(Dispatchers.Default) {
                    val gpuBenchmark = GPUBenchmark()
                    gpuBenchmark.runTest(this@MainActivity, 300) // Shorter test for display
                }
                
                // Update results on main thread
                withContext(Dispatchers.Main) {
                    gpuStatusText.text = "GPU Score: ${result.score} FPS"
                    gpuScoreText.text = "Score: ${result.score} FPS"
                    
                    // Keep the visual display running
                    Handler(Looper.getMainLooper()).postDelayed({
                        if (isGpuDisplayVisible) {
                            gpuStatusText.text = "GPU benchmark complete - Score: ${result.score} FPS\nVisualization running..."
                        }
                    }, 2000)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    gpuStatusText.text = "GPU benchmark failed: ${e.message}"
                    gpuScoreText.text = "Score: Error"
                }
            }
        }
    }
    
    private fun hideGpuDisplay() {
        gpuDisplayContainer.visibility = View.GONE
        showGpuButton.text = "Show GPU Test"
        isGpuDisplayVisible = false
        
        // Clean up GLSurfaceView
        glSurfaceView?.let { view ->
            gpuDisplayContainer.removeView(view)
            view.onPause()
        }
        glSurfaceView = null
    }

    override fun onResume() {
        super.onResume()
        glSurfaceView?.onResume()
    }

    override fun onPause() {
        super.onPause()
        glSurfaceView?.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        hideGpuDisplay()
    }
}