# SmolVLM Training & Inference Flow

## 📊 Current Android Inference Flow (3 Images Sequential)

```
┌─────────────────────────────────────────────────────────────────┐
│                         Android App                              │
│  User captures 3 images for fortune reading                     │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                    Image Preprocessing                           │
│  - Resize to 256x256 (SmolVLMManager.kt:35)                     │
│  - Aspect ratio preserved + black padding                        │
│  - RGBA → RGB conversion (3 channels)                           │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                    Sequential Classification                     │
│                                                                  │
│  FOR EACH of 3 images:                                          │
│    ┌──────────────────────────────────────────────────┐        │
│    │  1. Load image (256x256)                         │        │
│    │  2. Build prompt: "<__media__>\n{prompt}"        │        │
│    │  3. SmolVLM inference                             │        │
│    │     - Vision encoder processes image              │        │
│    │     - Text decoder generates tokens               │        │
│    │     - Max 32 tokens (LLamaAndroid.kt:49)         │        │
│    │  4. Output: "fire" or "water" etc. (single word) │        │
│    └──────────────────────────────────────────────────┘        │
│                                                                  │
│  Result: ["fire", "water", "wood"]                              │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                    Fortune Reading                               │
│  Combine 3 elements → Generate fortune                          │
└─────────────────────────────────────────────────────────────────┘
```

---

## 🎓 Training Flow (SHOULD MATCH ANDROID!)

### ❌ Current Training (WRONG - Mismatch with Android)

```
┌─────────────────────────────────────────────────────────────────┐
│                      Dataset Preparation                         │
│  - Download high-res images (512x512 or larger)                 │
│  - GPT-4V labels at full resolution                             │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                      Fine-tuning (PROBLEM!)                      │
│                                                                  │
│  Input:                                                          │
│    - Image: 512x512 ❌ (Android uses 256x256)                   │
│    - Prompt: "<image>Classify... Context: {reason}... Element:" │
│    - Processor auto-resizes to model's native size (384x384)    │
│                                                                  │
│  Output:                                                         │
│    - Single label: "fire"                                       │
│    - Max tokens: 128 ❌ (Android uses 32)                       │
│                                                                  │
│  Problem: Distribution shift between training and inference!    │
└─────────────────────────────────────────────────────────────────┘
```

### ✅ Corrected Training Flow (MATCHES ANDROID)

```
┌─────────────────────────────────────────────────────────────────┐
│                      Dataset Preparation                         │
│  - Download images (any resolution)                             │
│  - Resize to 256x256 with black padding ✅                      │
│  - RGBA → RGB conversion ✅                                     │
│  - GPT-4V labels on 256x256 version ✅                          │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                      Fine-tuning (CORRECTED)                     │
│                                                                  │
│  Input:                                                          │
│    - Image: 256x256 ✅ (matches Android)                        │
│    - Prompt: "<__media__>\n{classification_prompt}"             │
│      (same format as Android: SmolVLMManager.kt:195)            │
│                                                                  │
│  Output:                                                         │
│    - Single label: "fire"                                       │
│    - Max tokens: 32 ✅ (matches Android)                        │
│                                                                  │
│  Training loss computed only on output tokens (label)           │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                      Quantization (Q8_0)                         │
│  - Convert to GGUF                                              │
│  - 8-bit quantization                                           │
│  - Extract mmproj                                               │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                      Android Deployment                          │
│  - Same 256x256 images ✅                                       │
│  - Same prompt format ✅                                        │
│  - Same max tokens (32) ✅                                      │
│  - No distribution shift! ✅                                    │
└─────────────────────────────────────────────────────────────────┘
```

---

## 🔄 Detailed Training vs Inference Comparison

### Training (Per Image)

```
┌──────────────────────────────────────────────────────────────┐
│ TRAINING EXAMPLE                                             │
├──────────────────────────────────────────────────────────────┤
│                                                              │
│ Input Image:  [256x256 RGB, black padded]                   │
│               ┌─────────────────┐                           │
│               │   🔥            │                           │
│               │      Campfire   │                           │
│               │                 │                           │
│               └─────────────────┘                           │
│                                                              │
│ Prompt:       "<__media__>"                                 │
│               "Classify this image into one of these        │
│                elements: water, land, fire, wood, metal."   │
│               ""                                            │
│               "Context: bright orange campfire flames"      │
│               ""                                            │
│               "Element:"                                    │
│                                                              │
│ Target:       "fire"  [only 1 token!]                       │
│                                                              │
│ Max tokens:   32                                            │
│                                                              │
│ Loss:         Computed ONLY on "fire" token                 │
│               (prompt tokens masked with -100)              │
└──────────────────────────────────────────────────────────────┘
```

### Android Inference (Per Image, 3x Sequential)

