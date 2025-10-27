package com.example.fortuna_android.vlm

import android.Manifest
import android.animation.ObjectAnimator
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.fortuna_android.R
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Real-time camera VLM activity
 * Full screen camera with one-tap image description
 */
class VLMTestActivity : AppCompatActivity() {
    private val tag = "VLMTestActivity"

    // Views
    private lateinit var cameraPreview: PreviewView
    private lateinit var descriptionOverlay: TextView
    private lateinit var captureButton: FloatingActionButton
    private lateinit var testTextButton: FloatingActionButton
    private lateinit var loadingProgress: ProgressBar

    // VLM
    private lateinit var vlmManager: SmolVLMManager
    private var isModelLoaded = false

    // CameraX
    private var imageCapture: ImageCapture? = null
    private var camera: Camera? = null
    private lateinit var cameraExecutor: ExecutorService

    // Permission launcher
    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startCamera()
        } else {
            Toast.makeText(this, "Camera permission required", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_vlm_test)

        // Initialize views
        cameraPreview = findViewById(R.id.cameraPreview)
        descriptionOverlay = findViewById(R.id.descriptionOverlay)
        captureButton = findViewById(R.id.captureButton)
        testTextButton = findViewById(R.id.testTextButton)
        loadingProgress = findViewById(R.id.loadingProgress)

        // Initialize VLM manager
        vlmManager = SmolVLMManager.getInstance(this)

        // Initialize camera executor
        cameraExecutor = Executors.newSingleThreadExecutor()

        // Setup button listeners
        captureButton.setOnClickListener {
            captureAndAnalyze()
        }

        testTextButton.setOnClickListener {
            testTextOnly()
        }

        // Load model in background
        loadModel()

        // Request camera permission and start
        checkCameraPermission()
    }

    private fun checkCameraPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                startCamera()
            }
            else -> {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun loadModel() {
        lifecycleScope.launch {
            try {
                Log.i(tag, "Loading VLM model in background...")
                vlmManager.initialize()
                isModelLoaded = true
                Log.i(tag, "VLM model loaded successfully")
            } catch (e: Exception) {
                Log.e(tag, "Failed to load VLM model", e)
                Toast.makeText(
                    this@VLMTestActivity,
                    "Failed to load model: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(cameraPreview.surfaceProvider)
                }

            // ImageCapture with optimized resolution
            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .setTargetResolution(android.util.Size(640, 480)) // Small resolution for speed
                .build()

            // Select back camera
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // Unbind all use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                camera = cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview,
                    imageCapture
                )

            } catch (e: Exception) {
                Log.e(tag, "Camera binding failed", e)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun captureAndAnalyze() {
        if (!isModelLoaded) {
            Toast.makeText(this, "Model still loading, please wait...", Toast.LENGTH_SHORT).show()
            return
        }

        val imageCapture = imageCapture ?: run {
            Toast.makeText(this, "Camera not ready", Toast.LENGTH_SHORT).show()
            return
        }

        // Show loading
        showLoading(true)

        // Create temp file for capture
        val photoFile = File.createTempFile("capture_", ".jpg", cacheDir)
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        // Capture image to file
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    // Load bitmap from file
                    val bitmap = android.graphics.BitmapFactory.decodeFile(photoFile.absolutePath)

                    // Delete temp file
                    photoFile.delete()

                    if (bitmap != null) {
                        analyzeImage(bitmap)
                    } else {
                        showLoading(false)
                        Toast.makeText(
                            this@VLMTestActivity,
                            "Failed to load captured image",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e(tag, "Image capture failed", exception)
                    photoFile.delete()
                    showLoading(false)
                    Toast.makeText(
                        this@VLMTestActivity,
                        "Capture failed: ${exception.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )
    }

    private fun analyzeImage(bitmap: Bitmap) {
        lifecycleScope.launch {
            try {
                // 1. Optimize image - downscale to 336x336 for faster inference
                val optimizedBitmap = optimizeImageForVLM(bitmap)
                Log.i(tag, "Image optimized: ${bitmap.width}x${bitmap.height} → ${optimizedBitmap.width}x${optimizedBitmap.height}")

                val fullResponse = StringBuilder()

                // 2. Request JSON output with specific classification
                val prompt = """Analyze this image and respond ONLY with valid JSON in this exact format:
{
  "detected-object": "description of main object",
  "classified-label": "one of: fire, water, metal, wood, land"
}

Choose the label that best matches the image content. For example:
- fire: flames, sun, heat, red/orange colors
- water: ocean, rain, blue, liquids
- metal: machinery, tools, gray/silver objects
- wood: trees, plants, brown, natural
- land: earth, rocks, ground, mountains"""

                vlmManager.analyzeImage(optimizedBitmap, prompt)
                    .catch { e ->
                        Log.e(tag, "Error during analysis", e)
                        withContext(Dispatchers.Main) {
                            showLoading(false)
                            Toast.makeText(
                                this@VLMTestActivity,
                                "Analysis failed: ${e.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                    .collect { token ->
                        fullResponse.append(token)
                        withContext(Dispatchers.Main) {
                            updateDescriptionOverlay(fullResponse.toString())
                        }
                    }

                // Clean up optimized bitmap
                if (optimizedBitmap != bitmap) {
                    optimizedBitmap.recycle()
                }

                // Hide loading when done
                withContext(Dispatchers.Main) {
                    showLoading(false)
                }

            } catch (e: Exception) {
                Log.e(tag, "Failed to analyze image", e)
                withContext(Dispatchers.Main) {
                    showLoading(false)
                    Toast.makeText(
                        this@VLMTestActivity,
                        "Error: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    /**
     * Optimize image for VLM processing
     * Downscales to max 336x336 while maintaining aspect ratio
     * Reduces memory usage and speeds up inference
     */
    private fun optimizeImageForVLM(bitmap: Bitmap, maxSize: Int = 336): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        // If already small enough, return as-is
        if (width <= maxSize && height <= maxSize) {
            return bitmap
        }

        // Calculate scale to fit within maxSize x maxSize
        val scale = minOf(
            maxSize.toFloat() / width,
            maxSize.toFloat() / height
        )

        val newWidth = (width * scale).toInt()
        val newHeight = (height * scale).toInt()

        Log.d(tag, "Downscaling image: ${width}x${height} → ${newWidth}x${newHeight} (scale: $scale)")

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    private fun updateDescriptionOverlay(text: String) {
        descriptionOverlay.text = text

        if (descriptionOverlay.visibility != View.VISIBLE) {
            descriptionOverlay.visibility = View.VISIBLE

            // Fade in animation
            ObjectAnimator.ofFloat(descriptionOverlay, "alpha", 0f, 1f).apply {
                duration = 300
                start()
            }
        }
    }

    private fun testTextOnly() {
        if (!isModelLoaded) {
            Toast.makeText(this, "Model still loading, please wait...", Toast.LENGTH_SHORT).show()
            return
        }

        showLoading(true)

        lifecycleScope.launch {
            try {
                val fullResponse = StringBuilder()

                vlmManager.generateText("Who is the president of the United States? Answer in a single line.")
                    .catch { e ->
                        Log.e(tag, "Error during text generation", e)
                        withContext(Dispatchers.Main) {
                            showLoading(false)
                            Toast.makeText(
                                this@VLMTestActivity,
                                "Text test failed: ${e.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                    .collect { token ->
                        fullResponse.append(token)
                        withContext(Dispatchers.Main) {
                            updateDescriptionOverlay("TEXT TEST:\n${fullResponse}")
                        }
                    }

                withContext(Dispatchers.Main) {
                    showLoading(false)
                }

            } catch (e: Exception) {
                Log.e(tag, "Failed text test", e)
                withContext(Dispatchers.Main) {
                    showLoading(false)
                    Toast.makeText(
                        this@VLMTestActivity,
                        "Error: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun showLoading(loading: Boolean) {
        loadingProgress.visibility = if (loading) View.VISIBLE else View.GONE
        captureButton.isEnabled = !loading
        testTextButton.isEnabled = !loading
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()

        // Unload model
        lifecycleScope.launch {
            try {
                if (vlmManager.isLoaded()) {
                    vlmManager.unload()
                }
            } catch (e: Exception) {
                Log.e(tag, "Error during cleanup", e)
            }
        }
    }
}
