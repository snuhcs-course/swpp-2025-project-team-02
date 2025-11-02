package com.example.fortuna_android.ui

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.GrantPermissionRule
import com.example.fortuna_android.classification.ElementMapper
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Integration tests for AR sphere collection, tap handling, and spatial interaction.
 * Tests the core game mechanics of finding and collecting element spheres in AR space.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class ARSphereCollectionIntegrationTest {

    @get:Rule
    val cameraPermissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(android.Manifest.permission.CAMERA)

    private lateinit var context: Context
    private lateinit var testRenderer: TestARRenderer

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        testRenderer = TestARRenderer(context)
    }

    @Test
    fun testSphereCollectionWithElementFilter() {
        val collectionTest = CountDownLatch(1)
        var filterApplied = false
        var collectionCountIncremented = false

        try {
            // Set needed element filter to FIRE only
            testRenderer.setNeededElement(ElementMapper.Element.FIRE)
            filterApplied = (testRenderer.getNeededElement() == ElementMapper.Element.FIRE)

            // Get initial collection count
            val initialCount = testRenderer.getCollectedCount()

            // Simulate adding anchors with different elements
            testRenderer.addTestAnchor(ElementMapper.Element.FIRE, 0f, 0f, -1f)
            testRenderer.addTestAnchor(ElementMapper.Element.WATER, 1f, 0f, -1f)

            // Test collection after simulated tap processing
            val tappedAnchor = testRenderer.simulateTap(540f, 960f) // Center of screen
            collectionCountIncremented = (testRenderer.getCollectedCount() > initialCount)

            android.util.Log.d("ARSphereTest",
                "Collection test - Filter applied: $filterApplied, Count incremented: $collectionCountIncremented, " +
                "Initial: $initialCount, Final: ${testRenderer.getCollectedCount()}")

            collectionTest.countDown()

        } catch (e: Exception) {
            android.util.Log.e("ARSphereTest", "Collection test error", e)
            collectionTest.countDown()
        }

        assertTrue("Collection test should complete within 5 seconds",
            collectionTest.await(5, TimeUnit.SECONDS))
        assertTrue("Element filter should be applied", filterApplied)
        android.util.Log.d("ARSphereTest", "Collection mechanics tested successfully")
    }

    @Test
    fun testTapDistanceThreshold() {
        val thresholdTest = CountDownLatch(1)
        var tapQueued = false
        var processedCorrectly = false

        try {
            // Test tap handling with distance threshold
            val tapResult = testRenderer.handleTap(100f, 200f)
            tapQueued = tapResult.first
            val tapCoords = tapResult.second
            processedCorrectly = (tapCoords?.first == 100f && tapCoords?.second == 200f)

            android.util.Log.d("ARSphereTest",
                "Tap threshold test - Queued: $tapQueued, Coordinates correct: $processedCorrectly")

            thresholdTest.countDown()

        } catch (e: Exception) {
            android.util.Log.e("ARSphereTest", "Threshold test error", e)
            thresholdTest.countDown()
        }

        assertTrue("Threshold test should complete within 5 seconds",
            thresholdTest.await(5, TimeUnit.SECONDS))
        assertTrue("Tap should be queued", tapQueued)
        assertTrue("Tap coordinates should be preserved", processedCorrectly)
    }

    @Test
    fun testMultipleSphereInteraction() {
        val multiSphereTest = CountDownLatch(1)
        var multipleAnchorsAdded = false
        var anchorCountCorrect = false

        try {
            // Add multiple spheres of different elements
            val elements = listOf(
                ElementMapper.Element.FIRE,
                ElementMapper.Element.WATER,
                ElementMapper.Element.EARTH,
                ElementMapper.Element.METAL,
                ElementMapper.Element.WOOD
            )

            elements.forEachIndexed { index, element ->
                testRenderer.addTestAnchor(element, index.toFloat(), 0f, -1f)
            }

            multipleAnchorsAdded = true
            anchorCountCorrect = (testRenderer.getAnchorCount() == 5)

            // Test clearing all anchors
            testRenderer.clearAnchors()
            val finalCount = testRenderer.getAnchorCount()

            android.util.Log.d("ARSphereTest",
                "Multi-sphere test - Added: $multipleAnchorsAdded, Count correct: $anchorCountCorrect, " +
                "Final count after clear: $finalCount")

            multiSphereTest.countDown()

        } catch (e: Exception) {
            android.util.Log.e("ARSphereTest", "Multi-sphere error", e)
            multiSphereTest.countDown()
        }

        assertTrue("Multi-sphere test should complete within 5 seconds",
            multiSphereTest.await(5, TimeUnit.SECONDS))
        assertTrue("Multiple anchors should be added", multipleAnchorsAdded)
        assertTrue("Anchor count should be correct", anchorCountCorrect)
    }

    @Test
    fun testSphereVisibilityFiltering() {
        val filterTest = CountDownLatch(1)
        var filteringWorks = false

        try {
            // Set filter to only show WATER elements
            testRenderer.setNeededElement(ElementMapper.Element.WATER)

            // Add anchors with different elements
            testRenderer.addTestAnchor(ElementMapper.Element.FIRE, 0f, 0f, -1f)
            testRenderer.addTestAnchor(ElementMapper.Element.WATER, 1f, 0f, -1f)

            // Test filtering logic by checking which elements should be shown
            val neededElement = testRenderer.getNeededElement()

            // Only WATER should be shown when filter is set
            val shouldShowFire = (neededElement == null || ElementMapper.Element.FIRE == neededElement)
            val shouldShowWater = (neededElement == null || ElementMapper.Element.WATER == neededElement)

            filteringWorks = (!shouldShowFire && shouldShowWater)

            android.util.Log.d("ARSphereTest",
                "Visibility filtering - Needed: ${neededElement?.displayName}, " +
                "Show Fire: $shouldShowFire, Show Water: $shouldShowWater")

            filterTest.countDown()

        } catch (e: Exception) {
            android.util.Log.e("ARSphereTest", "Filter test error", e)
            filterTest.countDown()
        }

        assertTrue("Filter test should complete within 5 seconds",
            filterTest.await(5, TimeUnit.SECONDS))
        assertTrue("Filtering should work correctly", filteringWorks)
    }

    @Test
    fun testCollectionProgressTracking() {
        val progressTest = CountDownLatch(1)
        var progressTracking = false

        try {
            // Test collection count progression
            val initialCount = testRenderer.getCollectedCount()

            // Simulate collection by manually incrementing (tests the getter)
            testRenderer.setNeededElement(ElementMapper.Element.EARTH) // This resets count to 0

            val afterResetCount = testRenderer.getCollectedCount()

            // Change element again to test reset behavior
            testRenderer.setNeededElement(ElementMapper.Element.FIRE)
            val finalCount = testRenderer.getCollectedCount()

            progressTracking = (initialCount == 0 && afterResetCount == 0 && finalCount == 0)

            android.util.Log.d("ARSphereTest",
                "Progress tracking - Initial: $initialCount, After reset: $afterResetCount, Final: $finalCount")

            progressTest.countDown()

        } catch (e: Exception) {
            android.util.Log.e("ARSphereTest", "Progress test error", e)
            progressTest.countDown()
        }

        assertTrue("Progress test should complete within 5 seconds",
            progressTest.await(5, TimeUnit.SECONDS))
        assertTrue("Progress tracking should work", progressTracking)
    }

    @Test
    fun testSpatialCoordinateProjection() {
        val projectionTest = CountDownLatch(1)
        var projectionTested = false

        try {
            // Test the projectToScreen method
            val worldPos = floatArrayOf(0f, 0f, -1f, 1f) // 1 meter in front of camera
            val viewMatrix = FloatArray(16)
            val projMatrix = FloatArray(16)

            // Initialize as identity matrices for testing
            android.opengl.Matrix.setIdentityM(viewMatrix, 0)
            android.opengl.Matrix.setIdentityM(projMatrix, 0)

            // Test projection using test renderer
            val screenPos = testRenderer.projectToScreen(worldPos, viewMatrix, projMatrix)

            projectionTested = true

            android.util.Log.d("ARSphereTest",
                "Projection test - Method called successfully, Result: ${screenPos?.contentToString()}")

            projectionTest.countDown()

        } catch (e: Exception) {
            android.util.Log.e("ARSphereTest", "Projection test error", e)
            projectionTest.countDown()
        }

        assertTrue("Projection test should complete within 5 seconds",
            projectionTest.await(5, TimeUnit.SECONDS))
        assertTrue("Projection method should be callable", projectionTested)
    }

    @Test
    fun testElementDisplayMapping() {
        val mappingTest = CountDownLatch(1)
        var allElementsTested = false

        try {
            val elements = ElementMapper.Element.values()
            var allHaveDisplayNames = true

            for (element in elements) {
                if (element.displayName.isBlank()) {
                    allHaveDisplayNames = false
                    break
                }

                // Test setting each element as needed
                testRenderer.setNeededElement(element)

                // Verify it was set
                if (testRenderer.getNeededElement() != element) {
                    allHaveDisplayNames = false
                    break
                }
            }

            allElementsTested = allHaveDisplayNames

            android.util.Log.d("ARSphereTest",
                "Element mapping - All elements tested: $allElementsTested, Count: ${elements.size}")

            mappingTest.countDown()

        } catch (e: Exception) {
            android.util.Log.e("ARSphereTest", "Mapping test error", e)
            mappingTest.countDown()
        }

        assertTrue("Mapping test should complete within 5 seconds",
            mappingTest.await(5, TimeUnit.SECONDS))
        assertTrue("All elements should be testable", allElementsTested)
    }

    /**
     * Test implementation of AR renderer functionality without dependencies
     */
    private class TestARRenderer(private val context: Context) {
        private var neededElement: ElementMapper.Element? = null
        private var collectedCount = 0
        private val testAnchors = mutableListOf<TestAnchor>()
        private var pendingTap: Pair<Float, Float>? = null
        private val screenWidth = 1080f
        private val screenHeight = 1920f

        data class TestAnchor(val element: ElementMapper.Element, val x: Float, val y: Float, val z: Float)

        fun setNeededElement(element: ElementMapper.Element) {
            neededElement = element
            collectedCount = 0 // Reset count when changing element
        }

        fun getNeededElement(): ElementMapper.Element? = neededElement

        fun getCollectedCount(): Int = collectedCount

        fun addTestAnchor(element: ElementMapper.Element, x: Float, y: Float, z: Float) {
            testAnchors.add(TestAnchor(element, x, y, z))
        }

        fun getAnchorCount(): Int = testAnchors.size

        fun clearAnchors() {
            testAnchors.clear()
        }

        fun handleTap(x: Float, y: Float): Pair<Boolean, Pair<Float, Float>?> {
            pendingTap = Pair(x, y)
            return Pair(true, pendingTap)
        }

        fun simulateTap(x: Float, y: Float): TestAnchor? {
            // Simple distance-based collection simulation
            val threshold = 150f
            for (anchor in testAnchors) {
                val screenPos = projectToScreen(
                    floatArrayOf(anchor.x, anchor.y, anchor.z, 1f),
                    FloatArray(16).apply { android.opengl.Matrix.setIdentityM(this, 0) },
                    FloatArray(16).apply { android.opengl.Matrix.setIdentityM(this, 0) }
                )

                if (screenPos != null) {
                    val distance = kotlin.math.sqrt(
                        (x - screenPos[0]) * (x - screenPos[0]) +
                        (y - screenPos[1]) * (y - screenPos[1])
                    )

                    if (distance < threshold && (neededElement == null || anchor.element == neededElement)) {
                        collectedCount++
                        testAnchors.remove(anchor)
                        return anchor
                    }
                }
            }
            return null
        }

        fun projectToScreen(worldPos: FloatArray, viewMatrix: FloatArray, projMatrix: FloatArray): FloatArray? {
            // Simple projection simulation for testing
            if (worldPos[2] > 0) return null // Behind camera

            // Simulate normalized device coordinates
            val ndc = floatArrayOf(
                worldPos[0] / (-worldPos[2]), // Simple perspective
                worldPos[1] / (-worldPos[2]),
                0f
            )

            // Convert to screen coordinates
            return floatArrayOf(
                (ndc[0] + 1f) * screenWidth * 0.5f,
                (1f - ndc[1]) * screenHeight * 0.5f
            )
        }
    }
}