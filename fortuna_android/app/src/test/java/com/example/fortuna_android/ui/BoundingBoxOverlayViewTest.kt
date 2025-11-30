package com.example.fortuna_android.ui

import android.view.View
import com.example.fortuna_android.classification.DetectedObjectResult
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Unit tests for BoundingBoxOverlayView
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class BoundingBoxOverlayViewTest {

    @Test
    fun `test BoundingBoxOverlayView instantiation`() {
        val context = RuntimeEnvironment.getApplication()
        val view = BoundingBoxOverlayView(context)

        assertNotNull(view)
    }

    @Test
    fun `test BoundingBoxOverlayView is a View`() {
        val context = RuntimeEnvironment.getApplication()
        val view = BoundingBoxOverlayView(context)

        assertTrue(view is View)
    }

    @Test
    fun `test BoundingBoxOverlayView is not clickable`() {
        val context = RuntimeEnvironment.getApplication()
        val view = BoundingBoxOverlayView(context)

        assertFalse(view.isClickable)
        assertFalse(view.isFocusable)
    }

    @Test
    fun `test setBoundingBoxes with empty list`() {
        val context = RuntimeEnvironment.getApplication()
        val view = BoundingBoxOverlayView(context)

        view.setBoundingBoxes(emptyList())
        // Should not crash
    }

    @Test
    fun `test setBoundingBoxes with single box`() {
        val context = RuntimeEnvironment.getApplication()
        val view = BoundingBoxOverlayView(context)

        val box = DetectedObjectResult(
            confidence = 0.9f,
            label = "목",
            centerCoordinate = Pair(100, 100),
            width = 50,
            height = 50
        )

        view.setBoundingBoxes(listOf(box))
        // Should not crash
    }

    @Test
    fun `test setBoundingBoxes with analyzing state`() {
        val context = RuntimeEnvironment.getApplication()
        val view = BoundingBoxOverlayView(context)

        val box = DetectedObjectResult(
            confidence = 1.0f,
            label = "분석중...",
            centerCoordinate = Pair(100, 100),
            width = 50,
            height = 50
        )

        view.setBoundingBoxes(listOf(box))
        // Should not crash
    }

    @Test
    fun `test clearBoundingBoxes`() {
        val context = RuntimeEnvironment.getApplication()
        val view = BoundingBoxOverlayView(context)

        val box = DetectedObjectResult(
            confidence = 0.9f,
            label = "화",
            centerCoordinate = Pair(100, 100),
            width = 50,
            height = 50
        )

        view.setBoundingBoxes(listOf(box))
        view.clearBoundingBoxes()
        // Should not crash
    }

    @Test
    fun `test enterSizeSelectionMode`() {
        val context = RuntimeEnvironment.getApplication()
        val view = BoundingBoxOverlayView(context)

        view.enterSizeSelectionMode()
        // Should not crash
    }

    @Test
    fun `test updatePreviewSize`() {
        val context = RuntimeEnvironment.getApplication()
        val view = BoundingBoxOverlayView(context)

        view.enterSizeSelectionMode()
        view.updatePreviewSize(0.5f)
        view.updatePreviewSize(0.3f)
        view.updatePreviewSize(1.0f)
        // Should not crash
    }

    @Test
    fun `test exitSizeSelectionMode`() {
        val context = RuntimeEnvironment.getApplication()
        val view = BoundingBoxOverlayView(context)

        view.enterSizeSelectionMode()
        view.exitSizeSelectionMode(0.6f)
        // Should not crash
    }

    @Test
    fun `test setDetectedObjectSize`() {
        val context = RuntimeEnvironment.getApplication()
        val view = BoundingBoxOverlayView(context)

        view.setDetectedObjectSize(0.5f)
        view.setDetectedObjectSize(0.3f)
        view.setDetectedObjectSize(1.0f)
        // Should not crash
    }

    @Test
    fun `test size selection flow`() {
        val context = RuntimeEnvironment.getApplication()
        val view = BoundingBoxOverlayView(context)

        // Full flow
        view.enterSizeSelectionMode()
        view.updatePreviewSize(0.4f)
        view.updatePreviewSize(0.6f)
        view.exitSizeSelectionMode(0.6f)
        view.setDetectedObjectSize(0.6f)
        // Should not crash
    }

    @Test
    fun `test setAnalyzeStateListener`() {
        val context = RuntimeEnvironment.getApplication()
        val view = BoundingBoxOverlayView(context)

        var analyzeStarted = false
        var analyzeStopped = false

        val listener = object : BoundingBoxOverlayView.AnalyzeStateListener {
            override fun onAnalyzeStarted() {
                analyzeStarted = true
            }
            override fun onAnalyzeStopped() {
                analyzeStopped = true
            }
        }

        view.setAnalyzeStateListener(listener)

        // Trigger analyze state
        val box = DetectedObjectResult(
            confidence = 1.0f,
            label = "분석중...",
            centerCoordinate = Pair(100, 100),
            width = 50,
            height = 50
        )
        view.setBoundingBoxes(listOf(box))

        assertTrue(analyzeStarted)

        // Clear to stop
        view.clearBoundingBoxes()
        assertTrue(analyzeStopped)
    }
}
