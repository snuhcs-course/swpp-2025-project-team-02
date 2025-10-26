/*
 * Copyright 2021 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.fortuna_android.classification

import android.graphics.Rect

/**
 * Extended [DetectedObjectResult] with bounding box information.
 * Required for VLM-based classification to crop objects from camera images.
 *
 * @property confidence The model's reported confidence for this inference result (normalized over `[0, 1]`).
 * @property label The model's reported label for this result.
 * @property centerCoordinate A point on the image that best describes the object's location.
 * @property boundingBox The bounding box of the detected object (for cropping).
 */
data class ExtendedDetectedObjectResult(
    val confidence: Float,
    val label: String,
    val centerCoordinate: Pair<Int, Int>,
    val boundingBox: Rect
) {
    /**
     * Convert to legacy DetectedObjectResult for backward compatibility.
     */
    fun toDetectedObjectResult(): DetectedObjectResult {
        return DetectedObjectResult(confidence, label, centerCoordinate)
    }
}
