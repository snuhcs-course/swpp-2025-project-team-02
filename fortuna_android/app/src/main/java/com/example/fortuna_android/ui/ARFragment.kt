package com.example.fortuna_android.ui

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.app.Activity
import android.graphics.drawable.GradientDrawable
import android.media.MediaPlayer
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
import com.example.fortuna_android.classification.DetectedObjectResult
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

    // Track last tap coordinates for celebration animation
    private var lastTapX: Float = 0f
    private var lastTapY: Float = 0f

    // Background music player
    private var bgmPlayer: MediaPlayer? = null

    // Element detection sound effect players
    private var wartortlePlayer: MediaPlayer? = null  // Water
    private var pikachuPlayer: MediaPlayer? = null    // Earth
    private var charmanderPlayer: MediaPlayer? = null // Fire
    private var registeelPlayer: MediaPlayer? = null  // Metal
    private var sudowoodoPlayer: MediaPlayer? = null  // Wood

    // Track which element sounds have been played to prevent multiple plays per scan
    private var waterSoundPlayed = false
    private var earthSoundPlayed = false
    private var fireSoundPlayed = false
    private var metalSoundPlayed = false
    private var woodSoundPlayed = false

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
        setupBackgroundMusic()
        setupElementSoundEffects()
        fetchTodayProgress()
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
                // Reset all element sound flags for new scan
                resetElementSoundFlags()
                renderer.startObjectDetection()
                setScanningActive(true)
            }
        }

        binding.clearButton.setOnClickListener {
            if (::renderer.isInitialized) {
                renderer.clearAnchors()
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
     * Update bounding box overlay with detected objects
     */
    fun updateBoundingBoxes(objects: List<DetectedObjectResult>) {
        view?.post {
            val binding = _binding ?: return@post
            binding.boundingBoxOverlay.setBoundingBoxes(objects)
        }
    }

    /**
     * Clear bounding box overlay
     */
    fun clearBoundingBoxes() {
        view?.post {
            val binding = _binding ?: return@post
            binding.boundingBoxOverlay.clearBoundingBoxes()
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

    // VLM description overlay removed - using bounding box labels instead

    /**
     * Reset all element sound flags for new scan
     */
    private fun resetElementSoundFlags() {
        waterSoundPlayed = false
        earthSoundPlayed = false
        fireSoundPlayed = false
        metalSoundPlayed = false
        woodSoundPlayed = false
        Log.d(TAG, "Element sound flags reset for new scan")
    }

    /**
     * Called when an element is detected during scanning
     * Plays corresponding sound effect once per scan per element
     */
    fun onElementDetected(element: ElementMapper.Element) {
        when (element) {
            ElementMapper.Element.WATER -> {
                if (!waterSoundPlayed) {
                    waterSoundPlayed = true
                    playElementSound(wartortlePlayer, "💧 Water", "wartortle")
                }
            }
            ElementMapper.Element.EARTH -> {
                if (!earthSoundPlayed) {
                    earthSoundPlayed = true
                    playElementSound(pikachuPlayer, "🌍 Earth", "pikachu")
                }
            }
            ElementMapper.Element.FIRE -> {
                if (!fireSoundPlayed) {
                    fireSoundPlayed = true
                    playElementSound(charmanderPlayer, "🔥 Fire", "charmander")
                }
            }
            ElementMapper.Element.METAL -> {
                if (!metalSoundPlayed) {
                    metalSoundPlayed = true
                    playElementSound(registeelPlayer, "⚙️ Metal", "registeel")
                }
            }
            ElementMapper.Element.WOOD -> {
                if (!woodSoundPlayed) {
                    woodSoundPlayed = true
                    playElementSound(sudowoodoPlayer, "🌳 Wood", "sudowoodo")
                }
            }
            else -> {
                Log.d(TAG, "Element ${element.displayName} does not have a sound effect")
            }
        }
    }

    /**
     * Generic method to play element sound effects
     */
    private fun playElementSound(player: MediaPlayer?, elementName: String, soundName: String) {
        try {
            player?.let { mediaPlayer ->
                if (!mediaPlayer.isPlaying) {
                    mediaPlayer.seekTo(0)
                    mediaPlayer.start()
                    Log.i(TAG, "🔊 $elementName element detected! Playing $soundName sound effect")
                } else {
                    Log.d(TAG, "$soundName sound effect already playing, skipping")
                }
            } ?: Log.w(TAG, "$soundName player is null")
        } catch (e: Exception) {
            Log.e(TAG, "Error playing $soundName sound effect", e)
        }
    }

    /**
     * Setup background music for AR experience
     */
    private fun setupBackgroundMusic() {
        try {
            bgmPlayer = MediaPlayer().apply {
                setOnPreparedListener { mediaPlayer ->
                    mediaPlayer.isLooping = true
                    mediaPlayer.setVolume(0.3f, 0.3f) // Set moderate volume
                    Log.d(TAG, "Background music prepared and ready")
                }
                setOnErrorListener { _, what, extra ->
                    Log.e(TAG, "MediaPlayer error: what=$what, extra=$extra")
                    true // Return true to indicate we've handled the error
                }
                setOnCompletionListener {
                    Log.d(TAG, "Background music completed")
                }

                // Load the BGM file from raw resources
                val afd = requireContext().resources.openRawResourceFd(com.example.fortuna_android.R.raw.ar_background_music)
                setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                afd.close()
                prepareAsync() // Prepare asynchronously to avoid blocking UI
            }
            Log.d(TAG, "Background music setup initiated")
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up background music", e)
            bgmPlayer = null
        }
    }

    /**
     * Start background music
     */
    private fun startBackgroundMusic() {
        try {
            bgmPlayer?.let { player ->
                if (!player.isPlaying) {
                    player.start()
                    Log.d(TAG, "Background music started")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting background music", e)
        }
    }

    /**
     * Pause background music
     */
    private fun pauseBackgroundMusic() {
        try {
            bgmPlayer?.let { player ->
                if (player.isPlaying) {
                    player.pause()
                    Log.d(TAG, "Background music paused")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error pausing background music", e)
        }
    }

    /**
     * Stop and release background music
     */
    private fun stopBackgroundMusic() {
        try {
            bgmPlayer?.let { player ->
                if (player.isPlaying) {
                    player.stop()
                }
                player.reset()
                player.release()
                bgmPlayer = null
                Log.d(TAG, "Background music stopped and released")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping background music", e)
            bgmPlayer = null
        }
    }

    /**
     * Setup all element sound effects
     */
    private fun setupElementSoundEffects() {
        setupElementSound(
            { wartortlePlayer = it },
            com.example.fortuna_android.R.raw.wartortle,
            "Wartortle (Water)"
        )
        setupElementSound(
            { pikachuPlayer = it },
            com.example.fortuna_android.R.raw.pikachu,
            "Pikachu (Earth)"
        )
        setupElementSound(
            { charmanderPlayer = it },
            com.example.fortuna_android.R.raw.charmander,
            "Charmander (Fire)"
        )
        setupElementSound(
            { registeelPlayer = it },
            com.example.fortuna_android.R.raw.registeel,
            "Registeel (Metal)"
        )
        setupElementSound(
            { sudowoodoPlayer = it },
            com.example.fortuna_android.R.raw.sudowoodo,
            "Sudowoodo (Wood)"
        )
    }

    /**
     * Generic setup method for element sound effects
     */
    private fun setupElementSound(
        playerSetter: (MediaPlayer?) -> Unit,
        resourceId: Int,
        soundName: String
    ) {
        try {
            val player = MediaPlayer().apply {
                setOnPreparedListener { mediaPlayer ->
                    mediaPlayer.setVolume(0.8f, 0.8f) // Higher volume for sound effect
                    Log.d(TAG, "$soundName sound effect prepared and ready")
                }
                setOnErrorListener { _, what, extra ->
                    Log.e(TAG, "$soundName MediaPlayer error: what=$what, extra=$extra")
                    true // Return true to indicate we've handled the error
                }
                setOnCompletionListener {
                    Log.d(TAG, "$soundName sound effect completed")
                }

                // Load the sound file from raw resources
                val afd = requireContext().resources.openRawResourceFd(resourceId)
                setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                afd.close()
                prepareAsync() // Prepare asynchronously to avoid blocking UI
            }
            playerSetter(player)
            Log.d(TAG, "$soundName sound effect setup initiated")
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up $soundName sound effect", e)
            playerSetter(null)
        }
    }

    /**
     * Stop and release all element sound effects
     */
    private fun stopAllElementSoundEffects() {
        stopElementSound(wartortlePlayer, "Wartortle") { wartortlePlayer = null }
        stopElementSound(pikachuPlayer, "Pikachu") { pikachuPlayer = null }
        stopElementSound(charmanderPlayer, "Charmander") { charmanderPlayer = null }
        stopElementSound(registeelPlayer, "Registeel") { registeelPlayer = null }
        stopElementSound(sudowoodoPlayer, "Sudowoodo") { sudowoodoPlayer = null }
    }

    /**
     * Generic method to stop and release element sound effect
     */
    private fun stopElementSound(player: MediaPlayer?, soundName: String, nullifier: () -> Unit) {
        try {
            player?.let { mediaPlayer ->
                if (mediaPlayer.isPlaying) {
                    mediaPlayer.stop()
                }
                mediaPlayer.reset()
                mediaPlayer.release()
                nullifier()
                Log.d(TAG, "$soundName sound effect stopped and released")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping $soundName sound effect", e)
            nullifier()
        }
    }

    override fun onResume(owner: LifecycleOwner) {
        try {
            surfaceView?.onResume()
            startBackgroundMusic()
            Log.d(TAG, "Surface view resumed and BGM started")
        } catch (e: Exception) {
            Log.w(TAG, "Error resuming surface view", e)
        }
    }

    override fun onPause(owner: LifecycleOwner) {
        Log.d(TAG, "ARFragment onPause - pausing AR session")
        try {
            surfaceView?.onPause()
            pauseBackgroundMusic()
            Log.d(TAG, "Surface view paused and BGM paused in onPause")
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
            pauseBackgroundMusic()
            Log.d(TAG, "Surface view paused and BGM paused in onStop")
        } catch (e: Exception) {
            Log.w(TAG, "Error pausing surface view in onStop", e)
        }
    }

    /**
     * Fetch today's progress from API and display it
     * Loads current collection count and needed element
     */
    private fun fetchTodayProgress() {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.getTodayProgress()
                if (response.isSuccessful && response.body() != null) {
                    val progressData = response.body()!!.data

                    // Set needed element
                    val koreanElement = progressData.neededElement
                    neededElement = ElementMapper.fromKorean(koreanElement)

                    // Initialize local count with server count
                    localCollectedCount = progressData.currentCount

                    // Update UI
                    neededElement?.let { element ->
                        displayNeededElement(element)

                        // Set in renderer to filter spheres
                        if (::renderer.isInitialized) {
                            renderer.setNeededElement(element)
                        }
                    }

                    Log.i(TAG, "Today's progress loaded: ${progressData.currentCount}/${progressData.targetCount} - ${neededElement?.displayName}")
                } else {
                    Log.w(TAG, "Failed to fetch today's progress: ${response.code()}")
                    // Show all elements if API fails
                    if (::renderer.isInitialized) {
                        renderer.setNeededElement(null)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching today's progress", e)
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
            neededElementText.text = "오늘의 기운 ${ElementMapper.getElementHanja(element)}"
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

        binding.collectionProgressText.text = "$localCollectedCount / $TARGET_COLLECTION_COUNT 수집"

        // Check if quest is complete
        if (localCollectedCount >= TARGET_COLLECTION_COUNT) {
            onQuestComplete()
        }
    }

    /**
     * Handle quest completion - close AR and return to home
     * API calls are already made on each sphere collection
     */
    private fun onQuestComplete() {
        if (isAdded) {
            CustomToast.show(requireContext(), "오늘의 기운 수집 완료!")
        }
        Log.i(TAG, "Daily energy quest completed!")

        // Close AR after a short delay to let user see the completion message
        view?.postDelayed({
            if (isAdded) {
                Log.i(TAG, "Closing AR view after quest completion")
                findNavController().popBackStack()
            }
        }, 1500)
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
     * Immediately updates UI and triggers API call in background
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
            CustomToast.show(requireContext(), "수집 완료! ($localCollectedCount/$TARGET_COLLECTION_COUNT)")
        }

        // Trigger API call in background
        collectElementInBackground()
    }

    /**
     * Send collectElement API request in background (non-blocking)
     */
    private fun collectElementInBackground() {
        neededElement?.let { element ->
            if (element != ElementMapper.Element.OTHERS) {
                val elementEnglish = ElementMapper.toEnglish(element)

                lifecycleScope.launch {
                    try {
                        val request = com.example.fortuna_android.api.CollectElementRequest(chakraType = elementEnglish)
                        val response = RetrofitClient.instance.collectElement(request)

                        if (response.isSuccessful && response.body() != null) {
                            Log.i(TAG, "✅ Element collected via API: $elementEnglish")
                        } else {
                            Log.w(TAG, "⚠️ API call failed but user already saw success: ${response.code()}")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "⚠️ API call error but user already saw success", e)
                    }
                }
            } else {
                Log.w(TAG, "OTHERS element not supported by backend, skipping API call")
            }
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

            // Stop and release background music
            stopBackgroundMusic()

            // Stop and release all element sound effects
            stopAllElementSoundEffects()

            Log.d(TAG, "ARFragment cleanup completed")
        } catch (e: Exception) {
            Log.w(TAG, "Error during ARFragment cleanup", e)
        }

        _binding = null
    }
}