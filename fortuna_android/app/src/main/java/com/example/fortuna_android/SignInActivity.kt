package com.example.fortuna_android

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.fortuna_android.data.Api.LoginRequest
import com.example.fortuna_android.data.Api.LogoutRequest
import com.example.fortuna_android.data.Api.RetrofitClient
import com.example.fortuna_android.data.Api.UserProfile
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.SignInButton
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.launch

class SignInActivity : AppCompatActivity() {

    private lateinit var mGoogleSignInClient: GoogleSignInClient

    // UI 요소
    private lateinit var signInButton: SignInButton
    private lateinit var signOutButton: Button
    private lateinit var nameTextView: TextView
    private lateinit var emailTextView: TextView
    private lateinit var idTextView: TextView
    private lateinit var userInfoLayout: LinearLayout

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            handleSignInResult(task)
        } else {
            Toast.makeText(this, "로그인이 취소되었습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signin)

        initViews()
        setupGoogleSignIn()
        setupClickListeners()
    }

    override fun onStart() {
        super.onStart()
        val account = GoogleSignIn.getLastSignedInAccount(this)
        updateUI(account)
    }

    private fun initViews() {
        signInButton = findViewById(R.id.sign_in_button)
        signOutButton = findViewById(R.id.sign_out_button)
        nameTextView = findViewById(R.id.name_text_view)
        emailTextView = findViewById(R.id.email_text_view)
        idTextView = findViewById(R.id.id_text_view)
        userInfoLayout = findViewById(R.id.user_info_layout)
    }

    private fun setupGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("46186834187-83mthhs9phn9d1h9b4khvudhtbhsti44.apps.googleusercontent.com")
            .requestEmail()
            .build()

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso)
    }

    private fun setupClickListeners() {
        signInButton.setOnClickListener { signIn() }
        signOutButton.setOnClickListener { signOut() }
    }

    private fun signIn() {
        val signInIntent: Intent = mGoogleSignInClient.signInIntent
        googleSignInLauncher.launch(signInIntent)
    }

    private fun signOut() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val refreshToken = prefs.getString(REFRESH_TOKEN, null)

        if (refreshToken.isNullOrEmpty()) {
            Log.w(TAG, "Refresh token이 없습니다. 로컬에서만 로그아웃합니다.")
            performLocalSignOut()
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
                    Toast.makeText(this@SignInActivity, "로그아웃 되었습니다.", Toast.LENGTH_SHORT).show()
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e(TAG, "서버 로그아웃 실패: ${response.code()}, $errorBody")
                    Toast.makeText(this@SignInActivity, "서버 로그아웃 실패, 로컬에서 로그아웃합니다.", Toast.LENGTH_SHORT).show()
                }

                // 성공/실패 관계없이 로컬에서 로그아웃 수행
                performLocalSignOut()

            } catch (e: Exception) {
                Log.e(TAG, "로그아웃 요청 중 오류", e)
                Toast.makeText(this@SignInActivity, "네트워크 오류, 로컬에서 로그아웃합니다.", Toast.LENGTH_SHORT).show()
                performLocalSignOut()
            }
        }
    }

    private fun performLocalSignOut() {
        mGoogleSignInClient.signOut().addOnCompleteListener(this) {
            val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().clear().apply()

            Log.d(TAG, "로컬 로그아웃 완료 - 모든 토큰 제거됨")
            updateUI(null)
        }
    }

    private fun handleSignInResult(completedTask: com.google.android.gms.tasks.Task<GoogleSignInAccount>) {
        try {
            val account = completedTask.getResult(ApiException::class.java)
            Toast.makeText(this, "Google 로그인 성공!", Toast.LENGTH_SHORT).show()

            val idToken = account.idToken
            Log.d(TAG, account.displayName.toString())
            Log.d(TAG, account.id.toString())
            Log.d(TAG, account.idToken.toString())
            Log.d(TAG, account.email.toString())
            Log.d(TAG, account.familyName.toString())
            Log.d(TAG, account.givenName.toString())
            Log.d(TAG, account.serverAuthCode.toString())

            if (idToken != null) {
                Log.d(TAG, "Got ID Token: $idToken")
                sendTokenToServer(idToken)
            } else {
                Log.w(TAG, "ID Token is null")
                Toast.makeText(this, "ID 토큰을 가져올 수 없습니다.", Toast.LENGTH_SHORT).show()
            }
            updateUI(account)
        } catch (e: ApiException) {
            Log.w(TAG, "signInResult:failed code=" + e.statusCode)
            Toast.makeText(this, "로그인에 실패했습니다. (코드: ${e.statusCode})", Toast.LENGTH_LONG).show()
            updateUI(null)
        }
    }

    private fun sendTokenToServer(idToken: String) {
        lifecycleScope.launch {
            try {
                val request = LoginRequest(idToken = idToken)
                val response = RetrofitClient.instance.loginWithGoogle(request)
                if (response.isSuccessful) {
                    val loginResponse = response.body()
                    if (loginResponse != null) {
                        val backendToken = loginResponse.accessToken
                        val refreshToken = loginResponse.refreshToken
                        val username = loginResponse.name
                        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                        prefs.edit().putString(KEY_TOKEN, backendToken).apply()
                        prefs.edit().putString(REFRESH_TOKEN, refreshToken).apply()
                        Toast.makeText(this@SignInActivity, "'$username'님, 서버 로그인 성공!", Toast.LENGTH_SHORT).show()
                        Log.d(TAG, "토큰이 SharedPreferences에 저장되었습니다: $backendToken")

                        // 토큰 저장 후 자동으로 검증 진행
                        verifyTokenWithServer()
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e(TAG, "서버 로그인 실패: ${response.code()}, $errorBody")
                    Toast.makeText(this@SignInActivity, "서버 로그인 실패 (코드: ${response.code()})", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error sending token to server", e)
                Toast.makeText(this@SignInActivity, "서버 통신 중 오류 발생: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun verifyTokenWithServer() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val token = prefs.getString(KEY_TOKEN, null)
        Log.d(TAG, "token: ${token}")
        if (token.isNullOrEmpty()) {
            Toast.makeText(this, "저장된 토큰이 없습니다. 먼저 로그인해주세요.", Toast.LENGTH_SHORT).show()
            return
        }

        Log.d(TAG, "프로필 요청에 사용할 토큰: $token")
        Log.d(TAG, "자동으로 토큰 검증을 시작합니다...")

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.getProfile("Bearer $token")

                if (response.isSuccessful) {
                    val userProfile = response.body()
                    Log.d(TAG, "Profile API Response: $userProfile")
                    Log.d(TAG, "토큰 검증 성공! 사용자 프로필을 확인합니다.")

                    // 토큰 검증 성공 후 사용자 프로필 정보 가져오기
                    fetchUserProfile()
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e(TAG, "프로필 정보 받기 실패: ${response.code()}, $errorBody")
                    Toast.makeText(this@SignInActivity, "토큰 검증 실패 (코드: ${response.code()})", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error during profile verification", e)
                Toast.makeText(this@SignInActivity, "프로필 요청 중 오류 발생: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun fetchUserProfile() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val token = prefs.getString(KEY_TOKEN, null)

        if (token.isNullOrEmpty()) {
            Log.e(TAG, "토큰이 없습니다.")
            return
        }

        Log.d(TAG, "사용자 프로필 정보를 가져옵니다...")

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.getUserProfile("Bearer $token")

                if (response.isSuccessful) {
                    val userProfile = response.body()
                    Log.d(TAG, "User Profile Response: $userProfile")

                    if (userProfile != null) {
                        if (isProfileComplete(userProfile)) {
                            Log.d(TAG, "프로필 정보가 완성되었습니다. MainActivity로 이동합니다.")
                            navigateToMain()
                        } else {
                            Log.d(TAG, "프로필 정보가 불완전합니다. SignInActivity2로 이동합니다.")
                            navigateToSignInActivity2()
                        }
                    } else {
                        Log.e(TAG, "프로필 데이터가 null입니다.")
                        navigateToSignInActivity2()
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e(TAG, "사용자 프로필 가져오기 실패: ${response.code()}, $errorBody")
                    Toast.makeText(this@SignInActivity, "프로필 정보를 가져올 수 없습니다.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching user profile", e)
                Toast.makeText(this@SignInActivity, "프로필 요청 중 오류 발생: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun isProfileComplete(profile: UserProfile): Boolean {
        return !profile.nickname.isNullOrEmpty() &&
                !profile.birthDateLunar.isNullOrEmpty() &&
                !profile.birthDateSolar.isNullOrEmpty() &&
                !profile.solarOrLunar.isNullOrEmpty() &&
                !profile.birthTimeUnits.isNullOrEmpty() &&
                !profile.gender.isNullOrEmpty()
    }

    private fun navigateToSignInActivity2() {
        val intent = Intent(this, SignInActivity2::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun updateUI(account: GoogleSignInAccount?) {
        val isLoggedIn = account != null
        if (isLoggedIn) {
            nameTextView.text = "이름: ${account!!.displayName}"
            emailTextView.text = "이메일: ${account.email}"
            idTextView.text = "고유 ID: ${account.id}"
        }

        signInButton.visibility = if (isLoggedIn) View.GONE else View.VISIBLE
        userInfoLayout.visibility = if (isLoggedIn) View.VISIBLE else View.GONE
        signOutButton.visibility = if (isLoggedIn) View.VISIBLE else View.GONE
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    companion object {
        private const val TAG = "GoogleSignIn"
        private const val PREFS_NAME = "fortuna_prefs"
        private const val KEY_TOKEN = "jwt_token"
        private const val REFRESH_TOKEN = "refresh_token"
    }
}