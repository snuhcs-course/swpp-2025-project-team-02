package com.example.fortuna_android.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.fortuna_android.MainActivity
import com.example.fortuna_android.databinding.FragmentCameraBinding
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import androidx.camera.core.ImageCapture
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.core.Preview
import androidx.camera.core.CameraSelector
import androidx.camera.core.Camera
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraInfo
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.SurfaceOrientedMeteringPointFactory
import androidx.core.content.ContextCompat
import android.util.Log
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.view.animation.AccelerateDecelerateInterpolator
import java.util.concurrent.TimeUnit
import java.util.concurrent.ExecutionException
import androidx.camera.core.ImageCaptureException
import java.io.File
import java.util.Locale
import android.widget.Toast
import android.content.ContentValues
import android.provider.MediaStore
import android.os.Build
import java.io.IOException
import android.graphics.Matrix
import android.graphics.Bitmap
import android.location.LocationManager
import android.location.Location
import androidx.core.app.ActivityCompat
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.TimeZone
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.example.fortuna_android.api.RetrofitClient
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import android.graphics.BitmapFactory
import java.io.FileOutputStream

class CameraFragment : Fragment() {

    companion object {
        private const val TAG = "CameraFragment"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
    }
    private var _binding: FragmentCameraBinding? = null
    private val binding get() = _binding!!
    private var imageCapture: ImageCapture? = null

    private var camera: Camera? = null
    private var cameraControl: CameraControl? = null
    private var cameraInfo: CameraInfo? = null

    private lateinit var scaleGestureDetector: ScaleGestureDetector
    private var scaleFactor = 1f

    // Camera switching state
    private var currentCameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
    private var cameraProvider: ProcessCameraProvider? = null

    private lateinit var cameraExecutor: ExecutorService
    private var currentLocation: Location? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCameraBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupClickListeners()
        cameraExecutor = Executors.newSingleThreadExecutor()

        // Hide bottom navigation when camera fragment is shown
        if (activity is MainActivity) {
            (activity as? MainActivity)?.hideBottomNavigation()
        }

        // Start camera
        startCamera()

