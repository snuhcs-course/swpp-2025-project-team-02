package com.example.fortuna_android.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.util.Log
import android.view.View
import com.example.fortuna_android.classification.DetectedObjectResult

/**
 * Transparent overlay view that draws bounding boxes on top of AR camera view
 * Uses Android Canvas API for reliable rendering without GLSurfaceView conflicts
 */
class BoundingBoxOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    companion object {
        private const val TAG = "BoundingBoxOverlayView"
    }

    init {
        // Allow touch events to pass through this overlay
        isClickable = false
        isFocusable = false
    }

    private var boundingBoxes = listOf<DetectedObjectResult>()

    private val boxPaint = Paint().apply {
        color = Color.GREEN
        style = Paint.Style.STROKE
        strokeWidth = 4f
        isAntiAlias = true
    }

    private val textPaint = Paint().apply {
        color = Color.WHITE
        textSize = 40f
        isAntiAlias = true
        style = Paint.Style.FILL
    }

    private val textBackgroundPaint = Paint().apply {
        color = Color.argb(180, 0, 0, 0)
        style = Paint.Style.FILL
    }

    private val centerPointPaint = Paint().apply {
        color = Color.GREEN
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val spinnerPaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 6f
        isAntiAlias = true
    }

    // Spinner animation state
    private var spinnerRotation = 0f
    private val spinnerAnimationRunnable = object : Runnable {
        override fun run() {
            spinnerRotation = (spinnerRotation + 10f) % 360f
            invalidate()
            postDelayed(this, 16) // ~60 FPS
        }
    }

    /**
     * Update bounding boxes to display
     */
    fun setBoundingBoxes(boxes: List<DetectedObjectResult>) {
        val hadAnalyzing = boundingBoxes.any { it.label == "Analyzing..." }
        val hasAnalyzing = boxes.any { it.label == "Analyzing..." }

        boundingBoxes = boxes
        Log.d(TAG, "Updated with ${boxes.size} bounding boxes")

        // Start/stop spinner animation based on "Analyzing..." status
        if (hasAnalyzing && !hadAnalyzing) {
            post(spinnerAnimationRunnable)
        } else if (!hasAnalyzing && hadAnalyzing) {
            removeCallbacks(spinnerAnimationRunnable)
        }

        invalidate()  // Request redraw
    }

    /**
     * Clear all bounding boxes
     */
    fun clearBoundingBoxes() {
        boundingBoxes = emptyList()
        removeCallbacks(spinnerAnimationRunnable)  // Stop spinner animation
        invalidate()
        Log.d(TAG, "Cleared all bounding boxes and stopped spinner animation")
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (boundingBoxes.isEmpty()) {
            return
        }

        val viewWidth = width.toFloat()
        val viewHeight = height.toFloat()

        Log.d(TAG, "=== View Size: ${viewWidth.toInt()} x ${viewHeight.toInt()} ===")

        for ((index, box) in boundingBoxes.withIndex()) {
            drawBoundingBox(canvas, box, viewWidth, viewHeight, index)
        }
    }

    private fun drawBoundingBox(
        canvas: Canvas,
        box: DetectedObjectResult,
        viewWidth: Float,
        viewHeight: Float,
        index: Int
    ) {
        // For center detection, always draw at screen center
        val centerX = viewWidth / 2f
        val centerY = viewHeight / 2f

        // Create a fixed square bounding box in the center (like traditional object detection)
        val squareSize = Math.min(viewWidth, viewHeight) * 0.5f // 50% of screen size

        // Calculate bounding box coordinates
        val left = centerX - squareSize / 2f
        val top = centerY - squareSize / 2f
        val right = centerX + squareSize / 2f
        val bottom = centerY + squareSize / 2f

        // Draw rectangular bounding box (like MLKit)
        canvas.drawRect(left, top, right, bottom, boxPaint)

        // Draw center point
        canvas.drawCircle(centerX, centerY, 15f, centerPointPaint)

        // Draw spinner if analyzing
        if (box.label.contains("Analyzing")) {
            drawSpinner(canvas, centerX, centerY, squareSize / 2f - 20f)
        }

        val label = "${box.label} (${(box.confidence * 100).toInt()}%)"
        val textWidth = textPaint.measureText(label)
        val textHeight = textPaint.textSize

        // Position text above the bounding box
        val textX = centerX - textWidth / 2f
        val textY = top - 10f

        canvas.drawRect(
            textX - 8f,
            textY - textHeight,
            textX + textWidth + 8f,
            textY + 8f,
            textBackgroundPaint
        )
        canvas.drawText(label, textX, textY, textPaint)

        Log.d(TAG, "Drew fixed center square: ${box.label} at center($centerX, $centerY) size=${squareSize.toInt()}")
    }

    /**
     * Draw animated spinner (circular loading indicator)
     */
    private fun drawSpinner(canvas: Canvas, cx: Float, cy: Float, radius: Float) {
        // Draw circular arc that rotates
        val sweepAngle = 270f
        val startAngle = spinnerRotation

        canvas.save()
        canvas.drawArc(
            cx - radius, cy - radius,
            cx + radius, cy + radius,
            startAngle, sweepAngle,
            false, spinnerPaint
        )
        canvas.restore()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        // Stop spinner animation when view is detached
        removeCallbacks(spinnerAnimationRunnable)
    }
}
