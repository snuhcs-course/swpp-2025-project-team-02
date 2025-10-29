package com.example.fortuna_android.ui

import android.content.Context
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.navigation.Navigation
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import com.example.fortuna_android.R

@RunWith(AndroidJUnit4::class)
class ProfileEditFragmentInstrumentedTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val prefsName = "fortuna_prefs"

    @Before
    fun setup() {
        context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    @After
    fun tearDown() {
        context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    @Test
    fun testFragmentLaunchesSuccessfully() {
        val scenario = launchFragmentInContainer<ProfileEditFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        scenario.onFragment { fragment ->
            // Set up NavController for the fragment
            val navController = TestNavHostController(context)
            navController.setGraph(R.navigation.nav_graph)
            Navigation.setViewNavController(fragment.requireView(), navController)

            assertNotNull(fragment)
            assertTrue(fragment.isAdded)
        }

        scenario.close()
    }

    @Test
    fun testFragmentViewCreation() {
        val scenario = launchFragmentInContainer<ProfileEditFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        scenario.onFragment { fragment ->
            // Set up NavController for the fragment
            val navController = TestNavHostController(context)
            navController.setGraph(R.navigation.nav_graph)
            Navigation.setViewNavController(fragment.requireView(), navController)

            assertNotNull(fragment.view)
            assertNotNull(fragment.context)
        }

        scenario.close()
    }

    @Test
    fun testFragmentLifecycle() {
        val scenario = launchFragmentInContainer<ProfileEditFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        scenario.onFragment { fragment ->
            // Set up NavController for the fragment
            val navController = TestNavHostController(context)
            navController.setGraph(R.navigation.nav_graph)
            Navigation.setViewNavController(fragment.requireView(), navController)

            assertTrue(fragment.isResumed)
        }

        scenario.close()
    }

    @Test
    fun testFragmentConfigurationChange() {
        val scenario = launchFragmentInContainer<ProfileEditFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        scenario.onFragment { fragment ->
            // Set up NavController for the fragment
            val navController = TestNavHostController(context)
            navController.setGraph(R.navigation.nav_graph)
            Navigation.setViewNavController(fragment.requireView(), navController)
        }

        scenario.recreate()

        scenario.onFragment { fragment ->
            // Set up NavController again after recreation
            val navController = TestNavHostController(context)
            navController.setGraph(R.navigation.nav_graph)
            Navigation.setViewNavController(fragment.requireView(), navController)

            assertNotNull(fragment)
            assertTrue(fragment.isAdded)
            assertNotNull(fragment.view)
        }

        scenario.close()
    }

    @Test
    fun testFragmentWithToken() {
        context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
            .edit()
            .putString("jwt_token", "test_token_12345")
            .commit()

        val scenario = launchFragmentInContainer<ProfileEditFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        scenario.onFragment { fragment ->
            // Set up NavController for the fragment
            val navController = TestNavHostController(context)
            navController.setGraph(R.navigation.nav_graph)
            Navigation.setViewNavController(fragment.requireView(), navController)

            assertNotNull(fragment)
            assertTrue(fragment.isAdded)
        }

        scenario.close()
    }
}
