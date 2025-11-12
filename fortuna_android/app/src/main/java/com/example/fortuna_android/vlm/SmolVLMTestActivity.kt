package com.example.fortuna_android.vlm

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.view.View
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.fortuna_android.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream

/**
 * Simple test activity for SmolVLM model validation
 * Allows image selection and displays raw output + extracted label
 */
class SmolVLMTestActivity : AppCompatActivity() {
    private val tag = "SmolVLMTestActivity"

    // Views
    private lateinit var imageView: ImageView
    private lateinit var selectButton: Button
    private lateinit var inferButton: Button
    private lateinit var statusText: TextView
    private lateinit var rawOutputText: TextView
    private lateinit var extractedLabelText: TextView
    private lateinit var inferenceTimeText: TextView
    private lateinit var progressBar: ProgressBar

    // VLM
    private lateinit var vlmManager: SmolVLMManager
    private var selectedBitmap: Bitmap? = null
    private var isModelLoaded = false

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        Log.d(tag, "Image picker result received. URI: $uri")
        if (uri == null) {
            Log.w(tag, "No image selected (user cancelled or error)")
            statusText.text = "No image selected"
            return@registerForActivityResult
        }

        uri.let {
            lifecycleScope.launch {
                try {
                    Log.d(tag, "Opening input stream for URI: $uri")
                    val inputStream: InputStream? = contentResolver.openInputStream(it)
                    if (inputStream == null) {
                        Log.e(tag, "Failed to open input stream for URI: $uri")
                        statusText.text = "Failed to open image"
                        return@launch
                    }

                    Log.d(tag, "Decoding bitmap from stream")
                    selectedBitmap = BitmapFactory.decodeStream(inputStream)
                    inputStream.close()

                    if (selectedBitmap == null) {
                        Log.e(tag, "Failed to decode bitmap from URI: $uri")
                        statusText.text = "Failed to decode image"
                        return@launch
                    }

                    Log.d(tag, "Bitmap loaded successfully: ${selectedBitmap!!.width}x${selectedBitmap!!.height}")
                    imageView.setImageBitmap(selectedBitmap)
                    inferButton.isEnabled = true
                    statusText.text = "Image loaded: ${selectedBitmap!!.width}x${selectedBitmap!!.height}"
                } catch (e: Exception) {
                    statusText.text = "Failed to load image: ${e.message}"
                    Log.e(tag, "Failed to load image", e)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_smolvlm_test)

        // Bind views
        imageView = findViewById(R.id.imageView)
        selectButton = findViewById(R.id.selectButton)
        inferButton = findViewById(R.id.inferButton)
        statusText = findViewById(R.id.statusText)
        rawOutputText = findViewById(R.id.rawOutputText)
        extractedLabelText = findViewById(R.id.extractedLabelText)
        inferenceTimeText = findViewById(R.id.inferenceTimeText)
        progressBar = findViewById(R.id.progressBar)

        // Initialize VLM manager
        vlmManager = SmolVLMManager.getInstance(this)

        // Set up buttons
        selectButton.setOnClickListener {
            Log.d(tag, "Select button clicked - launching image picker")
            statusText.text = "Opening image picker..."
            try {
                imagePickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
                Log.d(tag, "Image picker launched successfully")
            } catch (e: Exception) {
                Log.e(tag, "Failed to launch image picker", e)
                statusText.text = "Error launching image picker: ${e.message}"
            }
        }

        inferButton.setOnClickListener {
            runInference()
        }
        inferButton.isEnabled = false

        // Load model
        lifecycleScope.launch {
            statusText.text = "Loading model..."
            progressBar.visibility = View.VISIBLE

            try {
                withContext(Dispatchers.IO) {
                    vlmManager.initialize()
                }
                isModelLoaded = true
                statusText.text = "Model loaded successfully"
            } catch (e: Exception) {
                statusText.text = "Failed to load model: ${e.message}"
                Log.e(tag, "Failed to load model", e)
            } finally {
                progressBar.visibility = View.GONE
            }
        }
    }

    private fun runInference() {
        val bitmap = selectedBitmap ?: return

        lifecycleScope.launch {
            progressBar.visibility = View.VISIBLE
            inferButton.isEnabled = false
            statusText.text = "Running inference..."
            rawOutputText.text = ""
            extractedLabelText.text = ""
            inferenceTimeText.text = ""

            try {
                val startTime = System.currentTimeMillis()
                val fullOutput = StringBuilder()

                vlmManager.analyzeImage(
                    bitmap = bitmap,
                    prompt = "Classify this image into one of these elements: water, land, fire, wood, metal.\n\nProvide your answer with a brief description."
                )
                    .flowOn(Dispatchers.IO)
                    .catch { e ->
                        statusText.text = "Inference error: ${e.message}"
                        Log.e(tag, "Inference error", e)
                    }
                    .collect { token ->
                        fullOutput.append(token)
                        rawOutputText.text = fullOutput.toString()
                    }

                val endTime = System.currentTimeMillis()
                val inferenceTime = endTime - startTime

                // Extract element
                val element = extractElementFromOutput(fullOutput.toString())

                extractedLabelText.text = "Element: ${element.uppercase()}"
                inferenceTimeText.text = "Inference time: ${inferenceTime}ms (${inferenceTime / 1000.0}s)"
                statusText.text = "Inference complete"
            } catch (e: Exception) {
                statusText.text = "Inference failed: ${e.message}"
                Log.e(tag, "Inference failed", e)
            } finally {
                progressBar.visibility = View.GONE
                inferButton.isEnabled = true
            }
        }
    }

    private fun extractElementFromOutput(output: String): String {
        val elements = listOf("water", "land", "fire", "wood", "metal")

        // Try: "The element is {element}"
        val pattern = "The element is (\\w+)".toRegex(RegexOption.IGNORE_CASE)
        val match = pattern.find(output)
        if (match != null) {
            val element = match.groupValues[1].lowercase()
            if (element in elements) {
                return element
            }
        }

        // Fallback: find any element name in output
        for (element in elements) {
            if (output.lowercase().contains(element)) {
                return element
            }
        }

        return "unknown"
    }
}