```
┌──────────────────────────────────────────────────────────────┐
│ INFERENCE - IMAGE 1 of 3                                     │
├──────────────────────────────────────────────────────────────┤
│                                                              │
│ Input Image:  [256x256 RGB, black padded]                   │
│               ┌─────────────────┐                           │
│               │   🔥            │                           │
│               │      Campfire   │                           │
│               │                 │                           │
│               └─────────────────┘                           │
│                                                              │
│ Prompt:       "<__media__>"                                 │
│               "Classify this image into one of these        │
│                elements: water, land, fire, wood, metal."   │
│               ""                                            │
│               "Element:"                                    │
│                                                              │
│ Generated:    "fire"  [greedy sampling, temperature 0]      │
│                                                              │
│ Max tokens:   32                                            │
│                                                              │
│ Inference time: ~5-8 seconds                                │
└──────────────────────────────────────────────────────────────┘
         │
         ▼
┌──────────────────────────────────────────────────────────────┐
│ INFERENCE - IMAGE 2 of 3                                     │
├──────────────────────────────────────────────────────────────┤
│ Input Image:  [256x256 RGB] 🌊 Ocean                        │
│ Generated:    "water"                                        │
│ Inference time: ~5-8 seconds                                │
└──────────────────────────────────────────────────────────────┘
         │
         ▼
┌──────────────────────────────────────────────────────────────┐
│ INFERENCE - IMAGE 3 of 3                                     │
├──────────────────────────────────────────────────────────────┤
│ Input Image:  [256x256 RGB] 🌳 Tree                         │
│ Generated:    "wood"                                         │
│ Inference time: ~5-8 seconds                                │
└──────────────────────────────────────────────────────────────┘
         │
         ▼
┌──────────────────────────────────────────────────────────────┐
│ FINAL OUTPUT                                                 │
├──────────────────────────────────────────────────────────────┤
│ Results: ["fire", "water", "wood"]                           │
│ Total time: ~15-24 seconds                                   │
│                                                              │
│ → Send to fortune generation system                         │
└──────────────────────────────────────────────────────────────┘
```

---

## 🎯 Key Alignment Points

| Aspect | Android | Training (Current ❌) | Training (Fixed ✅) |
|--------|---------|---------------------|-------------------|
| **Image size** | 256x256 | 512x512 → auto-resize | **256x256** |
| **Image format** | RGB, black padding | RGB | **RGB, black padding** |
| **Prompt format** | `<__media__>\n{prompt}` | `<image>{prompt}` | **`<__media__>\n{prompt}`** |
| **Max tokens** | 32 | 128 | **32** |
| **Output** | Single element | Single element | **Single element** |
| **Sampling** | Greedy (temp=0) | Greedy | **Greedy** |
| **# Images** | 3 (sequential) | 1 | **1 (train 3x separately)** |

---

## 📐 Image Preprocessing Details

### Android Resize Logic (SmolVLMManager.kt:278-321)

```kotlin
private fun resizeImageForVLM(bitmap: Bitmap, targetSize: Int): Bitmap {
    // targetSize = 256

    // 1. Calculate scale factor (maintain aspect ratio)
    val scaleFactor = minOf(
        targetSize.toFloat() / width,
        targetSize.toFloat() / height
    )

    // 2. Scale image
    val scaledBitmap = Bitmap.createScaledBitmap(...)

    // 3. Add black padding to make square
    val paddedBitmap = Bitmap.createBitmap(256, 256, ARGB_8888)
    canvas.drawColor(Color.BLACK)  // Black background
    canvas.drawBitmap(scaledBitmap, left, top, null)  // Center

    return paddedBitmap  // Always 256x256
}
```

### Training Should Match (Python)

```python
def resize_for_android(image: Image, target_size: int = 256) -> Image:
    """Match Android resize logic exactly"""

    # 1. Calculate scale factor
    scale = min(target_size / image.width, target_size / image.height)
    new_w = int(image.width * scale)
    new_h = int(image.height * scale)

    # 2. Resize
    resized = image.resize((new_w, new_h), Image.Resampling.LANCZOS)

    # 3. Create 256x256 canvas with black padding
    padded = Image.new("RGB", (target_size, target_size), (0, 0, 0))

    # 4. Center paste
    left = (target_size - new_w) // 2
    top = (target_size - new_h) // 2
    padded.paste(resized, (left, top))

    return padded  # 256x256 RGB with black padding
```

---

## 🔢 Token Budget Analysis

### Prompt Structure

```
<__media__>                                    # Image marker (1 token)
Classify this image into one of these         # ~8 tokens
elements: water, land, fire, wood, metal.     # ~10 tokens

Element:                                       # 2 tokens
```

**Total prompt: ~21 tokens**

### Generation Budget

```
Max generation: 32 tokens
Prompt: ~21 tokens
Available for output: ~11 tokens
Actual output: "fire" = 1 token ✅

Plenty of headroom!
```

---

## ⚠️ Critical Issues to Fix

### Issue 1: Image Resolution Mismatch
- **Current**: Training on 512x512, inference on 256x256
- **Impact**: Model sees different image quality → accuracy drop
- **Fix**: Resize to 256x256 BEFORE GPT-4V labeling

### Issue 2: Prompt Format Mismatch
- **Current**: Training uses `<image>`, Android uses `<__media__>`
- **Impact**: Model doesn't recognize image marker
- **Fix**: Use `<__media__>` in training

### Issue 3: Max Tokens Mismatch
- **Current**: Training allows 128 tokens, Android limits to 32
- **Impact**: Model might generate longer outputs (cut off)
- **Fix**: Set max_tokens=32 in training

### Issue 4: Context in Inference
- **Current**: Training has "Context: {reason}", inference doesn't
- **Impact**: Model expects context field
- **Fix**: Remove context from training OR add to inference

---

## ✅ Recommended Training Format (Final)

```python
# Training example
{
  "image": resized_to_256x256_with_padding,  # ✅ Matches Android
  "prompt": "<__media__>\nClassify this image into one of these elements: water, land, fire, wood, metal.\n\nElement:",  # ✅ Matches Android
  "target": "fire",  # ✅ Single word
  "max_tokens": 32,  # ✅ Matches Android
}
```

This ensures **ZERO distribution shift** between training and inference!
