package com.example.fortuna_android

import android.app.Activity
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
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : AppCompatActivity() {

    private lateinit var mGoogleSignInClient: GoogleSignInClient

    // UI 요소
    private lateinit var signInButton: SignInButton
    private lateinit var signOutButton: Button
    private lateinit var nameTextView: TextView
    private lateinit var emailTextView: TextView
    private lateinit var idTextView: TextView
    private lateinit var userInfoLayout: LinearLayout

    // Google 로그인 결과를 처리하기 위한 ActivityResultLauncher
    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            handleSignInResult(task)
        } else {
            // 사용자가 로그인을 취소했거나 다른 이유로 실패한 경우
            Toast.makeText(this, "로그인이 취소되었습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main) // XML 레이아웃 설정

        // UI 요소 초기화
        signInButton = findViewById(R.id.sign_in_button)
        signOutButton = findViewById(R.id.sign_out_button)
        nameTextView = findViewById(R.id.name_text_view)
        emailTextView = findViewById(R.id.email_text_view)
        idTextView = findViewById(R.id.id_text_view)
        userInfoLayout = findViewById(R.id.user_info_layout)


        // 1. Google 로그인 옵션 설정 (GoogleSignInOptions)
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("46186834187-3jaqipe6int3a9m213t9ks2ceac6gcrp.apps.googleusercontent.com") // 여기에 웹 클라이언트 ID를 입력하세요.
            .requestEmail()
            .build()

        // 2. GoogleSignInClient 객체 생성
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso)

        // 3. 버튼 클릭 리스너 설정
        signInButton.setOnClickListener { signIn() }
        signOutButton.setOnClickListener { signOut() }
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
            Toast.makeText(this, "로그아웃 되었습니다.", Toast.LENGTH_SHORT).show()
            updateUI(null)
        }
    }

    // 로그인 결과 처리 로직
    private fun handleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            val account = completedTask.getResult(ApiException::class.java)

            // 로그인 성공
            Toast.makeText(this, "Google 로그인 성공!", Toast.LENGTH_SHORT).show()

            // ID 토큰 확인
            val idToken = account.idToken
            if (idToken != null) {
                Log.d(TAG, "Got ID Token: $idToken")
                // 백엔드 서버에 ID 토큰을 보내 인증을 요청합니다.
                sendTokenToServer(idToken)
            } else {
                Log.w(TAG, "ID Token is null")
                Toast.makeText(this, "ID 토큰을 가져올 수 없습니다.", Toast.LENGTH_SHORT).show()
            }

            updateUI(account) // UI는 우선 Google 계정 정보로 업데이트합니다.
        } catch (e: ApiException) {
            // 로그인 실패
            Log.w(TAG, "signInResult:failed code=" + e.statusCode)
            Toast.makeText(this, "로그인에 실패했습니다. (코드: ${e.statusCode})", Toast.LENGTH_LONG).show()
            updateUI(null)
        }
    }

    // 백엔드 서버로 토큰을 전송하는 함수
    private fun sendTokenToServer(idToken: String) {
        // 코루틴을 사용하여 백그라운드에서 네트워크 작업을 수행합니다.
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val url = URL("http://localhost:8000/api/user/auth/google/") // API 엔드포인트
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json; utf-8")
                connection.setRequestProperty("Accept", "application/json")
                connection.doOutput = true

                // JSON 요청 본문 생성
                val jsonObject = JSONObject()
                jsonObject.put("id_token", idToken)
                val jsonInputString = jsonObject.toString()

                // 요청 본문 전송
                OutputStreamWriter(connection.outputStream, "UTF-8").use { writer ->
                    writer.write(jsonInputString)
                    writer.flush()
                }

                val responseCode = connection.responseCode
                Log.d(TAG, "Server Response Code: $responseCode")

                // 응답 읽기
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val reader = BufferedReader(InputStreamReader(connection.inputStream, "UTF-8"))
                    val response = StringBuilder()
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        response.append(line)
                    }
                    reader.close()

                    Log.d(TAG, "Server Response: $response")
                    val responseJson = JSONObject(response.toString())
                    val backendToken = responseJson.getString("token")
                    val username = responseJson.getString("username")

                    // UI 스레드에서 Toast 메시지 표시 및 로그 출력
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@MainActivity, "'$username'님, 서버 로그인 성공!", Toast.LENGTH_LONG).show()
                        // TODO: 받은 backendToken을 SharedPreferences 등에 저장하여
                        //       앱 내 다른 API 요청 시 인증 헤더에 사용하세요.
                        Log.d(TAG, "Backend Token: $backendToken")
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@MainActivity, "서버 인증 실패 (코드: $responseCode)", Toast.LENGTH_LONG).show()
                    }
                }
                connection.disconnect()
            } catch (e: Exception) {
                Log.e(TAG, "Error sending token to server", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "서버 통신 중 오류가 발생했습니다.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // 로그인 상태에 따라 UI를 업데이트하는 메소드
    private fun updateUI(account: GoogleSignInAccount?) {
        if (account != null) {
            nameTextView.text = "이름: ${account.displayName}"
            emailTextView.text = "이메일: ${account.email}"
            idTextView.text = "고유 ID: ${account.id}"

            signInButton.visibility = View.GONE
            userInfoLayout.visibility = View.VISIBLE
            signOutButton.visibility = View.VISIBLE
        } else {
            nameTextView.text = ""
            emailTextView.text = ""
            idTextView.text = ""

            signInButton.visibility = View.VISIBLE
            userInfoLayout.visibility = View.GONE
            signOutButton.visibility = View.GONE
        }
    }

    companion object {
        private const val TAG = "GoogleSignIn"
    }
}
