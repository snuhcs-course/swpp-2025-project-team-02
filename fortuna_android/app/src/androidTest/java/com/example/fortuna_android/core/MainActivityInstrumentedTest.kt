package com.example.fortuna_android.core

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.fortuna_android.MainActivity
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityInstrumentedTest {

    @Test
    fun launchMainActivity_doesNotCrash() {
        ActivityScenario.launch(MainActivity::class.java).close()
    }
}
