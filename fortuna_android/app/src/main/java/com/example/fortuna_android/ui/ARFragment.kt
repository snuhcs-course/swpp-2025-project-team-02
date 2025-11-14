package com.example.fortuna_android.ui

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.app.Activity
import android.os.VibrationEffect
import android.os.Vibrator
import android.content.Context
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
import androidx.activity.addCallback
import androidx.activity.OnBackPressedCallback
import androidx.navigation.NavController
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
    private var questCompletionShown: Boolean = false  // Track if completion notification was already shown

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

    // Capture sound effect player for sphere elimination
    private var capturePlayer: MediaPlayer? = null

    // Track which element sounds have been played to prevent multiple plays per scan
    private var waterSoundPlayed = false
    private var earthSoundPlayed = false
    private var fireSoundPlayed = false
    private var metalSoundPlayed = false
    private var woodSoundPlayed = false

    // Navigation destination change listener for cleanup on navigation away from AR
    private var navDestinationListener: NavController.OnDestinationChangedListener? = null

    /**
     * Safely execute navigation operations with proper error handling
     */
    private fun safeNavigation(action: () -> Unit) {
        try {
            if (isAdded) {
                action()
            } else {
                Log.w(TAG, "Fragment not added, skipping navigation operation")
            }
        } catch (e: IllegalStateException) {
            Log.w(TAG, "Navigation operation failed - NavController not available", e)
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error during navigation operation", e)
        }
    }

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

        setupNavigationHandling()
        setupARSession()
        setupClickListeners()
        setupTouchDetection()
        setupBackgroundMusic()
        setupElementSoundEffects()
        fetchTodayProgress()
    }

    private fun setupNavigationHandling() {
        // Handle system back button press
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                Log.d(TAG, "System back button pressed - using cleanup exit")
                cleanupAndExit()
            }
        })

        // Handle navigation away from AR fragment via bottom navigation
        safeNavigation {
            val navController = findNavController()
            navDestinationListener = NavController.OnDestinationChangedListener { _, destination, _ ->
                // Check if we're navigating away from ARFragment
                if (destination.id != com.example.fortuna_android.R.id.arFragment && isAdded) {
                    Log.d(TAG, "Navigation away from AR detected via destination change - performing cleanup")
                    // Perform immediate cleanup but don't navigate (navigation already in progress)
                    performImmediateCleanup()
                }
            }
            navController.addOnDestinationChangedListener(navDestinationListener!!)
            Log.d(TAG, "Navigation destination listener added")
        }
    }

    private fun setupARSession() {
        val mainActivity = activity as? MainActivity
        if (mainActivity == null) {
            Log.e(TAG, "Activity is not MainActivity")
            // Only try to navigate back if NavController is available
            safeNavigation {
                findNavController().popBackStack()
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
            if (isAdded) {
                SampleRender(this, renderer, requireContext().assets)
            }
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
            cleanupAndExit()
        }

        // Disable scan button initially - will be enabled when VLM is ready
        binding.scanButton.apply {
            isEnabled = false
            text = "수집 준비 중..."
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
                    CustomToast.show(requireContext(), "리셋")
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
                    text = "수집 중..."
                }
                false -> {
                    isEnabled = true
                    text = "수집"
                }
            }
        }
    }

    /**
     * Enable scan button when VLM is ready
     */
    fun enableScanButton() {
        val binding = _binding ?: return
        binding.scanButton.apply {
            isEnabled = true
            text = "수집"
        }
        Log.d(TAG, "Scan button enabled - VLM is ready")
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
            objectsDetected == 0 -> "원소를 찾을 수 없습니다.\n휴대폰을 움직여 주변 환경을 인식시켜주세요."
            anchorsCreated == 0 -> "원소를 찾을 수 없습니다.\n휴대폰을 움직여 주변 환경을 인식시켜주세요."
            else -> "원소 $objectsDetected 개 감지 성공!"
        }
        if (isAdded) {
            CustomToast.show(requireContext(), message)
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
        if (!isAdded) {
            Log.w(TAG, "Fragment not added, skipping background music setup")
            return
        }

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
        setupElementSound(
            { capturePlayer = it },
            com.example.fortuna_android.R.raw.capture,
            "Capture (Sphere Elimination)"
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
        if (!isAdded) {
            Log.w(TAG, "Fragment not added, skipping $soundName sound setup")
            playerSetter(null)
            return
        }

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
        stopElementSound(capturePlayer, "Capture") { capturePlayer = null }
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
            // Show surface view when resuming
            _binding?.surfaceview?.visibility = View.VISIBLE
            surfaceView?.onResume()
            startBackgroundMusic()
            Log.d(TAG, "Surface view shown, resumed and BGM started")
        } catch (e: Exception) {
            Log.w(TAG, "Error resuming surface view", e)
        }
    }

    override fun onPause(owner: LifecycleOwner) {
        Log.d(TAG, "ARFragment onPause - pausing AR session")
        try {
            // Hide surface view to prevent green screen during pause
            _binding?.surfaceview?.visibility = View.INVISIBLE
            surfaceView?.onPause()
            pauseBackgroundMusic()
            Log.d(TAG, "Surface view hidden, paused and BGM paused in onPause")
        } catch (e: Exception) {
            Log.w(TAG, "Error pausing surface view in onPause", e)
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        Log.d(TAG, "ARFragment onStop - stopping AR session")
        // Do not manually remove session helper - let MainActivity manage ARCore lifecycle
        // Just ensure our surface view is properly stopped
        try {
            // Ensure surface view is hidden and paused
            _binding?.surfaceview?.visibility = View.INVISIBLE
            surfaceView?.onPause()
            pauseBackgroundMusic()
            Log.d(TAG, "Surface view hidden, paused and BGM paused in onStop")
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

                    // Show AR tutorial after loading progress (so we know the element)
                    showARTutorialIfFirstTime()
                } else {
                    Log.w(TAG, "Failed to fetch today's progress: ${response.code()}")
                    // Show all elements if API fails
                    if (::renderer.isInitialized) {
                        renderer.setNeededElement(null)
                    }
                    // Still show tutorial even if API fails
                    showARTutorialIfFirstTime()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching today's progress", e)
                // Show all elements if error
                if (::renderer.isInitialized) {
                    renderer.setNeededElement(null)
                }
                // Still show tutorial even on error
                showARTutorialIfFirstTime()
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
            neededElementText.text = "찾아야하는 기운: ${ElementMapper.getElementHanja(element)}"
            updateCollectionProgress()

            // Set color indicator
            val color = ElementMapper.getElementColor(element)
            val background = elementColorIndicator.background as? GradientDrawable
            background?.setColor(color)
        }
    }

    /**
     * Show AR tutorial overlay if it's the first time user enters AR
     */
    private fun showARTutorialIfFirstTime() {
        if (!isAdded) {
            Log.w(TAG, "Fragment not added, skipping AR tutorial")
            return
        }

        val prefs = requireContext().getSharedPreferences("fortuna_prefs", android.content.Context.MODE_PRIVATE)
        val hasSeenARTutorial = prefs.getBoolean("has_seen_ar_tutorial", false)

        Log.d(TAG, "Checking AR tutorial: hasSeenARTutorial=$hasSeenARTutorial, neededElement=$neededElement")

        if (!hasSeenARTutorial) {
            // Show AR tutorial overlay
            Log.i(TAG, "Showing AR tutorial for first time")
            showARTutorial()

            // Mark as seen
            prefs.edit().putBoolean("has_seen_ar_tutorial", true).apply()
            Log.d(TAG, "AR tutorial marked as seen")
        } else {
            Log.d(TAG, "AR tutorial already seen, skipping")
        }
    }

    /**
     * Helper function to highlight specific text with color and bold
     */
    private fun highlightText(
        spannable: android.text.SpannableString,
        textToHighlight: String,
        color: Int,
        occurrenceIndex: Int = 0
    ) {
        val text = spannable.toString()
        var currentIndex = 0
        var foundCount = 0

        while (currentIndex < text.length) {
            val index = text.indexOf(textToHighlight, currentIndex)
            if (index == -1) break

            if (foundCount == occurrenceIndex) {
                // Apply color
                spannable.setSpan(
                    android.text.style.ForegroundColorSpan(color),
                    index,
                    index + textToHighlight.length,
                    android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )

                // Apply bold
                spannable.setSpan(
                    android.text.style.StyleSpan(android.graphics.Typeface.BOLD),
                    index,
                    index + textToHighlight.length,
                    android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                break
            }

            foundCount++
            currentIndex = index + textToHighlight.length
        }
    }

    /**
     * Show AR tutorial overlay with instructions
     */
    private fun showARTutorial() {
        val binding = _binding ?: return

        if (!isAdded) {
            Log.w(TAG, "Fragment not added, skipping AR tutorial")
            return
        }

        // Inflate tutorial overlay
        val tutorialView = LayoutInflater.from(requireContext())
            .inflate(com.example.fortuna_android.R.layout.overlay_ar_tutorial, binding.root as ViewGroup, false)

        // Add to root view (CoordinatorLayout in fragment_ar.xml)
        val rootView = binding.root as? ViewGroup
        if (rootView != null) {
            rootView.addView(tutorialView)
            Log.d(TAG, "AR tutorial overlay added to view hierarchy")
        } else {
            Log.e(TAG, "Failed to add AR tutorial: root is not a ViewGroup")
        }

        // Get element name for personalized message
        val elementName = when (neededElement) {
            ElementMapper.Element.WOOD -> "나무(木)"
            ElementMapper.Element.FIRE -> "불(火)"
            ElementMapper.Element.EARTH -> "흙(土)"
            ElementMapper.Element.METAL -> "쇠(金)"
            ElementMapper.Element.WATER -> "물(水)"
            null -> ""
            else -> ""
        }

        // Update message with element
        val messageView = tutorialView.findViewById<TextView>(com.example.fortuna_android.R.id.tvTutorialMessage)
        if (neededElement != null) {
            messageView.text = "주변에서 $elementName 와 관련된\n대상을 찾아보세요!"
        }

        // Apply styled text to instruction TextView
        val instructionView = tutorialView.findViewById<TextView>(com.example.fortuna_android.R.id.tvInstructionText)
        val instructionText = "1. 카메라를 돌려 대상을 찾고 수집 버튼을 클릭하세요\n2. 대상이 인식되면 오행 캐릭터가 등장합니다\n3. 캐릭터를 클릭하여 기운을 수집하세요!"
        val spannableString = android.text.SpannableString(instructionText)

        // Highlight words with color and bold
        val highlightColor = android.graphics.Color.parseColor("#03A9F4") // Blue color matching scan button

        // 1번: '카메라', '수집 버튼'
        highlightText(spannableString, "카메라", highlightColor)
        highlightText(spannableString, "수집 버튼", highlightColor)

        // 2번: '오행 캐릭터'
        highlightText(spannableString, "오행 캐릭터", highlightColor)

        // 3번: '클릭'
        highlightText(spannableString, "클릭", highlightColor, occurrenceIndex = 1) // Second occurrence of '클릭'

        instructionView.text = spannableString

        // Setup Got It button
        tutorialView.findViewById<View>(com.example.fortuna_android.R.id.btnGotIt).setOnClickListener {
            // Remove tutorial overlay with fade out animation
            val fadeOut = ObjectAnimator.ofFloat(tutorialView, "alpha", 1f, 0f)
            fadeOut.duration = 300
            fadeOut.addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    (binding.root as? ViewGroup)?.removeView(tutorialView)
                }
            })
            fadeOut.start()
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
     * Handle quest completion - show completion notification overlay
     * API calls are already made on each sphere collection
     */
    private fun onQuestComplete() {
        // Prevent showing completion notification multiple times
        if (questCompletionShown) {
            return
        }
        questCompletionShown = true

        Log.i(TAG, "Daily energy quest completed!")
        showQuestCompletionOverlay()
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
     * Called from renderer when a sphere is tapped
     * Handles both needed and non-needed element taps differently
     */
    fun onSphereCollected(tappedAnchor: ARRenderer.ARLabeledAnchor, wasNeededElement: Boolean, currentCount: Int) {
        Log.i(TAG, "Sphere tapped! Element: ${tappedAnchor.element.displayName}, wasNeeded: $wasNeededElement, count: $currentCount")

        // Always show celebration animation for any tap
        showCelebrationAnimation()

        // Trigger haptic feedback for sphere interaction
        triggerHapticFeedback(wasNeededElement)

        if (wasNeededElement && localCollectedCount < TARGET_COLLECTION_COUNT) {
            // Needed element: Normal collection behavior, but only if under target
            localCollectedCount++

            // Update UI with new count
            updateCollectionProgress()

            // Show success feedback
            if (isAdded) {
                CustomToast.show(requireContext(), "수집 완료! ($localCollectedCount/$TARGET_COLLECTION_COUNT)")
            }

            // Trigger API call in background
            collectElementInBackground()
        } else if (wasNeededElement && localCollectedCount >= TARGET_COLLECTION_COUNT) {
            // Needed element but target already reached
            if (isAdded) {
                CustomToast.show(requireContext(), "목표 달성 완료! (${TARGET_COLLECTION_COUNT}/${TARGET_COLLECTION_COUNT})")
            }
        } else {
            // Non-needed element: Show different feedback, no progress update
            if (isAdded) {
                val elementName = when (tappedAnchor.element) {
                    ElementMapper.Element.FIRE -> "불은"
                    ElementMapper.Element.WATER -> "물은"
                    ElementMapper.Element.WOOD -> "나무는"
                    ElementMapper.Element.METAL -> "금속은"
                    ElementMapper.Element.EARTH -> "땅은"
                    else -> tappedAnchor.element.displayName
                }
                CustomToast.show(requireContext(), "$elementName 더 이상 필요하지 않아요")
            }
            // No UI update, no count increment, no API call
        }
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
     * Play capture sound effect when sphere is eliminated
     */
    private fun playCaptureSound() {
        try {
            capturePlayer?.let { mediaPlayer ->
                if (!mediaPlayer.isPlaying) {
                    mediaPlayer.seekTo(0)
                    mediaPlayer.start()
                    Log.i(TAG, "🔊 Sphere captured! Playing capture sound effect")
                } else {
                    Log.d(TAG, "Capture sound effect already playing, skipping")
                }
            } ?: Log.w(TAG, "Capture player is null")
        } catch (e: Exception) {
            Log.e(TAG, "Error playing capture sound effect", e)
        }
    }

    /**
     * Show fireworks-style celebration animation at tap location
     */
    private fun showCelebrationAnimation() {
        // Play capture sound effect
        playCaptureSound()

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
     * Trigger haptic feedback for sphere interaction
     */
    private fun triggerHapticFeedback(wasNeededElement: Boolean) {
        try {
            val vibrator = requireContext().getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

            val vibrationEffect = if (wasNeededElement) {
                // Strong vibration for needed elements (success)
                VibrationEffect.createOneShot(1000, VibrationEffect.DEFAULT_AMPLITUDE)
            } else {
                // Light vibration for non-needed elements
                VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE)
            }
            vibrator.vibrate(vibrationEffect)

            Log.d(TAG, "Haptic feedback triggered: ${if (wasNeededElement) "strong (needed)" else "light (not needed)"}")
        } catch (e: Exception) {
            Log.w(TAG, "Error triggering haptic feedback", e)
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

    /**
     * Show quest completion overlay with congratulations message
     */
    private fun showQuestCompletionOverlay() {
        val binding = _binding ?: return

        // Use the dedicated quest completion overlay (not celebration overlay)
        binding.questCompletionOverlay.removeAllViews()
        binding.questCompletionOverlay.visibility = View.VISIBLE

        // Create a semi-transparent background
        val backgroundView = View(requireContext()).apply {
            setBackgroundColor(android.graphics.Color.parseColor("#80000000")) // Semi-transparent black
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            alpha = 0f
        }

        // Create completion notification card
        val completionCard = com.google.android.material.card.MaterialCardView(requireContext()).apply {
            cardElevation = 16f
            radius = 24f
            setCardBackgroundColor(android.graphics.Color.WHITE)
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = android.view.Gravity.CENTER
                setMargins(48, 48, 48, 48)
            }
            alpha = 0f
        }

        // Create content inside the card
        val contentLayout = android.widget.LinearLayout(requireContext()).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            gravity = android.view.Gravity.CENTER
            setPadding(64, 48, 64, 48)
        }

        // Success icon
        val successIcon = TextView(requireContext()).apply {
            text = "🎉"
            textSize = 48f
            gravity = android.view.Gravity.CENTER
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 16
            }
        }

        // Completion message
        val completionMessage = TextView(requireContext()).apply {
            text = "오늘의 기운 수집 완료!"
            textSize = 24f
            setTextColor(android.graphics.Color.parseColor("#2E7D32"))
            gravity = android.view.Gravity.CENTER
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 8
            }
        }

        // Subtitle message
        val subtitleMessage = TextView(requireContext()).apply {
            text = "계속 탐험 하거나 뒤로가기를 눌러 종료"
            textSize = 14f
            setTextColor(android.graphics.Color.parseColor("#666666"))
            gravity = android.view.Gravity.CENTER
        }

        // Assemble the views
        contentLayout.addView(successIcon)
        contentLayout.addView(completionMessage)
        contentLayout.addView(subtitleMessage)
        completionCard.addView(contentLayout)

        binding.questCompletionOverlay.addView(backgroundView)
        binding.questCompletionOverlay.addView(completionCard)

        // Animate the notification in
        val backgroundFadeIn = ObjectAnimator.ofFloat(backgroundView, "alpha", 0f, 1f)
        val cardFadeIn = ObjectAnimator.ofFloat(completionCard, "alpha", 0f, 1f)
        val cardScaleX = ObjectAnimator.ofFloat(completionCard, "scaleX", 0.7f, 1.0f)
        val cardScaleY = ObjectAnimator.ofFloat(completionCard, "scaleY", 0.7f, 1.0f)

        val animatorSet = AnimatorSet()
        animatorSet.playTogether(backgroundFadeIn, cardFadeIn, cardScaleX, cardScaleY)
        animatorSet.duration = 600
        animatorSet.interpolator = OvershootInterpolator()
        animatorSet.start()

        // Auto-hide after 4 seconds
        binding.questCompletionOverlay.postDelayed({
            hideQuestCompletionOverlay()
        }, 4000)

        Log.i(TAG, "Quest completion overlay shown")
    }

    /**
     * Hide quest completion overlay with fade out animation
     */
    private fun hideQuestCompletionOverlay() {
        val binding = _binding ?: return
        val overlay = binding.questCompletionOverlay

        if (overlay.childCount == 0) return

        val fadeOut = ObjectAnimator.ofFloat(overlay, "alpha", 1f, 0f)
        fadeOut.duration = 400
        fadeOut.addListener(object : android.animation.AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: android.animation.Animator) {
                overlay.removeAllViews()
                overlay.visibility = View.GONE
                overlay.alpha = 1f // Reset alpha for next time
            }
        })
        fadeOut.start()

        Log.i(TAG, "Quest completion overlay hidden")
    }

    /**
     * Perform immediate cleanup without navigation (used when navigation is already in progress)
     */
    private fun performImmediateCleanup() {
        Log.d(TAG, "Performing immediate cleanup for navigation")

        try {
            // Hide surface view immediately to prevent green screen
            _binding?.surfaceview?.visibility = View.INVISIBLE

            // Pause GLSurfaceView immediately
            surfaceView?.onPause()

            // Stop background music
            pauseBackgroundMusic()

            // Clear any ongoing operations
            if (::renderer.isInitialized) {
                renderer.clearAnchors()
                setScanningActive(false)
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error during immediate cleanup", e)
        }
    }

    /**
     * Clean up AR session and exit gracefully to prevent green screen glitch
     */
    private fun cleanupAndExit() {
        Log.d(TAG, "Starting cleanup and exit process")

        try {
            // Perform the same cleanup as immediate cleanup
            performImmediateCleanup()

            // Add a small delay to ensure cleanup is complete before navigation
            view?.postDelayed({
                safeNavigation {
                    findNavController().popBackStack()
                }
            }, 100)

        } catch (e: Exception) {
            Log.e(TAG, "Error during cleanup and exit", e)
            // Fallback: navigate immediately even if cleanup fails
            safeNavigation {
                findNavController().popBackStack()
            }
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()

        try {
            // Remove navigation destination listener
            navDestinationListener?.let { listener ->
                safeNavigation {
                    findNavController().removeOnDestinationChangedListener(listener)
                    Log.d(TAG, "Navigation destination listener removed")
                }
                navDestinationListener = null
            }

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