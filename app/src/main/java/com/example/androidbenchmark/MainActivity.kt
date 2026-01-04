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
        runBenchmarkButton.setOnClickListener { runAllBenchmarks() }
        showGpuButton.setOnClickListener { toggleGpuDisplay() }
        
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

        chartComposeView.setContent { ChartView(null) }
    }
    
    private fun runAllBenchmarks() {
        // Reset UI
        cpuScoreText.text = "Score: Running..."
        memoryScoreText.text = "Score: Running..."
        gpuScoreText.text = "Score: Running..."
        runBenchmarkButton.isEnabled = false
        
        // Clear previous chart results
        chartComposeView.setContent { ChartView(null) }
        
        lifecycleScope.launch {
            try {
                val results = withContext(Dispatchers.Default) {
                    // Delegate to BenchmarkManager to collect all metrics, including RAM and battery
                    benchmarkManager.runBenchmarksAndGetResults()
                }

                val processedResults = resultManager.processResults(results)
                
                // Update UI on main thread
                cpuScoreText.text = "Score: ${results.cpuScore} OPS"
                memoryScoreText.text = "Score: ${results.memoryScore} OPS"
                gpuScoreText.text = "Score: ${results.gpuScore} FPS"

                chartComposeView.setContent { ChartView(processedResults) }
                
                runBenchmarkButton.isEnabled = true
                
            } catch (e: Exception) {
                cpuScoreText.text = "Score: Error"
                memoryScoreText.text = "Score: Error"
                gpuScoreText.text = "Score: Error"
                runBenchmarkButton.isEnabled = true
            }
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
        
        glSurfaceView = GLSurfaceView(this).apply {
            setEGLContextClientVersion(2)
            preserveEGLContextOnPause = true
            val gpuBenchmark = GPUBenchmark()
            setRenderer(gpuBenchmark.createVisualRenderer())
            renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
        }
        
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.Main) {
                    gpuStatusText.text = "Starting GPU visualization..."
                    gpuDisplayContainer.removeAllViews()
                    gpuDisplayContainer.addView(glSurfaceView)
                }
                
                val result = withContext(Dispatchers.Default) {
                    val gpuBenchmark = GPUBenchmark()
                    gpuBenchmark.runTest(this@MainActivity, 300)
                }
                
                withContext(Dispatchers.Main) {
                    gpuStatusText.text = "GPU Score: ${result.score} FPS"
                    gpuScoreText.text = "Score: ${result.score} FPS"
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