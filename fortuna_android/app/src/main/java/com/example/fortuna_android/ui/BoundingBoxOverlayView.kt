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
        color = Color.RED
        style = Paint.Style.STROKE
        strokeWidth = 8f
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

    /**
     * Update bounding boxes to display
     */
    fun setBoundingBoxes(boxes: List<DetectedObjectResult>) {
        boundingBoxes = boxes
        Log.d(TAG, "Updated with ${boxes.size} bounding boxes")
        invalidate()  // Request redraw
    }

    /**
     * Clear all bounding boxes
     */
    fun clearBoundingBoxes() {
        boundingBoxes = emptyList()
        invalidate()
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
        // IMPORTANT: Coordinates are swapped due to rotation (height, width)
        // So we swap X and Y to match screen orientation
        val (rawY, rawX) = box.centerCoordinate

        // Scale coordinates from camera image size to view size
        // Camera: 1080x1920, View: 1080x2280
        val scaleX = viewWidth / 1080f
        val scaleY = viewHeight / 1920f

        val centerX = rawX * scaleX
        val centerY = rawY * scaleY

        // Use actual bounding box dimensions from ML Kit (also swapped)
        val boxWidth = box.height.toFloat() * scaleX
        val boxHeight = box.width.toFloat() * scaleY

        val left = centerX.toFloat() - boxWidth / 2f
        val top = centerY.toFloat() - boxHeight / 2f
        val right = left + boxWidth
        val bottom = top + boxHeight

        canvas.drawRect(left, top, right, bottom, boxPaint)

        // Draw center point for debugging
        canvas.drawCircle(centerX.toFloat(), centerY.toFloat(), 15f, centerPointPaint)

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
}
