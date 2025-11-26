package com.example.fortuna_android.common

/**
 * Centralized numeric and timing constants for the Fortuna Android app
 *
 * Contains all hardcoded numeric values used throughout the app
 * to improve maintainability and make values self-documenting.
 */
object AppConstants {

    // ============================================
    // AR Collection Constants
    // ============================================

    /** Number of elements required to complete daily collection quest */
    const val TARGET_COLLECTION_COUNT = 5

    /** Number of particles to spawn in celebration animation */
    const val CELEBRATION_PARTICLE_COUNT = 12

    /** Maximum distance for particle spawning (dp) */
    const val PARTICLE_MAX_DISTANCE = 200f

    /** Random distance variation for particles (dp) */
    const val PARTICLE_DISTANCE_VARIATION = 100f

    /** Tap detection threshold in pixels */
    const val TAP_THRESHOLD_PIXELS = 150f

    // ============================================
    // Timing Constants (milliseconds)
    // ============================================

    /** Maximum time to hold scan before auto-trigger (3 seconds) */
    const val SCAN_HOLD_TIME_MS = 3000L

    /** Delay before showing celebration animation */
    const val CELEBRATION_DELAY_MS = 300L

    /** Duration of fade-in animation */
    const val FADE_IN_DURATION_MS = 300L

    /** Duration of particle animation */
    const val PARTICLE_ANIMATION_DURATION_MS = 2000L

    /** Debounce time for rapid taps */
    const val TAP_DEBOUNCE_MS = 500L

    // ============================================
    // UI Layout Constants
    // ============================================

    /** Default texture size for text rendering */
    const val TEXT_TEXTURE_SIZE = 256

    /** Sweep angle for circular progress indicators */
    const val CIRCULAR_SWEEP_ANGLE = 270f

    // ============================================
    // Audio Constants
    // ============================================

    /** Sample rate for audio playback (Hz) */
    const val AUDIO_SAMPLE_RATE = 24000

    // ============================================
    // Notification Constants
    // ============================================

    /** Default notification hour (22:00 = 10 PM) */
    const val NOTIFICATION_HOUR_DEFAULT = 22

    /** Default notification minute */
    const val NOTIFICATION_MINUTE_DEFAULT = 0

    // ============================================
    // Permission Request Codes
    // ============================================

    /** General permission request code */
    const val PERMISSION_REQUEST_CODE = 1001

    /** Notification permission specific request code */
    const val NOTIFICATION_PERMISSION_REQUEST_CODE = 1001

    // ============================================
    // Calendar/Date Constants
    // ============================================

    /** Number of days to show in calendar view */
    const val CALENDAR_DAYS_COUNT = 30
}
