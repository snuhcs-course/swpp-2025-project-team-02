package com.example.fortuna_android.ui

import android.opengl.GLSurfaceView
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.fragment.findNavController
import com.example.fortuna_android.MainActivity
import com.example.fortuna_android.databinding.FragmentArBinding
import com.example.fortuna_android.util.CustomToast
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

class ARFragment : Fragment(), DefaultLifecycleObserver {

    companion object {
        private const val TAG = "ARFragment"
    }

    private var _binding: FragmentArBinding? = null
    private val binding get() = _binding!!

    private lateinit var renderer: ARRenderer
    private var surfaceView: GLSurfaceView? = null

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

    override fun onDestroyView() {
        super.onDestroyView()


        _binding = null
    }
}