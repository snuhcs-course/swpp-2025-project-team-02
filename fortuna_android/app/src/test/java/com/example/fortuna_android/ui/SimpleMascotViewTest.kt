package com.example.fortuna_android.ui

import android.graphics.drawable.BitmapDrawable
import androidx.test.core.app.ApplicationProvider
import com.example.fortuna_android.R
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31])
class SimpleMascotViewTest {

    @Test
    fun initializesWithDrawable() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val view = SimpleMascotView(context)

        val drawable = view.drawable
        assertNotNull("Mascot drawable should be set", drawable)

        // In Robolectric, assets may be missing; expect fallback icon which is a BitmapDrawable
        if (drawable != null) {
            assertTrue(
                "Drawable should be bitmap or vector fallback",
                drawable is BitmapDrawable || drawable.constantState != null
            )
        }
    }
}
