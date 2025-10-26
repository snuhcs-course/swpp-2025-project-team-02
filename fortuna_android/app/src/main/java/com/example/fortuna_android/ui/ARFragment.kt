package com.example.fortuna_android.ui

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
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
import com.google.ar.core.exceptions.UnavailableApkTooOldException
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException
import com.google.ar.core.exceptions.UnavailableSdkTooOldException
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException
import kotlinx.coroutines.launch

class ARFragment : Fragment(), DefaultLifecycleObserver {

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
    private var serverCollectedCount: Int = 0  // Track server-based collection count
    private var localCollectedCount: Int = 0  // Track local collection count during AR session

    // VLM state
    private val vlmResponseBuilder = StringBuilder()

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
        fetchCurrentCollectionCount()  // Fetch server-based count on load
    }

    private fun setupARSession() {
        val mainActivity = activity as? MainActivity
        if (mainActivity == null) {
            Log.e(TAG, "Activity is not MainActivity")
            findNavController().popBackStack()
            return
        }

        setupARCoreSession(mainActivity)
    }

    private fun setupARCoreSession(mainActivity: MainActivity) {

        // Configure ARCore session
        mainActivity.arCoreSessionHelper.exceptionCallback = { exception ->
            val message = when (exception) {
                is UnavailableArcoreNotInstalledException,
                is UnavailableUserDeclinedInstallationException -> "Please install ARCore"
                is UnavailableApkTooOldException -> "Please update ARCore"
                is UnavailableSdkTooOldException -> "Please update this app"
                is UnavailableDeviceNotCompatibleException -> "This device does not support AR"
                is CameraNotAvailableException -> "Camera not available. Try restarting the app."
                else -> "Failed to create AR session: $exception"
            }
            Log.e(TAG, message, exception)
            CustomToast.show(requireContext(), message)
        }

        mainActivity.arCoreSessionHelper.beforeSessionResume = { session ->
            Log.d(TAG, "ARCore session configuration starting...")

            try {
                val config = session.config.apply {
                    // Enable autofocus for better object detection
                    focusMode = Config.FocusMode.AUTO
                    Log.d(TAG, "✅ ARCore focus mode set to AUTO")

                    if (session.isDepthModeSupported(Config.DepthMode.AUTOMATIC)) {
                        depthMode = Config.DepthMode.AUTOMATIC
                        Log.d(TAG, "✅ ARCore depth mode set to AUTOMATIC")
                    } else {
                        Log.w(TAG, "⚠️ ARCore depth mode AUTOMATIC not supported")
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

        // Add ARCore session helper to lifecycle - this will trigger session resume
        lifecycle.addObserver(mainActivity.arCoreSessionHelper)

        // Log AR session setup
        Log.d(TAG, "ARFragment setup completed, ARCore session helper: ${mainActivity.arCoreSessionHelper}")
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.scanButton.setOnClickListener {
            renderer.startObjectDetection()
            setScanningActive(true)
        }

        binding.clearButton.setOnClickListener {
            renderer.clearAnchors()
            clearVLMDescription()
            CustomToast.show(requireContext(), "Anchors cleared")
        }
    }

    /**
     * Toggles the scan button state based on scanning status
     */
    fun setScanningActive(active: Boolean) {
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
        CustomToast.show(requireContext(), message)
    }

    /**
     * Start VLM analysis - called by renderer when beginning VLM processing
     */
    fun onVLMAnalysisStarted() {
        view?.post {
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
        surfaceView?.onResume()
    }

    override fun onPause(owner: LifecycleOwner) {
        Log.d(TAG, "ARFragment onPause - pausing AR session")
        surfaceView?.onPause()
    }

    override fun onStop(owner: LifecycleOwner) {
        Log.d(TAG, "ARFragment onStop - stopping AR session")
        // Ensure ARCore session is properly paused to release camera resources
        val mainActivity = activity as? MainActivity
        mainActivity?.arCoreSessionHelper?.let { helper ->
            try {
                // Remove the session helper from lifecycle to force cleanup
                lifecycle.removeObserver(helper)
                Log.d(TAG, "ARCore session helper removed from lifecycle")
            } catch (e: Exception) {
                Log.w(TAG, "Error removing ARCore session helper", e)
            }
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
                    displayNeededElement(neededElement!!)

                    // Set in renderer to filter spheres
                    renderer.setNeededElement(neededElement)

                    Log.i(TAG, "Needed element loaded: ${neededElement?.displayName}")
                } else {
                    Log.w(TAG, "Failed to fetch needed element: ${response.code()}")
                    // Show all elements if API fails
                    renderer.setNeededElement(null)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching needed element", e)
                // Show all elements if error
                renderer.setNeededElement(null)
            }
        }
    }

    /**
     * Fetch current collection count from server
     */
    private fun fetchCurrentCollectionCount() {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.getUserProfile()
                if (response.isSuccessful && response.body() != null) {
                    val profile = response.body()!!
                    val collectionStatus = profile.collectionStatus

                    // Get count for the needed element
                    neededElement?.let { element ->
                        val count = when (element) {
                            ElementMapper.Element.WOOD -> collectionStatus?.wood ?: 0
                            ElementMapper.Element.FIRE -> collectionStatus?.fire ?: 0
                            ElementMapper.Element.EARTH -> collectionStatus?.earth ?: 0
                            ElementMapper.Element.METAL -> collectionStatus?.metal ?: 0
                            ElementMapper.Element.WATER -> collectionStatus?.water ?: 0
                            ElementMapper.Element.OTHERS -> 0
                        }
                        serverCollectedCount = count
                        localCollectedCount = count  // Initialize local count from server
                        updateCollectionProgress()
                        Log.d(TAG, "Server collection count loaded: $count for ${element.displayName}")
                    }
                } else {
                    Log.w(TAG, "Failed to fetch collection count: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching collection count", e)
            }
        }
    }

    /**
     * Display the needed element in the UI banner
     */
    private fun displayNeededElement(element: ElementMapper.Element) {
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
        CustomToast.show(requireContext(), "Quest Complete! Energy Harmonized!")
        Log.i(TAG, "Daily energy quest completed!")

        // Save pending collection to SharedPreferences
        neededElement?.let { element ->
            if (element != ElementMapper.Element.OTHERS) {
                val englishElement = ElementMapper.toEnglish(element)
                PendingCollectionManager.savePendingCollection(requireContext(), englishElement, 1)
                Log.i(TAG, "Pending collection saved to SharedPreferences: $englishElement")
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
        gestureDetector = GestureDetectorCompat(requireContext(), object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapUp(e: MotionEvent): Boolean {
                Log.d(TAG, "Touch detected at (${e.x}, ${e.y})")

                // Queue tap to be processed on render thread
                renderer.handleTap(e.x, e.y)

                return true
            }
        })

        // Attach touch listener to surface view
        binding.surfaceview.setOnTouchListener { view, event ->
            gestureDetector.onTouchEvent(event)
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
        CustomToast.show(requireContext(), "Collected! ($localCollectedCount/$TARGET_COLLECTION_COUNT)")
    }

    /**
     * Show celebration animation when element is collected
     */
    private fun showCelebrationAnimation() {
        view?.post {
            _binding?.let { binding ->
                // Make overlay visible
                binding.celebrationOverlay.visibility = View.VISIBLE

                // Reset initial state
                binding.celebrationIcon.apply {
                    alpha = 0f
                    scaleX = 0f
                    scaleY = 0f
                    rotation = 0f
                }

                // Animate celebration icon - pop and rotate
                val iconScaleX = ObjectAnimator.ofFloat(binding.celebrationIcon, "scaleX", 0f, 1.2f, 1f)
                val iconScaleY = ObjectAnimator.ofFloat(binding.celebrationIcon, "scaleY", 0f, 1.2f, 1f)
                val iconFadeIn = ObjectAnimator.ofFloat(binding.celebrationIcon, "alpha", 0f, 1f)
                val iconRotate = ObjectAnimator.ofFloat(binding.celebrationIcon, "rotation", 0f, 360f)

                val iconAnimatorSet = AnimatorSet()
                iconAnimatorSet.playTogether(iconScaleX, iconScaleY, iconFadeIn, iconRotate)
                iconAnimatorSet.duration = 500
                iconAnimatorSet.interpolator = OvershootInterpolator()

                // Start animation
                iconAnimatorSet.start()

                // Hide celebration after 1 second
                binding.celebrationOverlay.postDelayed({
                    hideCelebrationAnimation()
                }, 1000)
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


        _binding = null
    }
}