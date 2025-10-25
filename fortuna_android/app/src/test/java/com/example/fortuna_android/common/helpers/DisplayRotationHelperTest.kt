package com.example.fortuna_android.common.helpers

import android.content.Context
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.display.DisplayManager
import android.view.Display
import android.view.Surface
import android.view.WindowManager
import androidx.test.core.app.ApplicationProvider
import com.google.ar.core.Session
import io.mockk.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for DisplayRotationHelper
 * Tests display rotation tracking and camera sensor orientation with 100% coverage
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class DisplayRotationHelperTest {

    private lateinit var context: Context
    private lateinit var displayRotationHelper: DisplayRotationHelper
    private lateinit var mockDisplayManager: DisplayManager
    private lateinit var mockCameraManager: CameraManager
    private lateinit var mockWindowManager: WindowManager
    private lateinit var mockDisplay: Display

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()

        // Mock system services
        mockDisplayManager = mockk(relaxed = true)
        mockCameraManager = mockk(relaxed = true)
        mockWindowManager = mockk(relaxed = true)
        mockDisplay = mockk(relaxed = true)

        // Setup context to return mocked system services
        val mockContext = spyk(context)
        every { mockContext.getSystemService(Context.DISPLAY_SERVICE) } returns mockDisplayManager
        every { mockContext.getSystemService(Context.CAMERA_SERVICE) } returns mockCameraManager
        every { mockContext.getSystemService(Context.WINDOW_SERVICE) } returns mockWindowManager
        every { mockWindowManager.defaultDisplay } returns mockDisplay

        displayRotationHelper = DisplayRotationHelper(mockContext)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    // ========== Constructor Tests ==========

    @Test
    fun `test DisplayRotationHelper initializes correctly`() {
        assertNotNull("DisplayRotationHelper should be initialized", displayRotationHelper)
    }

    @Test
    fun `test DisplayRotationHelper retrieves system services`() {
        // The constructor should retrieve all necessary system services
        // This is verified through successful instantiation
        assertNotNull("Helper should be created successfully", displayRotationHelper)
    }

    // ========== onResume and onPause Tests ==========

    @Test
    fun `test onResume registers display listener`() {
        // Act
        displayRotationHelper.onResume()

        // Assert
        verify { mockDisplayManager.registerDisplayListener(displayRotationHelper, null) }
    }

    @Test
    fun `test onPause unregisters display listener`() {
        // Act
        displayRotationHelper.onPause()

        // Assert
        verify { mockDisplayManager.unregisterDisplayListener(displayRotationHelper) }
    }

    @Test
    fun `test onResume and onPause lifecycle`() {
        // Act
        displayRotationHelper.onResume()
        displayRotationHelper.onPause()
        displayRotationHelper.onResume()
        displayRotationHelper.onPause()

        // Assert
        verify(exactly = 2) { mockDisplayManager.registerDisplayListener(displayRotationHelper, null) }
        verify(exactly = 2) { mockDisplayManager.unregisterDisplayListener(displayRotationHelper) }
    }

    // ========== onSurfaceChanged Tests ==========

    @Test
    fun `test onSurfaceChanged updates viewport dimensions`() {
        // Arrange
        val width = 1920
        val height = 1080

        // Act
        displayRotationHelper.onSurfaceChanged(width, height)

        // Assert - verified through updateSessionIfNeeded behavior
        val mockSession = mockk<Session>(relaxed = true)
        every { mockDisplay.rotation } returns Surface.ROTATION_0

        displayRotationHelper.updateSessionIfNeeded(mockSession)

        verify { mockSession.setDisplayGeometry(Surface.ROTATION_0, width, height) }
    }

    @Test
    fun `test onSurfaceChanged with different dimensions`() {
        // Arrange
        val testDimensions = listOf(
            320 to 240,
            640 to 480,
            1280 to 720,
            1920 to 1080,
            2560 to 1440
        )

        val mockSession = mockk<Session>(relaxed = true)
        every { mockDisplay.rotation } returns Surface.ROTATION_0

        testDimensions.forEach { (width, height) ->
            // Act
            displayRotationHelper.onSurfaceChanged(width, height)
            displayRotationHelper.updateSessionIfNeeded(mockSession)

            // Assert
            verify { mockSession.setDisplayGeometry(Surface.ROTATION_0, width, height) }
        }
    }

    @Test
    fun `test onSurfaceChanged called multiple times`() {
        // Act
        displayRotationHelper.onSurfaceChanged(100, 200)
        displayRotationHelper.onSurfaceChanged(300, 400)
        displayRotationHelper.onSurfaceChanged(500, 600)

        // Assert
        val mockSession = mockk<Session>(relaxed = true)
        every { mockDisplay.rotation } returns Surface.ROTATION_0

        displayRotationHelper.updateSessionIfNeeded(mockSession)

        // Only the last dimensions should be used
        verify { mockSession.setDisplayGeometry(Surface.ROTATION_0, 500, 600) }
    }

    // ========== updateSessionIfNeeded Tests ==========

    @Test
    fun `test updateSessionIfNeeded updates session when viewport changed`() {
        // Arrange
        displayRotationHelper.onSurfaceChanged(1920, 1080)
        val mockSession = mockk<Session>(relaxed = true)
        every { mockDisplay.rotation } returns Surface.ROTATION_90

        // Act
        displayRotationHelper.updateSessionIfNeeded(mockSession)

        // Assert
        verify { mockSession.setDisplayGeometry(Surface.ROTATION_90, 1920, 1080) }
    }

    @Test
    fun `test updateSessionIfNeeded does not update session when viewport not changed`() {
        // Arrange
        val mockSession = mockk<Session>(relaxed = true)

        // Act - Call without onSurfaceChanged
        displayRotationHelper.updateSessionIfNeeded(mockSession)

        // Assert
        verify(exactly = 0) { mockSession.setDisplayGeometry(any(), any(), any()) }
    }

    @Test
    fun `test updateSessionIfNeeded clears viewport changed flag`() {
        // Arrange
        displayRotationHelper.onSurfaceChanged(1920, 1080)
        val mockSession = mockk<Session>(relaxed = true)
        every { mockDisplay.rotation } returns Surface.ROTATION_0

        // Act - First call should update
        displayRotationHelper.updateSessionIfNeeded(mockSession)

        // Second call should not update (flag cleared)
        displayRotationHelper.updateSessionIfNeeded(mockSession)

        // Assert - Only called once
        verify(exactly = 1) { mockSession.setDisplayGeometry(any(), any(), any()) }
    }

    @Test
    fun `test updateSessionIfNeeded with different display rotations`() {
        // Arrange
        val mockSession = mockk<Session>(relaxed = true)
        val rotations = listOf(
            Surface.ROTATION_0,
            Surface.ROTATION_90,
            Surface.ROTATION_180,
            Surface.ROTATION_270
        )

        rotations.forEach { rotation ->
            // Arrange
            displayRotationHelper.onSurfaceChanged(1920, 1080)
            every { mockDisplay.rotation } returns rotation

            // Act
            displayRotationHelper.updateSessionIfNeeded(mockSession)

            // Assert
            verify { mockSession.setDisplayGeometry(rotation, 1920, 1080) }
        }
    }

    // ========== getCameraSensorRelativeViewportAspectRatio Tests ==========

    @Test
    fun `test getCameraSensorRelativeViewportAspectRatio for 90 degree rotation`() {
        // Arrange
        displayRotationHelper.onSurfaceChanged(1920, 1080)
        val cameraId = "0"
        val mockCharacteristics = mockk<CameraCharacteristics>(relaxed = true)

        every { mockCameraManager.getCameraCharacteristics(cameraId) } returns mockCharacteristics
        every { mockCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION) } returns 90
        every { mockDisplay.rotation } returns Surface.ROTATION_0

        // Act
        val aspectRatio = displayRotationHelper.getCameraSensorRelativeViewportAspectRatio(cameraId)

        // Assert - For 90 degree rotation, aspect ratio is height/width
        assertEquals("Aspect ratio should be height/width",
            1080f / 1920f, aspectRatio, 0.001f)
    }

    @Test
    fun `test getCameraSensorRelativeViewportAspectRatio for 270 degree rotation`() {
        // Arrange
        displayRotationHelper.onSurfaceChanged(1920, 1080)
        val cameraId = "0"
        val mockCharacteristics = mockk<CameraCharacteristics>(relaxed = true)

        every { mockCameraManager.getCameraCharacteristics(cameraId) } returns mockCharacteristics
        every { mockCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION) } returns 270
        every { mockDisplay.rotation } returns Surface.ROTATION_0

        // Act
        val aspectRatio = displayRotationHelper.getCameraSensorRelativeViewportAspectRatio(cameraId)

        // Assert - For 270 degree rotation, aspect ratio is height/width
        assertEquals("Aspect ratio should be height/width",
            1080f / 1920f, aspectRatio, 0.001f)
    }

    @Test
    fun `test getCameraSensorRelativeViewportAspectRatio for 0 degree rotation`() {
        // Arrange
        displayRotationHelper.onSurfaceChanged(1920, 1080)
        val cameraId = "0"
        val mockCharacteristics = mockk<CameraCharacteristics>(relaxed = true)

        every { mockCameraManager.getCameraCharacteristics(cameraId) } returns mockCharacteristics
        every { mockCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION) } returns 0
        every { mockDisplay.rotation } returns Surface.ROTATION_0

        // Act
        val aspectRatio = displayRotationHelper.getCameraSensorRelativeViewportAspectRatio(cameraId)

        // Assert - For 0 degree rotation, aspect ratio is width/height
        assertEquals("Aspect ratio should be width/height",
            1920f / 1080f, aspectRatio, 0.001f)
    }

    @Test
    fun `test getCameraSensorRelativeViewportAspectRatio for 180 degree rotation`() {
        // Arrange
        displayRotationHelper.onSurfaceChanged(1920, 1080)
        val cameraId = "0"
        val mockCharacteristics = mockk<CameraCharacteristics>(relaxed = true)

        every { mockCameraManager.getCameraCharacteristics(cameraId) } returns mockCharacteristics
        every { mockCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION) } returns 180
        every { mockDisplay.rotation } returns Surface.ROTATION_0

        // Act
        val aspectRatio = displayRotationHelper.getCameraSensorRelativeViewportAspectRatio(cameraId)

        // Assert - For 180 degree rotation, aspect ratio is width/height
        assertEquals("Aspect ratio should be width/height",
            1920f / 1080f, aspectRatio, 0.001f)
    }

    // ========== getCameraSensorToDisplayRotation Tests ==========

    @Test
    fun `test getCameraSensorToDisplayRotation calculates correct rotation`() {
        // Arrange
        val cameraId = "0"
        val mockCharacteristics = mockk<CameraCharacteristics>(relaxed = true)

        every { mockCameraManager.getCameraCharacteristics(cameraId) } returns mockCharacteristics
        every { mockCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION) } returns 90
        every { mockDisplay.rotation } returns Surface.ROTATION_0

        // Act
        val rotation = displayRotationHelper.getCameraSensorToDisplayRotation(cameraId)

        // Assert - (90 - 0 + 360) % 360 = 90
        assertEquals("Rotation should be 90 degrees", 90, rotation)
    }

    @Test
    fun `test getCameraSensorToDisplayRotation with different sensor orientations`() {
        // Arrange
        val cameraId = "0"
        val mockCharacteristics = mockk<CameraCharacteristics>(relaxed = true)

        val testCases = listOf(
            Triple(0, Surface.ROTATION_0, 0),      // (0 - 0 + 360) % 360 = 0
            Triple(90, Surface.ROTATION_0, 90),    // (90 - 0 + 360) % 360 = 90
            Triple(180, Surface.ROTATION_0, 180),  // (180 - 0 + 360) % 360 = 180
            Triple(270, Surface.ROTATION_0, 270),  // (270 - 0 + 360) % 360 = 270
            Triple(90, Surface.ROTATION_90, 0),    // (90 - 90 + 360) % 360 = 0
            Triple(90, Surface.ROTATION_180, 270), // (90 - 180 + 360) % 360 = 270
            Triple(90, Surface.ROTATION_270, 180)  // (90 - 270 + 360) % 360 = 180
        )

        every { mockCameraManager.getCameraCharacteristics(cameraId) } returns mockCharacteristics

        testCases.forEach { (sensorOrientation, displayRotation, expectedRotation) ->
            // Arrange
            every { mockCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION) } returns sensorOrientation
            every { mockDisplay.rotation } returns displayRotation

            // Act
            val rotation = displayRotationHelper.getCameraSensorToDisplayRotation(cameraId)

            // Assert
            assertEquals("Rotation should be $expectedRotation for sensor=$sensorOrientation, display=$displayRotation",
                expectedRotation, rotation)
        }
    }

    @Test
    fun `test getCameraSensorToDisplayRotation throws exception on camera access error`() {
        // Arrange
        val cameraId = "0"
        every { mockCameraManager.getCameraCharacteristics(cameraId) } throws CameraAccessException(
            CameraAccessException.CAMERA_ERROR)

        // Act & Assert
        val exception = assertThrows(RuntimeException::class.java) {
            displayRotationHelper.getCameraSensorToDisplayRotation(cameraId)
        }

        assertEquals("Exception message should mention display orientation",
            "Unable to determine display orientation", exception.message)
        assertNotNull("Exception should have a cause", exception.cause)
        assertTrue("Cause should be CameraAccessException",
            exception.cause is CameraAccessException)
    }

    // ========== DisplayListener Implementation Tests ==========

    @Test
    fun `test onDisplayAdded does nothing`() {
        // Act - Should not throw exception
        displayRotationHelper.onDisplayAdded(0)
        displayRotationHelper.onDisplayAdded(1)

        // Assert - No exception thrown
    }

    @Test
    fun `test onDisplayRemoved does nothing`() {
        // Act - Should not throw exception
        displayRotationHelper.onDisplayRemoved(0)
        displayRotationHelper.onDisplayRemoved(1)

        // Assert - No exception thrown
    }

    @Test
    fun `test onDisplayChanged sets viewport changed flag`() {
        // Arrange
        val mockSession = mockk<Session>(relaxed = true)
        every { mockDisplay.rotation } returns Surface.ROTATION_0

        // Act - Trigger display change
        displayRotationHelper.onDisplayChanged(0)

        // Since viewport dimensions aren't set, this should still not trigger update
        displayRotationHelper.updateSessionIfNeeded(mockSession)

        // But after setting dimensions, it should work
        displayRotationHelper.onSurfaceChanged(1920, 1080)
        displayRotationHelper.onDisplayChanged(0)
        displayRotationHelper.updateSessionIfNeeded(mockSession)

        // Assert
        verify { mockSession.setDisplayGeometry(any(), any(), any()) }
    }

    @Test
    fun `test onDisplayChanged with multiple display IDs`() {
        // Act - Should handle different display IDs
        displayRotationHelper.onDisplayChanged(0)
        displayRotationHelper.onDisplayChanged(1)
        displayRotationHelper.onDisplayChanged(2)

        // Assert - No exception thrown
    }

    // ========== Integration Tests ==========

    @Test
    fun `test complete lifecycle flow`() {
        // Arrange
        val mockSession = mockk<Session>(relaxed = true)
        every { mockDisplay.rotation } returns Surface.ROTATION_0

        // Act - Simulate complete lifecycle
        displayRotationHelper.onResume()
        displayRotationHelper.onSurfaceChanged(1920, 1080)
        displayRotationHelper.updateSessionIfNeeded(mockSession)
        displayRotationHelper.onPause()

        // Assert
        verify { mockDisplayManager.registerDisplayListener(displayRotationHelper, null) }
        verify { mockSession.setDisplayGeometry(Surface.ROTATION_0, 1920, 1080) }
        verify { mockDisplayManager.unregisterDisplayListener(displayRotationHelper) }
    }

    @Test
    fun `test display change triggers update`() {
        // Arrange
        displayRotationHelper.onSurfaceChanged(1920, 1080)
        val mockSession = mockk<Session>(relaxed = true)
        every { mockDisplay.rotation } returns Surface.ROTATION_0

        // Act - First update
        displayRotationHelper.updateSessionIfNeeded(mockSession)

        // Trigger display change
        displayRotationHelper.onDisplayChanged(0)

        // Second update should now work due to display change
        displayRotationHelper.updateSessionIfNeeded(mockSession)

        // Assert - Should be called twice
        verify(exactly = 2) { mockSession.setDisplayGeometry(any(), any(), any()) }
    }

    @Test
    fun `test different camera IDs`() {
        // Arrange
        displayRotationHelper.onSurfaceChanged(1920, 1080)

        val cameraIds = listOf("0", "1", "front", "back")
        val mockCharacteristics = mockk<CameraCharacteristics>(relaxed = true)

        every { mockCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION) } returns 90
        every { mockDisplay.rotation } returns Surface.ROTATION_0

        cameraIds.forEach { cameraId ->
            // Arrange
            every { mockCameraManager.getCameraCharacteristics(cameraId) } returns mockCharacteristics

            // Act
            val aspectRatio = displayRotationHelper.getCameraSensorRelativeViewportAspectRatio(cameraId)

            // Assert
            assertTrue("Aspect ratio should be positive", aspectRatio > 0)
        }
    }
}
