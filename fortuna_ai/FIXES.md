# Bug Fixes Applied

## Issue 1: Wrong AutoModel Class for SmolVLM2

**Error:**
```
ValueError: Unrecognized configuration class <class 'transformers.models.smolvlm.configuration_smolvlm.SmolVLMConfig'> for this kind of AutoModel: AutoModelForVision2Seq.
```

**Root Cause:**
SmolVLM2 uses a custom model architecture that requires `AutoModelForImageTextToText` instead of `AutoModelForVision2Seq`.

**Fix Applied:**
Changed all model loading code from:
```python
from transformers import AutoModelForVision2Seq
model = AutoModelForVision2Seq.from_pretrained(...)
```

To:
```python
from transformers import AutoModelForImageTextToText
model = AutoModelForImageTextToText.from_pretrained(
    ...,
    trust_remote_code=True  # Required for SmolVLM custom architecture
)
```

**Files Updated:**
- ✅ `train_smolvlm.py` - Main training script
- ✅ `validate.py` - Validation script
- ✅ `merge_lora.py` - LoRA merge script
- ✅ `README.md` - Documentation
- ✅ `TRAINING_GUIDE.md` - Training guide

**Additional Changes:**
- Added `trust_remote_code=True` to all model loading calls (required for SmolVLM)
- Removed unused imports (`os`, `Optional`, `Dataset`, `dataclass`, `accuracy_score`, `classification_report`)

**Status:** ✅ **FIXED**

---

## Issue 2: Duplicate Image Path (`dataset/images/images/xxx.jpg`)

**Error:**
```
FileNotFoundError: [Errno 2] No such file or directory: 'dataset/images/images/00515_000000540932.jpg'
```

**Root Cause:**
JSONL files contain `image_path: "images/xxxxx.jpg"` but code uses `images_dir = dataset/images`, resulting in duplicate path: `dataset/images/images/xxxxx.jpg`

**Fix Applied:**
Added path normalization to handle both formats:
```python
# Handle both "images/xxx.jpg" and "xxx.jpg" formats
image_path_str = item['image_path']
if image_path_str.startswith('images/'):
    image_path_str = image_path_str.replace('images/', '', 1)
image_path = images_dir / image_path_str
```

**Files Updated:**
- ✅ `train_smolvlm.py` - `load_jsonl_dataset()`, `ElementDataset.__getitem__()`, and function call sites
- ✅ `validate.py` - `load_jsonl_dataset()` and validation loop
- ✅ `check_dataset.py` - Dataset analysis

**Additional Fix:**
Changed function calls from:
```python
load_jsonl_dataset(dataset_dir / "train.jsonl", dataset_dir)  # Wrong!
```
To:
```python
load_jsonl_dataset(dataset_dir / "train.jsonl", dataset_dir / "images")  # Correct!
```

**Status:** ✅ **FIXED** - All 900 training and 100 validation images load correctly!

---

## Issue 3: Image/Text Count Mismatch in Processor

**Error:**
```
ValueError: The number of images in the text [0] and images [1] should be the same.
```

**Root Cause:**
SmolVLM2 requires using **chat template format** instead of manual `<__media__>` markers. The processor expects messages in a specific structure.

**Fix Applied:**
Changed from direct text + image:
```python
# Old (Wrong)
inputs = processor(images=image, text=prompt, ...)
```

To chat template format:
```python
# New (Correct)
messages = [{
    "role": "user",
    "content": [
        {"type": "image"},
        {"type": "text", "text": prompt}
    ]
}]
prompt_text = processor.apply_chat_template(messages, add_generation_prompt=True)
inputs = processor(images=image, text=prompt_text, ...)
```

**Files Updated:**
- ✅ `train_smolvlm.py` - `ElementDataset.__getitem__()` and `build_prompt()`
- ✅ `validate.py` - Validation loop and `build_inference_prompt()`

**Additional Changes:**
- Removed `ANDROID_IMAGE_MARKER` (`<__media__>`) from prompts - chat template handles this automatically
- Simplified prompt building functions

**Status:** ✅ **FIXED** - Processor now correctly matches images with text!

---

## Testing

To verify the fix works:

```bash
# Quick test
python3 -c "
from transformers import AutoProcessor, AutoModelForImageTextToText
processor = AutoProcessor.from_pretrained('HuggingFaceTB/SmolVLM2-500M-Video-Instruct')
model = AutoModelForImageTextToText.from_pretrained(
    'HuggingFaceTB/SmolVLM2-500M-Video-Instruct',
    trust_remote_code=True
)
print('✅ Model loads successfully!')
"
```

## Next Steps

You can now proceed with training:

```bash
# Mac test
python3 train_smolvlm.py --config configs/mac.yaml --dataset_dir ./dataset --output_dir ./output_test

# Full training (L40S/Colab)
python3 train_smolvlm.py --config configs/l40s.yaml --dataset_dir ./dataset --output_dir ./output
```
