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
        loadingProgress = findViewById(R.id.loadingProgress)

        // Initialize VLM manager
        vlmManager = SmolVLMManager.getInstance(this)

        // Initialize camera executor
        cameraExecutor = Executors.newSingleThreadExecutor()

        // Setup button listener
        captureButton.setOnClickListener {
            captureAndAnalyze()
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

            // ImageCapture
            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
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

        // Capture image to memory
        imageCapture.takePicture(
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    // Convert ImageProxy to Bitmap
                    val bitmap = imageProxyToBitmap(image)
                    image.close()

                    if (bitmap != null) {
                        analyzeImage(bitmap)
                    } else {
                        showLoading(false)
                        Toast.makeText(
                            this@VLMTestActivity,
                            "Failed to capture image",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e(tag, "Image capture failed", exception)
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
                val fullResponse = StringBuilder()

                vlmManager.analyzeImage(bitmap, "Describe what you see in this image in detail.")
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

    private fun imageProxyToBitmap(image: ImageProxy): Bitmap? {
        val buffer = image.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        return android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
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

    private fun showLoading(loading: Boolean) {
        loadingProgress.visibility = if (loading) View.VISIBLE else View.GONE
        captureButton.isEnabled = !loading
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