        // Get current location for GPS metadata
        getCurrentLocation()
    }

    private fun setupClickListeners() {
        val binding = _binding ?: return

        // Set up back button
        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }
        // Set up capture button
        binding.btnCapture.setOnClickListener { takePhoto() }
        // Set up camera switch button
        binding.btnSwitchCamera.setOnClickListener {
            switchCamera()
        }
    }

    private fun startCamera() {
        val currentContext = context ?: return
        val cameraProviderFuture = ProcessCameraProvider.getInstance(currentContext)

        cameraProviderFuture.addListener({
            try {
                // Store the camera provider for camera switching
                cameraProvider = cameraProviderFuture.get()
                bindCameraUseCases()
            } catch (e: ExecutionException) {
                Log.e(TAG, "Camera provider initialization failed", e)
            } catch (e: InterruptedException) {
                Log.e(TAG, "Camera provider initialization interrupted", e)
            }
        }, ContextCompat.getMainExecutor(currentContext))
    }

    private fun bindCameraUseCases() {
        val cameraProvider = cameraProvider ?: return
        val binding = _binding ?: return

        // Preview
        val preview = Preview.Builder()
            .build()
            .also {
                it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
            }

        // ImageCapture
        val rotation = binding.viewFinder.display?.rotation?: android.view.Surface.ROTATION_0
        imageCapture = ImageCapture.Builder()
            .setTargetRotation(rotation)
            .build()

        try {
            // Unbind use cases before rebinding
            cameraProvider.unbindAll()

            // Bind use cases to camera using current camera selector
            camera = cameraProvider.bindToLifecycle(
                this, currentCameraSelector, preview, imageCapture
            )

            // Store camera control and info
            cameraControl = camera?.cameraControl
            cameraInfo = camera?.cameraInfo

            // Set up touch focus and zoom for current camera
            setupCameraInteractions()

            Log.d(TAG, "Camera bound successfully: ${if (currentCameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) "Back" else "Front"}")

        } catch (exc: Exception) {
            Log.e(TAG, "Use case binding failed", exc)
        }
    }

    private fun switchCamera() {
        // Toggle between front and back camera
        currentCameraSelector = if (currentCameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
            CameraSelector.DEFAULT_FRONT_CAMERA
        } else {
            CameraSelector.DEFAULT_BACK_CAMERA
        }

        // Check if the selected camera is available
        cameraProvider?.let { provider ->
            if (provider.hasCamera(currentCameraSelector)) {
                // Rebind use cases with new camera selector
                bindCameraUseCases()
                Log.d(TAG, "Camera switched to: ${if (currentCameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) "Back" else "Front"}")
            } else {
                // Switch back if camera is not available
                currentCameraSelector = if (currentCameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
                    CameraSelector.DEFAULT_FRONT_CAMERA
                } else {
                    CameraSelector.DEFAULT_BACK_CAMERA
                }
                Log.w(TAG, "Selected camera not available")
            }
        }
    }

    private fun setupCameraInteractions() {
        val binding = _binding ?: return
        val currentContext = context ?: return

        // Set up scale gesture detector for zoom
        scaleGestureDetector = ScaleGestureDetector(currentContext, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                scaleFactor *= detector.scaleFactor

                // Get current zoom ratio
                val currentZoomRatio = cameraInfo?.zoomState?.value?.zoomRatio ?: 1f
                val maxZoomRatio = cameraInfo?.zoomState?.value?.maxZoomRatio ?: 1f
                val minZoomRatio = cameraInfo?.zoomState?.value?.minZoomRatio ?: 1f

                // Calculate new zoom ratio
                val newZoomRatio = (currentZoomRatio * detector.scaleFactor).coerceIn(minZoomRatio, maxZoomRatio)

                // Apply zoom
                cameraControl?.setZoomRatio(newZoomRatio)

                return true
            }
        })

        // Set up touch listener for focus and zoom
        binding.viewFinder.setOnTouchListener { view, event ->
            // Handle zoom gesture for all events
            scaleGestureDetector.onTouchEvent(event)

            return@setOnTouchListener when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    // Handle single tap focus immediately on touch down
                    if (event.pointerCount == 1) {
                        handleTapToFocus(event.x, event.y)
                        view.performClick() // For accessibility
                    }
                    true
                }
                else -> {
                    true
                }
            }
        }
    }

    private fun handleTapToFocus(x: Float, y: Float) {
        val binding = _binding ?: return

        // Show focus circle at tap location
        showFocusCircle(x, y)

        val factory = SurfaceOrientedMeteringPointFactory(
            binding.viewFinder.width.toFloat(),
            binding.viewFinder.height.toFloat()
        )

        val point = factory.createPoint(x, y)
        val action = FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF)
            .setAutoCancelDuration(3, TimeUnit.SECONDS)
            .build()

        cameraControl?.startFocusAndMetering(action)

        Log.d(TAG, "Focus requested at ($x, $y)")
    }

    private fun showFocusCircle(x: Float, y: Float) {
        val binding = _binding ?: return

        // Position the focus circle at the tap location
        val circleSize = 80 // 80dp converted to pixels
        val circleRadius = circleSize / 2

        // Center the circle on the tap point
        binding.focusCircle.x = x - circleRadius
        binding.focusCircle.y = y - circleRadius

        // Make circle visible and start animation
        binding.focusCircle.visibility = View.VISIBLE
        binding.focusCircle.alpha = 0f
        binding.focusCircle.scaleX = 1.5f
        binding.focusCircle.scaleY = 1.5f

        // Animate focus circle
        val scaleDownX = ObjectAnimator.ofFloat(binding.focusCircle, "scaleX", 1.5f, 1f)
        val scaleDownY = ObjectAnimator.ofFloat(binding.focusCircle, "scaleY", 1.5f, 1f)
        val fadeIn = ObjectAnimator.ofFloat(binding.focusCircle, "alpha", 0f, 1f)

        val animatorSet = AnimatorSet()
        animatorSet.playTogether(scaleDownX, scaleDownY, fadeIn)
        animatorSet.duration = 200
        animatorSet.interpolator = AccelerateDecelerateInterpolator()

        animatorSet.start()

        // Hide circle after 2 seconds
        binding.focusCircle.postDelayed({
            _binding?.let {
                hideFocusCircle()
            }
        }, 2000)
    }

    private fun hideFocusCircle() {
        val binding = _binding ?: return

        val fadeOut = ObjectAnimator.ofFloat(binding.focusCircle, "alpha", 1f, 0f)
        fadeOut.duration = 300
        fadeOut.addListener(object : android.animation.AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: android.animation.Animator) {
                _binding?.focusCircle?.visibility = View.GONE
            }
        })
        fadeOut.start()
    }

    private fun takePhoto() {
        // Get a stable reference to the modifiable image capture use case
        val imageCapture = imageCapture ?: return

        // Create time stamped name and MediaStore entry
        val name = SimpleDateFormat(FILENAME_FORMAT, Locale.getDefault())
            .format(System.currentTimeMillis())

        // Create output file in the app's private files directory
        val currentContext = context ?: return
        val photoFile = File(currentContext.getExternalFilesDir(null), "$name.jpeg")

        // Create output options object which contains file + metadata
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        // Set up image capture listener, which is triggered after photo has been taken
        val captureContext = context ?: return
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(captureContext),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exception: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exception.message}", exception)
                    if (isAdded) {
                        Toast.makeText(requireContext(), "Photo capture failed", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    Log.d(TAG, "Photo capture succeeded: ${output.savedUri}")
                    showPhotoPreview(photoFile)
                }
            }
        )
    }

    private fun showPhotoPreview(photoFile: File) {
        val binding = _binding ?: return

        // Load the captured photo using Glide with rotation correction
        loadPhotoWithGlide(photoFile)

        // Show the preview overlay
        binding.photoPreviewOverlay.visibility = View.VISIBLE

        // Hide back button and camera switch button during preview
        binding.btnBack.visibility = View.GONE
        binding.btnSwitchCamera.visibility = View.GONE
        binding.btnCapture.visibility = View.GONE

        // Set up approve and discard button listeners
        binding.btnApprove.setOnClickListener {
            approvePhoto(photoFile)
        }

        binding.btnDiscard.setOnClickListener {
            discardPhoto(photoFile)
        }
    }

    private fun loadPhotoWithGlide(photoFile: File) {
        val binding = _binding ?: return

        // Create Glide request options for image processing
        val requestOptions = RequestOptions()
            .diskCacheStrategy(DiskCacheStrategy.NONE) // Don't cache since it's a temporary file
            .skipMemoryCache(true) // Skip memory cache for fresh image
            .centerInside() // Fit the image properly in ImageView

        // Apply rotation correction based on camera
        val rotationAngle = when (currentCameraSelector) {
            CameraSelector.DEFAULT_BACK_CAMERA -> 0    // No rotation first - test if already correct
            CameraSelector.DEFAULT_FRONT_CAMERA -> 0   // No rotation first - test if already correct
            else -> 0
        }

        // Load image with Glide using a custom transformation
        Glide.with(this)
            .load(photoFile)
            .apply(requestOptions)
            .transform(RotateTransformation(rotationAngle))
            .into(binding.previewImage)

        Log.d(TAG, "Photo loaded with Glide, rotation: ${rotationAngle}°")
    }

    // Custom transformation class for image rotation
    private class RotateTransformation(private val rotateRotationAngle: Int) :
        com.bumptech.glide.load.resource.bitmap.BitmapTransformation() {

        override fun transform(
            pool: com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool,
            toTransform: Bitmap,
            outWidth: Int,
            outHeight: Int
        ): Bitmap {
            return if (rotateRotationAngle == 0) {
                toTransform
            } else {
                val matrix = Matrix()
                matrix.postRotate(rotateRotationAngle.toFloat())
                Bitmap.createBitmap(toTransform, 0, 0, toTransform.width, toTransform.height, matrix, true)
            }
        }

        override fun updateDiskCacheKey(messageDigest: java.security.MessageDigest) {
            messageDigest.update("rotate$rotateRotationAngle".toByteArray())
        }
    }


    private fun approvePhoto(photoFile: File) {
        // Add comprehensive metadata to photo before uploading
        addMetadataToPhoto(photoFile)

        // Upload to backend server first, then save to gallery
        uploadImageToBackend(photoFile)
    }

    private fun compressImageToWebP(jpegFile: File): File? {
        return try {
            val currentContext = context ?: return null

            // Read the JPEG file as bitmap
            val bitmap = BitmapFactory.decodeFile(jpegFile.absolutePath) ?: return null

            // Create WebP file
            val webpFileName = jpegFile.nameWithoutExtension + ".webp"
            val webpFile = File(currentContext.getExternalFilesDir(null), webpFileName)

            // Compress to WebP with quality 80 (good balance between size and quality)
            FileOutputStream(webpFile).use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY, 80, outputStream)
            }

            val originalSize = jpegFile.length() / 1024 // KB
            val compressedSize = webpFile.length() / 1024 // KB
            Log.d(TAG, "Image compressed: ${originalSize}KB (JPEG) -> ${compressedSize}KB (WebP)")

            // Recycle bitmap to free memory
            bitmap.recycle()

            webpFile
        } catch (e: Exception) {
            Log.e(TAG, "Failed to compress image to WebP", e)
            null
        }
    }

    private fun uploadImageToBackend(photoFile: File) {
        lifecycleScope.launch {
            try {
                // Show loading state (you can add a progress indicator in UI)
                Log.d(TAG, "Uploading image to backend...")

                // Convert JPEG to WebP for smaller file size
                val uploadFile = compressImageToWebP(photoFile) ?: photoFile
                val mimeType = if (uploadFile.extension == "webp") "image/webp" else "image/jpeg"

                // Prepare multipart request
                val requestFile = uploadFile.asRequestBody(mimeType.toMediaType())
                val imagePart = MultipartBody.Part.createFormData("image", uploadFile.name, requestFile)

                Log.d(TAG, "Uploading file: ${uploadFile.name} with MIME type: $mimeType")
                val chakraTypePart = "water".toRequestBody("text/plain".toMediaType()) // Example chakra type

                // Upload to backend
                val response = RetrofitClient.instance.uploadImage(imagePart, chakraTypePart)

                if (response.isSuccessful && response.body() != null) {
                    val uploadResponse = response.body()!!
                    Log.d(TAG, "Upload successful: ${uploadResponse.status}")
                    if (isAdded) {
                        Toast.makeText(requireContext(), "Image uploaded successfully!", Toast.LENGTH_SHORT).show()
                    }

                    // After successful upload, save to gallery
                    savePhotoToGallery(photoFile)
                } else {
                    Log.e(TAG, "Upload failed: ${response.code()} - ${response.message()}")
                    if (isAdded) {
                        Toast.makeText(requireContext(), "Upload failed: ${response.code()}", Toast.LENGTH_LONG).show()
                    }

                    // Still save to gallery even if upload fails
                    savePhotoToGallery(photoFile)
                }

                // Clean up WebP file if created
                if (uploadFile != photoFile && uploadFile.exists()) {
                    uploadFile.delete()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Upload error: ${e.message}", e)
                if (isAdded) {
                    Toast.makeText(requireContext(), "Upload error: ${e.message}", Toast.LENGTH_LONG).show()
                }

                // Still save to gallery even if upload fails
                savePhotoToGallery(photoFile)
            } finally {
                // Hide preview and navigate back regardless of upload result
                hidePhotoPreview()
                findNavController().popBackStack()
            }
        }
    }

    private fun getCurrentLocation() {
        val currentContext = context ?: return
        if (ActivityCompat.checkSelfPermission(
                currentContext,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Location permission not granted, GPS metadata will be skipped
            Log.d(TAG, "Location permission not granted")
            return
        }

        val locationManager = currentContext.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        if (locationManager == null) {
            Log.e(TAG, "LocationManager not available")
            return
        }

        try {
            // Get last known location from GPS provider
            val gpsLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            val networkLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

            // Choose the most recent/accurate location
            currentLocation = when {
                gpsLocation != null && networkLocation != null -> {
                    if (gpsLocation.time > networkLocation.time) gpsLocation else networkLocation
                }
                gpsLocation != null -> gpsLocation
                networkLocation != null -> networkLocation
                else -> null
            }

            if (currentLocation != null) {
                Log.d(TAG, "Location obtained: ${currentLocation!!.latitude}, ${currentLocation!!.longitude}")
            } else {
                Log.d(TAG, "No location available")
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Location permission denied", e)
        }
    }

    private fun addMetadataToPhoto(photoFile: File) {
        try {
            // Read existing EXIF data
            val exif = androidx.exifinterface.media.ExifInterface(photoFile.absolutePath)

            // Add GPS metadata if location is available
            currentLocation?.let { location ->
                exif.setGpsInfo(location)
                Log.d(TAG, "GPS metadata added: ${location.latitude}, ${location.longitude}")
            }

            // Add timestamp metadata
            val currentDateTime = Date()
            val dateTimeFormat = SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.getDefault())
            dateTimeFormat.timeZone = TimeZone.getDefault()
            val dateTimeString = dateTimeFormat.format(currentDateTime)

            exif.setAttribute(androidx.exifinterface.media.ExifInterface.TAG_DATETIME, dateTimeString)
            exif.setAttribute(androidx.exifinterface.media.ExifInterface.TAG_DATETIME_ORIGINAL, dateTimeString)
            exif.setAttribute(androidx.exifinterface.media.ExifInterface.TAG_DATETIME_DIGITIZED, dateTimeString)

            // Add device information metadata
            val deviceMake = Build.MANUFACTURER
            val deviceModel = Build.MODEL
            val androidVersion = Build.VERSION.RELEASE
            val softwareInfo = "Fortuna Camera - Android $androidVersion"

            exif.setAttribute(androidx.exifinterface.media.ExifInterface.TAG_MAKE, deviceMake)
            exif.setAttribute(androidx.exifinterface.media.ExifInterface.TAG_MODEL, deviceModel)
            exif.setAttribute(androidx.exifinterface.media.ExifInterface.TAG_SOFTWARE, softwareInfo)

            // Add camera information
            val cameraType = if (currentCameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) "Back Camera" else "Front Camera"
            exif.setAttribute(androidx.exifinterface.media.ExifInterface.TAG_USER_COMMENT, "Captured with $cameraType")

            // Save all metadata
            exif.saveAttributes()

            Log.d(TAG, "Comprehensive metadata added - Device: $deviceMake $deviceModel, Time: $dateTimeString, Camera: $cameraType")
        } catch (e: IOException) {
            Log.e(TAG, "Failed to add metadata to photo", e)
        }
    }

    private fun savePhotoToGallery(photoFile: File) {
        val name = "IMG_${SimpleDateFormat(FILENAME_FORMAT, Locale.getDefault()).format(System.currentTimeMillis())}"
        val mimeType = "image/jpeg"

        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/Fortuna")
            }
        }

        val currentContext = context ?: return
        val contentResolver = currentContext.contentResolver
        val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)

        uri?.let { imageUri ->
            try {
                contentResolver.openOutputStream(imageUri)?.use { outputStream ->
                    photoFile.inputStream().use { inputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                if (isAdded) {
                    Toast.makeText(requireContext(), "Photo saved to gallery!", Toast.LENGTH_SHORT).show()
                }
                Log.d(TAG, "Photo saved to gallery: $imageUri")

                // Clean up the temporary file
                photoFile.delete()
            } catch (e: IOException) {
                Log.e(TAG, "Failed to save photo to gallery", e)
                if (isAdded) {
                    Toast.makeText(requireContext(), "Failed to save photo", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun discardPhoto(photoFile: File) {
        // Delete the photo file
        if (photoFile.exists()) {
            photoFile.delete()
        }
        if (isAdded) {
            Toast.makeText(requireContext(), "Photo discarded", Toast.LENGTH_SHORT).show()
        }
        hidePhotoPreview()
    }

    private fun hidePhotoPreview() {
        val binding = _binding ?: return

        // Hide the preview overlay
        binding.photoPreviewOverlay.visibility = View.GONE

        // Show back button and camera switch button again
        binding.btnBack.visibility = View.VISIBLE
        binding.btnSwitchCamera.visibility = View.VISIBLE
        binding.btnCapture.visibility = View.VISIBLE

        // Clear the image to free memory
        binding.previewImage.setImageBitmap(null)
    }

    override fun onDestroyView() {
        super.onDestroyView()

        // Show bottom navigation when leaving camera fragment
        if (activity is MainActivity) {
            (activity as MainActivity).showBottomNavigation()
        }
        cameraExecutor.shutdown()
        _binding = null
    }
}