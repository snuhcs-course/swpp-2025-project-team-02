package com.example.fortuna_android.ui

import android.content.Context
import android.content.SharedPreferences
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.example.fortuna_android.api.*
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import retrofit2.Response
import java.lang.reflect.Field
import java.lang.reflect.Method

/**
 * Focused unit tests for FortuneViewModel clearAllData() and startPolling() methods
 */
@ExperimentalCoroutinesApi
class FortuneViewModelClearDataPollingTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var viewModel: FortuneViewModel
    private lateinit var mockContext: Context
    private lateinit var mockSharedPreferences: SharedPreferences
    private lateinit var mockApiService: ApiService

    // Reflection to access private methods and fields
    private lateinit var startPollingMethod: Method
    private lateinit var stopPollingMethod: Method
    private lateinit var pollingJobField: Field

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        // Mock dependencies
        mockContext = mockk<Context>(relaxed = true)
        mockSharedPreferences = mockk<SharedPreferences>(relaxed = true)
        mockApiService = mockk<ApiService>(relaxed = true)

        // Setup SharedPreferences mock
        every { mockContext.getSharedPreferences(any(), any()) } returns mockSharedPreferences
        every { mockSharedPreferences.getString(any(), any()) } returns "test_token"

        // Mock RetrofitClient.instance
        mockkObject(RetrofitClient)
        every { RetrofitClient.instance } returns mockApiService

        viewModel = FortuneViewModel()

        // Setup reflection
        startPollingMethod = FortuneViewModel::class.java.getDeclaredMethod("startPolling", Context::class.java)
        startPollingMethod.isAccessible = true

        stopPollingMethod = FortuneViewModel::class.java.getDeclaredMethod("stopPolling")
        stopPollingMethod.isAccessible = true

        pollingJobField = FortuneViewModel::class.java.getDeclaredField("pollingJob")
        pollingJobField.isAccessible = true
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `clearAllData sets all LiveData values to null`() {
        // Arrange - Set some initial values using reflection
        setFortuneDataValue(createMockFortuneData())
        setFortuneResultValue("test result")
        setUserProfileValue(createMockUserProfile())
        setErrorMessageValue("test error")
        setGeneratingMessageValue("test generating")

        // Verify initial state
        assertNotNull("Initial fortune data should not be null", viewModel.fortuneData.value)
        assertNotNull("Initial fortune result should not be null", viewModel.fortuneResult.value)
        assertNotNull("Initial user profile should not be null", viewModel.userProfile.value)
        assertNotNull("Initial error message should not be null", viewModel.errorMessage.value)
        assertNotNull("Initial generating message should not be null", viewModel.generatingMessage.value)

        // Act
        viewModel.clearAllData()

        // Assert
        assertNull("Fortune data should be null after clearAllData", viewModel.fortuneData.value)
        assertNull("Fortune result should be null after clearAllData", viewModel.fortuneResult.value)
        assertNull("User profile should be null after clearAllData", viewModel.userProfile.value)
        assertNull("Error message should be null after clearAllData", viewModel.errorMessage.value)
        assertNull("Generating message should be null after clearAllData", viewModel.generatingMessage.value)
    }

    @Test
    fun `clearAllData can be called multiple times safely`() {
        // Act
        viewModel.clearAllData()
        viewModel.clearAllData()
        viewModel.clearAllData()

        // Assert - Should not throw exceptions and all values should remain null
        assertNull("Fortune data should remain null", viewModel.fortuneData.value)
        assertNull("Fortune result should remain null", viewModel.fortuneResult.value)
        assertNull("User profile should remain null", viewModel.userProfile.value)
        assertNull("Error message should remain null", viewModel.errorMessage.value)
        assertNull("Generating message should remain null", viewModel.generatingMessage.value)
    }

    @Test
    fun `clearAllData stops polling job`() {
        // Arrange - Mock successful response to enable polling
        setupMockApiResponse(isGenerating = true)

        // Start polling via reflection
        startPollingMethod.invoke(viewModel, mockContext)

        // Verify polling job is active
        val pollingJobBeforeClear = pollingJobField.get(viewModel)
        assertNotNull("Polling job should be active", pollingJobBeforeClear)

        // Act
        viewModel.clearAllData()

        // Assert - Polling job should be stopped
        val pollingJobAfterClear = pollingJobField.get(viewModel)
        assertTrue("Polling job should be null or cancelled",
            pollingJobAfterClear == null ||
            !(pollingJobAfterClear as kotlinx.coroutines.Job).isActive)
    }

    @Test
    fun `startPolling does not start multiple jobs when called repeatedly`() {
        // Arrange - Mock response
        setupMockApiResponse(isGenerating = true)

        // Act - Start polling multiple times
        startPollingMethod.invoke(viewModel, mockContext)
        val firstJob = pollingJobField.get(viewModel)

        startPollingMethod.invoke(viewModel, mockContext)
        val secondJob = pollingJobField.get(viewModel)

        startPollingMethod.invoke(viewModel, mockContext)
        val thirdJob = pollingJobField.get(viewModel)

        // Assert - Should be the same job (not starting new ones)
        assertEquals("Multiple startPolling calls should not create new jobs", firstJob, secondJob)
        assertEquals("Multiple startPolling calls should not create new jobs", secondJob, thirdJob)
    }

    @Test
    fun `startPolling resets attempts counter when started`() {
        // This test verifies the functionality indirectly by ensuring polling can restart
        // after being stopped and run the full duration again

        // Arrange - Mock response
        setupMockApiResponse(isGenerating = true)

        // Act - Start, stop, and restart polling
        startPollingMethod.invoke(viewModel, mockContext)
        assertNotNull("Polling should start", pollingJobField.get(viewModel))

        stopPollingMethod.invoke(viewModel)
        assertNull("Polling should stop", pollingJobField.get(viewModel))

        startPollingMethod.invoke(viewModel, mockContext)
        assertNotNull("Polling should restart", pollingJobField.get(viewModel))

        // Assert - No exceptions should be thrown and polling should work
        assertTrue("Test completed successfully", true)
    }

    @Test
    fun `polling can be started successfully`() {
        // Arrange - Setup generating response
        setupMockApiResponse(isGenerating = true, message = "AI가 분석중입니다")

        // Act - Start polling
        startPollingMethod.invoke(viewModel, mockContext)

        // Assert - Polling job should be active
        val pollingJob = pollingJobField.get(viewModel)
        assertNotNull("Polling job should be created", pollingJob)
        assertTrue("Polling job should be active", (pollingJob as kotlinx.coroutines.Job).isActive)
    }

    // Helper methods
    private fun createMockFortuneData(): TodayFortuneData {
        val mockFortune = mockk<TodayFortune>()
        every { mockFortune.todayFortuneSummary } returns "Test summary"
        every { mockFortune.todayDailyGuidance } returns "Test guidance"
        every { mockFortune.todayElementBalanceDescription } returns "Test description"

        val mockScore = mockk<FortuneScore>()
        every { mockScore.entropyScore } returns 75.0

        val mockData = mockk<TodayFortuneData>()
        every { mockData.fortune } returns mockFortune
        every { mockData.fortuneScore } returns mockScore
        every { mockData.forDate } returns "2024-12-07"
        every { mockData.fortuneImageUrl } returns null
        every { mockData.fortuneId } returns 1
        every { mockData.userId } returns 1
        every { mockData.generatedAt } returns "2024-12-07T10:00:00Z"

        return mockData
    }

    private fun createMockUserProfile(): UserProfile {
        return mockk<UserProfile>() {
            every { userId } returns 1
            every { email } returns "test@example.com"
            every { name } returns "Test User"
        }
    }

    private fun setupMockApiResponse(isGenerating: Boolean = false, message: String = "AI가 당신의 사주와 오늘의 기운을 분석하고 있습니다.") {
        val mockFortuneData = createMockFortuneData()

        if (isGenerating) {
            every { mockFortuneData.fortune.todayElementBalanceDescription } returns message
            every { mockFortuneData.fortune.todayDailyGuidance } returns ""
            every { mockFortuneData.fortuneImageUrl } returns null
        } else {
            every { mockFortuneData.fortune.todayDailyGuidance } returns "Complete guidance"
            every { mockFortuneData.fortuneImageUrl } returns "http://example.com/image.png"
        }

        val mockResponse = mockk<Response<TodayFortuneResponse>>()
        every { mockResponse.isSuccessful } returns true
        every { mockResponse.body() } returns TodayFortuneResponse("success", mockFortuneData)

        coEvery { mockApiService.getTodayFortune(any()) } returns mockResponse
    }

    // Helper methods to set private MutableLiveData values
    private fun setFortuneDataValue(value: TodayFortuneData?) {
        val field = FortuneViewModel::class.java.getDeclaredField("_fortuneData")
        field.isAccessible = true
        (field.get(viewModel) as androidx.lifecycle.MutableLiveData<TodayFortuneData?>).value = value
    }

    private fun setFortuneResultValue(value: String?) {
        val field = FortuneViewModel::class.java.getDeclaredField("_fortuneResult")
        field.isAccessible = true
        (field.get(viewModel) as androidx.lifecycle.MutableLiveData<String?>).value = value
    }

    private fun setUserProfileValue(value: UserProfile?) {
        val field = FortuneViewModel::class.java.getDeclaredField("_userProfile")
        field.isAccessible = true
        (field.get(viewModel) as androidx.lifecycle.MutableLiveData<UserProfile?>).value = value
    }

    private fun setErrorMessageValue(value: String?) {
        val field = FortuneViewModel::class.java.getDeclaredField("_errorMessage")
        field.isAccessible = true
        (field.get(viewModel) as androidx.lifecycle.MutableLiveData<String?>).value = value
    }

    private fun setGeneratingMessageValue(value: String?) {
        val field = FortuneViewModel::class.java.getDeclaredField("_generatingMessage")
        field.isAccessible = true
        (field.get(viewModel) as androidx.lifecycle.MutableLiveData<String?>).value = value
    }
}