package com.example.fortuna_android.common.helpers

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import io.mockk.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for CameraPermissionHelper
 * Tests all permission-related functionality with 100% coverage
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class CameraPermissionHelperTest {

    private lateinit var activity: Activity

    @Before
    fun setUp() {
        activity = Robolectric.buildActivity(Activity::class.java).create().get()

        // Mock static methods
        mockkStatic(ContextCompat::class)
        mockkStatic(ActivityCompat::class)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    // ========== hasCameraPermission Tests ==========

    @Test
    fun `test hasCameraPermission returns true when permission is granted`() {
        // Arrange
        every {
            ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA)
        } returns PackageManager.PERMISSION_GRANTED

        // Act
        val hasPermission = CameraPermissionHelper.hasCameraPermission(activity)

        // Assert
        assertTrue("Should return true when permission is granted", hasPermission)
        verify { ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA) }
    }

    @Test
    fun `test hasCameraPermission returns false when permission is denied`() {
        // Arrange
        every {
            ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA)
        } returns PackageManager.PERMISSION_DENIED

        // Act
        val hasPermission = CameraPermissionHelper.hasCameraPermission(activity)

        // Assert
        assertFalse("Should return false when permission is denied", hasPermission)
        verify { ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA) }
    }

    @Test
    fun `test hasCameraPermission with different activity instances`() {
        // Arrange
        val activity1 = Robolectric.buildActivity(Activity::class.java).create().get()
        val activity2 = Robolectric.buildActivity(Activity::class.java).create().get()

        every {
            ContextCompat.checkSelfPermission(activity1, Manifest.permission.CAMERA)
        } returns PackageManager.PERMISSION_GRANTED

        every {
            ContextCompat.checkSelfPermission(activity2, Manifest.permission.CAMERA)
        } returns PackageManager.PERMISSION_DENIED

        // Act & Assert
        assertTrue("Activity1 should have permission",
            CameraPermissionHelper.hasCameraPermission(activity1))
        assertFalse("Activity2 should not have permission",
            CameraPermissionHelper.hasCameraPermission(activity2))
    }

    // ========== requestCameraPermission Tests ==========

    @Test
    fun `test requestCameraPermission calls ActivityCompat requestPermissions`() {
        // Arrange
        val permissionsSlot = slot<Array<String>>()
        val requestCodeSlot = slot<Int>()

        every {
            ActivityCompat.requestPermissions(
                activity,
                capture(permissionsSlot),
                capture(requestCodeSlot)
            )
        } just Runs

        // Act
        CameraPermissionHelper.requestCameraPermission(activity)

        // Assert
        verify { ActivityCompat.requestPermissions(activity, any(), any()) }
        assertEquals("Should request CAMERA permission",
            Manifest.permission.CAMERA, permissionsSlot.captured[0])
        assertEquals("Permission array should have 1 element",
            1, permissionsSlot.captured.size)
        assertEquals("Request code should be 0", 0, requestCodeSlot.captured)
    }

    @Test
    fun `test requestCameraPermission can be called multiple times`() {
        // Arrange
        every {
            ActivityCompat.requestPermissions(activity, any(), any())
        } just Runs

        // Act
        CameraPermissionHelper.requestCameraPermission(activity)
        CameraPermissionHelper.requestCameraPermission(activity)
        CameraPermissionHelper.requestCameraPermission(activity)

        // Assert
        verify(exactly = 3) {
            ActivityCompat.requestPermissions(activity, any(), any())
        }
    }

    @Test
    fun `test requestCameraPermission with different activities`() {
        // Arrange
        val activity1 = Robolectric.buildActivity(Activity::class.java).create().get()
        val activity2 = Robolectric.buildActivity(Activity::class.java).create().get()

        every {
            ActivityCompat.requestPermissions(any(), any(), any())
        } just Runs

        // Act
        CameraPermissionHelper.requestCameraPermission(activity1)
        CameraPermissionHelper.requestCameraPermission(activity2)

        // Assert
        verify { ActivityCompat.requestPermissions(activity1, any(), any()) }
        verify { ActivityCompat.requestPermissions(activity2, any(), any()) }
    }

    // ========== shouldShowRequestPermissionRationale Tests ==========

    @Test
    fun `test shouldShowRequestPermissionRationale returns true`() {
        // Arrange
        every {
            ActivityCompat.shouldShowRequestPermissionRationale(
                activity,
                Manifest.permission.CAMERA
            )
        } returns true

        // Act
        val shouldShow = CameraPermissionHelper.shouldShowRequestPermissionRationale(activity)

        // Assert
        assertTrue("Should return true when rationale should be shown", shouldShow)
        verify {
            ActivityCompat.shouldShowRequestPermissionRationale(
                activity,
                Manifest.permission.CAMERA
            )
        }
    }

    @Test
    fun `test shouldShowRequestPermissionRationale returns false`() {
        // Arrange
        every {
            ActivityCompat.shouldShowRequestPermissionRationale(
                activity,
                Manifest.permission.CAMERA
            )
        } returns false

        // Act
        val shouldShow = CameraPermissionHelper.shouldShowRequestPermissionRationale(activity)

        // Assert
        assertFalse("Should return false when rationale should not be shown", shouldShow)
        verify {
            ActivityCompat.shouldShowRequestPermissionRationale(
                activity,
                Manifest.permission.CAMERA
            )
        }
    }

    @Test
    fun `test shouldShowRequestPermissionRationale with different activities`() {
        // Arrange
        val activity1 = Robolectric.buildActivity(Activity::class.java).create().get()
        val activity2 = Robolectric.buildActivity(Activity::class.java).create().get()

        every {
            ActivityCompat.shouldShowRequestPermissionRationale(
                activity1,
                Manifest.permission.CAMERA
            )
        } returns true

        every {
            ActivityCompat.shouldShowRequestPermissionRationale(
                activity2,
                Manifest.permission.CAMERA
            )
        } returns false

        // Act & Assert
        assertTrue("Activity1 should show rationale",
            CameraPermissionHelper.shouldShowRequestPermissionRationale(activity1))
        assertFalse("Activity2 should not show rationale",
            CameraPermissionHelper.shouldShowRequestPermissionRationale(activity2))
    }

    // ========== launchPermissionSettings Tests ==========

    @Test
    fun `test launchPermissionSettings creates and starts correct intent`() {
        // Arrange
        val mockActivity = spyk(activity)
        val intentSlot = slot<Intent>()

        every { mockActivity.packageName } returns "com.example.fortuna_android"
        every { mockActivity.startActivity(capture(intentSlot)) } just Runs

        // Act
        CameraPermissionHelper.launchPermissionSettings(mockActivity)

        // Assert
        verify { mockActivity.startActivity(any()) }

        val capturedIntent = intentSlot.captured
        assertEquals("Intent action should be APPLICATION_DETAILS_SETTINGS",
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS, capturedIntent.action)

        assertNotNull("Intent data should not be null", capturedIntent.data)
        assertEquals("Intent data scheme should be 'package'",
            "package", capturedIntent.data?.scheme)
        assertEquals("Intent data should contain package name",
            "com.example.fortuna_android", capturedIntent.data?.schemeSpecificPart)
    }

    @Test
    fun `test launchPermissionSettings with different package names`() {
        // Arrange
        val mockActivity1 = spyk(Robolectric.buildActivity(Activity::class.java).create().get())
        val mockActivity2 = spyk(Robolectric.buildActivity(Activity::class.java).create().get())

        val intent1Slot = slot<Intent>()
        val intent2Slot = slot<Intent>()

        every { mockActivity1.packageName } returns "com.example.app1"
        every { mockActivity2.packageName } returns "com.example.app2"
        every { mockActivity1.startActivity(capture(intent1Slot)) } just Runs
        every { mockActivity2.startActivity(capture(intent2Slot)) } just Runs

        // Act
        CameraPermissionHelper.launchPermissionSettings(mockActivity1)
        CameraPermissionHelper.launchPermissionSettings(mockActivity2)

        // Assert
        assertEquals("First intent should contain first package name",
            "com.example.app1", intent1Slot.captured.data?.schemeSpecificPart)
        assertEquals("Second intent should contain second package name",
            "com.example.app2", intent2Slot.captured.data?.schemeSpecificPart)
    }

    @Test
    fun `test launchPermissionSettings can be called multiple times`() {
        // Arrange
        val mockActivity = spyk(activity)
        every { mockActivity.packageName } returns "com.example.fortuna_android"
        every { mockActivity.startActivity(any()) } just Runs

        // Act
        CameraPermissionHelper.launchPermissionSettings(mockActivity)
        CameraPermissionHelper.launchPermissionSettings(mockActivity)

        // Assert
        verify(exactly = 2) { mockActivity.startActivity(any()) }
    }

    @Test
    fun `test launchPermissionSettings intent has no fragment`() {
        // Arrange
        val mockActivity = spyk(activity)
        val intentSlot = slot<Intent>()

        every { mockActivity.packageName } returns "com.example.fortuna_android"
        every { mockActivity.startActivity(capture(intentSlot)) } just Runs

        // Act
        CameraPermissionHelper.launchPermissionSettings(mockActivity)

        // Assert
        val uri = intentSlot.captured.data
        assertNotNull("URI should not be null", uri)
        assertNull("URI fragment should be null", uri?.fragment)
    }

    // ========== Integration Tests ==========

    @Test
    fun `test typical permission flow - denied to granted`() {
        // Arrange - Initially denied
        every {
            ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA)
        } returns PackageManager.PERMISSION_DENIED
        every {
            ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.CAMERA)
        } returns false
        every {
            ActivityCompat.requestPermissions(activity, any(), any())
        } just Runs

        // Act & Assert - Check permission (denied)
        assertFalse("Permission should be denied initially",
            CameraPermissionHelper.hasCameraPermission(activity))

        // Act - Request permission
        CameraPermissionHelper.requestCameraPermission(activity)
        verify { ActivityCompat.requestPermissions(activity, any(), any()) }

        // Arrange - Now granted
        every {
            ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA)
        } returns PackageManager.PERMISSION_GRANTED

        // Assert - Permission now granted
        assertTrue("Permission should be granted after request",
            CameraPermissionHelper.hasCameraPermission(activity))
    }

    @Test
    fun `test typical permission flow - show rationale then settings`() {
        // Arrange
        val mockActivity = spyk(activity)
        every { mockActivity.packageName } returns "com.example.fortuna_android"
        every { mockActivity.startActivity(any()) } just Runs
        every {
            ContextCompat.checkSelfPermission(mockActivity, Manifest.permission.CAMERA)
        } returns PackageManager.PERMISSION_DENIED
        every {
            ActivityCompat.shouldShowRequestPermissionRationale(
                mockActivity,
                Manifest.permission.CAMERA
            )
        } returns true

        // Act & Assert
        assertFalse("Permission should be denied",
            CameraPermissionHelper.hasCameraPermission(mockActivity))
        assertTrue("Should show rationale",
            CameraPermissionHelper.shouldShowRequestPermissionRationale(mockActivity))

        // User denies permission permanently
        every {
            ActivityCompat.shouldShowRequestPermissionRationale(
                mockActivity,
                Manifest.permission.CAMERA
            )
        } returns false

        // Launch settings
        CameraPermissionHelper.launchPermissionSettings(mockActivity)
        verify { mockActivity.startActivity(any()) }
    }
}
