package com.example.fortuna_android.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.util.Log
import android.view.View
import androidx.core.content.ContextCompat
import com.example.fortuna_android.R
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

    // Preview mode paint (different color for size selection)
    private val previewBoxPaint = Paint().apply {
        color = Color.BLUE
        style = Paint.Style.STROKE
        strokeWidth = 6f
        isAntiAlias = true
    }

    private val previewTextPaint = Paint().apply {
        color = Color.WHITE
        textSize = 36f
        isAntiAlias = true
        style = Paint.Style.FILL
    }

    // Custom image at top-right corner of bounding box
    private val customImage: Drawable? by lazy {
        try {
            // Replace R.drawable.your_image_name with your actual drawable resource
            // For example: R.drawable.ic_corner_icon, R.drawable.scan_indicator, etc.
            ContextCompat.getDrawable(context, R.drawable.porygon)
        } catch (e: Exception) {
            Log.w(TAG, "Could not load custom image drawable", e)
            null
        }
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
        val hadAnalyzing = boundingBoxes.any { it.label == "Analyzing..." }
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

        // Calculate bounding box coordinates
        val left = centerX - squareSize / 2f
        val top = centerY - squareSize / 2f
        val right = centerX + squareSize / 2f
        val bottom = centerY + squareSize / 2f

        // Use different colors for preview vs normal mode
        val paintToUse = if (isInSizeSelectionMode) previewBoxPaint else boxPaint
        canvas.drawRect(left, top, right, bottom, paintToUse)

        // Draw center point
        canvas.drawCircle(centerX, centerY, 15f, centerPointPaint)

        // Draw spinner if analyzing
        if (box.label.contains("Analyzing")) {
            drawSpinner(canvas, centerX, centerY, squareSize / 2f - 20f)
        }

        // Different labels for preview vs normal mode
        val label = if (isInSizeSelectionMode) {
            "Size: ${(previewSizeRatio * 100).toInt()}%"
        } else {
            "${box.label} (${(box.confidence * 100).toInt()}%)"
        }

        val paintForText = if (isInSizeSelectionMode) previewTextPaint else textPaint
        val textWidth = paintForText.measureText(label)
        val textHeight = paintForText.textSize

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
        canvas.drawText(label, textX, textY, paintForText)

        // Draw custom image at top-left corner of bounding box
        drawCustomImage(canvas, left, top)

        Log.d(TAG, "Drew fixed center square: ${box.label} at center($centerX, $centerY) size=${squareSize.toInt()}")
    }

    /**
     * Draw custom image at the specified position (top-left corner of bounding box)
     */
    private fun drawCustomImage(canvas: Canvas, x: Float, y: Float) {
        customImage?.let { drawable ->
            // Define image size (you can adjust these values)
            val imageSize = 60 // 60dp size, you can make this configurable
            val imageSizePx = (imageSize * resources.displayMetrics.density).toInt()

            // Calculate bounds for top-left positioning
            // x, y represents the top-left corner of the bounding box
            val left = x.toInt()
            val top = y.toInt()
            val right = (x + imageSizePx).toInt()
            val bottom = (y + imageSizePx).toInt()

            // Set bounds and draw the image
            drawable.setBounds(left, top, right, bottom)
            drawable.draw(canvas)

            Log.d(TAG, "Drew custom image at top-left corner: ($left, $top, $right, $bottom)")
        } ?: run {
            Log.w(TAG, "Custom image drawable is null, skipping draw")
        }
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

        // Clear preview box - normal detection flow will add real boxes
        if (boundingBoxes.any { it.label == "Preview" }) {
            boundingBoxes = emptyList()
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
