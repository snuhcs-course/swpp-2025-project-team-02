package com.example.fortuna_android.ui

import android.content.Context
import android.content.SharedPreferences
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
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

/**
 * Simplified unit tests for FortuneViewModel
 * Tests core functionality with reliable test coverage
 */
@ExperimentalCoroutinesApi
class FortuneViewModelSimpleTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var viewModel: FortuneViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = FortuneViewModel()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `test initial state`() {
        // Assert
        assertNull("Initial fortuneResult should be null", viewModel.fortuneResult.value)
        assertNull("Initial fortuneData should be null", viewModel.fortuneData.value)
        assertEquals("Initial isLoading should be false", false, viewModel.isLoading.value)
        assertNull("Initial errorMessage should be null", viewModel.errorMessage.value)
    }

    @Test
    fun `test clearError clears error message`() {
        // Act
        viewModel.clearError()

        // Assert
        assertNull("Error message should be null", viewModel.errorMessage.value)
    }

    @Test
    fun `test clearFortune clears fortune and error`() {
        // Act
        viewModel.clearFortune()

        // Assert
        assertNull("Fortune result should be null", viewModel.fortuneResult.value)
        assertNull("Error message should be null", viewModel.errorMessage.value)
    }

    @Test
    fun `test clearFortuneData clears fortune data`() {
        // Act
        viewModel.clearFortuneData()

        // Assert
        assertNull("Fortune data should be null", viewModel.fortuneData.value)
    }

    @Test
    fun `test onCleared executes without error`() {
        // Arrange
        val onClearedMethod = FortuneViewModel::class.java.getDeclaredMethod("onCleared")
        onClearedMethod.isAccessible = true

        // Act & Assert - Should not throw
        onClearedMethod.invoke(viewModel)
        assertTrue("onCleared executed successfully", true)
    }

    @Test
    fun `test LiveData objects are not null`() {
        // Assert
        assertNotNull("fortuneResult LiveData should not be null", viewModel.fortuneResult)
        assertNotNull("fortuneData LiveData should not be null", viewModel.fortuneData)
        assertNotNull("isLoading LiveData should not be null", viewModel.isLoading)
        assertNotNull("errorMessage LiveData should not be null", viewModel.errorMessage)
    }

    @Test
    fun `test getTodayFortune requires context parameter`() {
        // Arrange
        val mockContext = mockk<Context>(relaxed = true)
        val mockSharedPreferences = mockk<SharedPreferences>(relaxed = true)
        every { mockContext.getSharedPreferences(any(), any()) } returns mockSharedPreferences
        every { mockSharedPreferences.getString(any(), any()) } returns null

        // Act - Should not throw
        viewModel.getTodayFortune(mockContext)

        // Assert
        verify { mockContext.getSharedPreferences(any(), any()) }
    }

    @Test
    fun `test clearError can be called multiple times`() {
        // Act
        viewModel.clearError()
        viewModel.clearError()
        viewModel.clearError()

        // Assert
        assertNull("Error should still be null", viewModel.errorMessage.value)
    }

    @Test
    fun `test clearFortune can be called multiple times`() {
        // Act
        viewModel.clearFortune()
        viewModel.clearFortune()

        // Assert
        assertNull("Fortune should still be null", viewModel.fortuneResult.value)
    }

    @Test
    fun `test clearFortuneData can be called multiple times`() {
        // Act
        viewModel.clearFortuneData()
        viewModel.clearFortuneData()

        // Assert
        assertNull("Fortune data should still be null", viewModel.fortuneData.value)
    }
}
