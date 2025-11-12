package com.example.fortuna_android.ui

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.min

/**
 * Custom overlay view for drawing bounding boxes on camera preview
 * Supports different visual states (pending, processing, completed, failed)
 * and handles touch events for user interaction
 */
class BoundingBoxOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // Detected objects to draw
    private var detectedObjects: List<DetectedObject> = emptyList()

    // Click listener for detected objects
    private var onObjectClickListener: ((DetectedObject) -> Unit)? = null

    // Paint objects for drawing
    private val boxPaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
        isAntiAlias = true
    }

    private val fillPaint = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val textPaint = Paint().apply {
        color = Color.WHITE
        textSize = 32f
        isAntiAlias = true
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    private val backgroundPaint = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    // Animation state for spinner
    private var spinnerRotation = 0f
    private val spinnerAnimator: Runnable = object : Runnable {
        override fun run() {
            spinnerRotation = (spinnerRotation + 10f) % 360f
            invalidate()
            postDelayed(this, 50) // ~20 FPS animation
        }
    }

    // Color scheme for different states
    companion object {
        private const val COLOR_PENDING = 0xFFFFFF00.toInt()      // Yellow
        private const val COLOR_PROCESSING = 0xFF00BFFF.toInt()   // Deep Sky Blue
        private const val COLOR_COMPLETED = 0xFF00FF00.toInt()    // Green
        private const val COLOR_FAILED = 0xFFFF0000.toInt()       // Red
        private const val COLOR_BACKGROUND = 0xCC000000.toInt()   // Semi-transparent black
    }

    /**
     * Update the list of detected objects to display
     */
    fun setDetectedObjects(objects: List<DetectedObject>) {
        detectedObjects = objects
        invalidate() // Trigger redraw

        // Start/stop spinner animation based on processing state
        val hasProcessing = objects.any { it.isProcessing() }
        if (hasProcessing) {
            startSpinnerAnimation()
        } else {
            stopSpinnerAnimation()
        }
    }

    /**
     * Set click listener for detected objects
     */
    fun setOnObjectClickListener(listener: (DetectedObject) -> Unit) {
        onObjectClickListener = listener
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw each detected object
        for (obj in detectedObjects) {
            drawDetectedObject(canvas, obj)
        }
    }

    /**
     * Draw a single detected object with appropriate styling
     */
    private fun drawDetectedObject(canvas: Canvas, obj: DetectedObject) {
        val box = obj.boundingBox

        // Determine color based on state
        val color = when (obj.vlmState) {
            VLMProcessingState.PENDING -> COLOR_PENDING
            VLMProcessingState.PROCESSING -> COLOR_PROCESSING
            VLMProcessingState.COMPLETED -> COLOR_COMPLETED
            VLMProcessingState.FAILED -> COLOR_FAILED
        }

        // Draw bounding box
        boxPaint.color = color
        canvas.drawRect(box, boxPaint)

        // Draw spinner for processing state
        if (obj.isProcessing()) {
            drawSpinner(canvas, box, color)
        }

        // Draw text label
        val displayText = obj.getDisplayText()
        drawTextLabel(canvas, box, displayText, color)

        // Draw clickable indicator for completed/failed states
        if (obj.isClickable()) {
            drawClickableIndicator(canvas, box)
        }
    }

    /**
     * Draw animated spinner for processing state
     */
    private fun drawSpinner(canvas: Canvas, box: RectF, color: Int) {
        val centerX = box.centerX()
        val centerY = box.centerY()
        val radius = min(box.width(), box.height()) / 8f

        fillPaint.color = color
        fillPaint.style = Paint.Style.STROKE
        fillPaint.strokeWidth = 6f

        val rect = RectF(
            centerX - radius,
            centerY - radius,
            centerX + radius,
            centerY + radius
        )

        // Draw arc that rotates
        canvas.drawArc(rect, spinnerRotation, 270f, false, fillPaint)

        fillPaint.style = Paint.Style.FILL // Reset
    }

    /**
     * Draw text label with background
     */
    private fun drawTextLabel(canvas: Canvas, box: RectF, text: String, color: Int) {
        // Measure text
        val textBounds = Rect()
        textPaint.getTextBounds(text, 0, text.length, textBounds)

        // Position label at top of bounding box
        val labelX = box.left + 8f
        val labelY = box.top - 8f

        // Draw background rectangle
        backgroundPaint.color = COLOR_BACKGROUND
        val bgRect = RectF(
            labelX - 4f,
            labelY - textBounds.height() - 4f,
            labelX + textBounds.width() + 8f,
            labelY + 4f
        )
        canvas.drawRect(bgRect, backgroundPaint)

        // Draw text
        textPaint.color = color
        canvas.drawText(text, labelX, labelY, textPaint)
    }

    /**
     * Draw indicator showing object is clickable
     */
    private fun drawClickableIndicator(canvas: Canvas, box: RectF) {
        // Draw small circle in bottom-right corner
        val indicatorRadius = 12f
        val indicatorX = box.right - 16f
        val indicatorY = box.bottom - 16f

        fillPaint.color = Color.WHITE
        canvas.drawCircle(indicatorX, indicatorY, indicatorRadius, fillPaint)

        fillPaint.color = COLOR_COMPLETED
        canvas.drawCircle(indicatorX, indicatorY, indicatorRadius - 3f, fillPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            val touchX = event.x
            val touchY = event.y

            // Find clicked object (check in reverse order to prioritize top objects)
            for (obj in detectedObjects.reversed()) {
                if (obj.isClickable() && obj.boundingBox.contains(touchX, touchY)) {
                    onObjectClickListener?.invoke(obj)
                    return true
                }
            }
        }
        // Don't consume touch events - let them pass through to GLSurfaceView for 3D interaction
        return false
    }

    /**
     * Start spinner animation
     */
    private fun startSpinnerAnimation() {
        removeCallbacks(spinnerAnimator)
        post(spinnerAnimator)
    }

    /**
     * Stop spinner animation
     */
    private fun stopSpinnerAnimation() {
        removeCallbacks(spinnerAnimator)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopSpinnerAnimation()
    }
}
