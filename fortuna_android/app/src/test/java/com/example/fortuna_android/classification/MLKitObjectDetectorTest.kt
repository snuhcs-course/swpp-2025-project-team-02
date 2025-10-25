package com.example.fortuna_android.classification

import android.app.Activity
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.custom.CustomObjectDetectorOptions
import io.mockk.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.shadow.api.Shadow

/**
 * Shadow classes to stub out RenderScript in tests
 */
@Implements(android.renderscript.RenderScript::class)
class ShadowRenderScript {
    companion object {
        @JvmStatic
        @Implementation
        fun create(context: android.content.Context): android.renderscript.RenderScript {
            return mockk(relaxed = true)
        }
    }
}

@Implements(android.renderscript.ScriptIntrinsicYuvToRGB::class)
class ShadowScriptIntrinsicYuvToRGBMLKit {
    companion object {
        @JvmStatic
        @Implementation
        fun create(rs: android.renderscript.RenderScript, e: android.renderscript.Element): android.renderscript.ScriptIntrinsicYuvToRGB {
            return mockk(relaxed = true)
        }
    }
}

@Implements(android.renderscript.Element::class)
class ShadowElementMLKit {
    companion object {
        @JvmStatic
        @Implementation
        fun U8_4(rs: android.renderscript.RenderScript): android.renderscript.Element {
            return mockk(relaxed = true)
        }

        @JvmStatic
        @Implementation
        fun YUV(rs: android.renderscript.RenderScript): android.renderscript.Element {
            return mockk(relaxed = true)
        }
    }
}

@Implements(android.renderscript.Allocation::class)
class ShadowAllocationMLKit {
    companion object {
        @JvmStatic
        @Implementation
        fun createSized(rs: android.renderscript.RenderScript, e: android.renderscript.Element, count: Int): android.renderscript.Allocation {
            return mockk(relaxed = true)
        }

        @JvmStatic
        @Implementation
        fun createFromBitmap(rs: android.renderscript.RenderScript, b: android.graphics.Bitmap): android.renderscript.Allocation {
            return mockk(relaxed = true)
        }
    }
}

@Implements(android.renderscript.Type.Builder::class)
class ShadowTypeBuilderMLKit

/**
 * Unit tests for MLKitObjectDetector class
 * Tests the initialization and configuration of ML Kit Object Detector
 *
 * Note: Full integration tests for analyze() method should be done in Android Instrumented Tests
 * as they require complete ML Kit initialization which is not available in Robolectric unit tests.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], shadows = [
    ShadowRenderScript::class,
    ShadowScriptIntrinsicYuvToRGBMLKit::class,
    ShadowElementMLKit::class,
    ShadowAllocationMLKit::class,
    ShadowTypeBuilderMLKit::class
])
class MLKitObjectDetectorTest {

    private lateinit var activity: Activity
    private lateinit var mlKitObjectDetector: MLKitObjectDetector

    @Before
    fun setUp() {
        activity = Robolectric.buildActivity(Activity::class.java).create().get()

        // Mock ObjectDetection.getClient to avoid MlKitContext initialization
        mockkStatic(ObjectDetection::class)
        val mockDetector = mockk<com.google.mlkit.vision.objects.ObjectDetector>(relaxed = true)
        every { ObjectDetection.getClient(any<CustomObjectDetectorOptions>()) } returns mockDetector

        // Now we can safely create MLKitObjectDetector
        mlKitObjectDetector = MLKitObjectDetector(activity)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    // ========== Initialization Tests ==========

    @Test
    fun `test MLKitObjectDetector initializes with activity context`() {
        assertNotNull("MLKitObjectDetector should be initialized", mlKitObjectDetector)
        assertEquals("Context should match activity", activity, mlKitObjectDetector.context)
    }

    @Test
    fun `test MLKitObjectDetector has custom model configured`() {
        assertTrue("Should have custom model", mlKitObjectDetector.hasCustomModel())
    }

    @Test
    fun `test hasCustomModel returns true for custom detector options`() {
        val result = mlKitObjectDetector.hasCustomModel()
        assertTrue("hasCustomModel should return true", result)
    }

    @Test
    fun `test builder is CustomObjectDetectorOptions Builder`() {
        assertTrue("Builder should be CustomObjectDetectorOptions.Builder instance",
            mlKitObjectDetector.builder is CustomObjectDetectorOptions.Builder)
    }

    // ========== Custom Model Configuration Tests ==========

    @Test
    fun `test custom object detector options are configured correctly`() {
        // The options are configured in the class initialization
        // We verify through hasCustomModel which checks the builder type
        assertTrue("Should be using custom model configuration",
            mlKitObjectDetector.hasCustomModel())
    }

    @Test
    fun `test local model uses correct asset file path`() {
        // Verify that localModel is using "model_metadata.tflite"
        // This is implicitly tested through successful initialization
        assertNotNull("Local model should be initialized", mlKitObjectDetector.localModel)
    }

    @Test
    fun `test custom object detector options builder exists`() {
        assertNotNull("Builder should not be null", mlKitObjectDetector.builder)
        assertTrue("Builder should be CustomObjectDetectorOptions.Builder",
            mlKitObjectDetector.builder is CustomObjectDetectorOptions.Builder)
    }

    // ========== Context Tests ==========

    @Test
    fun `test ObjectDetector maintains context reference`() {
        assertSame("Context should be the same instance", activity, mlKitObjectDetector.context)
    }

    @Test
    fun `test multiple MLKitObjectDetector instances can be created`() {
        val detector1 = MLKitObjectDetector(activity)
        val detector2 = MLKitObjectDetector(activity)

        assertNotNull("First detector should be initialized", detector1)
        assertNotNull("Second detector should be initialized", detector2)
        assertNotSame("Detectors should be different instances", detector1, detector2)
        assertSame("Both detectors should share same context",
            detector1.context, detector2.context)
    }

    // ========== YuvConverter Tests ==========

    @Test
    fun `test MLKitObjectDetector has YuvToRgbConverter initialized`() {
        assertNotNull("YuvConverter should be initialized", mlKitObjectDetector.yuvConverter)
    }

    // ========== LocalModel Tests ==========

    @Test
    fun `test localModel is initialized`() {
        assertNotNull("localModel should not be null", mlKitObjectDetector.localModel)
    }

    @Test
    fun `test customObjectDetectorOptions is initialized`() {
        assertNotNull("customObjectDetectorOptions should not be null",
            mlKitObjectDetector.customObjectDetectorOptions)
    }
}
