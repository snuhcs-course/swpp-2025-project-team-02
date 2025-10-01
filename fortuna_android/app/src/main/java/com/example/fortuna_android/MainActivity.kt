package com.example.fortuna_android

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
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
import androidx.lifecycle.lifecycleScope
import com.example.fortuna_android.api.LogoutRequest
import com.example.fortuna_android.api.RetrofitClient
import com.example.fortuna_android.databinding.ActivityMainBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import android.widget.Toast
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private var _binding: ActivityMainBinding? = null
    private val binding get() = _binding!!
    private lateinit var mGoogleSignInClient: GoogleSignInClient
    companion object {
        private const val TAG = "ProfileActivity"
        private const val PREFS_NAME = "fortuna_prefs"
        private const val KEY_TOKEN = "jwt_token"
        private const val REFRESH_TOKEN = "refresh_token"
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
            .findFragmentById(R.id.nav_host_fragment) as? NavHostFragment
        if (navHostFragment == null) {
            Log.e(TAG, "NavHostFragment not found")
            finish()
            return
        }
        val navController = navHostFragment.navController

        // Check permissions on startup
        requestPermissions()

        // Login associated tasks
        setupGoogleSignIn()
        checkLoginStatus()

        // Set the correct selected item to match the start destination
        binding.bottomNavigationView.selectedItemId = R.id.homeFragment

        // Connect the BottomNavigationView to the NavController with custom animations
        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            if (_binding == null) return@setOnItemSelectedListener false
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

            try {
                navController.navigate(item.itemId, null, options)
            } catch (e: IllegalStateException) {
                Log.w(TAG, "Navigation failed - activity may be finishing", e)
                return@setOnItemSelectedListener false
            }
            true
        }
    }

    override fun onResume() {
        super.onResume()
        checkLoginStatus()
    }

    private fun setupGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(BuildConfig.GOOGLE_CLIENT_ID)
            .requestEmail()
            .build()

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso)
    }

    private fun checkLoginStatus() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val token = prefs.getString(KEY_TOKEN, null)
        val account = GoogleSignIn.getLastSignedInAccount(this)

        if (token.isNullOrEmpty() || account == null) {
            Log.d(TAG, "User not logged in, redirecting to SignInActivity")
            navigateToSignIn()
        } else {
            Log.d(TAG, "User is logged in")
            //loadUserProfile(token)
        }
    }

    private fun navigateToSignIn() {
        val intent = Intent(this, AuthContainerActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
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
        val binding = _binding ?: return
        binding.bottomNavigationView.visibility = View.GONE
    }

    fun showBottomNavigation() {
        val binding = _binding ?: return
        binding.bottomNavigationView.visibility = View.VISIBLE
    }

    fun logout() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val refreshToken = prefs.getString(REFRESH_TOKEN, null)

        if (refreshToken.isNullOrEmpty()) {
            Log.w(TAG, "Refresh token이 없습니다. 로컬에서만 로그아웃합니다.")
            performLocalLogout()
            return
        }

        Log.d(TAG, "서버에 로그아웃 요청을 보냅니다...")

        lifecycleScope.launch {
            try {
                val request = LogoutRequest(refreshToken = refreshToken)
                val response = RetrofitClient.instance.logout(request)

                if (response.isSuccessful) {
                    val logoutResponse = response.body()
                    Log.d(TAG, "서버 로그아웃 성공: ${logoutResponse?.message}")
                    if (!isFinishing && !isDestroyed) {
                        Toast.makeText(this@MainActivity, "로그아웃 되었습니다.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e(TAG, "서버 로그아웃 실패: ${response.code()}, $errorBody")
                    if (!isFinishing && !isDestroyed) {
                        Toast.makeText(this@MainActivity, "서버 로그아웃 실패, 로컬에서 로그아웃합니다.", Toast.LENGTH_SHORT).show()
                    }
                }

                // 성공/실패 관계없이 로컬에서 로그아웃 수행
                performLocalLogout()

            } catch (e: Exception) {
                Log.e(TAG, "로그아웃 요청 중 오류", e)
                if (!isFinishing && !isDestroyed) {
                    Toast.makeText(this@MainActivity, "네트워크 오류, 로컬에서 로그아웃합니다.", Toast.LENGTH_SHORT).show()
                }
                performLocalLogout()
            }
        }
    }

    private fun performLocalLogout() {
        // Google 로그아웃
        mGoogleSignInClient.signOut().addOnCompleteListener(this) {
            // SharedPreferences에서 모든 데이터 제거
            val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().clear().apply()

            Log.d(TAG, "로컬 로그아웃 완료 - 모든 토큰 제거됨")
            navigateToSignIn()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}