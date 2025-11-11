# SmolVLM Element Classification Training

Train SmolVLM-500M to classify images into five Chinese elements (water, fire, land, wood, metal) for the Fortuna Android app.

## Quick Start

### 1. Install Dependencies

```bash
pip install -r requirements.txt
```

**Note:** If you're on Mac M1/M2, ensure you have PyTorch with MPS support:
```bash
pip install torch torchvision --index-url https://download.pytorch.org/whl/cpu
```

### 2. Verify Dataset

Your dataset should be in this structure:
```
dataset/
├── images/           # 256x256 JPEG images
├── train.jsonl       # Training samples (~900)
├── val.jsonl         # Validation samples (~100)
└── labels.jsonl      # All labels
```

Each JSONL line should have:
```json
{
  "image_id": "...",
  "image_path": "images/xxxxx.jpg",
  "element": "water|land|fire|wood|metal",
  "reason": "...",
  ...
}
```

### 3. Train the Model

#### On Mac (Testing)
```bash
python train_smolvlm.py --config configs/mac.yaml --dataset_dir ./dataset --output_dir ./output
```

#### On L40S Server (16GB RAM)
```bash
python train_smolvlm.py --config configs/l40s.yaml --dataset_dir ./dataset --output_dir ./output
```

#### On Colab Pro
```bash
python train_smolvlm.py --config configs/colab.yaml --dataset_dir ./dataset --output_dir ./output --wandb
```

**Training time estimates:**
- Mac M1: ~2-3 hours (testing only, not recommended for full training)
- L40S: ~30-45 minutes
- Colab (T4/A100): ~20-40 minutes

### 4. Validate the Model

```bash
python validate.py --model_dir ./output/final --dataset_dir ./dataset
```

Quick test on 10 samples:
```bash
python validate.py --model_dir ./output/final --dataset_dir ./dataset --max_samples 10
```

Save results to JSON:
```bash
python validate.py --model_dir ./output/final --dataset_dir ./dataset --output results.json
```

## Configuration Files

### `configs/mac.yaml` - Mac M1/M2
- Batch size: 1
- Gradient accumulation: 8
- No bf16 (MPS doesn't support it yet)
- Good for: Testing, debugging

### `configs/l40s.yaml` - L40S Server (16GB RAM)
- Batch size: 2
- Gradient accumulation: 8
- bf16 enabled
- Gradient checkpointing enabled
- Good for: Production training with limited RAM

### `configs/colab.yaml` - Google Colab Pro
- Batch size: 4
- Gradient accumulation: 4
- bf16 enabled
- Good for: Fast training with more RAM

## Expected Results

**Target Accuracy:** 80-90% on validation set

Per-element accuracy varies:
- **Water**: ~85% (rivers, ocean, rain)
- **Fire**: ~90% (flames, lights, sun)
- **Land**: ~80% (ground, roads, mountains)
- **Wood**: ~85% (trees, plants, foliage)
- **Metal**: ~80% (phones, cars, buildings)

## Next Steps: Android Deployment

### 1. Merge LoRA Adapters

The trained model is a LoRA adapter. You need to merge it with the base model:

```python
from transformers import AutoModelForVision2Seq, AutoProcessor
from peft import PeftModel

# Load base model
base_model = AutoModelForVision2Seq.from_pretrained(
    "HuggingFaceM4/SmolVLM2-500M-Video-Instruct"
)

# Load LoRA adapter
model = PeftModel.from_pretrained(base_model, "./output/final")

# Merge and unload
merged_model = model.merge_and_unload()

# Save merged model
merged_model.save_pretrained("./output/merged")
processor = AutoProcessor.from_pretrained("./output/final")
processor.save_pretrained("./output/merged")
```

### 2. Convert to GGUF

Use llama.cpp's conversion script:

```bash
# Clone llama.cpp if you haven't
git clone https://github.com/ggerganov/llama.cpp
cd llama.cpp

# Convert to GGUF (Q8_0 quantization for Android)
python convert-hf-to-gguf.py ../output/merged \
    --outfile ../output/SmolVLM2-500M-Element-Q8_0.gguf \
    --outtype q8_0

# Also convert the mmproj (vision encoder)
python convert-hf-to-gguf.py ../output/merged \
    --outfile ../output/mmproj-SmolVLM2-500M-Element-Q8_0.gguf \
    --outtype q8_0 \
    --vocab-type bpe
```

### 3. Deploy to Android

1. Copy GGUF files to Android assets:
   ```
   fortuna_android/app/src/main/assets/models/
   ├── SmolVLM2-500M-Element-Q8_0.gguf
   └── mmproj-SmolVLM2-500M-Element-Q8_0.gguf
   ```

2. Update `SmolVLMManager.kt` to use new model:
   ```kotlin
   private const val MODEL_FILENAME = "SmolVLM2-500M-Element-Q8_0.gguf"
   private const val MMPROJ_FILENAME = "mmproj-SmolVLM2-500M-Element-Q8_0.gguf"
   ```

3. Update the prompt in `ARRenderer.kt` to match training format:
   ```kotlin
   const val VLM_PROMPT = "Classify this image into one of these elements: water, land, fire, wood, metal.\n\nElement:"
   ```

## Troubleshooting

### Out of Memory (OOM)

**On Mac:**
- Reduce batch size to 1
- Increase gradient accumulation steps
- Close other applications

**On L40S/Colab:**
- Enable gradient checkpointing (should already be enabled)
- Reduce batch size
- Use bf16 if available

### Low Accuracy

**Check dataset quality:**
```bash
# Verify dataset distribution
python -c "
import json
from collections import Counter

with open('dataset/train.jsonl') as f:
    elements = [json.loads(line)['element'] for line in f]
    print(Counter(elements))
"
```

**Possible fixes:**
- Train for more epochs (3-5 instead of 2-3)
- Increase LoRA rank (r=32 instead of 16)
- Check for class imbalance in dataset

### Model generates wrong format

The model should output just the element name (e.g., "water"). If it outputs extra text:

1. Check the training data format
2. Ensure labels are clean (no extra whitespace)
3. Increase max_new_tokens during inference to 32

## Advanced: Cropped Dataset Enhancement

After the baseline POC works, enhance with cropped bounding boxes:

1. Run object detection on training images
2. Extract bounding box regions
3. Create augmented dataset with both full images + crops
4. Retrain with mixed data

This reduces distribution shift between training (full images) and inference (cropped regions from object detector).

## File Structure

```
fortuna_ai/
├── README.md              # This file
├── requirements.txt       # Python dependencies
├── train_smolvlm.py      # Main training script
├── validate.py           # Validation script
├── configs/
│   ├── mac.yaml          # Mac M1/M2 config
│   ├── l40s.yaml         # L40S server config
│   └── colab.yaml        # Colab config
├── dataset/              # Your dataset
│   ├── images/
│   ├── train.jsonl
│   └── val.jsonl
└── output/               # Training output
    ├── checkpoint-*/     # Training checkpoints
    └── final/            # Best model
```

## License

This training code is for the Fortuna Android project.
