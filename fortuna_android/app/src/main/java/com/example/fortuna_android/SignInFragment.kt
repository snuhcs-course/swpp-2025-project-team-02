package com.example.fortuna_android

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.fortuna_android.api.LoginRequest
import com.example.fortuna_android.api.LogoutRequest
import com.example.fortuna_android.api.RetrofitClient
import com.example.fortuna_android.api.UserProfile
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.SignInButton
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.launch

class SignInFragment : Fragment() {

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
            Toast.makeText(requireContext(), "로그인이 취소되었습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_signin, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupGoogleSignIn()
        setupClickListeners()
    }

    override fun onStart() {
        super.onStart()
        val account = GoogleSignIn.getLastSignedInAccount(requireContext())
        updateUI(account)
    }

    private fun initViews(view: View) {
        signInButton = view.findViewById(R.id.sign_in_button)
        signOutButton = view.findViewById(R.id.sign_out_button)
        nameTextView = view.findViewById(R.id.name_text_view)
        emailTextView = view.findViewById(R.id.email_text_view)
        idTextView = view.findViewById(R.id.id_text_view)
        userInfoLayout = view.findViewById(R.id.user_info_layout)
    }

    private fun setupGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(BuildConfig.GOOGLE_CLIENT_ID)
            .requestEmail()
            .build()

        mGoogleSignInClient = GoogleSignIn.getClient(requireContext(), gso)
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
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
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
                    Toast.makeText(requireContext(), "로그아웃 되었습니다.", Toast.LENGTH_SHORT).show()
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e(TAG, "서버 로그아웃 실패: ${response.code()}, $errorBody")
                    Toast.makeText(requireContext(), "서버 로그아웃 실패, 로컬에서 로그아웃합니다.", Toast.LENGTH_SHORT).show()
                }

                performLocalSignOut()

            } catch (e: Exception) {
                Log.e(TAG, "로그아웃 요청 중 오류", e)
                Toast.makeText(requireContext(), "네트워크 오류, 로컬에서 로그아웃합니다.", Toast.LENGTH_SHORT).show()
                performLocalSignOut()
            }
        }
    }

    private fun performLocalSignOut() {
        mGoogleSignInClient.signOut().addOnCompleteListener {
            val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().clear().apply()

            Log.d(TAG, "로컬 로그아웃 완료 - 모든 토큰 제거됨")
            updateUI(null)
        }
    }

    private fun handleSignInResult(completedTask: com.google.android.gms.tasks.Task<GoogleSignInAccount>) {
        try {
            val account = completedTask.getResult(ApiException::class.java)
            Toast.makeText(requireContext(), "Google 로그인 성공!", Toast.LENGTH_SHORT).show()

            val idToken = account.idToken
            Log.d(TAG, account.displayName.toString())
            Log.d(TAG, account.id.toString())
            Log.d(TAG, account.idToken.toString())
            Log.d(TAG, account.email.toString())

            if (idToken != null) {
                Log.d(TAG, "Got ID Token: $idToken")
                sendTokenToServer(idToken)
            } else {
                Log.w(TAG, "ID Token is null")
                Toast.makeText(requireContext(), "ID 토큰을 가져올 수 없습니다.", Toast.LENGTH_SHORT).show()
            }
            updateUI(account)
        } catch (e: ApiException) {
            Log.w(TAG, "signInResult:failed code=" + e.statusCode)
            Toast.makeText(requireContext(), "로그인에 실패했습니다. (코드: ${e.statusCode})", Toast.LENGTH_LONG).show()
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
                        val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                        prefs.edit().putString(KEY_TOKEN, backendToken).apply()
                        prefs.edit().putString(REFRESH_TOKEN, refreshToken).apply()
                        Toast.makeText(requireContext(), "'$username'님, 서버 로그인 성공!", Toast.LENGTH_SHORT).show()
                        Log.d(TAG, "토큰이 SharedPreferences에 저장되었습니다: $backendToken")

                        verifyTokenWithServer()
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e(TAG, "서버 로그인 실패: ${response.code()}, $errorBody")
                    Toast.makeText(requireContext(), "서버 로그인 실패 (코드: ${response.code()})", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error sending token to server", e)
                Toast.makeText(requireContext(), "서버 통신 중 오류 발생: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun verifyTokenWithServer() {
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val token = prefs.getString(KEY_TOKEN, null)
        Log.d(TAG, "token: ${token}")
        if (token.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "저장된 토큰이 없습니다. 먼저 로그인해주세요.", Toast.LENGTH_SHORT).show()
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

                    fetchUserProfile()
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e(TAG, "프로필 정보 받기 실패: ${response.code()}, $errorBody")
                    Toast.makeText(requireContext(), "토큰 검증 실패 (코드: ${response.code()})", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error during profile verification", e)
                Toast.makeText(requireContext(), "프로필 요청 중 오류 발생: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun fetchUserProfile() {
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
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
                            Log.d(TAG, "프로필 정보가 완성되었습니다. ProfileActivity로 이동합니다.")
                            navigateToMain()
                        } else {
                            Log.d(TAG, "프로필 정보가 불완전합니다. ProfileInputFragment로 이동합니다.")
                            navigateToProfileInput()
                        }
                    } else {
                        Log.e(TAG, "프로필 데이터가 null입니다.")
                        navigateToProfileInput()
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e(TAG, "사용자 프로필 가져오기 실패: ${response.code()}, $errorBody")
                    Toast.makeText(requireContext(), "프로필 정보를 가져올 수 없습니다.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching user profile", e)
                Toast.makeText(requireContext(), "프로필 요청 중 오류 발생: ${e.message}", Toast.LENGTH_SHORT).show()
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

    private fun navigateToProfileInput() {
        (activity as? AuthContainerActivity)?.showProfileInputFragment()
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
        val intent = Intent(requireContext(), ProfileActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        requireActivity().finish()
    }

    companion object {
        private const val TAG = "SignInFragment"
        private const val PREFS_NAME = "fortuna_prefs"
        private const val KEY_TOKEN = "jwt_token"
        private const val REFRESH_TOKEN = "refresh_token"
    }
}
