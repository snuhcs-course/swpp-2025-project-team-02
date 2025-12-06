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
import kotlinx.coroutines.delay
import android.util.Log
import com.example.fortuna_android.api.TodayFortuneData
import com.example.fortuna_android.api.RetrofitClient
import com.example.fortuna_android.api.UserProfile
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FortuneViewModel : ViewModel() {

    companion object {
        private const val TAG = "FortuneViewModel"
        private const val PREFS_NAME = "fortuna_prefs"
        private const val KEY_TOKEN = "jwt_token"
        private const val POLLING_INTERVAL_MS = 30000L // 30 seconds
        private const val MAX_POLLING_ATTEMPTS = 20 // 최대 10분 (30초 x 20)
        private const val AI_GENERATING_MESSAGE = "AI가 당신의 사주와 오늘의 기운을 분석하고 있습니다."

        /** 현재 기기 날짜를 yyyy-MM-dd 형식으로 반환 */
        fun getTodayDateString(): String {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            return dateFormat.format(Date())
        }
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

    // LiveData for user profile (사주팔자 data)
    private val _userProfile = MutableLiveData<UserProfile?>()
    val userProfile: LiveData<UserProfile?> = _userProfile

    // LiveData for AI generation status message
    private val _generatingMessage = MutableLiveData<String?>()
    val generatingMessage: LiveData<String?> = _generatingMessage

    // Coroutine job for fortune generation
    private var fortuneJob: Job? = null

    // Coroutine job for polling
    private var pollingJob: Job? = null

    // Polling attempt counter
    private var pollingAttempts = 0

    fun getTodayFortune(context: Context, forceRefresh: Boolean = false) {
        // Don't start new request if one is already running (unless forcing refresh)
        if (!forceRefresh && _isLoading.value == true) {
            Log.d(TAG, "Fortune generation already in progress")
            return
        }

        // If data already exists and not forcing refresh, skip API call
        if (!forceRefresh && _fortuneData.value != null) {
            Log.d(TAG, "Fortune data already loaded, skipping API call")
            return
        }

        // Stop any ongoing polling when starting new request
        stopPolling()

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
                val todayDate = getTodayDateString()
                Log.d(TAG, "Fetching fortune for date: $todayDate")
                val response = RetrofitClient.instance.getTodayFortune(todayDate)

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

                    // Check if AI is generating (special message)
                    val isAiGenerating = fortune.todayElementBalanceDescription?.contains(AI_GENERATING_MESSAGE) == true

                    if (!fortune.todayDailyGuidance.isNullOrEmpty() &&
                        !isAiGenerating &&
                        !fortuneResponse.data.fortuneImageUrl.isNullOrBlank()) {
                        // Fortune is complete including image - stop polling
                        Log.d(TAG, "Fortune generation completed with image!")
                        stopPolling()

                        // Update success state
                        val fortuneScore = (fortuneResponse.data.fortuneScore.entropyScore / 100.0).toInt()
                        val fortuneText = "${fortuneResponse.data.forDate}\nOverall Fortune: $fortuneScore\n\n${fortune.todayElementBalanceDescription}\n\n${fortune.todayDailyGuidance}"
                        _fortuneResult.postValue(fortuneText)

                        // Post TodayFortuneData for navigation to detail page
                        _fortuneData.postValue(fortuneResponse.data)

                        _isLoading.postValue(false)
                        _errorMessage.postValue(null)
                        _generatingMessage.postValue(null)
                    } else if (isAiGenerating) {
                        // AI is generating - show card with generating message
                        Log.w(TAG, "AI is generating fortune... Starting polling")

                        // Post the fortune data even though AI is generating
                        // This will show the card with "AI is generating..." message
                        _fortuneData.postValue(fortuneResponse.data)

                        _fortuneResult.postValue(null)
                        _isLoading.postValue(false)  // Show card, not loading spinner
                        _errorMessage.postValue(null)
                        _generatingMessage.postValue(fortune.todayElementBalanceDescription)

                        // Start polling if not already started
                        startPolling(context)
                    } else {
                        // Fortune is pending or incomplete (other reasons)
                        Log.w(TAG, "Fortune data is incomplete or pending")
                        _fortuneResult.postValue(null)
                        _fortuneData.postValue(null)
                        _isLoading.postValue(false)
                        _errorMessage.postValue("현재 API 사용량 관리를 위해 모든 계정에 대한 일일 자동 사주운세 및 이미지 생성을 제한해 두었습니다.\n프로필 변경시 사주운세가 다시 제공됩니다.")
                        _generatingMessage.postValue(null)
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

    fun loadUserProfile(forceRefresh: Boolean = false) {
        // If user profile already loaded and not forcing refresh, skip API call
        if (!forceRefresh && _userProfile.value != null) {
            Log.d(TAG, "User profile already loaded, skipping API call")
            return
        }

        Log.d(TAG, "Loading user profile... (forceRefresh=$forceRefresh)")

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.instance.getUserProfile()

                if (response.isSuccessful && response.body() != null) {
                    _userProfile.postValue(response.body())
                    Log.d(TAG, "User profile loaded successfully")
                } else {
                    Log.e(TAG, "Failed to load user profile: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading user profile", e)
            }
        }
    }

    /**
     * Refresh user profile by forcing a reload from server
     */
    fun refreshUserProfile() {
        Log.d(TAG, "Refreshing user profile...")
        loadUserProfile(forceRefresh = true)
    }

    /**
     * Refresh fortune data by forcing a reload from server
     */
    fun refreshFortuneData(context: Context) {
        Log.d(TAG, "Refreshing fortune data...")
        getTodayFortune(context, forceRefresh = true)
    }

    /**
     * Clear all cached data - useful when profile is updated and fortune needs regeneration
     */
    fun clearAllData() {
        Log.d(TAG, "Clearing all cached data")
        stopPolling() // Stop any ongoing polling
        _userProfile.value = null
        _fortuneData.value = null
        _fortuneResult.value = null
        _errorMessage.value = null
        _generatingMessage.value = null
    }

    /**
     * Start polling for fortune data every 30 seconds
     */
    private fun startPolling(context: Context) {
        // If polling is already running, don't start another
        if (pollingJob?.isActive == true) {
            Log.d(TAG, "Polling already active, skipping start")
            return
        }

        // Reset counter
        pollingAttempts = 0

        Log.d(TAG, "Starting fortune polling (interval: ${POLLING_INTERVAL_MS}ms, max attempts: $MAX_POLLING_ATTEMPTS)")

        pollingJob = viewModelScope.launch(Dispatchers.IO) {
            while (pollingAttempts < MAX_POLLING_ATTEMPTS) {
                // Wait before next poll
                delay(POLLING_INTERVAL_MS)

                pollingAttempts++
                Log.d(TAG, "Polling attempt $pollingAttempts/$MAX_POLLING_ATTEMPTS")

                try {
                    // Fetch fortune data again
                    val todayDate = getTodayDateString()
                    val response = RetrofitClient.instance.getTodayFortune(todayDate)

                    if (response.isSuccessful && response.body() != null) {
                        val fortuneResponse = response.body()!!
                        val fortune = fortuneResponse.data.fortune

                        // Check if AI is still generating
                        val isAiGenerating = fortune.todayElementBalanceDescription?.contains(AI_GENERATING_MESSAGE) == true

                        if (!fortune.todayDailyGuidance.isNullOrEmpty() &&
                            !isAiGenerating &&
                            !fortuneResponse.data.fortuneImageUrl.isNullOrBlank()) {
                            // Fortune is complete including image!
                            Log.d(TAG, "Fortune generation completed during polling with image!")

                            // Update success state
                            val fortuneScore = (fortuneResponse.data.fortuneScore.entropyScore / 100.0).toInt()
                            val fortuneText = "${fortuneResponse.data.forDate}\nOverall Fortune: $fortuneScore\n\n${fortune.todayElementBalanceDescription}\n\n${fortune.todayDailyGuidance}"
                            _fortuneResult.postValue(fortuneText)

                            // Post TodayFortuneData - this will update the card with complete fortune
                            _fortuneData.postValue(fortuneResponse.data)

                            _isLoading.postValue(false)
                            _errorMessage.postValue(null)
                            _generatingMessage.postValue(null)

                            // Stop polling
                            break
                        } else if (isAiGenerating) {
                            Log.d(TAG, "Fortune still generating, continuing to poll...")

                            // Update the fortune data even during generation
                            // This ensures the card shows the latest "generating..." message
                            _fortuneData.postValue(fortuneResponse.data)

                            // Update generating message if changed
                            if (fortune.todayElementBalanceDescription != null) {
                                _generatingMessage.postValue(fortune.todayElementBalanceDescription)
                            }
                        } else {
                            Log.d(TAG, "Fortune in unknown state, continuing to poll...")
                        }
                    } else {
                        Log.w(TAG, "Polling request failed: ${response.code()}")
                    }

                } catch (e: Exception) {
                    Log.e(TAG, "Error during polling: ${e.message}", e)
                }
            }

            // If we exhausted all attempts
            if (pollingAttempts >= MAX_POLLING_ATTEMPTS) {
                Log.w(TAG, "Polling stopped: Maximum attempts reached")
                _isLoading.postValue(false)
                _errorMessage.postValue("운세 생성이 지연되고 있습니다. 잠시 후 다시 시도해주세요.")
                _generatingMessage.postValue(null)
            }
        }
    }

    /**
     * Stop polling for fortune data
     */
    private fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
        pollingAttempts = 0
        Log.d(TAG, "Polling stopped")
    }

    override fun onCleared() {
        super.onCleared()
        fortuneJob?.cancel()
        stopPolling()
    }
}