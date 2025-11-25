package com.example.fortuna_android.classification

import android.app.Activity

/**
 * Factory Method Pattern: Abstract Creator
 */
abstract class ObjectDetectorFactory {
    /**
     * Common factory information method
     */
    fun info() {
        println("ObjectDetector Factory")
    }

    /**
     * Factory Method: Creates an ObjectDetector instance
     * @param context Activity context required for detector initialization
     * @return ObjectDetector instance (MLKit or VLM-based)
     */
    abstract fun createDetector(context: Activity): ObjectDetector
}
