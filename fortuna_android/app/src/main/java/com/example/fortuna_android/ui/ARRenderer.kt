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
import com.example.fortuna_android.classification.VLMObjectDetector
import com.example.fortuna_android.classification.ObjectDetector
import com.example.fortuna_android.classification.ObjectDetectorFactory
import com.example.fortuna_android.classification.ConfigurableDetectorFactory
import com.example.fortuna_android.classification.ElementMapper
import com.example.fortuna_android.common.AppConstants
import com.example.fortuna_android.common.helpers.DisplayRotationHelper
import com.example.fortuna_android.common.helpers.ImageUtils
import com.example.fortuna_android.vlm.SmolVLMManager
import com.example.fortuna_android.common.samplerender.SampleRender
import com.example.fortuna_android.common.samplerender.arcore.BackgroundRenderer
import com.example.fortuna_android.render.ObjectRender
import com.example.fortuna_android.render.PointCloudRender
import com.example.fortuna_android.render.RenderComponent
import com.example.fortuna_android.render.CompositeRenderer
import com.example.fortuna_android.render.BackgroundRendererComponent
import com.example.fortuna_android.render.PointCloudRendererComponent
import com.example.fortuna_android.render.ObjectRendererComponent
import com.example.fortuna_android.render.RenderContext
import com.example.fortuna_android.render.FrustumCuller
import com.google.ar.core.exceptions.CameraNotAvailableException
import com.google.ar.core.exceptions.NotYetAvailableException
import com.google.ar.core.exceptions.SessionPausedException
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

    // Composite Pattern: Root renderer managing all rendering components
    // Similar to: AllMenus composite managing PancakeHouseMenu and DinerMenu
    private lateinit var rootRenderer: CompositeRenderer

    // Keep old renderers for backward compatibility during transition
    private lateinit var backgroundRenderer: BackgroundRenderer
    private val pointCloudRender = PointCloudRender()
    private val objectRenderer = ObjectRender()

    private val viewMatrix = FloatArray(16)
    private val projectionMatrix = FloatArray(16)
    private val viewProjectionMatrix = FloatArray(16)

    private val arLabeledAnchors = Collections.synchronizedList(mutableListOf<ARLabeledAnchor>())
    private val pendingVLMClassifications = Collections.synchronizedList(mutableListOf<PendingVLMClassification>())
    private var scanButtonWasPressed = false
    private var vlmClassificationComplete = false  // Track VLM completion to hide bounding box
    private var cropSizeRatio = 0.3f  // Current crop size ratio for VLM

    private lateinit var vlmAnalyzer: VLMObjectDetector
    private var currentAnalyzer: ObjectDetector? = null  // Will be set to VLM analyzer when loaded

    // Factory Method Pattern: Detector factory instance
    private lateinit var detectorFactory: ObjectDetectorFactory

    // Element mapper for converting ML labels to Chinese Five Elements categories
    private val elementMapper = ElementMapper(fragment.requireContext())

    // AR Game: needed element from API (null = show all elements)
    private var neededElement: ElementMapper.Element? = null
    private var collectedCount = 0

    // Screen dimensions for projection
    private var screenWidth = 0f
    private var screenHeight = 0f

    // Pending tap to process on next frame
    private var pendingTap: Pair<Float, Float>? = null
    // VLM manager for scene understanding
    private val vlmManager = SmolVLMManager.getInstance(fragment.requireContext())
    private var isVLMLoaded = false
    private var isVLMAnalyzing = false

    // Animation timing
    private var lastFrameTime = 0L

    // Frustum culling
    private val frustumCuller = FrustumCuller()

    enum class AnimationType {
        JUMPING, ROTATING
    }

    data class ARLabeledAnchor(
        val anchor: Anchor,
        val element: ElementMapper.Element,
        val distance: Float = 1.5f,  // Distance from camera when created (default 1.5m)
        val animationType: AnimationType = if (kotlin.random.Random.nextBoolean()) AnimationType.JUMPING else AnimationType.ROTATING
    )

    // Data class to store initial detection with anchor for later VLM classification
    data class PendingVLMClassification(
        val anchor: Anchor,
        val distance: Float,
        val originalLabel: String,
        val boundingBoxCenter: Pair<Float, Float>
    )

    override fun onResume(owner: LifecycleOwner) {
        displayRotationHelper.onResume()
    }

    override fun onPause(owner: LifecycleOwner) {
        displayRotationHelper.onPause()
    }

    /**
     * Start object detection with custom crop ratio - called when scan button is released
     */
    fun startObjectDetection(customCropRatio: Float = 0.3f) {
        // Prevent scanning if VLM isn't ready
        if (!isVLMLoaded) {
            Log.w(TAG, "VLM not ready yet, ignoring scan request")
            return
        }

        // Clamp crop ratio to valid range
        cropSizeRatio = customCropRatio.coerceIn(VLMObjectDetector.MIN_SIZE_RATIO, VLMObjectDetector.MAX_SIZE_RATIO)

        scanButtonWasPressed = true
        vlmClassificationComplete = false  // Reset flag for new detection
        // Clear any pending classifications from previous detection
        synchronized(pendingVLMClassifications) {
            pendingVLMClassifications.clear()
        }
        Log.d(TAG, "Object detection started with crop ratio: $cropSizeRatio, cleared pending classifications")
    }

    /**
     * Clear all anchors
     */
    fun clearAnchors() {
        synchronized(arLabeledAnchors) {
            arLabeledAnchors.clear()
        }
        synchronized(pendingVLMClassifications) {
            pendingVLMClassifications.clear()
        }
        Log.d(TAG, "AR anchors and pending classifications cleared")
    }

    /**
     * Set the needed element for AR Game mode
     * All detected spheres will be displayed, but only the needed element will count towards collection
     * Pass null to count all elements
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
     * Get the current crop size ratio for bounding box display
     */
    fun getCurrentCropSizeRatio(): Float = cropSizeRatio

    /**
     * Get frustum culling statistics for performance monitoring
     * @return Pair(totalTests, culledObjects)
     */
    fun getFrustumCullingStats(): Pair<Int, Int> = frustumCuller.getCullingStats()

    /**
     * Reset frustum culling statistics
     */
    fun resetFrustumCullingStats() = frustumCuller.resetStats()

    /**
     * Get debug information about frustum planes (for visualization)
     */
    fun getFrustumDebugInfo(): List<String> = frustumCuller.getFrustumPlanesDebugInfo()

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
     * Returns Pair(tappedAnchor, wasNeededElement) or null if no tap
     */
    private fun processTap(x: Float, y: Float): Pair<ARLabeledAnchor, Boolean>? {
        // Screen-space distance threshold (in pixels)
        val tapThreshold = AppConstants.TAP_THRESHOLD_PIXELS

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

                // Calculate object dimensions based on distance and scale
                val anchorPose = anchor.pose
                val distanceScale = kotlin.math.max(1.0f, labeledAnchor.distance / 1.5f) // REFERENCE_DISTANCE from ObjectRender
                val baseScale = 0.1f * distanceScale // OBJECT_SCALE from ObjectRender
                val effectiveScale = kotlin.math.max(0.08f, baseScale) // MIN_VISUAL_SCALE from ObjectRender

                // Estimate object height (scale factor * 2 for sphere diameter + bounce height)
                val objectHeight = effectiveScale * 2.0f + 0.05f // Adding BOUNCE_HEIGHT for animation

                // Check multiple points along the object's height for tap detection
                var minDistance = Float.MAX_VALUE
                val checkPoints = 5 // Number of points to check along the object height

                for (i in 0 until checkPoints) {
                    val heightOffset = (i.toFloat() / (checkPoints - 1)) * objectHeight
                    val worldPos = floatArrayOf(
                        anchorPose.tx(),
                        anchorPose.ty() + heightOffset,
                        anchorPose.tz(),
                        1f
                    )
                    val screenPos = projectToScreen(worldPos, viewMatrix, projectionMatrix)

                    if (screenPos != null) {
                        // Calculate 2D distance on screen
                        val dx = screenPos[0] - x
                        val dy = screenPos[1] - y
                        val distance = kotlin.math.sqrt(dx * dx + dy * dy)

                        if (distance < minDistance) {
                            minDistance = distance
                        }
                    }
                }

                Log.d(TAG, "${labeledAnchor.element.displayName} sphere closest distance: ${minDistance.toInt()}px (height: $objectHeight)")

                if (minDistance < closestDistance && minDistance < tapThreshold) {
                    closestDistance = minDistance
                    closestAnchor = labeledAnchor
                }
            }
        }

        // If we found a close enough anchor, remove it and handle counting
        if (closestAnchor != null) {
            synchronized(arLabeledAnchors) {
                arLabeledAnchors.remove(closestAnchor)
            }

            // Only count towards collected if it matches the needed element and hasn't reached target
            val shouldCount = (neededElement == null || closestAnchor.element == neededElement) && collectedCount < 5
            if (shouldCount) {
                collectedCount++
                Log.i(TAG, "🎮 Collected ${closestAnchor.element.displayName} sphere! Total: $collectedCount (distance: ${closestDistance.toInt()} px)")
            } else if (neededElement != null && closestAnchor.element != neededElement) {
                Log.i(TAG, "🗑️ Eliminated ${closestAnchor.element.displayName} sphere (not needed, wanted ${neededElement?.displayName})")
            } else if (collectedCount >= 5) {
                Log.i(TAG, "🎯 Target reached! Cannot collect more ${closestAnchor.element.displayName} spheres (${collectedCount}/5)")
            }

            return Pair(closestAnchor, shouldCount)
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
        // Initialize renderers FIRST - before composite tree
        // This ensures the same instances are used for both animation updates and rendering
        backgroundRenderer = BackgroundRenderer(render).apply {
            setUseDepthVisualization(render, false)
        }

        // Initialize object renderer for animation updates
        objectRenderer.onSurfaceCreated(render)

        // Composite Pattern: Build rendering tree structure
        rootRenderer = CompositeRenderer("AR Root", "Main AR rendering pipeline").apply {
            // Background layer (rendered first) - pass the shared backgroundRenderer instance
            add(BackgroundRendererComponent(backgroundRenderer))

            // 3D Scene layer (composite of point cloud and objects)
            val sceneRenderer = CompositeRenderer("3D Scene", "AR 3D content layer")
            sceneRenderer.add(PointCloudRendererComponent())
            sceneRenderer.add(ObjectRendererComponent(objectRenderer))  // Pass shared instance
            add(sceneRenderer)
        }

        // Initialize the composite tree
        // This will call onSurfaceCreated on all child components recursively
        rootRenderer.onSurfaceCreated(render)

        // Log the tree structure for debugging
        rootRenderer.printTree()

        // Note: pointCloudRender is not used anymore - PointCloudRendererComponent has its own instance
        // Keeping the old objectRenderer instance for animation updates only

        // Initialize VLM and VLMObjectDetector in background
        launch(Dispatchers.IO) {
            try {
                Log.i(TAG, "Loading VLM model in background...")
                vlmManager.initialize()
                isVLMLoaded = true

                // Factory Method Pattern: Create detector factory
                // Similar to: val shapeFactory: ShapeFactory = TypedShapeFactory()
                detectorFactory = ConfigurableDetectorFactory(
                    vlmManager = vlmManager,
                    onVLMClassified = { result ->
                        try {
                            // VLM classification complete - now use stored anchors
                            vlmClassificationComplete = true  // Mark VLM as complete
                            Log.i(TAG, "VLM classification complete: ${result.label}")

                            // Process pending classifications using stored anchors
                            synchronized(pendingVLMClassifications) {
                                val pending = pendingVLMClassifications.toList()
                                pendingVLMClassifications.clear()

                                if (pending.isNotEmpty()) {
                                    // Map VLM result to element
                                    val element = elementMapper.mapLabelToElement(result.label)
                                    Log.i(TAG, "VLM object '${result.label}' mapped to element: ${element.displayName}")

                                    // Create labeled anchors using stored anchor positions
                                    val newAnchors = pending.map { pendingClassification ->
                                        Log.i(TAG, "✅ Using stored anchor for '${element.displayName}' (was '${pendingClassification.originalLabel}') at pose: ${pendingClassification.anchor.pose}")
                                        ARLabeledAnchor(pendingClassification.anchor, element, pendingClassification.distance)
                                    }

                                    // Add to main anchors list
                                    synchronized(arLabeledAnchors) {
                                        arLabeledAnchors.addAll(newAnchors)
                                    }

                                    // Notify fragment about detection results on main thread
                                    fragment.view?.post {
                                        fragment.onObjectDetectionCompleted(newAnchors.size, pending.size)
                                        // Only play music if anchors were successfully created
                                        if (newAnchors.isNotEmpty()) {
                                            fragment.onElementDetected(element)
                                        }
                                    }
                                } else {
                                    // No pending classifications - still need to reset scan button
                                    Log.w(TAG, "VLM classification complete but no pending classifications found")
                                    fragment.view?.post {
                                        fragment.onObjectDetectionCompleted(0, 0)
                                    }
                                }
                            }

                            // Clear bounding box immediately after VLM classification
                            fragment.view?.post {
                                fragment.clearBoundingBoxes()
                                Log.i(TAG, "Bounding box cleared after VLM classification")
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error in VLM callback processing", e)
                            // Ensure scan button is reset even if callback processing fails
                            vlmClassificationComplete = true
                            fragment.view?.post {
                                fragment.onObjectDetectionCompleted(0, 0)
                                fragment.clearBoundingBoxes()
                            }
                            // Clear pending classifications on error
                            synchronized(pendingVLMClassifications) {
                                pendingVLMClassifications.clear()
                            }
                        }
                    }
                )

                // Factory Method Pattern: Use factory to create detector
                // Similar to: val shape1 = shapeFactory.createShape("Circle")
                vlmAnalyzer = (detectorFactory as ConfigurableDetectorFactory).createDetectorByType(
                    context = fragment.requireActivity(),
                    type = "VLM"
                ) as VLMObjectDetector

                currentAnalyzer = vlmAnalyzer

                // Enable scan button now that VLM is ready
                fragment.view?.post {
                    fragment.enableScanButton()
                }

                Log.i(TAG, "VLM model loaded successfully - switched to VLM detection")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load VLM model", e)
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

        // Background rendering is now handled by Composite Pattern (BackgroundRendererComponent)
        // Update geometry here, but actual drawing is done by rootRenderer.draw() below
        backgroundRenderer.updateDisplayGeometry(frame)
        //  backgroundRenderer.drawBackground(render)

        // Get camera and projection matrices
        val camera = frame.camera
        camera.getViewMatrix(viewMatrix, 0)
        camera.getProjectionMatrix(projectionMatrix, 0, 0.01f, 100.0f)
        Matrix.multiplyMM(viewProjectionMatrix, 0, projectionMatrix, 0, viewMatrix, 0)

        // Handle tracking failures
        if (camera.trackingState != TrackingState.TRACKING) {
            return
        }

        // Composite Pattern: Render using composite tree
        // Similar to: allMenus.print() which recursively prints all menu items
        // This single call will render background, point cloud, and objects
        val anchorsCopy = synchronized(arLabeledAnchors) {
            arLabeledAnchors.toList()
        }

        // Frustum Culling: Extract frustum planes and filter visible anchors
        frustumCuller.extractFrustumPlanes(viewProjectionMatrix)
        val visibleAnchors = performFrustumCulling(anchorsCopy)

        val renderContext = RenderContext(
            viewMatrix = viewMatrix,
            projectionMatrix = projectionMatrix,
            viewProjectionMatrix = viewProjectionMatrix,
            frame = frame,
            arLabeledAnchors = visibleAnchors,
            allAnchors = anchorsCopy
        )

        // Single unified call - treats all components uniformly
        rootRenderer.draw(render, renderContext)

        // OLD CODE BELOW - kept for reference/debugging
        // Draw point cloud
        // frame.acquirePointCloud().use { pointCloud ->
        //     pointCloudRender.drawPointCloud(render, pointCloud, viewProjectionMatrix)
        // }

        // Process pending tap (if any)
        val tap = pendingTap
        if (tap != null) {
            pendingTap = null
            val tapResult = processTap(tap.first, tap.second)

            // Notify fragment on main thread
            if (tapResult != null) {
                val (tappedAnchor, wasNeededElement) = tapResult
                fragment.view?.post {
                    fragment.onSphereCollected(tappedAnchor, wasNeededElement, collectedCount)
                }
            }
        }

        // Process object detection if scan button was pressed
        if (scanButtonWasPressed) {
            scanButtonWasPressed = false
            val cameraImage = frame.tryAcquireCameraImage()
            if (cameraImage != null) {
                // Run ML model on IO thread
                launch(Dispatchers.IO) {
                    try {
                        val cameraId = session.cameraConfig.cameraId
                        val imageRotation = displayRotationHelper.getCameraSensorToDisplayRotation(cameraId)

                        // Use VLM analyzer with custom crop ratio if available
                        objectResults = if (vlmAnalyzer != null) {
                            vlmAnalyzer.analyze(cameraImage, imageRotation, cropSizeRatio)
                        } else {
                            currentAnalyzer?.analyze(cameraImage, imageRotation)
                        }
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
            Log.i(TAG, "Detected ${objects.size} objects using ${currentAnalyzer?.javaClass?.simpleName ?: "Unknown"}")

            // Update bounding box overlay (skip if VLM classification already complete)
            if (!vlmClassificationComplete) {
                fragment.updateBoundingBoxes(objects)
            } else {
                Log.d(TAG, "Skipping bounding box update - VLM classification complete")
            }

            // VLM mode: Store anchors for later use when VLM callback completes
            Log.i(TAG, "VLM mode: Creating anchors and storing for later classification")

            val pendingClassifications = objects.mapNotNull { obj ->
                val (atX, atY) = obj.centerCoordinate
                Log.d(TAG, "Creating anchor for VLM object '${obj.label}' at coordinates ($atX, $atY)")

                val anchorResult = createAnchor(atX.toFloat(), atY.toFloat(), frame)
                if (anchorResult != null) {
                    val (anchor, distance) = anchorResult
                    Log.i(TAG, "✅ Created anchor for VLM processing: '${obj.label}' at pose: ${anchor.pose}, distance: ${distance}m")
                    PendingVLMClassification(anchor, distance, obj.label, atX.toFloat() to atY.toFloat())
                } else {
                    Log.w(TAG, "❌ Failed to create anchor for VLM object '${obj.label}' at ($atX, $atY)")
                    null
                }
            }

            // Store pending classifications
            synchronized(pendingVLMClassifications) {
                pendingVLMClassifications.addAll(pendingClassifications)
            }

            Log.i(TAG, "Stored ${pendingClassifications.size} anchors for VLM classification")
        }
    }

    /**
     * Perform frustum culling on AR anchors to improve rendering performance
     *
     * @param anchors List of all AR anchors to test
     * @return List of anchors that are visible within the camera frustum
     */
    private fun performFrustumCulling(anchors: List<ARLabeledAnchor>): List<ARLabeledAnchor> {
        if (anchors.isEmpty()) return anchors

        val visibleAnchors = anchors.filter { labeledAnchor ->
            val anchor = labeledAnchor.anchor

            // Skip anchors that aren't tracking properly
            if (anchor.trackingState != TrackingState.TRACKING) {
                return@filter false
            }

            val pose = anchor.pose
            val position = pose.translation

            // Calculate sphere radius for culling test
            // Use same scaling logic as ObjectRender for consistent culling
            val distanceScale = kotlin.math.max(1.0f, labeledAnchor.distance / 1.5f) // REFERENCE_DISTANCE
            val baseScale = 0.1f * distanceScale // OBJECT_SCALE
            val effectiveScale = kotlin.math.max(0.08f, baseScale) // MIN_VISUAL_SCALE

            // Add some margin to prevent objects from popping in/out at frustum edges
            val sphereRadius = effectiveScale + 0.05f // Add 5cm margin

            // Test if sphere is within camera frustum
            frustumCuller.isSphereInFrustum(
                position[0],
                position[1],
                position[2],
                sphereRadius
            )
        }

        // Log culling statistics periodically
        val cullRate = if (anchors.isNotEmpty()) {
            ((anchors.size - visibleAnchors.size).toFloat() / anchors.size * 100).toInt()
        } else {
            0
        }

        if (kotlin.random.Random.nextDouble() < 0.02) { // Log 2% of frames
            Log.d(TAG, "Frustum Culling: ${anchors.size} total → ${visibleAnchors.size} visible ($cullRate% culled)")
        }

        return visibleAnchors
    }

    // Temporary arrays to prevent allocations in createAnchor
    private val convertFloats = FloatArray(4)
    private val convertFloatsOut = FloatArray(4)

    /**
     * Create an anchor using (x, y) coordinates in the IMAGE_PIXELS coordinate space
     * Returns Pair(Anchor, Distance) or null if failed
     */
    private fun createAnchor(xImage: Float, yImage: Float, frame: Frame): Pair<Anchor, Float>? {
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
            val anchor = result.trackable.createAnchor(result.hitPose)
            Pair(anchor, result.distance)
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