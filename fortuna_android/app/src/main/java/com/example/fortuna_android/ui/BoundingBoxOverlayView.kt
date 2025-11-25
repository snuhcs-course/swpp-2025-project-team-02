package com.example.fortuna_android.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
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

    interface AnalyzeStateListener {
        fun onAnalyzeStarted()
        fun onAnalyzeStopped()
    }

    private var analyzeStateListener: AnalyzeStateListener? = null

    fun setAnalyzeStateListener(listener: AnalyzeStateListener?) {
        analyzeStateListener = listener
    }

    init {
        // Allow touch events to pass through this overlay
        isClickable = false
        isFocusable = false
    }

    private var boundingBoxes = listOf<DetectedObjectResult>()

    // Dynamic sizing state
    private var isInSizeSelectionMode = false
    private var previewSizeRatio = 0.3f // Current preview size (30%-100%)
    private var detectedSizeRatio = 0.3f // Size used for detected objects

    private val textPaint = Paint().apply {
        color = Color.WHITE
        textSize = 60f
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

    private val previewTextPaint = Paint().apply {
        color = Color.WHITE
        textSize = 60f
        isAntiAlias = true
        style = Paint.Style.FILL
    }

    // Shading overlay paint for non-bounding box areas
    private val shadingPaint = Paint().apply {
        color = Color.argb(120, 0, 0, 0) // Semi-transparent black
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    // Preview shading paint (same opacity as normal shading for consistency)
    private val previewShadingPaint = Paint().apply {
        color = Color.argb(120, 0, 0, 0) // Semi-transparent black - same as shadingPaint
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    // Highlight border for the bounding box
    private val highlightPaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 3f
        isAntiAlias = true
    }

    private val previewHighlightPaint = Paint().apply {
        color = Color.argb(255, 100, 150, 255)
        style = Paint.Style.STROKE
        strokeWidth = 4f
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
        val hadAnalyzing = boundingBoxes.any { it.label == "분석중..." || it.label == "Analyzing..." }
        val hasAnalyzing = boxes.any { it.label == "분석중..." || it.label == "Analyzing..." }

        boundingBoxes = boxes
        Log.d(TAG, "Updated with ${boxes.size} bounding boxes")

        // Start/stop spinner animation and analyze audio based on "Analyzing..." status
        if (hasAnalyzing && !hadAnalyzing) {
            post(spinnerAnimationRunnable)
            analyzeStateListener?.onAnalyzeStarted()
        } else if (!hasAnalyzing && hadAnalyzing) {
            removeCallbacks(spinnerAnimationRunnable)
            analyzeStateListener?.onAnalyzeStopped()
        }

        invalidate()  // Request redraw
    }

    /**
     * Clear all bounding boxes
     */
    fun clearBoundingBoxes() {
        val hadAnalyzing = boundingBoxes.any { it.label == "분석중..." || it.label == "Analyzing..." }
        boundingBoxes = emptyList()
        removeCallbacks(spinnerAnimationRunnable)  // Stop spinner animation

        // Stop analyze audio if it was playing
        if (hadAnalyzing) {
            analyzeStateListener?.onAnalyzeStopped()
        }

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

        // Use dynamic size ratio - either from detected objects or preview mode
        val sizeRatio = if (isInSizeSelectionMode) previewSizeRatio else detectedSizeRatio
        val squareSize = kotlin.math.min(viewWidth, viewHeight) * sizeRatio

        // Calculate bounding box coordinates (IMPORTANT: These square coordinates are used for actual VLM cropping)
        // Visual feedback will be circular, but the actual analysis area remains square
        val left = centerX - squareSize / 2f
        val top = centerY - squareSize / 2f
        val right = centerX + squareSize / 2f
        val bottom = centerY + squareSize / 2f

        // Create the shading effect by drawing the entire screen with overlay,
        // then cutting out the bounding box area
        drawShadedOverlay(canvas, left, top, right, bottom, viewWidth, viewHeight)

        // Draw subtle circular highlight border around the viewing area
        val highlightPaintToUse = if (isInSizeSelectionMode) previewHighlightPaint else highlightPaint
        val circleRadius = squareSize / 2f
        canvas.drawCircle(centerX, centerY, circleRadius, highlightPaintToUse)

        // Draw center point
        val centerPointColor = if (isInSizeSelectionMode) Color.argb(255, 100, 150, 255) else Color.WHITE
        centerPointPaint.color = centerPointColor
        canvas.drawCircle(centerX, centerY, 8f, centerPointPaint)

        // Draw spinner if analyzing
        if (box.label.contains("분석중") || box.label.contains("Analyzing")) {
            drawSpinner(canvas, centerX, centerY, squareSize / 2f - 20f)
        }

        // Different labels for preview vs normal mode
        val label = if (isInSizeSelectionMode) {
            "주변에서 원소를 찾아보세요!"
        } else {
            "${box.label} (${(box.confidence * 100).toInt()}%)"
        }

        val paintForText = if (isInSizeSelectionMode) previewTextPaint else textPaint
        val textWidth = paintForText.measureText(label)
        val textHeight = paintForText.textSize

        // Position text above the bounding box
        val textX = centerX - textWidth / 2f
        val textY = top - 20f

        canvas.drawRect(
            textX - 12f,
            textY - textHeight,
            textX + textWidth + 12f,
            textY + 12f,
            textBackgroundPaint
        )
        canvas.drawText(label, textX, textY, paintForText)

        Log.d(TAG, "Drew shaded overlay with highlighted area: ${box.label} at center($centerX, $centerY) size=${squareSize.toInt()}")
    }

    /**
     * Draw shaded overlay covering the entire screen except for the circular viewing area
     * Note: The actual crop area remains square, this is only for visual feedback
     */
    private fun drawShadedOverlay(
        canvas: Canvas,
        boundingLeft: Float,
        boundingTop: Float,
        boundingRight: Float,
        boundingBottom: Float,
        viewWidth: Float,
        viewHeight: Float
    ) {
        val shadingPaintToUse = if (isInSizeSelectionMode) previewShadingPaint else shadingPaint

        // Calculate circle parameters based on the square bounding box
        val centerX = (boundingLeft + boundingRight) / 2f
        val centerY = (boundingTop + boundingBottom) / 2f
        val squareSize = boundingRight - boundingLeft
        val circleRadius = squareSize / 2f

        // Create a path that covers the entire screen except the circular area
        val overlayPath = Path().apply {
            // Add the entire screen as a rectangle
            addRect(0f, 0f, viewWidth, viewHeight, Path.Direction.CW)
            // Subtract the circular area (this creates a "hole")
            addCircle(centerX, centerY, circleRadius, Path.Direction.CCW)
        }

        canvas.drawPath(overlayPath, shadingPaintToUse)
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

    /**
     * Enter size selection mode - shows blue preview box that can grow
     */
    fun enterSizeSelectionMode() {
        isInSizeSelectionMode = true
        previewSizeRatio = 0.3f // Start at minimum size

        // Create a fake "Preview" result to show the box
        if (boundingBoxes.isEmpty()) {
            val previewResult = DetectedObjectResult(
                confidence = 1.0f,
                label = "Preview",
                centerCoordinate = Pair(0, 0), // Will be ignored since we draw at center
                width = 0,
                height = 0
            )
            boundingBoxes = listOf(previewResult)
        }

        invalidate()
        Log.d(TAG, "Entered size selection mode")
    }

    /**
     * Update the preview size during size selection
     */
    fun updatePreviewSize(sizeRatio: Float) {
        if (isInSizeSelectionMode) {
            previewSizeRatio = sizeRatio.coerceIn(0.3f, 1.0f)
            invalidate()
        }
    }

    /**
     * Exit size selection mode and set the final detected size
     */
    fun exitSizeSelectionMode(finalSizeRatio: Float) {
        isInSizeSelectionMode = false
        detectedSizeRatio = finalSizeRatio.coerceIn(0.3f, 1.0f)

        // Don't clear preview box immediately - keep it to avoid flickering
        // It will be replaced when VLM analysis starts with "Analyzing..." boxes
        // Just update the label to indicate transition
        if (boundingBoxes.any { it.label == "Preview" }) {
            boundingBoxes = listOf(
                DetectedObjectResult(
                    confidence = 1.0f,
                    label = "Starting Analysis...",
                    centerCoordinate = Pair(0, 0),
                    width = 0,
                    height = 0
                )
            )
        }

        invalidate()
        Log.d(TAG, "Exited size selection mode with final size: ${(finalSizeRatio * 100).toInt()}%")
    }

    /**
     * Set the size ratio for detected objects during VLM analysis
     */
    fun setDetectedObjectSize(sizeRatio: Float) {
        detectedSizeRatio = sizeRatio.coerceIn(0.3f, 1.0f)
        if (!isInSizeSelectionMode) {
            invalidate()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        // Stop spinner animation when view is detached
        removeCallbacks(spinnerAnimationRunnable)
    }
}
