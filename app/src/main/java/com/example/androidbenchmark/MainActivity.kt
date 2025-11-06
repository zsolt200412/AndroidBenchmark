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
import androidx.lifecycle.lifecycleScope
import com.example.androidbenchmark.benchmark.BenchmarkManager
import com.example.androidbenchmark.benchmark.GPUBenchmark
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    private lateinit var benchmarkManager: BenchmarkManager
    private lateinit var cpuScoreText: TextView
    private lateinit var memoryScoreText: TextView
    private lateinit var gpuScoreText: TextView
    private lateinit var gpuDisplayContainer: FrameLayout
    private lateinit var gpuStatusText: TextView
    private lateinit var runBenchmarkButton: Button
    private lateinit var showGpuButton: Button
    
    private var glSurfaceView: GLSurfaceView? = null
    private var isGpuDisplayVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        initViews()
        benchmarkManager = BenchmarkManager(this)
        
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
    }
    
    private fun runAllBenchmarks() {
        // Reset UI
        cpuScoreText.text = "Score: Running..."
        memoryScoreText.text = "Score: Running..."
        gpuScoreText.text = "Score: Running..."
        runBenchmarkButton.isEnabled = false
        
        lifecycleScope.launch {
            try {
                // Run benchmarks in background thread
                val results = withContext(Dispatchers.Default) {
                    benchmarkManager.runBenchmarksAndGetResults()
                }
                
                // Update UI on main thread
                cpuScoreText.text = "Score: ${results.cpuScore}"
                memoryScoreText.text = "Score: ${results.memoryScore}"
                gpuScoreText.text = "Score: ${results.gpuScore} FPS"
                
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
                val score = withContext(Dispatchers.Default) {
                    val gpuBenchmark = GPUBenchmark()
                    gpuBenchmark.runTest(this@MainActivity, 300) // Shorter test for display
                }
                
                // Update results on main thread
                withContext(Dispatchers.Main) {
                    gpuStatusText.text = "GPU Score: $score FPS"
                    gpuScoreText.text = "Score: $score FPS"
                    
                    // Keep the visual display running
                    Handler(Looper.getMainLooper()).postDelayed({
                        if (isGpuDisplayVisible) {
                            gpuStatusText.text = "GPU benchmark complete - Score: $score FPS\nVisualization running..."
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
        
        // Best-effort cleanup
        try {
            benchmarkManager.shutdown()
        } catch (_: Exception) {
        }
    }
}