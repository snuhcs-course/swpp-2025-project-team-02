package com.example.fortuna_android.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.util.Log
import com.example.fortuna_android.api.RetrofitClient
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FortuneViewModel : ViewModel() {

    companion object {
        private const val TAG = "FortuneViewModel"
    }

    // LiveData for fortune result
    private val _fortuneResult = MutableLiveData<String?>()
    val fortuneResult: LiveData<String?> = _fortuneResult

    // LiveData for loading state
    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    // LiveData for error messages
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    // Coroutine job for fortune generation
    private var fortuneJob: Job? = null

    fun getFortune(includePhotos: Boolean = true) {
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

                // Prepare request parameters
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val currentDate = dateFormat.format(Date())

                Log.d(TAG, "Fortune request parameters: date=$currentDate, includePhotos=$includePhotos")

                // Network call on IO thread
                val response = RetrofitClient.instance.getFortune(currentDate, includePhotos)

                // Process result
                if (response.isSuccessful && response.body() != null) {
                    val fortuneResponse = response.body()!!
                    Log.d(TAG, "Fortune received: ${fortuneResponse.status}")

                    // Update success state
                    _fortuneResult.postValue(fortuneResponse.data.fortune.fortuneSummary)
                    _isLoading.postValue(false)
                    _errorMessage.postValue(null)

                } else {
                    Log.e(TAG, "Fortune request failed: ${response.code()}")

                    // Update error state
                    _fortuneResult.postValue(null)
                    _isLoading.postValue(false)
                    _errorMessage.postValue("Failed to get fortune. Server returned ${response.code()}. Please try again.")
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
                    else -> "Network error: ${e.message ?: "Unknown error"}"
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

    override fun onCleared() {
        super.onCleared()
        fortuneJob?.cancel()
    }
}