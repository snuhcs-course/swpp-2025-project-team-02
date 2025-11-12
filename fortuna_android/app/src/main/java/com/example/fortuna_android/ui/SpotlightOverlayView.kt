package com.example.fortuna_android.ui

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

/**
 * Custom view that draws a dim overlay with a circular spotlight hole
 * Used to highlight specific UI elements during tutorial
 */
class SpotlightOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    init {
        // Required for PorterDuff.Mode.CLEAR to work properly
        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }

    private val overlayPaint = Paint().apply {
        color = Color.argb(204, 0, 0, 0) // 80% opacity black (#CC000000)
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val clearPaint = Paint().apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        isAntiAlias = true
    }

    // Spotlight circle properties
    private var spotlightX: Float = 0f
    private var spotlightY: Float = 0f
    private var spotlightRadius: Float = 0f
    private var isSpotlightEnabled: Boolean = false

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw the overlay
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), overlayPaint)

        // Draw the spotlight hole if enabled
        if (isSpotlightEnabled) {
            canvas.drawCircle(spotlightX, spotlightY, spotlightRadius, clearPaint)
        }
    }

    /**
     * Enable spotlight on a specific view
     * @param targetView The view to highlight
     * @param padding Additional padding around the view (default 40dp)
     */
    fun setSpotlight(targetView: View, padding: Float = 40f) {
        val location = IntArray(2)
        targetView.getLocationInWindow(location)

        // Get this view's location for offset calculation
        val overlayLocation = IntArray(2)
        this.getLocationInWindow(overlayLocation)

        // Calculate spotlight center relative to this view
        spotlightX = location[0] - overlayLocation[0] + targetView.width / 2f
        spotlightY = location[1] - overlayLocation[1] + targetView.height / 2f

        // Calculate radius to cover the entire view with padding
        val maxDimension = maxOf(targetView.width, targetView.height)
        spotlightRadius = maxDimension / 2f + padding

        isSpotlightEnabled = true
        invalidate()
    }

    /**
     * Disable spotlight (show full dim overlay)
     */
    fun clearSpotlight() {
        isSpotlightEnabled = false
        invalidate()
    }

    /**
     * Set overlay opacity (0-255)
     */
    fun setOverlayOpacity(alpha: Int) {
        overlayPaint.alpha = alpha.coerceIn(0, 255)
        invalidate()
    }
}
