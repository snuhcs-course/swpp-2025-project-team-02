package com.example.fortuna_android

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
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
import com.example.fortuna_android.util.CustomToast
import com.example.fortuna_android.api.RefreshTokenRequest
import com.example.fortuna_android.ui.ARCoreSessionLifecycleHelper
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private var _binding: ActivityMainBinding? = null
    private val binding get() = _binding!!
    private lateinit var mGoogleSignInClient: GoogleSignInClient

    // ARCore session lifecycle helper
    val arCoreSessionHelper = ARCoreSessionLifecycleHelper(this)
    companion object {
        private const val TAG = "MainActivity"
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

        // Add ARCore session lifecycle observer
        lifecycle.addObserver(arCoreSessionHelper)

        // edge-to-edge handling
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
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

        // Request notification permission and generate FCM token
        NotificationManager.requestNotificationPermission(this)
        NotificationManager.generateFCMToken(this) { token ->
            Log.d(TAG, "FCM Token: $token")
        }

//         Schedule daily notification by Android(not firebase)
//        NotificationManager.fortuneGeneratedNotification(
//            this,
//            "Daily Fortune",
//            "Your daily fortune is ready! ✨",
//            1
//        )

        // Initialize RetrofitClient with context for auth interceptor
        RetrofitClient.initialize(this)

        // Login associated tasks
        setupGoogleSignIn()
        checkLoginStatus()
    }

    override fun onResume() {
        super.onResume()
        // 다른 앱 방문 후 되돌아 올 때 로그인 풀리며 프로필 정보 불러오지 못하는 버그 수정.
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val accessToken = prefs.getString(KEY_TOKEN, null)
        val refreshToken = prefs.getString(REFRESH_TOKEN, null)

        if (accessToken.isNullOrEmpty() || refreshToken.isNullOrEmpty()) {
            // 토큰이 없으면 로그인 화면으로 이동
            navigateToSignIn()
        } else {
            // 토큰이 있으면 유효성 검증 및 갱신
            lifecycleScope.launch {
                validateAndRefreshToken(accessToken, refreshToken)
            }
        }
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
        val accessToken = prefs.getString(KEY_TOKEN, null)
        val refreshToken = prefs.getString(REFRESH_TOKEN, null)

        if (accessToken.isNullOrEmpty() || refreshToken.isNullOrEmpty()) {
            Log.d(TAG, "No tokens found, redirecting to SignInActivity")
            navigateToSignIn()
            return
        }
        // Validate token by trying to get profile
        lifecycleScope.launch {
            validateAndRefreshToken(accessToken, refreshToken)
        }
    }

    private suspend fun validateAndRefreshToken(accessToken: String, refreshToken: String) {
        try {
            // Try to validate token by getting profile
            val profileResponse = RetrofitClient.instance.getUserProfile()

            if (profileResponse.isSuccessful) {
                Log.d(TAG, "Token is valid, user is logged in")
                // Token is valid, user can continue
            } else if (profileResponse.code() == 401) {
                Log.d(TAG, "Token expired, attempting to refresh")
                // Token expired, try to refresh
                refreshAccessToken(refreshToken)
            } else {
                Log.e(TAG, "Profile fetch failed: ${profileResponse.code()}")
                navigateToSignIn()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error validating token", e)
            navigateToSignIn()
        }
    }

    private suspend fun refreshAccessToken(refreshToken: String) {
        try {
            val refreshRequest = RefreshTokenRequest(refresh = refreshToken)
            val response = RetrofitClient.instance.refreshToken(refreshRequest)

            if (response.isSuccessful) {
                val newAccessToken = response.body()?.access
                if (newAccessToken != null) {
                    // Save new access token
                    val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    prefs.edit().putString(KEY_TOKEN, newAccessToken).apply()
                    Log.d(TAG, "Token refreshed successfully")
                } else {
                    Log.e(TAG, "Refresh token response body is null")
                    navigateToSignIn()
                }
            } else {
                Log.e(TAG, "Token refresh failed: ${response.code()}")
                navigateToSignIn()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error refreshing token", e)
            navigateToSignIn()
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

    private fun checkAndNavigateToCamera(navController: androidx.navigation.NavController) {
        val missingPermissions = REQUIRED_PERMISSIONS.filter { permission ->
            ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isEmpty()) {
            // All permissions granted - navigate to camera
            Log.d(TAG, "All permissions granted - navigating to camera")
            try {
                navController.navigate(R.id.cameraFragment)
            } catch (e: IllegalStateException) {
                Log.w(TAG, "Navigation to camera failed - activity may be finishing", e)
            }
        } else {
            // Some permissions missing - request them
            Log.d(TAG, "Missing permissions: ${missingPermissions.joinToString()}")
            CustomToast.show(this, "Some permissions missing. Requesting permissions...")
            requestPermissions()
        }
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

        // Forward to ARCore session helper for camera permission handling
        arCoreSessionHelper.onRequestPermissionsResult(requestCode, permissions, grantResults)

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


    /**
     * Release any active camera resources to prepare for ARCore usage
     */
    fun releaseCameraResources() {
        Log.d(TAG, "Attempting to release camera resources for ARCore")

        try {
            // Use the public method to release camera resources
            arCoreSessionHelper.releaseCameraResources()
            Log.d(TAG, "Camera resources released successfully")

        } catch (e: Exception) {
            Log.e(TAG, "Error releasing camera resources", e)
        }
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
                        CustomToast.show(this@MainActivity, "로그아웃 되었습니다.")
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e(TAG, "서버 로그아웃 실패: ${response.code()}, $errorBody")
                    if (!isFinishing && !isDestroyed) {
                        CustomToast.show(this@MainActivity, "서버 로그아웃 실패, 로컬에서 로그아웃합니다.")
                    }
                }

                // 성공/실패 관계없이 로컬에서 로그아웃 수행
                performLocalLogout()

            } catch (e: Exception) {
                Log.e(TAG, "로그아웃 요청 중 오류", e)
                if (!isFinishing && !isDestroyed) {
                    CustomToast.show(this@MainActivity, "네트워크 오류, 로컬에서 로그아웃합니다.")
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