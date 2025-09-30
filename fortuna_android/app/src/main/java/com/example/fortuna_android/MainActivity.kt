package com.example.fortuna_android

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.fortuna_android.data.Api.LogoutRequest
import com.example.fortuna_android.data.Api.RetrofitClient
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var mGoogleSignInClient: GoogleSignInClient
    private lateinit var welcomeTextView: TextView
    private lateinit var sajuTextView: TextView
    private lateinit var logoutButton: Button

    // 사주팔자 행렬
    private lateinit var yearlyGanji1: TextView
    private lateinit var yearlyGanji2: TextView
    private lateinit var monthlyGanji1: TextView
    private lateinit var monthlyGanji2: TextView
    private lateinit var dailyGanji1: TextView
    private lateinit var dailyGanji2: TextView
    private lateinit var hourlyGanji1: TextView
    private lateinit var hourlyGanji2: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setupGoogleSignIn()
        initViews()
        setupClickListeners()
        checkLoginStatus()
    }

    private fun setupGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("46186834187-83mthhs9phn9d1h9b4khvudhtbhsti44.apps.googleusercontent.com")
            .requestEmail()
            .build()

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso)
    }

    override fun onResume() {
        super.onResume()
        checkLoginStatus()
    }

    private fun initViews() {
        welcomeTextView = findViewById(R.id.welcome_text_view)
        sajuTextView = findViewById(R.id.saju_view_text)
        logoutButton = findViewById(R.id.logout_button)

        // 사주팔자 TextViews
        yearlyGanji1 = findViewById(R.id.yearly_ganji_1)
        yearlyGanji2 = findViewById(R.id.yearly_ganji_2)
        monthlyGanji1 = findViewById(R.id.monthly_ganji_1)
        monthlyGanji2 = findViewById(R.id.monthly_ganji_2)
        dailyGanji1 = findViewById(R.id.daily_ganji_1)
        dailyGanji2 = findViewById(R.id.daily_ganji_2)
        hourlyGanji1 = findViewById(R.id.hourly_ganji_1)
        hourlyGanji2 = findViewById(R.id.hourly_ganji_2)
    }

    private fun setupClickListeners() {
        logoutButton.setOnClickListener { logout() }
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
            loadUserProfile(token)
        }
    }

    private fun loadUserProfile(token: String) {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.getUserProfile("Bearer $token")

                if (response.isSuccessful) {
                    val profile = response.body()
                    if (profile != null) {
                        val nickname = profile.nickname ?: profile.name
                        welcomeTextView.text = "환영합니다, ${nickname}님!"
                        sajuTextView.text = "${nickname}님의 사주팔자"
                        displaySaju(
                            profile.yearlyGanji,
                            profile.monthlyGanji,
                            profile.dailyGanji,
                            profile.hourlyGanji
                        )
                    }
                } else {
                    Log.e(TAG, "프로필 로드 실패: ${response.code()}")
                    Toast.makeText(this@MainActivity, "프로필을 불러올 수 없습니다.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "프로필 로드 중 오류", e)
                Toast.makeText(this@MainActivity, "프로필 로드 오류: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun displaySaju(yearly: String?, monthly: String?, daily: String?, hourly: String?) {
        // 각 간지를 천간(첫글자)과 지지(둘째글자)로 분리하여 표시
        if (yearly != null && yearly.length == 2) {
            setGanjiText(yearlyGanji1, yearly[0].toString())
            setGanjiText(yearlyGanji2, yearly[1].toString())
        }

        if (monthly != null && monthly.length == 2) {
            setGanjiText(monthlyGanji1, monthly[0].toString())
            setGanjiText(monthlyGanji2, monthly[1].toString())
        }

        if (daily != null && daily.length == 2) {
            setGanjiText(dailyGanji1, daily[0].toString())
            setGanjiText(dailyGanji2, daily[1].toString())
        }

        if (hourly != null && hourly.length == 2) {
            setGanjiText(hourlyGanji1, hourly[0].toString())
            setGanjiText(hourlyGanji2, hourly[1].toString())
        }
    }

    private fun setGanjiText(textView: TextView, text: String) {
        textView.text = text
        textView.setBackgroundColor(getGanjiColor(text))
    }

    private fun getGanjiColor(ganji: String): Int {
        return when (ganji) {
            // 목(木) - 초록색
            "갑", "을", "인", "묘" -> Color.parseColor("#0BEFA0")
            // 화(火) - 빨간색
            "병", "정", "사", "오" -> Color.parseColor("#F93E3E")
            // 토(土) - 노란색
            "무", "기", "술", "미", "축", "진" -> Color.parseColor("#FF9500")
            // 금(金) - 흰색
            "경", "신", "유" -> Color.parseColor("#C1BFBF")
            // 수(水) - 회색
            "임", "계", "자", "해" -> Color.parseColor("#2BB3FC")
            else -> Color.parseColor("#CCCCCC")
        }
    }

    private fun logout() {
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
                    Toast.makeText(this@MainActivity, "로그아웃 되었습니다.", Toast.LENGTH_SHORT).show()
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e(TAG, "서버 로그아웃 실패: ${response.code()}, $errorBody")
                    Toast.makeText(this@MainActivity, "서버 로그아웃 실패, 로컬에서 로그아웃합니다.", Toast.LENGTH_SHORT).show()
                }

                // 성공/실패 관계없이 로컬에서 로그아웃 수행
                performLocalLogout()

            } catch (e: Exception) {
                Log.e(TAG, "로그아웃 요청 중 오류", e)
                Toast.makeText(this@MainActivity, "네트워크 오류, 로컬에서 로그아웃합니다.", Toast.LENGTH_SHORT).show()
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

    private fun navigateToSignIn() {
        val intent = Intent(this, SignInActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    companion object {
        private const val TAG = "MainActivity"
        private const val PREFS_NAME = "fortuna_prefs"
        private const val KEY_TOKEN = "jwt_token"
        private const val REFRESH_TOKEN = "refresh_token"
    }
}
