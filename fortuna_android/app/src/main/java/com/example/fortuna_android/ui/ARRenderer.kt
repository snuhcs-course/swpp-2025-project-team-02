package com.example.fortuna_android.ui

import android.opengl.Matrix
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.ar.core.Anchor
import com.google.ar.core.Coordinates2d
import com.google.ar.core.Frame
import com.google.ar.core.TrackingState
import com.example.fortuna_android.MainActivity
import com.example.fortuna_android.classification.DetectedObjectResult
import com.example.fortuna_android.classification.MLKitObjectDetector
import com.example.fortuna_android.classification.ObjectDetector
import com.example.fortuna_android.classification.ElementMapper
import com.example.fortuna_android.classification.utils.VLM_PROMPT
import com.example.fortuna_android.common.helpers.DisplayRotationHelper
import com.example.fortuna_android.common.helpers.ImageUtils
import com.example.fortuna_android.vlm.SmolVLMManager
import com.example.fortuna_android.common.samplerender.SampleRender
import com.example.fortuna_android.common.samplerender.arcore.BackgroundRenderer
import com.example.fortuna_android.render.LabelRender
import com.example.fortuna_android.render.PointCloudRender
import com.google.ar.core.exceptions.CameraNotAvailableException
import com.google.ar.core.exceptions.NotYetAvailableException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.catch
import java.util.Collections

/**
 * AR Renderer for object detection using ARCore
 */
