package com.example.fortuna_android.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.util.Log
import com.example.fortuna_android.api.TodayFortuneData
import com.example.fortuna_android.api.RetrofitClient

class FortuneViewModel : ViewModel() {

    companion object {
        private const val TAG = "FortuneViewModel"
        private const val PREFS_NAME = "fortuna_prefs"
        private const val KEY_TOKEN = "jwt_token"
    }

    // LiveData for fortune result
    private val _fortuneResult = MutableLiveData<String?>()
    val fortuneResult: LiveData<String?> = _fortuneResult

    // LiveData for TodayFortuneData object (for navigation to detail page)
    private val _fortuneData = MutableLiveData<TodayFortuneData?>()
    val fortuneData: LiveData<TodayFortuneData?> = _fortuneData

    // LiveData for loading state
    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    // LiveData for error messages
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    // Coroutine job for fortune generation
    private var fortuneJob: Job? = null

    fun getTodayFortune(context: Context) {
        // Don't start new request if one is already running
        if (_isLoading.value == true) {
            Log.d(TAG, "Fortune generation already in progress")
            return
        }

        // Cancel any existing job
        fortuneJob?.cancel()
        fortuneJob = null

        fortuneJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                // Update loading state
                _isLoading.postValue(true)
                _fortuneResult.postValue(null)
                _errorMessage.postValue(null)

                Log.d(TAG, "Starting background fortune generation...")

                // Get JWT token from SharedPreferences
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val accessToken = prefs.getString(KEY_TOKEN, null)

                if (accessToken.isNullOrEmpty()) {
                    _isLoading.postValue(false)
                    _errorMessage.postValue("Authentication required. Please log in again.")
                    return@launch
                }

                // Network call on IO thread - AuthInterceptor will handle token automatically
                val response = RetrofitClient.instance.getTodayFortune()

                // Process result
                if (response.isSuccessful && response.body() != null) {
                    val fortuneResponse = response.body()!!
                    Log.d(TAG, "Fortune received: ${fortuneResponse.status}")
                    Log.d(TAG, "Fortune Response Data: ${fortuneResponse.data}")
                    Log.d(TAG, "Fortune Score: ${fortuneResponse.data.fortuneScore}")
                    Log.d(TAG, "Elements: ${fortuneResponse.data.fortuneScore.elements}")

                    // Check each pillar
                    fortuneResponse.data.fortuneScore.elements.forEach { (key, pillar) ->
                        if (pillar != null) {
                            Log.d(TAG, "Pillar[$key]: ${pillar.twoLetters}, Stem: ${pillar.stem.koreanName}(${pillar.stem.element}), Branch: ${pillar.branch.koreanName}(${pillar.branch.element})")
                        } else {
                            Log.w(TAG, "Pillar[$key] is null")
                        }
                    }

                    // Check if fortune data is complete
                    val fortune = fortuneResponse.data.fortune
                    if (!fortune.fortuneSummary.isNullOrEmpty()) {
                        // Update success state
                        val fortuneText = "${fortuneResponse.data.forDate}\n${fortune.fortuneSummary}\nOverall Fortune: ${fortune.overallFortune}\n${fortune.specialMessage}"
                        _fortuneResult.postValue(fortuneText)

                        // Post TodayFortuneData for navigation to detail page
                        _fortuneData.postValue(fortuneResponse.data)

                        _isLoading.postValue(false)
                        _errorMessage.postValue(null)
                    } else {
                        // Fortune is pending or incomplete
                        Log.w(TAG, "Fortune data is incomplete or pending")
                        _fortuneResult.postValue(null)
                        _fortuneData.postValue(null)
                        _isLoading.postValue(false)
                        _errorMessage.postValue("Your fortune is still being generated. Please wait until it's ready.")
                    }

                } else {
                    Log.e(TAG, "Fortune request failed: ${response.code()}")

                    // Handle specific error codes
                    val errorMessage = when (response.code()) {
                        404 -> "No fortune available for today. Please take some photos first to generate your fortune."
                        else -> "Failed to get fortune. Server returned ${response.code()}. Please try again."
                    }

                    // Update error state
                    _fortuneResult.postValue(null)
                    _isLoading.postValue(false)
                    _errorMessage.postValue(errorMessage)
                }

            } catch (e: Exception) {
                Log.e(TAG, "Fortune request error: ${e.message}", e)

                // Create detailed error message
                val errorMessage = when {
                    e.message?.contains("ConnectException") == true ||
                    e.message?.contains("UnknownHostException") == true ||
                    e.message?.contains("timeout") == true ->
                        "Cannot connect to server at ${RetrofitClient.getBaseUrl()}. Please check if the server is running."
                    e.message?.contains("SocketTimeoutException") == true ->
                        "Server request timed out. The server might be slow or unavailable."
                    else -> ""
                }

                // Update error state
                _fortuneResult.postValue(null)
                _isLoading.postValue(false)
                _errorMessage.postValue(errorMessage)
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun clearFortune() {
        _fortuneResult.value = null
        _errorMessage.value = null
    }

    fun clearFortuneData() {
        _fortuneData.value = null
    }

    override fun onCleared() {
        super.onCleared()
        fortuneJob?.cancel()
    }
}