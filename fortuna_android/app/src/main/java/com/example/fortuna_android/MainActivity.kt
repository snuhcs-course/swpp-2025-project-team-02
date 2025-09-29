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
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.SignInButton
import com.google.android.gms.common.api.ApiException
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.launch
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST


// Retrofit API 통신을 위한 데이터 클래스 정의

// POST /api/auth/google/ 요청 시 Body에 담을 데이터
data class LoginRequest(
    @SerializedName("id_token") val idToken: String
)

// POST /api/auth/google/ 요청 성공 시 응답 데이터
data class LoginResponse(
    @SerializedName("accessToken") val accessToken: String,
    @SerializedName("user") val user: User
)

// GET /api/profile/ 요청 성공 시 응답 데이터
data class User(
    @SerializedName("pk") val id: Int,
    @SerializedName("username") val username: String,
    @SerializedName("email") val email: String
)


// --- Retrofit API 인터페이스 정의 ---

interface ApiService {
    @POST("api/user/auth/google/")
    suspend fun loginWithGoogle(@Body request: LoginRequest): Response<LoginResponse>

    @GET("api/user/profile/")
    suspend fun getProfile(@Header("Authorization") token: String): Response<User>
}


// --- Retrofit 클라이언트 싱글턴 객체 ---

object RetrofitClient {
    private const val BASE_URL = "http://172.30.1.66:8000/"

    val instance: ApiService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        retrofit.create(ApiService::class.java)
    }
}


class MainActivity : AppCompatActivity() {

    private lateinit var mGoogleSignInClient: GoogleSignInClient

    // UI 요소
    private lateinit var signInButton: SignInButton
    private lateinit var signOutButton: Button
    private lateinit var verifyButton: Button
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
        setContentView(R.layout.activity_main)

        signInButton = findViewById(R.id.sign_in_button)
        signOutButton = findViewById(R.id.sign_out_button)
        verifyButton = findViewById(R.id.verify_button)
        nameTextView = findViewById(R.id.name_text_view)
        emailTextView = findViewById(R.id.email_text_view)
        idTextView = findViewById(R.id.id_text_view)
        userInfoLayout = findViewById(R.id.user_info_layout)

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("46186834187-83mthhs9phn9d1h9b4khvudhtbhsti44.apps.googleusercontent.com") // 웹 클라이언트 ID를 입력하세요.
            .requestEmail()
            .build()

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso)

        signInButton.setOnClickListener { signIn() }
        signOutButton.setOnClickListener { signOut() }
        verifyButton.setOnClickListener { verifyTokenWithServer() }
    }

    override fun onStart() {
        super.onStart()
        val account = GoogleSignIn.getLastSignedInAccount(this)
        updateUI(account)
    }

    private fun signIn() {
        val signInIntent: Intent = mGoogleSignInClient.signInIntent
        googleSignInLauncher.launch(signInIntent)
    }

    private fun signOut() {
        mGoogleSignInClient.signOut().addOnCompleteListener(this) {
            val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().remove(KEY_TOKEN).apply()

            Toast.makeText(this, "로그아웃 되었습니다.", Toast.LENGTH_SHORT).show()
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
                        val username = loginResponse.user
                        Log.d(TAG, "loginResponse (String): $loginResponse")
                        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                        prefs.edit().putString(KEY_TOKEN, backendToken).apply()

                        Toast.makeText(this@MainActivity, "'$username'님, 서버 로그인 성공!", Toast.LENGTH_LONG).show()
                        Log.d(TAG, "토큰이 SharedPreferences에 저장되었습니다: $backendToken")
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e(TAG, "서버 로그인 실패: ${response.code()}, $errorBody")
                    Toast.makeText(this@MainActivity, "서버 로그인 실패 (코드: ${response.code()})", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error sending token to server", e)
                Toast.makeText(this@MainActivity, "서버 통신 중 오류 발생: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun verifyTokenWithServer() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val token = prefs.getString(KEY_TOKEN, null)

        if (token.isNullOrEmpty()) {
            Toast.makeText(this, "저장된 토큰이 없습니다. 먼저 로그인해주세요.", Toast.LENGTH_SHORT).show()
            return
        }

        Log.d(TAG, "프로필 요청에 사용할 토큰: $token")
        Toast.makeText(this, "서버에 프로필 정보를 요청합니다...", Toast.LENGTH_SHORT).show()

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.getProfile("Bearer $token")

                if (response.isSuccessful) {
                    val userProfile = response.body()
                    Log.d(TAG, "Profile API Response: $userProfile")
                    Toast.makeText(this@MainActivity, "프로필 정보 받기 성공!\n$userProfile", Toast.LENGTH_LONG).show()
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e(TAG, "프로필 정보 받기 실패: ${response.code()}, $errorBody")
                    Toast.makeText(this@MainActivity, "프로필 정보 받기 실패 (코드: ${response.code()})", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error during profile verification", e)
                Toast.makeText(this@MainActivity, "프로필 요청 중 오류 발생: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
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
        verifyButton.visibility = if (isLoggedIn) View.VISIBLE else View.GONE
    }

    companion object {
        private const val TAG = "GoogleSignIn"
        private const val PREFS_NAME = "fortuna_prefs"
        private const val KEY_TOKEN = "jwt_token"
    }
}