class ARRenderer(private val fragment: ARFragment) :
    DefaultLifecycleObserver,
    SampleRender.Renderer,
    CoroutineScope by MainScope() {

    companion object {
        private const val TAG = "ARRenderer"
    }

    private val activity: MainActivity
        get() = fragment.activity as MainActivity

    private val displayRotationHelper = DisplayRotationHelper(fragment.requireActivity())
    private lateinit var backgroundRenderer: BackgroundRenderer
    private val pointCloudRender = PointCloudRender()
    private val labelRenderer = LabelRender()

    private val viewMatrix = FloatArray(16)
    private val projectionMatrix = FloatArray(16)
    private val viewProjectionMatrix = FloatArray(16)

    private val arLabeledAnchors = Collections.synchronizedList(mutableListOf<ARLabeledAnchor>())
    private var scanButtonWasPressed = false

    private val mlKitAnalyzer = MLKitObjectDetector(fragment.requireActivity())
    private var currentAnalyzer: ObjectDetector = mlKitAnalyzer

    // Element mapper for converting ML labels to Chinese Five Elements categories
    private val elementMapper = ElementMapper(fragment.requireContext())

    // VLM manager for scene understanding
    private val vlmManager = SmolVLMManager.getInstance(fragment.requireContext())
    private var isVLMLoaded = false
    private var isVLMAnalyzing = false

    data class ARLabeledAnchor(val anchor: Anchor, val element: ElementMapper.Element)

    override fun onResume(owner: LifecycleOwner) {
        displayRotationHelper.onResume()
    }

    override fun onPause(owner: LifecycleOwner) {
        displayRotationHelper.onPause()
    }

    /**
     * Start object detection - called when scan button is pressed
     */
    fun startObjectDetection() {
        scanButtonWasPressed = true
        Log.d(TAG, "Object detection started")
    }

    /**
     * Clear all anchors
     */
    fun clearAnchors() {
        synchronized(arLabeledAnchors) {
            arLabeledAnchors.clear()
        }
        Log.d(TAG, "AR anchors cleared")
    }

    override fun onSurfaceCreated(render: SampleRender) {
        backgroundRenderer = BackgroundRenderer(render).apply {
            setUseDepthVisualization(render, false)
        }
        pointCloudRender.onSurfaceCreated(render)
        labelRenderer.onSurfaceCreated(render)

        // Load VLM model in background
        launch(Dispatchers.IO) {
            try {
                Log.i(TAG, "Loading VLM model in background...")
                vlmManager.initialize()
                isVLMLoaded = true
                Log.i(TAG, "VLM model loaded successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load VLM model", e)
            }
        }
    }

    override fun onSurfaceChanged(render: SampleRender?, width: Int, height: Int) {
        displayRotationHelper.onSurfaceChanged(width, height)
    }

    private var objectResults: List<DetectedObjectResult>? = null

    override fun onDrawFrame(render: SampleRender) {
        val session = activity.arCoreSessionHelper.sessionCache ?: return

        try {
            val textureId = backgroundRenderer.cameraColorTexture.textureId

            // Validate texture ID is within 32-bit integer range
            if (textureId < 0 || textureId > Int.MAX_VALUE) {
                Log.e(TAG, "Texture ID $textureId exceeds 32-bit integer range")
                return
            }

            session.setCameraTextureNames(intArrayOf(textureId))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set camera texture names", e)
            return
        }

        // Notify ARCore session that the view size changed
        displayRotationHelper.updateSessionIfNeeded(session)

        val frame = try {
            session.update()
        } catch (e: CameraNotAvailableException) {
            Log.e(TAG, "Camera not available during onDrawFrame", e)
            return
        } catch (e: Exception) {
            Log.e(TAG, "ARCore session update failed", e)
            return
        }

        backgroundRenderer.updateDisplayGeometry(frame)
        backgroundRenderer.drawBackground(render)

        // Get camera and projection matrices
        val camera = frame.camera
        camera.getViewMatrix(viewMatrix, 0)
        camera.getProjectionMatrix(projectionMatrix, 0, 0.01f, 100.0f)
        Matrix.multiplyMM(viewProjectionMatrix, 0, projectionMatrix, 0, viewMatrix, 0)

        // Handle tracking failures
        if (camera.trackingState != TrackingState.TRACKING) {
            return
        }

        // Draw point cloud
        frame.acquirePointCloud().use { pointCloud ->
            pointCloudRender.drawPointCloud(render, pointCloud, viewProjectionMatrix)
        }

        // Process object detection if scan button was pressed
        if (scanButtonWasPressed) {
            scanButtonWasPressed = false
            try {
                val cameraImage = frame.acquireCameraImage()
                if (cameraImage != null) {
                // Run ML model on IO thread
                launch(Dispatchers.IO) {
                    try {
                        val cameraId = session.cameraConfig.cameraId
                        val imageRotation = displayRotationHelper.getCameraSensorToDisplayRotation(cameraId)
                        objectResults = currentAnalyzer.analyze(cameraImage, imageRotation)
                        cameraImage.close()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error during object analysis", e)
                        cameraImage.close()
                        // Reset scan button state on main thread
                        fragment.view?.post {
                            fragment.setScanningActive(false)
                        }
                    }
                }
                } else {
                    // Reset scan button state if camera image not available
                    fragment.setScanningActive(false)
                }
            } catch (e: NotYetAvailableException) {
                Log.w(TAG, "Camera image not yet available", e)
                fragment.setScanningActive(false)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to acquire camera image", e)
                fragment.setScanningActive(false)
            }
        }

        // Process object detection results
        val objects = objectResults
        if (objects != null) {
            objectResults = null
            Log.i(TAG, "=== OBJECT DETECTION RESULTS ===")
            Log.i(TAG, "ML Kit detected ${objects.size} objects")

            // Map detected objects to element categories
            val elementResults = objects.map { obj ->
                val element = elementMapper.mapLabelToElement(obj.label)
                Log.i(TAG, "Object '${obj.label}' mapped to element: ${element.displayName}")
                obj to element
            }

            elementResults.forEachIndexed { index, (obj, element) ->
                val (atX, atY) = obj.centerCoordinate
                Log.i(TAG, "Object $index: '${obj.label}' -> '${element.displayName}' at coordinates ($atX, $atY) with confidence ${obj.confidence}")
            }

            val anchors = elementResults.mapNotNull { (obj, element) ->
                val (atX, atY) = obj.centerCoordinate
                Log.d(TAG, "Attempting to create anchor for '${element.displayName}' (from '${obj.label}') at image coordinates ($atX, $atY)")

                val anchor = createAnchor(atX.toFloat(), atY.toFloat(), frame)
                if (anchor != null) {
                    Log.i(TAG, "✅ Successfully created anchor for '${element.displayName}' at pose: ${anchor.pose}")
                    ARLabeledAnchor(anchor, element)
                } else {
                    Log.w(TAG, "❌ Failed to create anchor for '${element.displayName}' at ($atX, $atY)")
                    null
                }
            }

            // Thread-safe way to add anchors
            synchronized(arLabeledAnchors) {
                arLabeledAnchors.addAll(anchors)
            }

            // Notify fragment about detection results on main thread
            fragment.view?.post {
                fragment.onObjectDetectionCompleted(anchors.size, objects.size)
            }

            // Trigger VLM analysis if model is loaded and not currently analyzing
            if (isVLMLoaded && !isVLMAnalyzing && objects.isNotEmpty()) {
                analyzeCameraImageWithVLM(frame)
            }
        }

        // Draw text labels at their anchor positions - create a safe copy to avoid concurrent modification
        val anchorsCopy = synchronized(arLabeledAnchors) {
            arLabeledAnchors.toList()
        }

        for (arLabeledAnchor in anchorsCopy) {
            val anchor = arLabeledAnchor.anchor
            if (anchor.trackingState != TrackingState.TRACKING) continue

            // Draw text label for all elements
            labelRenderer.draw(
                render,
                viewProjectionMatrix,
                anchor.pose,
                camera.pose,
                arLabeledAnchor.element.displayName
            )
        }
    }

    // Temporary arrays to prevent allocations in createAnchor
    private val convertFloats = FloatArray(4)
    private val convertFloatsOut = FloatArray(4)

    /**
     * Create an anchor using (x, y) coordinates in the IMAGE_PIXELS coordinate space
     */
    private fun createAnchor(xImage: Float, yImage: Float, frame: Frame): Anchor? {
        return try {
            // IMAGE_PIXELS -> VIEW
            convertFloats[0] = xImage
            convertFloats[1] = yImage
            frame.transformCoordinates2d(
                Coordinates2d.IMAGE_PIXELS,
                convertFloats,
                Coordinates2d.VIEW,
                convertFloatsOut
            )

            Log.d(TAG, "Coordinate transform: IMAGE($xImage, $yImage) -> VIEW(${convertFloatsOut[0]}, ${convertFloatsOut[1]})")

            // Conduct a hit test using the VIEW coordinates
            val hits = frame.hitTest(convertFloatsOut[0], convertFloatsOut[1])
            Log.d(TAG, "Hit test returned ${hits.size} results")

            val result = hits.getOrNull(0)
            if (result == null) {
                Log.w(TAG, "No hit test results for VIEW coordinates (${convertFloatsOut[0]}, ${convertFloatsOut[1]})")
                return null
            }

            Log.d(TAG, "Hit result: trackable=${result.trackable::class.simpleName}, distance=${result.distance}")
            result.trackable.createAnchor(result.hitPose)
        } catch (e: NotYetAvailableException) {
            Log.w(TAG, "Camera pose not yet available for anchor creation")
            null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create anchor", e)
            null
        }
    }

    /**
     * Analyze camera image with VLM for scene understanding
     */
    private fun analyzeCameraImageWithVLM(frame: Frame) {
        if (isVLMAnalyzing) {
            Log.d(TAG, "VLM analysis already in progress, skipping")
            return
        }

        isVLMAnalyzing = true
        fragment.view?.post {
            fragment.onVLMAnalysisStarted()
        }

        launch(Dispatchers.IO) {
            try {
                Log.i(TAG, "Starting VLM analysis...")

                // Acquire camera image
                val cameraImage = frame.acquireCameraImage()

                // Convert YUV to Bitmap
                val bitmap = ImageUtils.convertYuvImageToBitmap(cameraImage)
                cameraImage.close()

                if (bitmap == null) {
                    Log.e(TAG, "Failed to convert camera image to bitmap")
                    isVLMAnalyzing = false
                    return@launch
                }

                // Optimize for VLM (downscale for faster inference)
                // Smaller size = faster evaluation but lower quality
                val optimizedBitmap = ImageUtils.optimizeImageForVLM(bitmap, 224)
                Log.i(TAG, "Image optimized: ${bitmap.width}x${bitmap.height} → ${optimizedBitmap.width}x${optimizedBitmap.height}")

                // Clean up original if different
                if (optimizedBitmap != bitmap) {
                    bitmap.recycle()
                }

                // VLM prompt for scene description
                // Stream VLM results
                vlmManager.analyzeImage(optimizedBitmap, VLM_PROMPT)
                    .catch { e ->
                        Log.e(TAG, "VLM analysis error", e)
                        fragment.view?.post {
                            fragment.updateVLMDescription("\nError: ${e.message}")
                        }
                    }
                    .collect { token ->
                        fragment.updateVLMDescription(token)
                    }

                // Clean up optimized bitmap
                optimizedBitmap.recycle()

                Log.i(TAG, "VLM analysis completed")
                fragment.view?.post {
                    fragment.onVLMAnalysisCompleted()
                }

            } catch (e: NotYetAvailableException) {
                Log.w(TAG, "Camera image not yet available for VLM", e)
            } catch (e: Exception) {
                Log.e(TAG, "VLM analysis failed", e)
                fragment.view?.post {
                    fragment.updateVLMDescription("\nAnalysis failed: ${e.message}")
                }
            } finally {
                isVLMAnalyzing = false
            }
        }
    }
}