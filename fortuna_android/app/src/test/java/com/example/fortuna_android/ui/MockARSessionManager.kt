package com.example.fortuna_android.ui

import androidx.lifecycle.LifecycleOwner
import com.google.ar.core.Session

/**
 * Mock implementation of ARSessionManager for testing purposes.
 * This allows testing ARFragment functionality without requiring actual ARCore dependencies.
 */
class MockARSessionManager : ARSessionManager {

    // Test controllable properties
    var mockSession: Session? = null
    var shouldThrowException: Exception? = null
    var createSessionCallCount = 0
    var permissionRequestCallCount = 0

    // Interface implementation
    override var exceptionCallback: ((Exception) -> Unit)? = null
    override var beforeSessionResume: ((Session) -> Unit)? = null
    override val sessionCache: Session? get() = mockSession

    override fun tryCreateSession(): Session? {
        createSessionCallCount++

        shouldThrowException?.let { exception ->
            exceptionCallback?.invoke(exception)
            return null
        }

        return mockSession
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        permissionRequestCallCount++
        // Mock implementation - can be customized for specific test scenarios
    }

    // Lifecycle methods (no-op for testing)
    fun onResume(owner: LifecycleOwner) {
        mockSession?.let { session ->
            beforeSessionResume?.invoke(session)
        }
    }

    fun onPause(owner: LifecycleOwner) {
        // No-op for testing
    }

    fun onDestroy(owner: LifecycleOwner) {
        // No-op for testing
        mockSession = null
    }

    // Test helper methods
    fun simulateSessionCreationSuccess(session: Session) {
        mockSession = session
        shouldThrowException = null
    }

    fun simulateSessionCreationFailure(exception: Exception) {
        mockSession = null
        shouldThrowException = exception
    }

    fun reset() {
        mockSession = null
        shouldThrowException = null
        createSessionCallCount = 0
        permissionRequestCallCount = 0
        exceptionCallback = null
        beforeSessionResume = null
    }
}