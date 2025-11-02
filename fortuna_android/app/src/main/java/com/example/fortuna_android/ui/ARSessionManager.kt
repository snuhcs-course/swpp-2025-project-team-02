package com.example.fortuna_android.ui

import androidx.lifecycle.LifecycleObserver
import com.google.ar.core.Session

/**
 * Interface for managing ARCore session lifecycle and operations.
 * This abstraction enables easy testing by allowing mock implementations.
 */
interface ARSessionManager : LifecycleObserver {

    /**
     * Callback invoked when ARCore session creation fails or encounters exceptions
     */
    var exceptionCallback: ((Exception) -> Unit)?

    /**
     * Callback invoked before session.resume() to configure the session
     */
    var beforeSessionResume: ((Session) -> Unit)?

    /**
     * The current ARCore session cache, null if no session is active
     */
    val sessionCache: Session?

    /**
     * Handle permission request results for camera permissions
     */
    fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    )

    /**
     * Attempt to create an ARCore session
     * @return Session if successful, null if installation required or failed
     */
    fun tryCreateSession(): Session?
}
