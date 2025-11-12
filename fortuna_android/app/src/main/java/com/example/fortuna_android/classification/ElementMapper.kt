package com.example.fortuna_android.classification

import android.content.Context
import android.util.Log
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Maps detection labels to Chinese Five Elements + Others categories
 * Based on the classification defined in assets/class.txt
 */
class ElementMapper(private val context: Context) {

    companion object {
        private const val TAG = "ElementMapper"
        private const val CLASS_FILE = "class.txt"

        /**
         * Convert Korean element name to Element enum
         * API returns: 목/화/토/금/수
         */
        fun fromKorean(koreanElement: String): Element {
            return when (koreanElement) {
                "목" -> Element.WOOD   // Wood
                "화" -> Element.FIRE   // Fire
                "토" -> Element.EARTH  // Earth
                "금" -> Element.METAL  // Metal
                "수" -> Element.WATER  // Water
                else -> {
                    Log.w(TAG, "Unknown Korean element: $koreanElement, defaulting to OTHERS")
                    Element.OTHERS
                }
            }
        }

        /**
         * Convert Element enum to Korean name
         */
        fun toKorean(element: Element): String {
            return when (element) {
                Element.WOOD -> "목"
                Element.FIRE -> "화"
                Element.EARTH -> "토"
                Element.METAL -> "금"
                Element.WATER -> "수"
                Element.OTHERS -> "기타"
            }
        }

        /**
         * Convert Element enum to English name (for API)
         * API expects: fire/water/earth/metal/wood
         */
        fun toEnglish(element: Element): String {
            return when (element) {
                Element.WOOD -> "wood"
                Element.FIRE -> "fire"
                Element.EARTH -> "earth"
                Element.METAL -> "metal"
                Element.WATER -> "water"
                Element.OTHERS -> "wood"
            }
        }

        /**
         * Get the color code for an element (for UI display)
         */
        fun getElementColor(element: Element): Int {
            return when (element) {
                Element.WOOD -> 0xFF4CAF50.toInt()   // Green
                Element.FIRE -> 0xFFF44336.toInt()   // Red
                Element.EARTH -> 0xFFFFEB3B.toInt()  // Yellow
                Element.METAL -> 0xFFFFFFFF.toInt()  // White
                Element.WATER -> 0xFF2196F3.toInt()  // Blue
                Element.OTHERS -> 0xFF9E9E9E.toInt() // Gray
            }
        }

        /**
         * Get element description text for UI
         */
        fun getElementDisplayText(element: Element): String {
            return when (element) {
                Element.WOOD -> "Wood (Green)"
                Element.FIRE -> "Fire (Red)"
                Element.EARTH -> "Earth (Yellow)"
                Element.METAL -> "Metal (White)"
                Element.WATER -> "Water (Blue)"
                Element.OTHERS -> "Others"
            }
        }

        /**
         * Get element display text in Hanja only (for AR UI)
         * Returns format: "한자"
         */
        fun getElementHanja(element: Element): String {
            return when (element) {
                Element.WOOD -> "木"
                Element.FIRE -> "火"
                Element.EARTH -> "土"
                Element.METAL -> "金"
                Element.WATER -> "水"
                Element.OTHERS -> "기타"
            }
        }
    }

    // Categories based on Chinese Five Elements
    enum class Element(val displayName: String) {
        FIRE("Fire"),
        METAL("Metal"),
        EARTH("Earth"),
        WOOD("Wood"),
        WATER("Water"),
        OTHERS("Others")
    }

    private val labelToElementMap = mutableMapOf<String, Element>()
    private var isInitialized = false

    init {
        loadClassMapping()
    }

