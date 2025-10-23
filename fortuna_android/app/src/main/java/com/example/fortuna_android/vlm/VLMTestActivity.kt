package com.example.fortuna_android.vlm

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.example.fortuna_android.R
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException

/**
 * Test activity for SmolVLM integration
 * Provides UI to test model loading and text generation
 */
class VLMTestActivity : AppCompatActivity() {
    private val tag = "VLMTestActivity"

    private lateinit var vlmManager: SmolVLMManager
    private lateinit var statusText: TextView
    private lateinit var outputText: TextView
    private lateinit var promptInput: EditText
    private lateinit var loadModelButton: Button
    private lateinit var generateButton: Button
    private lateinit var benchmarkButton: Button
    private lateinit var unloadButton: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var imagePreview: ImageView
    private lateinit var cameraButton: Button
    private lateinit var galleryButton: Button
    private lateinit var analyzeImageButton: Button

    private var currentImageBitmap: Bitmap? = null
    private var currentPhotoUri: Uri? = null

    // Activity result launchers
    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && currentPhotoUri != null) {
            loadImageFromUri(currentPhotoUri!!)
        }
    }

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { loadImageFromUri(it) }
    }

    private val cameraPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            openCamera()
        } else {
            Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_vlm_test)

        // Initialize VLM manager
        vlmManager = SmolVLMManager.getInstance(this)

        // Initialize views
        initViews()

        // Set up button listeners
        setupListeners()

        updateUIState(false)
    }

    private fun initViews() {
        statusText = findViewById(R.id.statusText)
        outputText = findViewById(R.id.outputText)
        promptInput = findViewById(R.id.promptInput)
        loadModelButton = findViewById(R.id.loadModelButton)
        generateButton = findViewById(R.id.generateButton)
        benchmarkButton = findViewById(R.id.benchmarkButton)
        unloadButton = findViewById(R.id.unloadButton)
        progressBar = findViewById(R.id.progressBar)
        imagePreview = findViewById(R.id.imagePreview)
        cameraButton = findViewById(R.id.cameraButton)
        galleryButton = findViewById(R.id.galleryButton)
        analyzeImageButton = findViewById(R.id.analyzeImageButton)
    }

    private fun setupListeners() {
        loadModelButton.setOnClickListener {
            loadModel()
        }

        generateButton.setOnClickListener {
            generateText()
        }

        benchmarkButton.setOnClickListener {
            runBenchmark()
        }

        unloadButton.setOnClickListener {
            unloadModel()
        }

        cameraButton.setOnClickListener {
            checkCameraPermissionAndOpen()
        }

        galleryButton.setOnClickListener {
            openGallery()
        }

        analyzeImageButton.setOnClickListener {
            analyzeImage()
        }
    }

    private fun loadModel() {
        lifecycleScope.launch {
            try {
                setLoading(true, "Loading model...")
                statusText.text = "Initializing SmolVLM model..."

                vlmManager.initialize()

                statusText.text = "Model loaded successfully!"
                setLoading(false)
                updateUIState(true)

            } catch (e: Exception) {
                Log.e(tag, "Failed to load model", e)
                statusText.text = "Error loading model: ${e.message}"
                setLoading(false)
                updateUIState(false)
            }
        }
    }

    private fun generateText() {
        val prompt = promptInput.text.toString()
        if (prompt.isBlank()) {
            statusText.text = "Please enter a prompt"
            return
        }

        lifecycleScope.launch {
            try {
                setLoading(true, "Generating...")
                outputText.text = ""
                statusText.text = "Generating response..."

                val fullResponse = StringBuilder()

                vlmManager.generateText(prompt)
                    .catch { e ->
                        Log.e(tag, "Error during generation", e)
                        statusText.text = "Error: ${e.message}"
                    }
                    .collect { token ->
                        fullResponse.append(token)
                        outputText.text = fullResponse.toString()
                    }

                setLoading(false)
                statusText.text = "Generation complete"

            } catch (e: Exception) {
                Log.e(tag, "Failed to generate text", e)
                statusText.text = "Error: ${e.message}"
                setLoading(false)
            }
        }
    }

    private fun runBenchmark() {
        lifecycleScope.launch {
            try {
                setLoading(true, "Running benchmark...")
                statusText.text = "Running benchmark..."

                val result = vlmManager.benchmark(pp = 512, tg = 128, pl = 1, nr = 1)

                outputText.text = result
                statusText.text = "Benchmark complete"
                setLoading(false)

            } catch (e: Exception) {
                Log.e(tag, "Failed to run benchmark", e)
                statusText.text = "Error: ${e.message}"
                setLoading(false)
            }
        }
    }

    private fun unloadModel() {
        lifecycleScope.launch {
            try {
                setLoading(true, "Unloading...")
                vlmManager.unload()
                statusText.text = "Model unloaded"
                outputText.text = ""
                setLoading(false)
                updateUIState(false)
            } catch (e: Exception) {
                Log.e(tag, "Failed to unload model", e)
                statusText.text = "Error: ${e.message}"
                setLoading(false)
            }
        }
    }

    private fun setLoading(loading: Boolean, message: String = "") {
        progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        if (loading && message.isNotEmpty()) {
            statusText.text = message
        }
    }

    private fun updateUIState(modelLoaded: Boolean) {
        loadModelButton.isEnabled = !modelLoaded
        generateButton.isEnabled = modelLoaded
        benchmarkButton.isEnabled = modelLoaded
        unloadButton.isEnabled = modelLoaded
        promptInput.isEnabled = modelLoaded
        cameraButton.isEnabled = modelLoaded
        galleryButton.isEnabled = modelLoaded
        analyzeImageButton.isEnabled = modelLoaded && currentImageBitmap != null
    }

    private fun checkCameraPermissionAndOpen() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                openCamera()
            }
            else -> {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun openCamera() {
        try {
            val photoFile = createImageFile()
            val photoUri = FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                photoFile
            )
            currentPhotoUri = photoUri
            cameraLauncher.launch(photoUri)
        } catch (e: IOException) {
            Log.e(tag, "Failed to create image file", e)
            Toast.makeText(this, "Failed to open camera", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openGallery() {
        galleryLauncher.launch("image/*")
    }

    private fun createImageFile(): File {
        val storageDir = cacheDir
        return File.createTempFile(
            "IMG_${System.currentTimeMillis()}_",
            ".jpg",
            storageDir
        )
    }

    private fun loadImageFromUri(uri: Uri) {
        try {
            val bitmap = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                val source = android.graphics.ImageDecoder.createSource(contentResolver, uri)
                android.graphics.ImageDecoder.decodeBitmap(source)
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(contentResolver, uri)
            }

            currentImageBitmap = bitmap
            imagePreview.setImageBitmap(bitmap)
            statusText.text = "Image loaded successfully"
            updateUIState(vlmManager.isLoaded())
        } catch (e: Exception) {
            Log.e(tag, "Failed to load image", e)
            Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show()
        }
    }

    private fun analyzeImage() {
        val bitmap = currentImageBitmap
        if (bitmap == null) {
            statusText.text = "Please select an image first"
            return
        }

        val prompt = promptInput.text.toString()
        if (prompt.isBlank()) {
            statusText.text = "Please enter a prompt"
            return
        }

        lifecycleScope.launch {
            try {
                setLoading(true, "Analyzing image...")
                outputText.text = ""
                statusText.text = "Analyzing image with VLM..."

                val fullResponse = StringBuilder()

                vlmManager.analyzeImage(bitmap, prompt)
                    .catch { e ->
                        Log.e(tag, "Error during image analysis", e)
                        statusText.text = "Error: ${e.message}"
                    }
                    .collect { token ->
                        fullResponse.append(token)
                        outputText.text = fullResponse.toString()
                    }

                setLoading(false)
                statusText.text = "Analysis complete"

            } catch (e: Exception) {
                Log.e(tag, "Failed to analyze image", e)
                statusText.text = "Error: ${e.message}"
                setLoading(false)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Unload model when activity is destroyed
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
