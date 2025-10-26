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

import android.app.Activity
import android.graphics.Bitmap
import android.media.Image
import com.example.fortuna_android.classification.utils.ImageUtils
import com.example.fortuna_android.classification.utils.VertexUtils.rotateCoordinates
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.common.model.LocalModel
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.custom.CustomObjectDetectorOptions
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import kotlinx.coroutines.tasks.asDeferred

/**
 * Analyzes an image using ML Kit.
 */
class MLKitObjectDetector(
  context: Activity,
  private val minObjectSizePercent: Float = 0.01f,
  private val maxObjectSizePercent: Float = 0.8f,
  private val maxDetectedObjects: Int = 3
) : ObjectDetector(context) {
  // To use a custom model, follow steps on https://developers.google.com/ml-kit/vision/object-detection/custom-models/android.
  // val model = LocalModel.Builder().setAssetFilePath("inception_v4_1_metadata_1.tflite").build()
  // val builder = CustomObjectDetectorOptions.Builder(model)

  // For the ML Kit default model, use the following:

  val localModel = LocalModel.Builder()
    .setAssetFilePath("model_metadata.tflite")
    .build()
  val builder = CustomObjectDetectorOptions.Builder(localModel)
  val customObjectDetectorOptions = builder
    .setDetectorMode(CustomObjectDetectorOptions.SINGLE_IMAGE_MODE)
    .enableClassification()
    .setClassificationConfidenceThreshold(0.8f)
    .setMaxPerObjectLabelCount(1)
    .enableMultipleObjects()  // Enable detection of multiple objects
    .build()

  private val detector = ObjectDetection.getClient(customObjectDetectorOptions)



  override suspend fun analyze(image: Image, imageRotation: Int): List<DetectedObjectResult> {
    // `image` is in YUV (https://developers.google.com/ar/reference/java/com/google/ar/core/Frame#acquireCameraImage()),
    val convertYuv = convertYuv(image)

    // The model performs best on upright images, so rotate it.
    val rotatedImage = ImageUtils.rotateBitmap(convertYuv, imageRotation)

    val inputImage = InputImage.fromBitmap(rotatedImage, 0)

    val mlKitDetectedObjects = detector.process(inputImage).asDeferred().await()

    // Calculate image area for size constraints
    val imageArea = (rotatedImage.width * rotatedImage.height).toFloat()
    val minObjectArea = imageArea * minObjectSizePercent
    val maxObjectArea = imageArea * maxObjectSizePercent

    return mlKitDetectedObjects.mapNotNull { obj ->
      val bestLabel = obj.labels.maxByOrNull { label -> label.confidence } ?: return@mapNotNull null

      // Check object size constraints
      val boundingBox = obj.boundingBox
      val objectArea = (boundingBox.width() * boundingBox.height()).toFloat()
      if (objectArea < minObjectArea || objectArea > maxObjectArea) {
        return@mapNotNull null
      }

      val coords = boundingBox.exactCenterX().toInt() to boundingBox.exactCenterY().toInt()
      val rotatedCoordinates = coords.rotateCoordinates(rotatedImage.width, rotatedImage.height, imageRotation)
      DetectedObjectResult(bestLabel.confidence, bestLabel.text, rotatedCoordinates)
    }.sortedByDescending { it.confidence } // Sort by confidence (highest first)
      .take(maxDetectedObjects) // Limit number of detected objects
  }

  /**
   * Analyze image and return extended results with bounding boxes.
   * Required for VLM-based classification to crop objects.
   */
  suspend fun analyzeExtended(image: Image, imageRotation: Int): List<ExtendedDetectedObjectResult> {
    // `image` is in YUV
    val convertYuv = convertYuv(image)

    // Rotate image for best model performance
    val rotatedImage = ImageUtils.rotateBitmap(convertYuv, imageRotation)

    val inputImage = InputImage.fromBitmap(rotatedImage, 0)

    val mlKitDetectedObjects = detector.process(inputImage).asDeferred().await()

    // Calculate image area for size constraints
    val imageArea = (rotatedImage.width * rotatedImage.height).toFloat()
    val minObjectArea = imageArea * minObjectSizePercent
    val maxObjectArea = imageArea * maxObjectSizePercent

    return mlKitDetectedObjects.mapNotNull { obj ->
      val bestLabel = obj.labels.maxByOrNull { label -> label.confidence } ?: return@mapNotNull null

      // Check object size constraints
      val boundingBox = obj.boundingBox
      val objectArea = (boundingBox.width() * boundingBox.height()).toFloat()
      if (objectArea < minObjectArea || objectArea > maxObjectArea) {
        return@mapNotNull null
      }

      val coords = boundingBox.exactCenterX().toInt() to boundingBox.exactCenterY().toInt()
      val rotatedCoordinates = coords.rotateCoordinates(rotatedImage.width, rotatedImage.height, imageRotation)

      // Return ExtendedDetectedObjectResult with bounding box for VLM cropping
      ExtendedDetectedObjectResult(
        confidence = bestLabel.confidence,
        label = bestLabel.text,
        centerCoordinate = rotatedCoordinates,
        boundingBox = boundingBox  // Preserve bounding box
      )
    }.sortedByDescending { it.confidence } // Sort by confidence (highest first)
      .take(maxDetectedObjects) // Limit number of detected objects
  }

  @Suppress("USELESS_IS_CHECK")
  fun hasCustomModel() = builder is CustomObjectDetectorOptions.Builder
}