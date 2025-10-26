package com.example.fortuna_android.classification.utils

// Ultra-short prompt for fast VLM classification (1-token expected response)
// Expected outputs: "wood", "fire", "earth", "metal", "water"
const val VLM_CLASSIFICATION_PROMPT = """wood/fire/earth/metal/water?"""

// Original scene description prompt (for general VLM usage)
const val VLM_SCENE_PROMPT = """Describe what you see in this image in a phrase."""

// Backward compatibility alias
const val VLM_PROMPT = VLM_SCENE_PROMPT