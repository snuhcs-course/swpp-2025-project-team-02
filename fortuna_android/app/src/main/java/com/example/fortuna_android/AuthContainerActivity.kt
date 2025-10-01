package com.example.fortuna_android

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.fortuna_android.databinding.ActivityAuthContainerBinding

class AuthContainerActivity : AppCompatActivity() {
    private var _binding: ActivityAuthContainerBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityAuthContainerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState == null) {
            showSignInFragment()
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