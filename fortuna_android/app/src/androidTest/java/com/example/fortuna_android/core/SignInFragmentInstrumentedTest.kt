package com.example.fortuna_android.core

import android.widget.Button
import com.google.android.gms.common.SignInButton
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.fortuna_android.R
import com.example.fortuna_android.SignInFragment
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Lean smoke tests for SignInFragment aligned with current implementation.
 */
@RunWith(AndroidJUnit4::class)
class SignInFragmentInstrumentedTest {

    @Test
    fun signInFragment_rendersButtons() {
        val scenario = launchFragmentInContainer<SignInFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        scenario.onFragment { fragment ->
            val signIn = fragment.view?.findViewById<SignInButton>(R.id.sign_in_button)
            val signOut = fragment.view?.findViewById<Button>(R.id.sign_out_button)
            assertNotNull(signIn)
            assertNotNull(signOut)
        }

        scenario.close()
    }
}
