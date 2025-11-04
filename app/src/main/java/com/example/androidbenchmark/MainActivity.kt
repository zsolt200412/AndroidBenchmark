package com.example.androidbenchmark

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.androidbenchmark.ui.theme.AndroidBenchmarkTheme
import com.example.androidbenchmark.benchmark.BenchmarkManager

class MainActivity : ComponentActivity() {
    private lateinit var benchmarkManager: BenchmarkManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        benchmarkManager = BenchmarkManager(this)
        enableEdgeToEdge()
        setContent {
            AndroidBenchmarkTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        name = "Android",
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    /**
     * Called by UI to start the full benchmark suite.
     * This simply delegates to [BenchmarkManager.startAllTests].
     */
    fun startBenchmark() {
        benchmarkManager.startAllTests()
    }

    override fun onDestroy() {
        super.onDestroy()
        // best-effort cleanup
        try {
            benchmarkManager.shutdown()
        } catch (_: Exception) {
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    AndroidBenchmarkTheme {
        Greeting("Android")
    }
}