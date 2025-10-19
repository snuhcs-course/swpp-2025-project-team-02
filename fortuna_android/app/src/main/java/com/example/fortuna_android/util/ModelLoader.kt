package com.example.fortuna_android.util

import android.content.Context
import android.util.Log
import com.google.android.filament.Engine
import io.github.sceneview.ar.node.ArModelNode
import io.github.sceneview.math.Position

/**
 * 오행(五行) 3D 모델 로더
 * GLB 파일을 로딩하고 관리하는 클래스
 * io.github.sceneview 라이브러리 사용
 */
class ModelLoader(private val context: Context) {

    companion object {
        private const val TAG = "ModelLoader"
    }

    // 오행 요소 정의
    enum class Element(val fileName: String, val displayName: String) {
        FIRE("fire.glb", "불"),
        EARTH("earth.glb", "흙"),
        METAL("metal.glb", "금"),
        WOOD("wood.glb", "나무"),
        WATER("water.glb", "물")
    }

    /**
     * GLB 모델 노드 생성
     * @param engine Filament Engine (ArSceneView.engine에서 가져옴)
     * @param element 오행 요소
     * @return ArModelNode
     */
    fun createModelNode(
        engine: Engine,
        element: Element
    ): ArModelNode {
        val modelPath = "models/${element.fileName}"

        Log.d(TAG, "Creating model node for: ${element.displayName} ($modelPath)")

        return ArModelNode(
            engine = engine,
            modelGlbFileLocation = modelPath,
            autoAnimate = true,
            scaleToUnits = 0.5f,
            centerOrigin = Position(0f, 0f, 0f)
        )
    }

    /**
     * naive approach example
     * 예시: Object Detection 라벨에 따라 적절한 Element 반환
     */
    fun getElementForLabel(label: String): Element? {
        return when {
            // 불 관련 키워드
            label.contains("fire", ignoreCase = true) ||
            label.contains("flame", ignoreCase = true) ||
            label.contains("torch", ignoreCase = true) ||
            label.contains("lighter", ignoreCase = true)
            -> Element.FIRE

            // 흙 관련 키워드
            label.contains("rock", ignoreCase = true) ||
            label.contains("stone", ignoreCase = true) ||
            label.contains("ground", ignoreCase = true) ||
            label.contains("soil", ignoreCase = true)
            -> Element.EARTH

            // 금속 관련 키워드
            label.contains("metal", ignoreCase = true) ||
            label.contains("iron", ignoreCase = true) ||
            label.contains("steel", ignoreCase = true) ||
            label.contains("coin", ignoreCase = true) ||
            label.contains("key", ignoreCase = true)
            -> Element.METAL

            // 나무 관련 키워드
            label.contains("tree", ignoreCase = true) ||
            label.contains("wood", ignoreCase = true) ||
            label.contains("plant", ignoreCase = true) ||
            label.contains("leaf", ignoreCase = true) ||
            label.contains("branch", ignoreCase = true)
            -> Element.WOOD

            // 물 관련 키워드
            label.contains("water", ignoreCase = true) ||
            label.contains("bottle", ignoreCase = true) ||
            label.contains("cup", ignoreCase = true) ||
            label.contains("glass", ignoreCase = true)
            -> Element.WATER

            else -> null
        }
    }

    /**
     * GLB 파일 경로 반환
     */
    fun getModelPath(element: Element): String {
        return "models/${element.fileName}"
    }

    /**
     * 모델 존재 여부 확인
     */
    fun checkModelExists(element: Element): Boolean {
        return try {
            context.assets.open(getModelPath(element)).use { true }
        } catch (e: Exception) {
            Log.e(TAG, "Model not found: ${element.fileName}", e)
            false
        }
    }

    /**
     * 모든 모델 존재 여부 확인
     */
    fun checkAllModelsExist(): Boolean {
        val allExist = Element.values().all { checkModelExists(it) }
        if (allExist) {
            Log.d(TAG, "All 5 GLB models found in assets/models/")
        } else {
            Log.w(TAG, "Some GLB models are missing")
        }
        return allExist
    }
}
