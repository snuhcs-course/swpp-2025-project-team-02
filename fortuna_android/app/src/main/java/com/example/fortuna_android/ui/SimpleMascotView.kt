package com.example.fortuna_android.ui

import android.content.Context
import android.graphics.BitmapFactory
import android.util.AttributeSet
import com.example.fortuna_android.R
import java.io.IOException

/**
 * Simple view to display mascot character
 * Uses mascot.png from assets
 */
class SimpleMascotView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : androidx.appcompat.widget.AppCompatImageView(context, attrs, defStyleAttr) {

    init {
        scaleType = ScaleType.FIT_CENTER
        loadMascotImage()
    }

    private fun loadMascotImage() {
        try {
            // Load mascot.png from assets
            val inputStream = context.assets.open("rendered/mascot/mascot.png")
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()

            setImageBitmap(bitmap)
        } catch (e: IOException) {
            // Fallback to default icon if mascot image not found
            setImageResource(R.drawable.ic_help)
            val drawable = drawable
            drawable?.setTint(context.getColor(android.R.color.holo_purple))
        }
    }
}
