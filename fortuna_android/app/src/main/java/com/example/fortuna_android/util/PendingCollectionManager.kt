package com.example.fortuna_android.util

import android.content.Context
import android.util.Log

/**
 * Manages pending element collections in SharedPreferences
 * Used to persist collection data when AR session closes before POST request completes
 */
object PendingCollectionManager {
    private const val TAG = "PendingCollectionManager"
    private const val PREFS_NAME = "fortuna_pending_collections"
    private const val KEY_PENDING_ELEMENT = "pending_element"
    private const val KEY_PENDING_COUNT = "pending_count"
    private const val KEY_HAS_PENDING = "has_pending"

    /**
     * Save a pending collection to be processed later
     * @param context Android context
     * @param elementEnglish Element type in English (fire/water/earth/metal/wood)
     * @param count Number of elements collected (typically 1 for completed quest)
     */
    fun savePendingCollection(context: Context, elementEnglish: String, count: Int = 1) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().apply {
            putString(KEY_PENDING_ELEMENT, elementEnglish)
            putInt(KEY_PENDING_COUNT, count)
            putBoolean(KEY_HAS_PENDING, true)
            apply()
        }
        Log.d(TAG, "Saved pending collection: $elementEnglish x$count")
    }

    /**
     * Check if there is a pending collection
     */
    fun hasPendingCollection(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_HAS_PENDING, false)
    }

    /**
     * Get pending collection data
     * @return Pair of (elementEnglish, count) or null if no pending collection
     */
    fun getPendingCollection(context: Context): Pair<String, Int>? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(KEY_HAS_PENDING, false)) {
            return null
        }

        val element = prefs.getString(KEY_PENDING_ELEMENT, null)
        val count = prefs.getInt(KEY_PENDING_COUNT, 0)

        return if (element != null && count > 0) {
            Log.d(TAG, "Retrieved pending collection: $element x$count")
            Pair(element, count)
        } else {
            Log.w(TAG, "Invalid pending collection data")
            null
        }
    }

    /**
     * Clear pending collection after successfully processing it
     */
    fun clearPendingCollection(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().apply {
            remove(KEY_PENDING_ELEMENT)
            remove(KEY_PENDING_COUNT)
            putBoolean(KEY_HAS_PENDING, false)
            apply()
        }
        Log.d(TAG, "Cleared pending collection")
    }
}