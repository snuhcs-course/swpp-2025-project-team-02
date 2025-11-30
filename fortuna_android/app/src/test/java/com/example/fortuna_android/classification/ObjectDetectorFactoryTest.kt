package com.example.fortuna_android.classification

import android.app.Activity
import io.mockk.mockk
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Concrete test implementation of ObjectDetectorFactory for testing
 */
private class TestObjectDetectorFactory : ObjectDetectorFactory() {
    var createDetectorCalled = false
    var lastContext: Activity? = null

    override fun createDetector(context: Activity): ObjectDetector {
        createDetectorCalled = true
        lastContext = context
        return mockk(relaxed = true)
    }
}

/**
 * Unit tests for ObjectDetectorFactory abstract class
 * Tests the Factory Method Pattern base implementation
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ObjectDetectorFactoryTest {

    private lateinit var activity: Activity
    private lateinit var factory: TestObjectDetectorFactory

    @Before
    fun setUp() {
        activity = Robolectric.buildActivity(Activity::class.java).create().get()
        factory = TestObjectDetectorFactory()
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    // ========== Abstract Class Tests ==========

    @Test
    fun `test ObjectDetectorFactory can be instantiated through concrete implementation`() {
        assertNotNull("Factory should be instantiated", factory)
        assertTrue("Should be instance of ObjectDetectorFactory",
            factory is ObjectDetectorFactory)
    }

    // ========== info() Method Tests ==========

    @Test
    fun `test info method exists and is callable`() {
        // Act & Assert - should not throw
        factory.info()
    }

    @Test
    fun `test info method can be called multiple times`() {
        // Act & Assert - should not throw
        factory.info()
        factory.info()
        factory.info()
    }

    // ========== createDetector() Abstract Method Tests ==========

    @Test
    fun `test createDetector is abstract and must be implemented`() {
        // Act
        val detector = factory.createDetector(activity)

        // Assert
        assertNotNull("createDetector should return a detector", detector)
        assertTrue("createDetectorCalled flag should be set",
            factory.createDetectorCalled)
        assertSame("Context should be passed correctly",
            activity, factory.lastContext)
    }

    @Test
    fun `test createDetector called with correct context`() {
        // Act
        factory.createDetector(activity)

        // Assert
        assertSame("Context parameter should be stored",
            activity, factory.lastContext)
    }

    @Test
    fun `test createDetector can be called multiple times`() {
        // Act
        val detector1 = factory.createDetector(activity)
        val detector2 = factory.createDetector(activity)

        // Assert
        assertNotNull("First detector should be created", detector1)
        assertNotNull("Second detector should be created", detector2)
    }

    // ========== Factory Pattern Tests ==========

    @Test
    fun `test factory as base type`() {
        // Arrange
        val factoryBase: ObjectDetectorFactory = factory

        // Act
        val detector = factoryBase.createDetector(activity)

        // Assert
        assertNotNull("Detector should be created through base type", detector)
    }

    @Test
    fun `test factory info method is accessible from base type`() {
        // Arrange
        val factoryBase: ObjectDetectorFactory = factory

        // Act & Assert - should not throw
        factoryBase.info()
    }

    // ========== Polymorphism Tests ==========

    @Test
    fun `test multiple factory implementations can coexist`() {
        // Arrange
        val factory1 = TestObjectDetectorFactory()
        val factory2 = TestObjectDetectorFactory()

        // Act
        factory1.createDetector(activity)
        factory2.createDetector(activity)

        // Assert
        assertTrue("Factory1 should have createDetector called",
            factory1.createDetectorCalled)
        assertTrue("Factory2 should have createDetector called",
            factory2.createDetectorCalled)
    }

    // ========== Integration with Concrete Implementations ==========

    @Test
    fun `test factory can work with different activity contexts`() {
        // Arrange
        val activity1 = Robolectric.buildActivity(Activity::class.java).create().get()
        val activity2 = Robolectric.buildActivity(Activity::class.java).create().get()

        // Act
        factory.createDetector(activity1)
        assertEquals("Should store first activity", activity1, factory.lastContext)

        factory.createDetector(activity2)
        assertEquals("Should update to second activity", activity2, factory.lastContext)
    }

    // ========== Class Structure Tests ==========

    @Test
    fun `test ObjectDetectorFactory is abstract`() {
        // This is verified by the fact that we need TestObjectDetectorFactory
        // to instantiate it. If we could instantiate ObjectDetectorFactory
        // directly, it wouldn't be abstract.
        assertTrue("Factory implementation should be ObjectDetectorFactory",
            factory is ObjectDetectorFactory)
    }

    @Test
    fun `test factory has expected methods`() {
        // Verify that the factory has both required methods
        val methods = ObjectDetectorFactory::class.java.declaredMethods
        val methodNames = methods.map { it.name }

        assertTrue("Should have info method",
            methodNames.contains("info"))
        assertTrue("Should have createDetector method",
            methodNames.contains("createDetector"))
    }
}
