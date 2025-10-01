package com.example.fortuna_android

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment

class AuthContainerActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_auth_container)

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

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}