package com.example.fortuna_android

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.NavHostFragment
import androidx.appcompat.app.AlertDialog
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.NavOptions
import com.example.fortuna_android.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private var _binding: ActivityMainBinding? = null
    private val binding get() = _binding!!

    companion object {
        private const val PERMISSION_REQUEST_CODE = 1001
        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // edge-to-edge handling
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        // Find the NavHostFragment and get its NavController
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        // Check permissions on startup
        requestPermissions()

        // Set the correct selected item to match the start destination
        binding.bottomNavigationView.selectedItemId = R.id.homeFragment

        // Connect the BottomNavigationView to the NavController with custom animations
        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            val options = when (item.itemId) {
                R.id.profileFragment -> {
                    // ProfileFragment: slide in from right
                    NavOptions.Builder()
                        .setEnterAnim(R.anim.slide_in_right)
                        .setExitAnim(R.anim.slide_out_left)
                        .setPopEnterAnim(R.anim.slide_in_left)
                        .setPopExitAnim(R.anim.slide_out_right)
                        .build()
                }
                R.id.searchFragment -> {
                    // SearchFragment: slide in from left
                    NavOptions.Builder()
                        .setEnterAnim(R.anim.slide_in_left)
                        .setExitAnim(R.anim.slide_out_right)
                        .setPopEnterAnim(R.anim.slide_in_right)
                        .setPopExitAnim(R.anim.slide_out_left)
                        .build()
                }
                else -> {
                    // Default animations for other fragments (Home)
                    NavOptions.Builder()
                        .setEnterAnim(android.R.anim.fade_in)
                        .setExitAnim(android.R.anim.fade_out)
                        .setPopEnterAnim(android.R.anim.fade_in)
                        .setPopExitAnim(android.R.anim.fade_out)
                        .build()
                }
            }

            navController.navigate(item.itemId, null, options)
            true
        }
    }

    private fun checkPermissions(): Boolean {
        for (permission in REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    permission
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return false
            }
        }
        return true
    }

    fun requestPermissions() {
        if (!checkPermissions()) {
            val permissionsToRequest = mutableListOf<String>()

            // Check each required permission
            for (permission in REQUIRED_PERMISSIONS) {
                if (ContextCompat.checkSelfPermission(
                        this,
                        permission
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    permissionsToRequest.add(permission)
                }
            }
            // Request permissions if any are missing
            if (permissionsToRequest.isNotEmpty()) {
                ActivityCompat.requestPermissions(
                    this,
                    permissionsToRequest.toTypedArray(),
                    PERMISSION_REQUEST_CODE
                )
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == PERMISSION_REQUEST_CODE) {
            // Check if grantResults is empty (UI was interrupted)
            if (grantResults.isEmpty()) {
                // Permission request was cancelled/interrupted
                // Handle gracefully - maybe retry or show explanation
                return
            }

            val deniedPermissions = mutableListOf<String>()

            for (i in permissions.indices) {
                if (i < grantResults.size && grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    deniedPermissions.add(permissions[i])
                }
            }

            if (deniedPermissions.isEmpty()) {
                // All permissions granted
                // You can add any initialization logic that requires permissions here
            } else {
                // Some permissions were denied
                showPermissionDeniedDialog(deniedPermissions)
            }
        }
    }

    private fun showPermissionDeniedDialog(deniedPermissions: List<String>) {
        val permissionNames = deniedPermissions.map { permission ->
            when (permission) {
                Manifest.permission.CAMERA -> "Camera"
                Manifest.permission.ACCESS_FINE_LOCATION -> "Location"
                else -> permission
            }
        }

        val message = "The following permissions are required for the app to work properly:\n\n" +
                "• ${permissionNames.joinToString("\n• ")}\n\n" +
                "Please grant these permissions in Settings to use all features."

        AlertDialog.Builder(this)
            .setTitle("Permissions Required")
            .setMessage(message)
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .setCancelable(false)
            .show()
    }

    fun hideBottomNavigation() {
        _binding?.bottomNavigationView?.visibility = View.GONE
    }

    fun showBottomNavigation() {
        _binding?.bottomNavigationView?.visibility = View.VISIBLE
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}