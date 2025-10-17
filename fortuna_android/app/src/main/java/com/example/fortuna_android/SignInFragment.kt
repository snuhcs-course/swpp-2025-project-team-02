package com.example.fortuna_android

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.example.fortuna_android.databinding.FragmentSigninBinding
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.fortuna_android.api.LoginRequest
import com.example.fortuna_android.api.LogoutRequest
import com.example.fortuna_android.api.RetrofitClient
import com.example.fortuna_android.api.UserProfile
import com.example.fortuna_android.util.CustomToast
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.SignInButton
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.launch

class SignInFragment : Fragment() {
    private var _binding: FragmentSigninBinding? = null
    private val binding get() = _binding!!

    private var mGoogleSignInClient: GoogleSignInClient? = null

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            handleSignInResult(task)
        } else {
            if (isAdded) {
                CustomToast.show(requireContext(), "로그인이 취소되었습니다.")
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSigninBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupGoogleSignIn()
        setupClickListeners()
    }

    override fun onStart() {
        super.onStart()
        if (!isAdded) return
        val account = GoogleSignIn.getLastSignedInAccount(requireContext())
        updateUI(account)
    }


    private fun setupGoogleSignIn() {
        if (!isAdded) return
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(BuildConfig.GOOGLE_CLIENT_ID)
            .requestEmail()
            .build()

        mGoogleSignInClient = GoogleSignIn.getClient(requireContext(), gso)
    }

    private fun setupClickListeners() {
        val binding = _binding ?: return
        binding.signInButton.setOnClickListener { signIn() }
        binding.signOutButton.setOnClickListener { signOut() }
    }

    private fun signIn() {
        val client = mGoogleSignInClient
        if (client == null) {
            if (isAdded) {
                Toast.makeText(requireContext(), "Google Sign-In이 초기화되지 않았습니다.", Toast.LENGTH_SHORT).show()
            }
            return
        }
        val signInIntent: Intent = client.signInIntent
        googleSignInLauncher.launch(signInIntent)
    }

    private fun signOut() {
        if (!isAdded) return
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
                    if (isAdded) {
                        Toast.makeText(requireContext(), "로그아웃 되었습니다.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e(TAG, "서버 로그아웃 실패: ${response.code()}, $errorBody")
                    if (isAdded) {
                        Toast.makeText(requireContext(), "서버 로그아웃 실패, 로컬에서 로그아웃합니다.", Toast.LENGTH_SHORT).show()
                    }
                }

                performLocalSignOut()

            } catch (e: Exception) {
                Log.e(TAG, "로그아웃 요청 중 오류", e)
                if (isAdded) {
                    Toast.makeText(requireContext(), "네트워크 오류, 로컬에서 로그아웃합니다.", Toast.LENGTH_SHORT).show()
                }
                performLocalSignOut()
            }
        }
    }

    private fun performLocalSignOut() {
        mGoogleSignInClient?.signOut()?.addOnCompleteListener {
            if (!isAdded) return@addOnCompleteListener
            val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().clear().apply()

            Log.d(TAG, "로컬 로그아웃 완료 - 모든 토큰 제거됨")
            updateUI(null)
        }
    }

    private fun handleSignInResult(completedTask: com.google.android.gms.tasks.Task<GoogleSignInAccount>) {
        try {
            val account = completedTask.getResult(ApiException::class.java)
            if (isAdded) {
                Toast.makeText(requireContext(), "Google 로그인 성공!", Toast.LENGTH_SHORT).show()
            }

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
                if (isAdded) {
                    Toast.makeText(requireContext(), "ID 토큰을 가져올 수 없습니다.", Toast.LENGTH_SHORT).show()
                }
            }
            updateUI(account)
        } catch (e: ApiException) {
            Log.w(TAG, "signInResult:failed code=" + e.statusCode)
            if (isAdded) {
                Toast.makeText(requireContext(), "로그인에 실패했습니다. (코드: ${e.statusCode})", Toast.LENGTH_LONG).show()
            }
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
                        if (!isAdded) return@launch
                        val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                        prefs.edit().putString(KEY_TOKEN, backendToken).apply()
                        prefs.edit().putString(REFRESH_TOKEN, refreshToken).apply()
                        if (isAdded) {
                            Toast.makeText(requireContext(), "'$username'님, 서버 로그인 성공!", Toast.LENGTH_SHORT).show()
                        }
                        Log.d(TAG, "토큰이 SharedPreferences에 저장되었습니다: $backendToken")

                        verifyTokenWithServer()
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e(TAG, "서버 로그인 실패: ${response.code()}, $errorBody")
                    if (isAdded) {
                        Toast.makeText(requireContext(), "서버 로그인 실패 (코드: ${response.code()})", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error sending token to server", e)
                if (isAdded) {
                    Toast.makeText(requireContext(), "서버 통신 중 오류 발생: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun verifyTokenWithServer() {
        if (!isAdded) return
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val token = prefs.getString(KEY_TOKEN, null)
        Log.d(TAG, "token: ${token}")
        if (token.isNullOrEmpty()) {
            if (isAdded) {
                Toast.makeText(requireContext(), "저장된 토큰이 없습니다. 먼저 로그인해주세요.", Toast.LENGTH_SHORT).show()
            }
            return
        }

        Log.d(TAG, "프로필 요청에 사용할 토큰: $token")
        Log.d(TAG, "자동으로 토큰 검증을 시작합니다...")

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.getProfile()

                if (response.isSuccessful) {
                    val userProfile = response.body()
                    Log.d(TAG, "Profile API Response: $userProfile")
                    Log.d(TAG, "토큰 검증 성공! 사용자 프로필을 확인합니다.")

                    fetchUserProfile()
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e(TAG, "프로필 정보 받기 실패: ${response.code()}, $errorBody")
                    if (isAdded) {
                        Toast.makeText(requireContext(), "토큰 검증 실패 (코드: ${response.code()})", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error during profile verification", e)
                if (isAdded) {
                    Toast.makeText(requireContext(), "프로필 요청 중 오류 발생: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun fetchUserProfile() {
        if (!isAdded) return
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val token = prefs.getString(KEY_TOKEN, null)

        if (token.isNullOrEmpty()) {
            Log.e(TAG, "토큰이 없습니다.")
            return
        }

        Log.d(TAG, "사용자 프로필 정보를 가져옵니다...")

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.getUserProfile()

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
                    if (isAdded) {
                        Toast.makeText(requireContext(), "프로필 정보를 가져올 수 없습니다.", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching user profile", e)
                if (isAdded) {
                    Toast.makeText(requireContext(), "프로필 요청 중 오류 발생: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun isProfileComplete(profile: UserProfile): Boolean {
        val hasBirthDate = !profile.birthDateLunar.isNullOrEmpty() || !profile.birthDateSolar.isNullOrEmpty()
        return !profile.nickname.isNullOrEmpty() &&
                hasBirthDate &&
                !profile.solarOrLunar.isNullOrEmpty() &&
                !profile.birthTimeUnits.isNullOrEmpty() &&
                !profile.gender.isNullOrEmpty()
    }

    private fun navigateToProfileInput() {
        (activity as? AuthContainerActivity)?.showProfileInputFragment()
    }

    private fun updateUI(account: GoogleSignInAccount?) {
        val binding = _binding ?: return
        val isLoggedIn = account != null
        if (isLoggedIn) {
            binding.nameTextView.text = "이름: ${account!!.displayName}"
            binding.emailTextView.text = "이메일: ${account.email}"
            binding.idTextView.text = "고유 ID: ${account.id}"
        }

        binding.signInButton.visibility = if (isLoggedIn) View.GONE else View.VISIBLE
        binding.userInfoLayout.visibility = if (isLoggedIn) View.VISIBLE else View.GONE
        binding.signOutButton.visibility = if (isLoggedIn) View.VISIBLE else View.GONE
    }

    private fun navigateToMain() {
        if (!isAdded) return
        val intent = Intent(requireContext(), MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        activity?.finish()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val TAG = "SignInFragment"
        private const val PREFS_NAME = "fortuna_prefs"
        private const val KEY_TOKEN = "jwt_token"
        private const val REFRESH_TOKEN = "refresh_token"
    }
}
