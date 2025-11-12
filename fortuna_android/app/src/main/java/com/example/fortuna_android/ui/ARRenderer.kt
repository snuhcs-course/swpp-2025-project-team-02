package com.example.fortuna_android.ui

import android.graphics.RectF
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
import com.example.fortuna_android.classification.VLMElementClassifier
import com.example.fortuna_android.common.helpers.DisplayRotationHelper
import com.example.fortuna_android.common.helpers.ImageUtils
import com.example.fortuna_android.common.samplerender.SampleRender
import com.example.fortuna_android.common.samplerender.arcore.BackgroundRenderer
import com.example.fortuna_android.render.ObjectRender
import com.example.fortuna_android.render.PointCloudRender
import com.google.ar.core.exceptions.CameraNotAvailableException
import com.google.ar.core.exceptions.NotYetAvailableException
import com.google.ar.core.exceptions.SessionPausedException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    private val objectRenderer = ObjectRender()

    private val viewMatrix = FloatArray(16)
    private val projectionMatrix = FloatArray(16)
    private val viewProjectionMatrix = FloatArray(16)

    private val arLabeledAnchors = Collections.synchronizedList(mutableListOf<ARLabeledAnchor>())
    private var scanButtonWasPressed = false

    private val mlKitAnalyzer = MLKitObjectDetector(fragment.requireActivity())
    private var currentAnalyzer: ObjectDetector = mlKitAnalyzer

    // Element mapper for converting ML labels to Chinese Five Elements categories
    private val elementMapper = ElementMapper(fragment.requireContext())

    // AR Game: needed element from API (null = show all elements)
    private var neededElement: ElementMapper.Element? = null
    private var collectedCount = 0

    // Bounding box visualization: track detected objects with VLM state
    private val detectedObjects = Collections.synchronizedList(mutableListOf<DetectedObject>())

    // Screen dimensions for projection
    private var screenWidth = 0f
    private var screenHeight = 0f

    // Camera image dimensions (captured during scan for coordinate transformation)
    private var cameraImageWidth = 0f
    private var cameraImageHeight = 0f

    // Pending tap to process on next frame
    private var pendingTap: Pair<Float, Float>? = null

    // Pending anchor creation (results from VLM/ElementMapper, total object count)
    private var pendingAnchorCreation: Pair<List<Pair<DetectedObjectResult, ElementMapper.Element>>, Int>? = null

    // VLM classifier for element detection
    private val vlmClassifier = VLMElementClassifier(fragment.requireContext())
    private var isVLMLoaded = false

    // Animation timing
    private var lastFrameTime = 0L

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
     * Clear all anchors and detected objects
     */
    fun clearAnchors() {
        synchronized(arLabeledAnchors) {
            arLabeledAnchors.clear()
        }
        synchronized(detectedObjects) {
            detectedObjects.clear()
        }
        // Update the overlay to clear bounding boxes
        fragment.view?.post {
            fragment.updateDetectedObjects(emptyList())
        }
        Log.d(TAG, "AR anchors and detected objects cleared")
    }

    /**
     * Set the needed element for AR Game mode
     * Only spheres matching this element will be displayed
     * Pass null to show all elements
     */
    fun setNeededElement(element: ElementMapper.Element?) {
        neededElement = element
        collectedCount = 0
        Log.i(TAG, "AR Game: Needed element set to ${element?.displayName ?: "ALL"}")
    }

    /**
     * Get the current collection count for game progress
     */
    fun getCollectedCount(): Int = collectedCount

    /**
     * Queue a tap to be processed on the next render frame
     * This avoids GL context issues by deferring to the render thread
     */
    fun handleTap(x: Float, y: Float) {
        Log.d(TAG, "Tap queued at ($x, $y)")
        pendingTap = Pair(x, y)
    }

    /**
     * Process a queued tap (called from onDrawFrame on GL thread)
     * Returns the collected anchor or null
     */
    private fun processTap(x: Float, y: Float): ARLabeledAnchor? {
        // Screen-space distance threshold (in pixels)
        val tapThreshold = 150f // pixels

        var closestAnchor: ARLabeledAnchor? = null
        var closestDistance = Float.MAX_VALUE

        // Check all anchors and find the closest one in screen space
        val anchorCount = synchronized(arLabeledAnchors) {
            arLabeledAnchors.size
        }
        Log.d(TAG, "Processing tap at ($x, $y), checking $anchorCount anchors (needed element: ${neededElement?.displayName ?: "ALL"})")

        synchronized(arLabeledAnchors) {
            for (labeledAnchor in arLabeledAnchors) {
                val anchor = labeledAnchor.anchor
                if (anchor.trackingState != TrackingState.TRACKING) continue

                // Skip if not the needed element (if filter is set)
                if (neededElement != null && labeledAnchor.element != neededElement) {
                    continue
                }

                // Project anchor position to screen coordinates
                val anchorPose = anchor.pose
                val worldPos = floatArrayOf(anchorPose.tx(), anchorPose.ty(), anchorPose.tz(), 1f)
                val screenPos = projectToScreen(worldPos, viewMatrix, projectionMatrix)

                if (screenPos != null) {
                    // Calculate 2D distance on screen
                    val dx = screenPos[0] - x
                    val dy = screenPos[1] - y
                    val distance = kotlin.math.sqrt(dx * dx + dy * dy)

                    Log.d(TAG, "${labeledAnchor.element.displayName} sphere at (${screenPos[0].toInt()}, ${screenPos[1].toInt()}), distance: ${distance.toInt()}px")

                    if (distance < closestDistance && distance < tapThreshold) {
                        closestDistance = distance
                        closestAnchor = labeledAnchor
                    }
                } else {
                    Log.d(TAG, "${labeledAnchor.element.displayName} sphere not on screen")
                }
            }
        }

        // If we found a close enough anchor, collect it
        if (closestAnchor != null) {
            synchronized(arLabeledAnchors) {
                arLabeledAnchors.remove(closestAnchor)
            }
            collectedCount++
            Log.i(TAG, "🎮 Collected ${closestAnchor.element.displayName} sphere! Total: $collectedCount (distance: ${closestDistance.toInt()} px)")
            return closestAnchor
        }

        if (closestDistance == Float.MAX_VALUE) {
            Log.w(TAG, "No spheres found on screen")
        } else {
            Log.d(TAG, "No sphere tapped (closest was ${closestDistance.toInt()} px, threshold: ${tapThreshold.toInt()} px)")
        }
        return null
    }

    /**
     * Project a 3D world position to 2D screen coordinates
     * Returns [x, y] in screen pixels, or null if behind camera
     */
    private fun projectToScreen(worldPos: FloatArray, viewMat: FloatArray, projMat: FloatArray): FloatArray? {
        // Check if screen dimensions are set
        if (screenWidth <= 0f || screenHeight <= 0f) {
            Log.w(TAG, "Screen dimensions not set yet")
            return null
        }

        val viewPos = FloatArray(4)
        val clipPos = FloatArray(4)

        // World to view space
        Matrix.multiplyMV(viewPos, 0, viewMat, 0, worldPos, 0)

        // View to clip space
        Matrix.multiplyMV(clipPos, 0, projMat, 0, viewPos, 0)

        // Check if behind camera
        if (clipPos[3] <= 0f) return null

        // Perspective divide
        val ndcX = clipPos[0] / clipPos[3]
        val ndcY = clipPos[1] / clipPos[3]

        // Check if outside screen bounds
        if (ndcX < -1f || ndcX > 1f || ndcY < -1f || ndcY > 1f) return null

        // NDC to screen space
        val screenX = (ndcX + 1f) * 0.5f * screenWidth
        val screenY = (1f - ndcY) * 0.5f * screenHeight // Flip Y

        return floatArrayOf(screenX, screenY)
    }

    override fun onSurfaceCreated(render: SampleRender) {
        backgroundRenderer = BackgroundRenderer(render).apply {
            setUseDepthVisualization(render, false)
        }
        pointCloudRender.onSurfaceCreated(render)
        objectRenderer.onSurfaceCreated(render)
        // Load VLM classifier model in background
        launch(Dispatchers.IO) {
            try {
                Log.i(TAG, "Loading VLM classifier model in background...")
                vlmClassifier.initialize()
                isVLMLoaded = true
                Log.i(TAG, "VLM classifier model loaded successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load VLM classifier model", e)
            }
        }
    }

    override fun onSurfaceChanged(render: SampleRender?, width: Int, height: Int) {
        displayRotationHelper.onSurfaceChanged(width, height)
        screenWidth = width.toFloat()
        screenHeight = height.toFloat()
        Log.i(TAG, "Screen dimensions: ${screenWidth}x${screenHeight}")
    }

    private var objectResults: List<DetectedObjectResult>? = null

    override fun onDrawFrame(render: SampleRender) {
        val session = activity.arCoreSessionHelper?.sessionCache ?: return

        // Update animation time for bouncing objects
        val currentTime = System.currentTimeMillis()
        if (lastFrameTime == 0L) {
            lastFrameTime = currentTime
        }
        val deltaTime = (currentTime - lastFrameTime) / 1000.0f // Convert to seconds
        lastFrameTime = currentTime
        objectRenderer.updateAnimation(deltaTime)

        // Follow reference code pattern: set camera texture names first, then update session
        session.setCameraTextureNames(intArrayOf(backgroundRenderer.cameraColorTexture.textureId))

        // Notify ARCore session that the view size changed
        displayRotationHelper.updateSessionIfNeeded(session)

        val frame = try {
            session.update()
        } catch (e: CameraNotAvailableException) {
            Log.e(TAG, "Camera not available during onDrawFrame", e)
            return
        } catch (e: NotYetAvailableException) {
            // Motion tracking or depth data not ready yet - this is normal during startup
            Log.d(TAG, "ARCore data not yet available, skipping frame")
            return
        } catch (e: SessionPausedException) {
            // Session is paused - skip this frame silently as it's normal during lifecycle changes
            Log.d(TAG, "ARCore session is paused, skipping frame")
            return
        } catch (e: Exception) {
            Log.e(TAG, "ARCore session update failed: ${e.message}", e)
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

        // Process pending tap (if any)
        val tap = pendingTap
        if (tap != null) {
            pendingTap = null
            val collectedAnchor = processTap(tap.first, tap.second)

            // Notify fragment on main thread
            if (collectedAnchor != null) {
                fragment.view?.post {
                    fragment.onSphereCollected(collectedCount)
                }
            }
        }

        // Process pending anchor creation (if any)
        val anchorCreation = pendingAnchorCreation
        if (anchorCreation != null && cameraImageWidth > 0 && cameraImageHeight > 0) {
            pendingAnchorCreation = null
            val (elementResults, totalObjects) = anchorCreation

            val anchors = elementResults.mapNotNull { (obj, element) ->
                val (atX, atY) = obj.centerCoordinate
                Log.d(TAG, "Attempting to create anchor for '${element.displayName}' (from '${obj.label}') at image coordinates ($atX, $atY)")

                val anchor = createAnchor(atX.toFloat(), atY.toFloat(), cameraImageWidth, cameraImageHeight, frame)
                if (anchor != null) {
                    Log.i(TAG, "✅ Successfully created anchor for '${element.displayName}' at pose: ${anchor.pose}")
                    ARLabeledAnchor(anchor, element)
                } else {
                    Log.w(TAG, "❌ Failed to create anchor for '${element.displayName}' at ($atX, $atY)")
                    null
                }
            }

            synchronized(arLabeledAnchors) {
                arLabeledAnchors.addAll(anchors)
            }

            Log.i(TAG, "🎯 SUMMARY: Created ${anchors.size} AR anchors from $totalObjects detected objects")
            Log.i(TAG, "🎨 Total Pokemon in scene: ${arLabeledAnchors.size}")

            fragment.view?.post {
                fragment.onObjectDetectionCompleted(anchors.size, totalObjects)
            }
        }

        // Process object detection if scan button was pressed
        if (scanButtonWasPressed) {
            scanButtonWasPressed = false
            val cameraImage = frame.tryAcquireCameraImage()
            if (cameraImage != null) {
                // Capture camera dimensions while image is valid (needed for anchor creation later)
                val capturedImageWidth = cameraImage.width.toFloat()
                val capturedImageHeight = cameraImage.height.toFloat()

                // Run ML model on IO thread
                launch(Dispatchers.IO) {
                    try {
                        val cameraId = session.cameraConfig.cameraId
                        val imageRotation = displayRotationHelper.getCameraSensorToDisplayRotation(cameraId)
                        objectResults = currentAnalyzer.analyze(cameraImage, imageRotation)
                        // Store dimensions for anchor creation
                        cameraImageWidth = capturedImageWidth
                        cameraImageHeight = capturedImageHeight
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
        }

        // Process object detection results
        val objects = objectResults
        if (objects != null) {
            objectResults = null
            Log.i(TAG, "=== OBJECT DETECTION RESULTS ===")
            Log.i(TAG, "ML Kit detected ${objects.size} objects")

            // Create DetectedObject instances for bounding box visualization
            val newDetectedObjects = objects.map { obj ->
                DetectedObject(
                    boundingBox = RectF(obj.boundingBox),
                    label = obj.label,
                    confidence = obj.confidence,
                    vlmState = VLMProcessingState.PENDING
                )
            }
            synchronized(detectedObjects) {
                detectedObjects.clear()
                detectedObjects.addAll(newDetectedObjects)
            }

            // Update overlay with PENDING objects
            fragment.view?.post {
                fragment.updateDetectedObjects(newDetectedObjects.toList())
            }

            // Classify detected objects using VLM
            if (isVLMLoaded && vlmClassifier.isReady()) {
                launch(Dispatchers.IO) {
                    val elementResults = objects.mapIndexed { index, obj ->
                        // Update state to PROCESSING
                        newDetectedObjects[index].vlmState = VLMProcessingState.PROCESSING
                        fragment.view?.post {
                            val objectsCopy = synchronized(detectedObjects) { detectedObjects.toList() }
                            fragment.updateDetectedObjects(objectsCopy)
                        }

                        Log.i(TAG, "Classifying object '${obj.label}' with VLM...")
                        val vlmResult = vlmClassifier.classifyElement(obj.croppedBitmap)

                        if (vlmResult != null) {
                            Log.i(TAG, "VLM classified '${obj.label}' as: ${vlmResult.element?.displayName}")
                            // Update DetectedObject with VLM results
                            newDetectedObjects[index].vlmState = VLMProcessingState.COMPLETED
                            newDetectedObjects[index].classifiedElement = vlmResult.element
                            newDetectedObjects[index].rawVlmOutput = vlmResult.rawOutput

                            // Update overlay with COMPLETED state
                            fragment.view?.post {
                                val objectsCopy = synchronized(detectedObjects) { detectedObjects.toList() }
                                fragment.updateDetectedObjects(objectsCopy)
                            }

                            obj to (vlmResult.element ?: elementMapper.mapLabelToElement(obj.label))
                        } else {
                            Log.w(TAG, "VLM failed to classify '${obj.label}', using fallback")
                            // Mark as FAILED
                            newDetectedObjects[index].vlmState = VLMProcessingState.FAILED
                            newDetectedObjects[index].vlmError = "VLM classification failed"

                            // Update overlay with FAILED state
                            fragment.view?.post {
                                val objectsCopy = synchronized(detectedObjects) { detectedObjects.toList() }
                                fragment.updateDetectedObjects(objectsCopy)
                            }

                            // Fallback to ElementMapper if VLM fails
                            obj to elementMapper.mapLabelToElement(obj.label)
                        }
                    }

                    elementResults.forEachIndexed { index, (obj, element) ->
                        val (atX, atY) = obj.centerCoordinate
                        Log.i(TAG, "Object $index: '${obj.label}' -> '${element.displayName}' at coordinates ($atX, $atY) with confidence ${obj.confidence}")
                    }

                    // Check for detected elements and notify fragment with sound effects
                    val detectedElements = elementResults.map { (_, element) -> element }.toSet()
                    detectedElements.forEach { element ->
                        fragment.view?.post {
                            fragment.onElementDetected(element)
                        }
                    }

                    // Store results to process anchors on next GL frame
                    pendingAnchorCreation = elementResults to objects.size
                }
            } else {
                Log.w(TAG, "VLM not ready, using ElementMapper fallback")
                // Fallback to ElementMapper if VLM not loaded
                val elementResults = objects.mapIndexed { index, obj ->
                    val element = elementMapper.mapLabelToElement(obj.label)
                    Log.i(TAG, "Object '${obj.label}' mapped to element: ${element.displayName}")

                    // Update DetectedObject with fallback classification
                    newDetectedObjects[index].vlmState = VLMProcessingState.COMPLETED
                    newDetectedObjects[index].classifiedElement = element
                    newDetectedObjects[index].rawVlmOutput = "Used ElementMapper fallback (VLM not ready)"

                    obj to element
                }

                // Update overlay with completed fallback results
                fragment.view?.post {
                    val objectsCopy = synchronized(detectedObjects) { detectedObjects.toList() }
                    fragment.updateDetectedObjects(objectsCopy)
                }

                // Process with ElementMapper (same logic as before)
                elementResults.forEachIndexed { index, (obj, element) ->
                    val (atX, atY) = obj.centerCoordinate
                    Log.i(TAG, "Object $index: '${obj.label}' -> '${element.displayName}' at coordinates ($atX, $atY) with confidence ${obj.confidence}")
                }

                val detectedElements = elementResults.map { (_, element) -> element }.toSet()
                detectedElements.forEach { element ->
                    fragment.view?.post {
                        fragment.onElementDetected(element)
                    }
                }

                // Store results to process anchors on next GL frame
                pendingAnchorCreation = elementResults to objects.size
            }
        }

        // Draw 3D sphere objects at their anchor positions - create a safe copy to avoid concurrent modification
        val anchorsCopy = synchronized(arLabeledAnchors) {
            arLabeledAnchors.toList()
        }

        for (arLabeledAnchor in anchorsCopy) {
            val anchor = arLabeledAnchor.anchor
            if (anchor.trackingState != TrackingState.TRACKING) continue

            // AR Game: Filter to show only needed element (if set)
            val shouldShow = if (neededElement != null) {
                arLabeledAnchor.element == neededElement
            } else {
                true // Show all elements if no filter is set
            }

            if (shouldShow) {
                // Draw 3D Pokemon object for each element
                Log.v(TAG, "🎮 Drawing ${arLabeledAnchor.element.displayName} at ${anchor.pose.translation}")
                objectRenderer.draw(
                    render,
                    viewMatrix,
                    projectionMatrix,
                    anchor.pose,
                    arLabeledAnchor.element
                )
            }
        }
    }

    // Temporary arrays to prevent allocations in createAnchor
    private val convertFloats = FloatArray(4)
    private val convertFloatsOut = FloatArray(4)

    /**
     * Create an anchor using (x, y) coordinates in the IMAGE_PIXELS coordinate space
     * If no plane is detected, creates anchor at fixed distance from camera
     * @param xImage X coordinate in image pixels
     * @param yImage Y coordinate in image pixels
     * @param imageWidth Width of camera image in pixels
     * @param imageHeight Height of camera image in pixels
     * @param frame Current ARCore frame (must be valid when called)
     */
    private fun createAnchor(xImage: Float, yImage: Float, imageWidth: Float, imageHeight: Float, frame: Frame): Anchor? {
        return try {
            // Normalize to 0.0-1.0 range for VIEW coordinates
            val viewX = xImage / imageWidth
            val viewY = yImage / imageHeight

            Log.d(TAG, "Coordinate transform: IMAGE($xImage, $yImage) -> VIEW($viewX, $viewY) (image size: ${imageWidth}x${imageHeight})")

            // Try hit test first (prefer plane-based anchors when available)
            val hits = frame.hitTest(viewX, viewY)
            Log.d(TAG, "Hit test returned ${hits.size} results")

            val hitResult = hits.getOrNull(0)
            if (hitResult != null) {
                Log.d(TAG, "✅ Hit result: trackable=${hitResult.trackable::class.simpleName}, distance=${hitResult.distance}")
                return hitResult.trackable.createAnchor(hitResult.hitPose)
            }

            // No plane detected - create anchor at fixed distance from camera
            Log.d(TAG, "No plane detected, creating anchor at fixed distance from camera")
            val camera = frame.camera
            val cameraPose = camera.pose

            // Create a ray from camera through the tapped point
            // Convert VIEW coordinates to NDC (Normalized Device Coordinates: -1 to 1)
            val ndcX = viewX * 2.0f - 1.0f
            val ndcY = -(viewY * 2.0f - 1.0f)  // Flip Y axis

            // Get camera projection matrix and invert it
            val projectionMatrix = FloatArray(16)
            camera.getProjectionMatrix(projectionMatrix, 0, 0.1f, 100.0f)

            // Simple ray direction calculation (forward with small offset based on tap location)
            // Place Pokemon 1.5 meters in front of camera
            val distance = 1.5f
            val translation = floatArrayOf(
                ndcX * 0.3f,  // Horizontal offset based on tap
                ndcY * 0.3f,  // Vertical offset based on tap
                -distance      // Forward from camera
            )

            // Transform by camera orientation
            val anchorPose = cameraPose.compose(Pose(translation, floatArrayOf(0f, 0f, 0f, 1f)))

            Log.i(TAG, "✅ Created anchor at fixed distance: ${distance}m from camera")
            session.createAnchor(anchorPose)
        } catch (e: NotYetAvailableException) {
            Log.w(TAG, "Camera pose not yet available for anchor creation")
            null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create anchor", e)
            null
        }
    }

    /**
     * Utility method for Frame.acquireCameraImage that maps NotYetAvailableException to null.
     * Follows the same pattern as the reference ARCore code.
     */
    private fun Frame.tryAcquireCameraImage() = try {
        acquireCameraImage()
    } catch (e: NotYetAvailableException) {
        null
    } catch (e: Throwable) {
        throw e
    }
}