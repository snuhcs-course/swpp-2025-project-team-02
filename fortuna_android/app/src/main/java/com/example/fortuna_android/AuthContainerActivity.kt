package com.example.fortuna_android

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.fortuna_android.databinding.ActivityAuthContainerBinding

class AuthContainerActivity : AppCompatActivity() {
    private var _binding: ActivityAuthContainerBinding? = null
    private val binding get() = _binding!!

    companion object {
        const val EXTRA_SHOW_PROFILE_INPUT = "show_profile_input"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Hide only the status bar, keep navigation bar visible
        window.decorView.systemUiVisibility = (
            android.view.View.SYSTEM_UI_FLAG_FULLSCREEN or
            android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        )

        _binding = ActivityAuthContainerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState == null) {
            // check
            val showProfileInput = intent.getBooleanExtra(EXTRA_SHOW_PROFILE_INPUT, false)
            if (showProfileInput) {
                showProfileInputFragment()
            } else {
                showSignInFragment()
            }
        }
    }

    fun showSignInFragment() {
        replaceFragment(SignInFragment())
    }

    fun showProfileInputFragment() {
        replaceFragment(ProfileInputFragment())
    }

    // Set addToBackStak True when fragments stack should be preserved
    private fun replaceFragment(fragment: Fragment, addToBackStack: Boolean = false) {
        val binding = _binding ?: return
        if (supportFragmentManager.isStateSaved) return

        val transaction = supportFragmentManager.beginTransaction()
            .setCustomAnimations(
                android.R.anim.fade_in,
                android.R.anim.fade_out
            )
            .replace(binding.fragmentContainer.id, fragment)

        if (addToBackStack) {
            transaction.addToBackStack(null)
        }

        transaction.commitAllowingStateLoss()
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}