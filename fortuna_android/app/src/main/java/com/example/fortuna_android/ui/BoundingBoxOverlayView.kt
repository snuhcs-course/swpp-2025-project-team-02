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
        // DetectedObjectResult coordinates are in IMAGE_PIXELS coordinate space
        // We need to convert them to VIEW coordinates like ARCore does
        val (imageX, imageY) = box.centerCoordinate

        // The key insight: ARCore's camera view is typically displayed with these transformations:
        // 1. Camera captures in landscape orientation
        // 2. View displays in portrait orientation
        // 3. ARCore handles this with transformCoordinates2d(IMAGE_PIXELS -> VIEW)

        // Since we can't access the Frame here, we need to replicate the transformation
        // For most Android devices in portrait mode with back camera:
        // - Camera image: 1920x1080 (landscape)
        // - View: portrait orientation
        // - Standard transformation: 90-degree rotation

        // Typical camera resolution (this should ideally be dynamic)
        val cameraWidth = 1920f
        val cameraHeight = 1080f

        // Convert IMAGE_PIXELS coordinates to VIEW coordinates
        // This replicates what ARCore's transformCoordinates2d does
        // For 90-degree rotation (landscape camera -> portrait view):
        // IMAGE(x,y) -> VIEW(cameraHeight - y, x)
        val viewX = (cameraHeight - imageY.toFloat()) * (viewWidth / cameraHeight)
        val viewY = imageX.toFloat() * (viewHeight / cameraWidth)

        // Scale the bounding box dimensions
        // After 90-degree rotation, width and height are swapped
        val scaledWidth = box.height.toFloat() * (viewWidth / cameraHeight)
        val scaledHeight = box.width.toFloat() * (viewHeight / cameraWidth)

        val centerX = viewX
        val centerY = viewY
        val boxWidth = scaledWidth
        val boxHeight = scaledHeight

        val left = centerX.toFloat() - boxWidth / 2f
        val top = centerY.toFloat() - boxHeight / 2f
        val right = left + boxWidth
        val bottom = top + boxHeight

        canvas.drawRect(left, top, right, bottom, boxPaint)

        // Draw center point for debugging
        canvas.drawCircle(centerX.toFloat(), centerY.toFloat(), 15f, centerPointPaint)

        // Draw spinner if analyzing
        if (box.label == "Analyzing...") {
            drawSpinner(canvas, centerX.toFloat(), centerY.toFloat(), 40f)
        }

        val label = "${box.label} (${(box.confidence * 100).toInt()}%)"
        val textWidth = textPaint.measureText(label)
        val textHeight = textPaint.textSize

        val textX = left
        val textY = top - 10f

        canvas.drawRect(
            textX,
            textY - textHeight,
            textX + textWidth + 16f,
            textY + 8f,
            textBackgroundPaint
        )
        canvas.drawText(label, textX + 8f, textY, textPaint)

        Log.d(TAG, "Drew box $index: ${box.label} at center($centerX, $centerY) " +
                "size(${box.width}x${box.height}) " +
                "bounds[L:${left.toInt()}, T:${top.toInt()}, R:${right.toInt()}, B:${bottom.toInt()}]")
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
