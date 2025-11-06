package com.example.androidbenchmark.benchmark

import android.content.Context
import android.opengl.EGL14
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class GPUBenchmark {
    private var glSurfaceView: GLSurfaceView? = null
    private var renderer: BenchmarkRenderer? = null
    
    fun runTest(context: Context, numFrames: Int = 1000): Int {
        return try {
            // Run headless with an offscreen EGL PBuffer so rendering actually occurs
            runOffscreen(numFrames)
        } catch (e: Exception) {
            0
        }
    }

    // Added: provide a visual renderer for on-screen display of the same scene
    fun createVisualRenderer(): GLSurfaceView.Renderer {
        // Use a very large target frame count to keep rendering continuously
        return BenchmarkRenderer(Int.MAX_VALUE)
    }

    // Headless EGL offscreen rendering to ensure frames are produced without attaching a view
    private fun runOffscreen(targetFrames: Int, width: Int = 720, height: Int = 1280): Int {
        val display = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        if (display == EGL14.EGL_NO_DISPLAY) return 0

        val version = IntArray(2)
        if (!EGL14.eglInitialize(display, version, 0, version, 1)) return 0

        val attribList = intArrayOf(
            EGL14.EGL_RED_SIZE, 8,
            EGL14.EGL_GREEN_SIZE, 8,
            EGL14.EGL_BLUE_SIZE, 8,
            EGL14.EGL_ALPHA_SIZE, 8,
            EGL14.EGL_DEPTH_SIZE, 16,
            EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
            EGL14.EGL_SURFACE_TYPE, EGL14.EGL_PBUFFER_BIT,
            EGL14.EGL_NONE
        )
        val numConfigs = IntArray(1)
        val configs = arrayOfNulls<android.opengl.EGLConfig>(1)
        if (!EGL14.eglChooseConfig(display, attribList, 0, configs, 0, 1, numConfigs, 0) || numConfigs[0] <= 0) {
            EGL14.eglTerminate(display)
            return 0
        }

        val contextAttribs = intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE)
        val context = EGL14.eglCreateContext(display, configs[0], EGL14.EGL_NO_CONTEXT, contextAttribs, 0)
        val surfaceAttribs = intArrayOf(EGL14.EGL_WIDTH, width, EGL14.EGL_HEIGHT, height, EGL14.EGL_NONE)
        val surface = EGL14.eglCreatePbufferSurface(display, configs[0], surfaceAttribs, 0)
        if (context == null || context == EGL14.EGL_NO_CONTEXT || surface == null || surface == EGL14.EGL_NO_SURFACE) {
            EGL14.eglTerminate(display)
            return 0
        }

        EGL14.eglMakeCurrent(display, surface, surface, context)
        try {
            val benchRenderer = BenchmarkRenderer(targetFrames)
            renderer = benchRenderer
            benchRenderer.onSurfaceCreated(null, null)
            benchRenderer.onSurfaceChanged(null, width, height)

            for (i in 0 until targetFrames) {
                benchRenderer.onDrawFrame(null)
                // Ensure GL commands complete each frame in offscreen mode
                GLES20.glFinish()
            }

            return benchRenderer.getGPUScore()
        } finally {
            EGL14.eglMakeCurrent(display, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
            EGL14.eglDestroySurface(display, surface)
            EGL14.eglDestroyContext(display, context)
            EGL14.eglTerminate(display)
        }
    }
    
    private class BenchmarkRenderer(private val targetFrames: Int) : GLSurfaceView.Renderer {
        private val mvpMatrix = FloatArray(16)
        private val projectionMatrix = FloatArray(16)
        private val viewMatrix = FloatArray(16)
        
        private var startTime: Long = 0
        private var frameCount = 0
        private var benchmarkComplete = false
        private var gpuScore = 0
        
        // Vertex data for textured cube
        private val cubeVertices = floatArrayOf(
            // Front face
            -1.0f, -1.0f,  1.0f,  0.0f, 0.0f,
             1.0f, -1.0f,  1.0f,  1.0f, 0.0f,
             1.0f,  1.0f,  1.0f,  1.0f, 1.0f,
            -1.0f,  1.0f,  1.0f,  0.0f, 1.0f,
            // Back face
            -1.0f, -1.0f, -1.0f,  0.0f, 0.0f,
             1.0f, -1.0f, -1.0f,  1.0f, 0.0f,
             1.0f,  1.0f, -1.0f,  1.0f, 1.0f,
            -1.0f,  1.0f, -1.0f,  0.0f, 1.0f
        )
        
        private val cubeIndices = shortArrayOf(
            0, 1, 2, 0, 2, 3, // Front
            4, 5, 6, 4, 6, 7, // Back
            0, 4, 7, 0, 7, 3, // Left
            1, 5, 6, 1, 6, 2, // Right
            3, 2, 6, 3, 6, 7, // Top
            0, 1, 5, 0, 5, 4  // Bottom
        )
        
        private lateinit var vertexBuffer: FloatBuffer
        private lateinit var indexBuffer: ByteBuffer
        
        private var shaderProgram = 0
        private var positionHandle = 0
        private var texCoordHandle = 0
        private var mvpMatrixHandle = 0
        private var lightPosHandle = 0
        
        // Vertex shader with lighting
        private val vertexShaderCode = """
            attribute vec4 vPosition;
            attribute vec2 aTexCoord;
            uniform mat4 uMVPMatrix;
            uniform vec3 uLightPos;
            varying vec2 vTexCoord;
            varying vec3 vLighting;
            
            void main() {
                gl_Position = uMVPMatrix * vPosition;
                vTexCoord = aTexCoord;
                
                // Simple lighting calculation
                vec3 normal = vec3(0.0, 0.0, 1.0);
                vec3 lightDirection = normalize(uLightPos - vPosition.xyz);
                float lightIntensity = max(dot(normal, lightDirection), 0.1);
                vLighting = vec3(lightIntensity);
            }
        """.trimIndent()
        
        // Fragment shader with texture and lighting
        private val fragmentShaderCode = """
            precision mediump float;
            varying vec2 vTexCoord;
            varying vec3 vLighting;
            
            void main() {
                // Create procedural texture pattern
                vec2 grid = fract(vTexCoord * 8.0);
                float pattern = step(0.5, grid.x) + step(0.5, grid.y);
                pattern = mod(pattern, 2.0);
                
                vec3 color = mix(vec3(0.8, 0.2, 0.2), vec3(0.2, 0.8, 0.2), pattern);
                gl_FragColor = vec4(color * vLighting, 1.0);
            }
        """.trimIndent()
        
        override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
            GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
            GLES20.glEnable(GLES20.GL_DEPTH_TEST)
            
            // Initialize buffers
            initBuffers()
            
            // Create shader program
            shaderProgram = createShaderProgram()
            
            // Get shader handles
            positionHandle = GLES20.glGetAttribLocation(shaderProgram, "vPosition")
            texCoordHandle = GLES20.glGetAttribLocation(shaderProgram, "aTexCoord")
            mvpMatrixHandle = GLES20.glGetUniformLocation(shaderProgram, "uMVPMatrix")
            lightPosHandle = GLES20.glGetUniformLocation(shaderProgram, "uLightPos")
            
            startTime = System.nanoTime()
        }
        
        override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
            GLES20.glViewport(0, 0, width, height)
            
            val ratio = width.toFloat() / height.toFloat()
            Matrix.frustumM(projectionMatrix, 0, -ratio, ratio, -1f, 1f, 3f, 20f)
        }
        
        override fun onDrawFrame(gl: GL10?) {
            if (benchmarkComplete) return
            
            // Clear the screen
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
            
            // Use shader program
            GLES20.glUseProgram(shaderProgram)
            
            // Set up camera
            Matrix.setLookAtM(viewMatrix, 0, 0f, 0f, 10f, 0f, 0f, 0f, 0f, 1f, 0f)
            
            // Render multiple objects with different transformations
            for (i in 0 until 5) {
                renderScene(i)
                applyLighting()
                applyShaders()
            }
            
            frameCount++

            println("Rendered frame $frameCount")
            
            // Check if benchmark is complete
            if (frameCount >= targetFrames) {
                val endTime = System.nanoTime()
                val elapsedTimeSeconds = (endTime - startTime) / 1_000_000_000.0
                val fps = frameCount / elapsedTimeSeconds
                
                // Complexity factor based on number of objects, vertices, and shader complexity
                val complexityFactor = 50
                gpuScore = (fps * complexityFactor).toInt()
                println("GPU Benchmark complete: $gpuScore FPS")
                benchmarkComplete = true
            }
        }
        
        private fun renderScene(objectIndex: Int) {
            val modelMatrix = FloatArray(16)
            Matrix.setIdentityM(modelMatrix, 0)
            
            // Apply transformations for multiple objects
            Matrix.translateM(modelMatrix, 0, 
                (objectIndex - 2) * 2.5f, 
                kotlin.math.sin((objectIndex + frameCount) * 0.1).toFloat() * 2f, 
                0f
            )
            Matrix.rotateM(modelMatrix, 0, frameCount * 2f + objectIndex * 45f, 1f, 1f, 0f)
            
            // Calculate MVP matrix
            val tempMatrix = FloatArray(16)
            Matrix.multiplyMM(tempMatrix, 0, viewMatrix, 0, modelMatrix, 0)
            Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, tempMatrix, 0)
            
            // Pass MVP matrix to shader
            GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0)
            
            // Enable vertex arrays
            GLES20.glEnableVertexAttribArray(positionHandle)
            GLES20.glEnableVertexAttribArray(texCoordHandle)
            
            // Bind vertex data
            GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 20, vertexBuffer)
            vertexBuffer.position(3)
            GLES20.glVertexAttribPointer(texCoordHandle, 2, GLES20.GL_FLOAT, false, 20, vertexBuffer)
            vertexBuffer.position(0)
            
            // Draw the object
            GLES20.glDrawElements(GLES20.GL_TRIANGLES, cubeIndices.size, GLES20.GL_UNSIGNED_SHORT, indexBuffer)
        }
        
        private fun applyLighting() {
            // Dynamic light position
            val lightPos = floatArrayOf(
                kotlin.math.cos(frameCount * 0.05).toFloat() * 5f,
                kotlin.math.sin(frameCount * 0.05).toFloat() * 5f,
                5f
            )
            GLES20.glUniform3fv(lightPosHandle, 1, lightPos, 0)
        }
        
        private fun applyShaders() {
            // Shader effects are already applied in the fragment shader
            // This could be extended with post-processing effects
        }
        
        private fun initBuffers() {
            // Vertex buffer
            val bb = ByteBuffer.allocateDirect(cubeVertices.size * 4)
            bb.order(ByteOrder.nativeOrder())
            vertexBuffer = bb.asFloatBuffer()
            vertexBuffer.put(cubeVertices)
            vertexBuffer.position(0)
            
            // Index buffer
            indexBuffer = ByteBuffer.allocateDirect(cubeIndices.size * 2)
            indexBuffer.order(ByteOrder.nativeOrder())
            val sb = indexBuffer.asShortBuffer()
            sb.put(cubeIndices)
            indexBuffer.position(0)
        }
        
        private fun createShaderProgram(): Int {
            val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
            val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)
            
            val program = GLES20.glCreateProgram()
            GLES20.glAttachShader(program, vertexShader)
            GLES20.glAttachShader(program, fragmentShader)
            GLES20.glLinkProgram(program)
            
            return program
        }
        
        private fun loadShader(type: Int, shaderCode: String): Int {
            val shader = GLES20.glCreateShader(type)
            GLES20.glShaderSource(shader, shaderCode)
            GLES20.glCompileShader(shader)
            return shader
        }
        
        fun getGPUScore(): Int = gpuScore
    }
}