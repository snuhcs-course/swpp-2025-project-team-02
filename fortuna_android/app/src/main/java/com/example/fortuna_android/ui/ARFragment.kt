package com.example.fortuna_android.ui

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.app.Activity
import android.graphics.drawable.GradientDrawable
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.util.Log
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.view.GestureDetectorCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.fortuna_android.MainActivity
import com.example.fortuna_android.api.RetrofitClient
import com.example.fortuna_android.classification.ElementMapper
import com.example.fortuna_android.databinding.FragmentArBinding
import com.example.fortuna_android.util.CustomToast
import com.example.fortuna_android.util.PendingCollectionManager
import com.example.fortuna_android.common.samplerender.SampleRender
import com.google.ar.core.CameraConfig
import com.google.ar.core.CameraConfigFilter
import com.google.ar.core.Config
import com.google.ar.core.exceptions.CameraNotAvailableException
import com.google.ar.core.exceptions.SessionPausedException
import com.google.ar.core.exceptions.UnavailableApkTooOldException
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException
import com.google.ar.core.exceptions.UnavailableSdkTooOldException
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException
import android.hardware.camera2.CameraAccessException
import kotlinx.coroutines.launch

class ARFragment(
    private val sessionManagerFactory: ((Activity) -> ARSessionManager)? = null
) : Fragment(), DefaultLifecycleObserver {

    companion object {
        private const val TAG = "ARFragment"
        private const val TARGET_COLLECTION_COUNT = 5
    }

    private var _binding: FragmentArBinding? = null
    private val binding get() = _binding!!

    private lateinit var renderer: ARRenderer
    private var surfaceView: GLSurfaceView? = null
    private lateinit var gestureDetector: GestureDetectorCompat
    private var neededElement: ElementMapper.Element? = null
    private var localCollectedCount: Int = 0  // Track local collection count during AR session

    // ARCore session manager - managed by this fragment
    private lateinit var sessionManager: ARSessionManager

    // VLM state
    private val vlmResponseBuilder = StringBuilder()

    // Track last tap coordinates for celebration animation
    private var lastTapX: Float = 0f
    private var lastTapY: Float = 0f

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentArBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        setupARSession()
        setupClickListeners()
        setupTouchDetection()
        fetchNeededElement()
    }

    private fun setupARSession() {
        val mainActivity = activity as? MainActivity
        if (mainActivity == null) {
            Log.e(TAG, "Activity is not MainActivity")
            // Only try to navigate back if NavController is available
            try {
                findNavController().popBackStack()
            } catch (e: IllegalStateException) {
                Log.w(TAG, "NavController not available, likely in test environment")
            }
            return
        }

        setupARCoreSession(mainActivity)
    }

    private fun setupARCoreSession(mainActivity: MainActivity) {
        // Create ARCore session manager for this fragment using factory or default implementation
        sessionManager = sessionManagerFactory?.invoke(requireActivity())
            ?: ARCoreSessionLifecycleHelper(requireActivity())

        // Set session manager on MainActivity for renderer access (maintain compatibility)
        if (sessionManager is ARCoreSessionLifecycleHelper) {
            mainActivity.arCoreSessionHelper = sessionManager as ARCoreSessionLifecycleHelper
        }

        // Configure ARCore session
        sessionManager.exceptionCallback = { exception ->
            val message = when (exception) {
                is UnavailableArcoreNotInstalledException,
                is UnavailableUserDeclinedInstallationException -> "Please install ARCore"
                is UnavailableApkTooOldException -> "Please update ARCore"
                is UnavailableSdkTooOldException -> "Please update this app"
                is UnavailableDeviceNotCompatibleException -> "This device does not support AR"
                is CameraNotAvailableException -> "Camera not available. Try restarting the app."
                is CameraAccessException -> {
                    when (exception.reason) {
                        CameraAccessException.CAMERA_DISCONNECTED -> "Camera disconnected. Please restart the app."
                        CameraAccessException.CAMERA_ERROR -> "Camera error occurred. Please restart the app."
                        CameraAccessException.CAMERA_IN_USE -> "Camera is in use by another app."
                        CameraAccessException.MAX_CAMERAS_IN_USE -> "Too many cameras in use."
                        CameraAccessException.CAMERA_DISABLED -> "Camera is disabled by device policy."
                        else -> "Camera access error: ${exception.message}"
                    }
                }
                else -> "Failed to create AR session: $exception"
            }
            Log.e(TAG, message, exception)
            if (isAdded) {
                CustomToast.show(requireContext(), message)
            }
        }

        sessionManager.beforeSessionResume = { session ->
            Log.d(TAG, "ARCore session configuration starting...")

            try {
                val config = session.config.apply {
                    // Enable autofocus for better object detection
                    focusMode = Config.FocusMode.AUTO
                    Log.d(TAG, "✅ ARCore focus mode set to AUTO")

                    if (session.isDepthModeSupported(Config.DepthMode.AUTOMATIC)) {
                        depthMode = Config.DepthMode.AUTOMATIC
                        Log.d(TAG, "✅ ARCore depth mode set to AUTOMATIC")
                    } else if (session.isDepthModeSupported(Config.DepthMode.RAW_DEPTH_ONLY)) {
                        depthMode = Config.DepthMode.RAW_DEPTH_ONLY
                        Log.d(TAG, "✅ ARCore depth mode set to RAW_DEPTH_ONLY")
                    } else {
                        depthMode = Config.DepthMode.DISABLED
                        Log.w(TAG, "⚠️ ARCore depth mode not supported, using DISABLED")
                    }
                }

                session.configure(config)
                Log.d(TAG, "✅ ARCore session configured successfully")

                // Configure camera for best quality
                val filter = CameraConfigFilter(session)
                    .setFacingDirection(CameraConfig.FacingDirection.BACK)
                val configs = session.getSupportedCameraConfigs(filter)
                val sort = compareByDescending<CameraConfig> { it.imageSize.width }
                    .thenByDescending { it.imageSize.height }
                session.cameraConfig = configs.sortedWith(sort)[0]
                Log.d(TAG, "✅ Camera config set: ${session.cameraConfig.imageSize}")

            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to configure ARCore session", e)
            }
        }

        // Initialize renderer
        renderer = ARRenderer(this)
        lifecycle.addObserver(renderer)

        // Setup GL Surface View
        surfaceView = binding.surfaceview.apply {
            SampleRender(this, renderer, requireContext().assets)
        }

        lifecycle.addObserver(this)

        // Add ARCore session manager to fragment lifecycle for proper autofocus control
        lifecycle.addObserver(sessionManager)

        // Log AR session setup
        Log.d(TAG, "ARFragment setup completed, ARCore session manager: $sessionManager")
    }

    private fun setupClickListeners() {
        val binding = _binding ?: return

        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.scanButton.setOnClickListener {
            if (::renderer.isInitialized) {
                renderer.startObjectDetection()
                setScanningActive(true)
            }
        }

        binding.clearButton.setOnClickListener {
            if (::renderer.isInitialized) {
                renderer.clearAnchors()
                clearVLMDescription()
                if (isAdded) {
                    CustomToast.show(requireContext(), "Anchors cleared")
                }
            }
        }
    }

    /**
     * Toggles the scan button state based on scanning status
     */
    fun setScanningActive(active: Boolean) {
        val binding = _binding ?: return

        binding.scanButton.apply {
            when (active) {
                true -> {
                    isEnabled = false
                    text = "Scanning..."
                }
                false -> {
                    isEnabled = true
                    text = "Scan"
                }
            }
        }
    }

    /**
     * Called when object detection completes
     */
    fun onObjectDetectionCompleted(anchorsCreated: Int, objectsDetected: Int) {
        setScanningActive(false)
        val message = when {
            objectsDetected == 0 -> "No objects detected"
            anchorsCreated == 0 -> "Objects detected but couldn't create anchors"
            else -> "Detected $objectsDetected objects, created $anchorsCreated anchors"
        }
        if (isAdded) {
           // CustomToast.show(requireContext(), message)
        }
    }

    /**
     * Start VLM analysis - called by renderer when beginning VLM processing
     */
    fun onVLMAnalysisStarted() {
        view?.post {
            val binding = _binding ?: return@post
            vlmResponseBuilder.clear()
            binding.vlmDescriptionOverlay.text = "Analyzing scene..."
            binding.vlmDescriptionOverlay.visibility = View.VISIBLE
        }
    }

    /**
     * Update VLM description with streaming token
     * Called from background thread by ARRenderer
     */
    fun updateVLMDescription(token: String) {
        view?.post {
            val binding = _binding ?: return@post
            vlmResponseBuilder.append(token)
            binding.vlmDescriptionOverlay.text = vlmResponseBuilder.toString()
            binding.vlmDescriptionOverlay.visibility = View.VISIBLE
        }
    }

    /**
     * Clear VLM description overlay
     */
    fun clearVLMDescription() {
        view?.post {
            val binding = _binding ?: return@post
            vlmResponseBuilder.clear()
            binding.vlmDescriptionOverlay.visibility = View.GONE
        }
    }

    /**
     * Called when VLM analysis completes
     */
    fun onVLMAnalysisCompleted() {
        // VLM result stays visible - user can clear with Clear button
        Log.d(TAG, "VLM analysis completed")
    }

    override fun onResume(owner: LifecycleOwner) {
        try {
            surfaceView?.onResume()
            Log.d(TAG, "Surface view resumed")
        } catch (e: Exception) {
            Log.w(TAG, "Error resuming surface view", e)
        }
    }

    override fun onPause(owner: LifecycleOwner) {
        Log.d(TAG, "ARFragment onPause - pausing AR session")
        try {
            surfaceView?.onPause()
            Log.d(TAG, "Surface view paused in onPause")
        } catch (e: Exception) {
            Log.w(TAG, "Error pausing surface view in onPause", e)
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        Log.d(TAG, "ARFragment onStop - stopping AR session")
        // Do not manually remove session helper - let MainActivity manage ARCore lifecycle
        // Just ensure our surface view is properly stopped
        try {
            surfaceView?.onPause()
            Log.d(TAG, "Surface view paused in onStop")
        } catch (e: Exception) {
            Log.w(TAG, "Error pausing surface view in onStop", e)
        }
    }

    /**
     * Fetch the needed element from API and display it
     */
    private fun fetchNeededElement() {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.getNeededElement()
                if (response.isSuccessful && response.body() != null) {
                    val koreanElement = response.body()!!.data.neededElement
                    neededElement = ElementMapper.fromKorean(koreanElement)

                    // Update UI
                    neededElement?.let { element ->
                        displayNeededElement(element)

                        // Set in renderer to filter spheres
                        if (::renderer.isInitialized) {
                            renderer.setNeededElement(element)
                        }
                    }

                    Log.i(TAG, "Needed element loaded: ${neededElement?.displayName}")
                } else {
                    Log.w(TAG, "Failed to fetch needed element: ${response.code()}")
                    // Show all elements if API fails
                    if (::renderer.isInitialized) {
                        renderer.setNeededElement(null)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching needed element", e)
                // Show all elements if error
                if (::renderer.isInitialized) {
                    renderer.setNeededElement(null)
                }
            }
        }
    }

    /**
     * Display the needed element in the UI banner
     */
    private fun displayNeededElement(element: ElementMapper.Element) {
        val binding = _binding ?: return

        binding.apply {
            neededElementBanner.visibility = View.VISIBLE
            neededElementText.text = "Collect: ${ElementMapper.getElementDisplayText(element)}"
            updateCollectionProgress()

            // Set color indicator
            val color = ElementMapper.getElementColor(element)
            val background = elementColorIndicator.background as? GradientDrawable
            background?.setColor(color)
        }
    }

    /**
     * Update collection progress display based on local count
     */
    private fun updateCollectionProgress() {
        val binding = _binding ?: return

        binding.collectionProgressText.text = "$localCollectedCount / $TARGET_COLLECTION_COUNT collected"

        // Check if quest is complete
        if (localCollectedCount >= TARGET_COLLECTION_COUNT) {
            onQuestComplete()
        }
    }

    /**
     * Handle quest completion - save to SharedPreferences then close AR
     * The actual POST request will be handled by HomeFragment after AR session closes
     */
    private fun onQuestComplete() {
        if (isAdded) {
            CustomToast.show(requireContext(), "Quest Complete! Energy Harmonized!")
        }
        Log.i(TAG, "Daily energy quest completed!")

        // Save pending collection to SharedPreferences
        neededElement?.let { element ->
            if (element != ElementMapper.Element.OTHERS) {
                val englishElement = ElementMapper.toEnglish(element)
                if (isAdded) {
                    PendingCollectionManager.savePendingCollection(requireContext(), englishElement, 1)
                    Log.i(TAG, "Pending collection saved to SharedPreferences: $englishElement")
                }
            } else {
                Log.w(TAG, "OTHERS element not supported by backend, skipping")
            }
        }

        // Close AR after a short delay to let user see the completion message
        view?.postDelayed({
            if (isAdded) {
                Log.i(TAG, "Closing AR view after quest completion")
                findNavController().popBackStack()
            }
        }, 1500) // 1.5 second delay (reduced from 2s since we're not waiting for API)
    }

    /**
     * Setup touch detection for sphere collection
     */
    private fun setupTouchDetection() {
        val currentContext = context ?: return
        val binding = _binding ?: return

        gestureDetector = GestureDetectorCompat(currentContext, object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapUp(e: MotionEvent): Boolean {
                Log.d(TAG, "Touch detected at (${e.x}, ${e.y})")

                // Save tap coordinates for celebration animation
                lastTapX = e.x
                lastTapY = e.y

                // Queue tap to be processed on render thread
                if (::renderer.isInitialized) {
                    renderer.handleTap(e.x, e.y)
                }

                return true
            }
        })

        // Attach touch listener to surface view
        binding.surfaceview.setOnTouchListener { view, event ->
            if (::gestureDetector.isInitialized) {
                gestureDetector.onTouchEvent(event)
            }
            true
        }

        Log.i(TAG, "Touch detection setup complete")
    }

    /**
     * Called from renderer when a sphere is successfully collected
     * Only updates local count - API call happens when quest is complete (5/5)
     */
    fun onSphereCollected(count: Int) {
        Log.i(TAG, "Sphere collected! Local count: $count")

        // Increment local collection count
        localCollectedCount++

        // Update UI with new count
        updateCollectionProgress()

        // Show celebration animation
        showCelebrationAnimation()

        // Show feedback to user
        if (isAdded) {
            CustomToast.show(requireContext(), "Collected! ($localCollectedCount/$TARGET_COLLECTION_COUNT)")
        }
    }

    /**
     * Show fireworks-style celebration animation at tap location
     */
    private fun showCelebrationAnimation() {
        view?.post {
            _binding?.let { binding ->
                // Clear any existing particles
                binding.celebrationOverlay.removeAllViews()
                binding.celebrationOverlay.visibility = View.VISIBLE

                // Create firework particles
                val particleCount = 12
                val colors = listOf(
                    "#FFD700", // Gold
                    "#FF6B6B", // Red
                    "#4ECDC4", // Cyan
                    "#45B7D1", // Blue
                    "#FFA07A", // Light Salmon
                    "#98D8C8", // Mint
                    "#F7DC6F", // Yellow
                    "#BB8FCE"  // Purple
                )
                val emojis = listOf("✨", "⭐", "💫", "🌟", "✨", "⭐")

                for (i in 0 until particleCount) {
                    val particle = TextView(requireContext()).apply {
                        text = emojis[i % emojis.size]
                        textSize = 32f
                        alpha = 1f
                        layoutParams = FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.WRAP_CONTENT,
                            FrameLayout.LayoutParams.WRAP_CONTENT
                        )

                        // Position at tap location
                        x = lastTapX - 20 // Center the emoji (half of approximate size)
                        y = lastTapY - 20
                    }

                    binding.celebrationOverlay.addView(particle)

                    // Calculate angle for this particle (evenly distributed in circle)
                    val angle = (360f / particleCount) * i
                    val radians = Math.toRadians(angle.toDouble())

                    // Distance particles will travel
                    val distance = 200f + (Math.random() * 100).toFloat()

                    // Calculate end position
                    val endX = lastTapX + (Math.cos(radians) * distance).toFloat() - 20
                    val endY = lastTapY + (Math.sin(radians) * distance).toFloat() - 20

                    // Animate particle
                    val moveX = ObjectAnimator.ofFloat(particle, "x", lastTapX - 20, endX)
                    val moveY = ObjectAnimator.ofFloat(particle, "y", lastTapY - 20, endY)
                    val fadeOut = ObjectAnimator.ofFloat(particle, "alpha", 1f, 0f)
                    val scale = ObjectAnimator.ofFloat(particle, "scaleX", 1f, 0.3f)
                    val scaleY = ObjectAnimator.ofFloat(particle, "scaleY", 1f, 0.3f)
                    val rotate = ObjectAnimator.ofFloat(particle, "rotation", 0f, 360f * 2)

                    val animatorSet = AnimatorSet()
                    animatorSet.playTogether(moveX, moveY, fadeOut, scale, scaleY, rotate)
                    animatorSet.duration = 800 + (Math.random() * 200).toLong() // Vary duration slightly
                    animatorSet.interpolator = AccelerateDecelerateInterpolator()
                    animatorSet.start()
                }

                // Hide celebration overlay after animation completes
                binding.celebrationOverlay.postDelayed({
                    hideCelebrationAnimation()
                }, 1200)
            }
        }
    }

    /**
     * Hide celebration animation with fade out
     */
    private fun hideCelebrationAnimation() {
        _binding?.let { binding ->
            val fadeOut = ObjectAnimator.ofFloat(binding.celebrationOverlay, "alpha", 1f, 0f)
            fadeOut.duration = 300
            fadeOut.addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    _binding?.celebrationOverlay?.visibility = View.GONE
                    _binding?.celebrationOverlay?.alpha = 1f // Reset alpha for next time
                }
            })
            fadeOut.start()
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()

        try {
            // Remove this fragment's lifecycle observer
            lifecycle.removeObserver(this)

            // Remove renderer lifecycle observer if initialized
            if (::renderer.isInitialized) {
                lifecycle.removeObserver(renderer)
            }

            // Remove ARCore session manager lifecycle observer if initialized
            if (::sessionManager.isInitialized) {
                lifecycle.removeObserver(sessionManager)
            }

            // Clean up surface view reference
            surfaceView = null

            Log.d(TAG, "ARFragment cleanup completed")
        } catch (e: Exception) {
            Log.w(TAG, "Error during ARFragment cleanup", e)
        }

        _binding = null
    }
}