    /**
     * Load the class mapping from assets/class.txt
     */
    private fun loadClassMapping() {
        try {
            val inputStream = context.assets.open(CLASS_FILE)
            val reader = BufferedReader(InputStreamReader(inputStream))

            var currentElement = Element.OTHERS
            var lineNumber = 0

            reader.useLines { lines ->
                lines.forEach { line ->
                    lineNumber++
                    val trimmedLine = line.trim()

                    when {
                        trimmedLine.startsWith("1. Fire") -> {
                            currentElement = Element.FIRE
                            Log.d(TAG, "Found Fire section at line $lineNumber")
                        }
                        trimmedLine.startsWith("2. Metal") -> {
                            currentElement = Element.METAL
                            Log.d(TAG, "Found Metal section at line $lineNumber")
                        }
                        trimmedLine.startsWith("3. Ground") -> {
                            currentElement = Element.EARTH
                            Log.d(TAG, "Found Earth section at line $lineNumber")
                        }
                        trimmedLine.startsWith("4. Wood") -> {
                            currentElement = Element.WOOD
                            Log.d(TAG, "Found Wood section at line $lineNumber")
                        }
                        trimmedLine.startsWith("5. Water") -> {
                            currentElement = Element.WATER
                            Log.d(TAG, "Found Water section at line $lineNumber")
                        }
                        trimmedLine.startsWith("6. others") -> {
                            currentElement = Element.OTHERS
                            Log.d(TAG, "Found Others section at line $lineNumber")
                        }
                        trimmedLine.isNotEmpty() && !trimmedLine.startsWith("1.") &&
                        !trimmedLine.startsWith("2.") && !trimmedLine.startsWith("3.") &&
                        !trimmedLine.startsWith("4.") && !trimmedLine.startsWith("5.") &&
                        !trimmedLine.startsWith("6.") -> {
                            // This is a class label, map it to current element
                            labelToElementMap[trimmedLine.lowercase()] = currentElement
                        }
                    }
                }
            }

            isInitialized = true
            Log.d(TAG, "Loaded ${labelToElementMap.size} class mappings")

        } catch (e: Exception) {
            Log.e(TAG, "Error loading class mapping from $CLASS_FILE", e)
            isInitialized = false
        }
    }

    /**
     * Map a detected label to its corresponding element category
     */
    fun mapLabelToElement(detectedLabel: String): Element {
        if (!isInitialized) {
            Log.w(TAG, "ElementMapper not initialized, returning OTHERS for label: $detectedLabel")
            return Element.OTHERS
        }

        val normalizedLabel = detectedLabel.lowercase().trim()

        // VLM element name direct matching (for VLM-based detection)
        // VLM returns: Water, Fire, Earth, Metal, Wood (capitalized)
        when (normalizedLabel) {
            "water" -> {
                Log.d(TAG, "VLM element match: '$detectedLabel' -> Water")
                return Element.WATER
            }
            "fire" -> {
                Log.d(TAG, "VLM element match: '$detectedLabel' -> Fire")
                return Element.FIRE
            }
            "earth" -> {
                Log.d(TAG, "VLM element match: '$detectedLabel' -> Earth")
                return Element.EARTH
            }
            "metal" -> {
                Log.d(TAG, "VLM element match: '$detectedLabel' -> Metal")
                return Element.METAL
            }
            "wood" -> {
                Log.d(TAG, "VLM element match: '$detectedLabel' -> Wood")
                return Element.WOOD
            }
        }

        // Direct match with class.txt mappings
        labelToElementMap[normalizedLabel]?.let { element ->
            Log.d(TAG, "Direct match: '$detectedLabel' -> ${element.displayName}")
            return element
        }

        // Fuzzy matching - check if detected label contains any of our class labels
        for ((classLabel, element) in labelToElementMap) {
            if (normalizedLabel.contains(classLabel) || classLabel.contains(normalizedLabel)) {
                Log.d(TAG, "Fuzzy match: '$detectedLabel' contains '$classLabel' -> ${element.displayName}")
                return element
            }
        }

        // Check for partial word matches
        val detectedWords = normalizedLabel.split(" ")
        for ((classLabel, element) in labelToElementMap) {
            val classWords = classLabel.split(" ")
            for (detectedWord in detectedWords) {
                for (classWord in classWords) {
                    if (detectedWord.length > 3 && classWord.length > 3 &&
                        (detectedWord.contains(classWord) || classWord.contains(detectedWord))) {
                        Log.d(TAG, "Word match: '$detectedWord' matches '$classWord' -> ${element.displayName}")
                        return element
                    }
                }
            }
        }

        Log.d(TAG, "No match found for '$detectedLabel', returning OTHERS")
        return Element.OTHERS
    }

    /**
     * Get all available elements
     */
    fun getAllElements(): List<Element> = Element.values().toList()

    /**
     * Check if mapper is properly initialized
     */
    fun isReady(): Boolean = isInitialized

    /**
     * Get mapping statistics for debugging
     */
    fun getMappingStats(): String {
        val stats = mutableMapOf<Element, Int>()
        labelToElementMap.values.forEach { element ->
            stats[element] = stats.getOrDefault(element, 0) + 1
        }

        return buildString {
            appendLine("Element Mapping Statistics:")
            stats.forEach { (element, count) ->
                appendLine("  ${element.displayName}: $count items")
            }
            appendLine("  Total: ${labelToElementMap.size} mappings")
        }
    }
}