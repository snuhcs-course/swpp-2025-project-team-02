package com.example.fortuna_android.common

import android.graphics.Color

/**
 * Centralized color constants for the Fortuna Android app
 *
 * Contains all hardcoded color values used throughout the app,
 * particularly for the five elements (오행) color system.
 */
object AppColors {

    // ============================================
    // Five Elements Colors (오행 색상)
    // ============================================

    /** Wood element color - Green (나무/목) */
    const val ELEMENT_WOOD = "#0BEFA0"

    /** Fire element color - Red (불/화) */
    const val ELEMENT_FIRE = "#F93E3E"

    /** Earth element color - Orange (흙/토) */
    const val ELEMENT_EARTH = "#FF9500"

    /** Metal element color - Gray/Silver (쇠/금) */
    const val ELEMENT_METAL = "#C1BFBF"

    /** Water element color - Blue (물/수) */
    const val ELEMENT_WATER = "#2BB3FC"

    // ============================================
    // Alternative Element Colors
    // ============================================

    /** Alternative Metal color - Light Silver */
    const val ELEMENT_METAL_ALT = "#C0C0C0"

    /** Alternative Earth color - Brown */
    const val ELEMENT_EARTH_BROWN = "#8B4513"

    // ============================================
    // UI Background Colors
    // ============================================

    /** Dark background color for completed items */
    const val BACKGROUND_DARK = "#2A2A2A"

    /** Regular dark background */
    const val BACKGROUND_REGULAR = "#1E1E1E"

    /** Empty/inactive item color */
    const val BACKGROUND_EMPTY = "#3A3A3A"

    /** Default/fallback color - White */
    const val COLOR_WHITE = "#FFFFFF"

    /** Default/fallback color - Light Gray */
    const val COLOR_LIGHT_GRAY = "#CCCCCC"

    /** Black color */
    const val COLOR_BLACK = "#000000"

    /** Gold/Yellow color for highlights */
    const val COLOR_GOLD = "#FFD700"

    /** Gray color for inactive text */
    const val COLOR_GRAY = "#888888"

    // ============================================
    // Helper Methods
    // ============================================

    /**
     * Get element color by Korean name
     * @param element Korean element name (목/화/토/금/수)
     * @return Color integer value
     */
    fun getElementColorByKorean(element: String): Int {
        return when (element) {
            "목", "wood" -> Color.parseColor(ELEMENT_WOOD)
            "화", "fire" -> Color.parseColor(ELEMENT_FIRE)
            "토", "earth" -> Color.parseColor(ELEMENT_EARTH)
            "금", "metal" -> Color.parseColor(ELEMENT_METAL)
            "수", "water" -> Color.parseColor(ELEMENT_WATER)
            else -> Color.parseColor(COLOR_LIGHT_GRAY)
        }
    }

    /**
     * Get element color by English name
     * @param element English element name (wood/fire/earth/metal/water)
     * @return Color integer value
     */
    fun getElementColorByEnglish(element: String): Int {
        return when (element.lowercase()) {
            "wood", "나무" -> Color.parseColor(ELEMENT_WOOD)
            "fire", "불" -> Color.parseColor(ELEMENT_FIRE)
            "earth", "흙" -> Color.parseColor(ELEMENT_EARTH)
            "metal", "쇠" -> Color.parseColor(ELEMENT_METAL_ALT)
            "water", "물" -> Color.parseColor(ELEMENT_WATER)
            else -> Color.parseColor(COLOR_WHITE)
        }
    }

    /**
     * Get element color by heavenly stem (천간)
     * @param stem Heavenly stem character (갑/을/병/정/무/기/경/신/임/계)
     * @return Color integer value
     */
    fun getElementColorByStem(stem: String): Int {
        return when (stem) {
            "갑", "을" -> Color.parseColor(ELEMENT_WOOD)
            "병", "정" -> Color.parseColor(ELEMENT_FIRE)
            "무", "기" -> Color.parseColor(ELEMENT_EARTH)
            "경", "신" -> Color.parseColor(ELEMENT_METAL_ALT)
            "임", "계" -> Color.parseColor(ELEMENT_WATER)
            else -> Color.parseColor(COLOR_LIGHT_GRAY)
        }
    }

    /**
     * Get element color by earthly branch (지지)
     * @param branch Earthly branch character (인/묘/사/오/신/유/해/자/술/미/축/진)
     * @return Color integer value
     */
    fun getElementColorByBranch(branch: String): Int {
        return when (branch) {
            "인", "묘" -> Color.parseColor(ELEMENT_WOOD)
            "사", "오" -> Color.parseColor(ELEMENT_FIRE)
            "술", "미", "축", "진" -> Color.parseColor(ELEMENT_EARTH)
            "신", "유" -> Color.parseColor(ELEMENT_METAL)
            "해", "자" -> Color.parseColor(ELEMENT_WATER)
            else -> Color.parseColor(COLOR_LIGHT_GRAY)
        }
    }
}
