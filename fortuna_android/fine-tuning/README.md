# SmolVLM Fine-tuning for Element Classification

Complete pipeline for fine-tuning SmolVLM-500M to classify images into 5 natural elements (water, land, fire, wood, metal) for Android deployment.

## Overview

This pipeline fine-tunes [SmolVLM2-500M-Video-Instruct](https://huggingface.co/HuggingFaceM4/SmolVLM2-500M-Video-Instruct) using:
- **LoRA** (Low-Rank Adaptation) for efficient training
- **GPT-4V** for automated dataset labeling
- **Label-only output** to minimize inference tokens (faster on mobile)
- **Weights & Biases** for experiment tracking
- **GGUF Q8_0** quantization for Android deployment

**Key optimization**: Model learns from detailed reasoning during training but outputs only the element label during inference, saving ~10-20 tokens per classification.

---

## Prerequisites

### Hardware
- **GPU**: L40S, A100, or any GPU with ≥24GB VRAM
- **Storage**: ~30GB for datasets and models
- **RAM**: ≥32GB recommended

### Software
- Python 3.10+
- CUDA 12.1+ (for GPU acceleration)
- Git LFS (for large model files)

### API Keys
- **OpenAI API key** (for GPT-4V labeling)
- **Unsplash API key** (optional, for image downloads)
- **Weights & Biases account** (for experiment tracking)

---

## Installation

### 1. Clone Repository
```bash
cd fortuna_android/fine-tuning
```

### 2. Create Virtual Environment
```bash
python3 -m venv venv
source venv/bin/activate  # On Windows: venv\Scripts\activate
```

### 3. Install Dependencies
```bash
pip install -r requirements.txt
```

### 4. Set Environment Variables
```bash
export OPENAI_API_KEY="sk-..."
export UNSPLASH_ACCESS_KEY="..."  # Optional
export WANDB_API_KEY="..."
```

Or create a `.env` file:
```bash
OPENAI_API_KEY=sk-...
UNSPLASH_ACCESS_KEY=...
WANDB_API_KEY=...
```

---

## Pipeline Steps

### Step 1: Prepare Dataset (2-4 hours)

Collect 1000 everyday photos and label them using GPT-4V:

```bash
python prepare_dataset.py \
    --output_dir ./dataset \
    --num_images 1000 \
    --openai_api_key $OPENAI_API_KEY \
    --min_confidence 0.7
```

**What it does:**
1. Downloads diverse everyday images from Unsplash
2. Labels each image using GPT-4V with structured prompts
3. Filters low-confidence labels (< 0.7)
4. Creates train/val split (90/10)
5. Saves metadata with reasons (for training context)

**Output:**
```
dataset/
├── images/          # 1000+ JPEG images
├── train.json       # ~900 training examples
├── val.json         # ~100 validation examples
└── metadata.json    # Full dataset metadata
```

**Cost estimate:** ~$10-20 (GPT-4V API calls)

---

### Step 2: Fine-tune Model (4-8 hours on L40S)

Train SmolVLM with LoRA:

```bash
python finetune_smolvlm.py \
    --dataset_dir ./dataset \
    --output_dir ./models/smolvlm-element-classifier \
    --wandb_project smolvlm-element-classification \
    --batch_size 4 \
    --gradient_accumulation_steps 4 \
    --num_epochs 3 \
    --learning_rate 2e-4 \
    --bf16
```

**Key arguments:**
- `--lora_r 16`: LoRA rank (higher = more parameters, better quality)
- `--lora_alpha 32`: LoRA scaling factor
- `--bf16`: Use bfloat16 precision (faster, L40S/A100 only)
- `--wandb_project`: W&B project name for tracking

**Training strategy:**
- **Input**: `<image>Classify this image... Context: {reason} Element:`
- **Output**: `fire` (label only, no explanation)
- **Loss**: Only computed on output tokens (prompt ignored)

**W&B Tracking:**
- Training/validation loss
- Accuracy curves
- Learning rate schedule
- GPU memory usage
- Per-class metrics

**Output:**
```
models/smolvlm-element-classifier/
├── final/           # Final model (merged base + LoRA)
├── lora_adapter/    # LoRA weights only
└── logs/            # Training logs
```

---

### Step 3: Merge LoRA Weights (5-10 minutes)

Merge LoRA adapter into base model:

```bash
python merge_lora.py \
    --base_model HuggingFaceM4/SmolVLM2-500M-Video-Instruct \
    --lora_path ./models/smolvlm-element-classifier/lora_adapter \
    --output_path ./models/smolvlm-element-classifier/merged
```

**Why merge?**
- GGUF conversion requires full model weights
- Simplifies deployment (single model file)
- No runtime overhead for LoRA adapter loading

---

### Step 4: Convert to GGUF Q8_0 (10-20 minutes)

Convert to quantized GGUF format for Android:

```bash
bash convert_to_gguf.sh \
    ./models/smolvlm-element-classifier/merged \
    ./models/gguf
```

**What it does:**
1. Converts to FP16 GGUF (intermediate)
2. Quantizes to Q8_0 (8-bit weights)
3. Extracts mmproj (vision encoder)
4. Cleans up intermediate files

**Output:**
```
models/gguf/
├── SmolVLM2-500M-Element-Q8_0.gguf           # ~417MB
└── mmproj-SmolVLM2-500M-Element-Q8_0.gguf    # ~104MB
```

**Size comparison:**
- Original (FP16): ~1GB
- Q8_0: ~417MB (60% reduction)
- Q4_K_M: ~220MB (alternative, slight quality loss)

---

### Step 5: Validate Model (10-30 minutes)

Test accuracy on validation set:

```bash
python validate_model.py \
    --model_path ./models/smolvlm-element-classifier/final \
    --dataset_dir ./dataset \
    --output_path ./validation_results.json \
    --plot_cm
```

**Metrics:**
- Overall accuracy
- Per-class precision/recall/F1
- Confusion matrix
- Error analysis

**Expected accuracy:** ≥85% (depends on dataset quality)

---

### Step 6: Deploy to Android

1. **Copy GGUF files to Android project:**
   ```bash
   cp models/gguf/*.gguf ../app/src/main/assets/models/
   ```

2. **Update model names in `app/build.gradle.kts`:**
   ```kotlin
   val VLM_MODEL_FILENAME = "SmolVLM2-500M-Element-Q8_0.gguf"
   val VLM_MMPROJ_FILENAME = "mmproj-SmolVLM2-500M-Element-Q8_0.gguf"
   ```

3. **Update constants in `SmolVLMManager.kt`:**
   ```kotlin
   private const val MODEL_FILENAME = "SmolVLM2-500M-Element-Q8_0.gguf"
   private const val MMPROJ_FILENAME = "mmproj-SmolVLM2-500M-Element-Q8_0.gguf"
   ```

4. **Update classification prompt (optional):**
   ```kotlin
   // In PromptPreferences.kt
   fun getDefaultPrompt(): String {
       return "Classify this image into one of these elements: water, land, fire, wood, metal.\n\nElement:"
   }
   ```

5. **Rebuild APK:**
   ```bash
   cd ..
   ./gradlew assembleDebug
   ```

6. **Test on device:**
   - Open VLMTestActivity
   - Capture 3 images
   - Verify classification accuracy and speed

---

## Inference Performance

### Expected Results (on Android)

| Device | Single Image | 3 Images (Sequential) |
|--------|-------------|----------------------|
| Snapdragon 8 Gen 2 | ~5-8s | ~15-24s |
| Snapdragon 8 Gen 3 | ~4-6s | ~12-18s |
| Dimensity 9200 | ~6-10s | ~18-30s |

**Optimizations applied:**
- 256x256 image size (optimal for SmolVLM on mobile)
- Q8_0 quantization (60% size reduction)
- Label-only output (~5 tokens vs ~15-25 tokens)
- Greedy sampling (temperature 0, deterministic)
- GPU acceleration (OpenCL)

---

## Advanced Usage

### Custom Dataset

If you have your own labeled dataset:

1. **Format:**
   ```json
   [
     {
       "image_id": "00001",
       "image_path": "images/00001.jpg",
       "element": "fire",
       "reason": "bright orange campfire flames",
       "confidence": 0.95
     }
   ]
   ```

2. **Split into train/val:**
   ```python
   import json
   import random

   with open("custom_data.json") as f:
       data = json.load(f)

   random.shuffle(data)
   split = int(len(data) * 0.9)

   with open("train.json", "w") as f:
       json.dump(data[:split], f, indent=2)

   with open("val.json", "w") as f:
       json.dump(data[split:], f, indent=2)
   ```

3. **Fine-tune as usual**

### Hyperparameter Tuning

Experiment with W&B sweeps:

```yaml
# sweep.yaml
program: finetune_smolvlm.py
method: bayes
metric:
  name: accuracy
  goal: maximize
parameters:
  learning_rate:
    min: 1e-5
    max: 5e-4
  lora_r:
    values: [8, 16, 32]
  lora_alpha:
    values: [16, 32, 64]
```

```bash
wandb sweep sweep.yaml
wandb agent <sweep_id>
```

### Alternative Quantizations

For faster inference with slight quality trade-off:

```bash
# Q4_K_M: ~220MB, faster but less accurate
llama-quantize \
    models/gguf/smolvlm-element-fp16.gguf \
    models/gguf/SmolVLM2-500M-Element-Q4_K_M.gguf \
    Q4_K_M
```

---

## Troubleshooting

### Out of Memory (OOM) during training

**Solution 1:** Reduce batch size
```bash
python finetune_smolvlm.py --batch_size 2 --gradient_accumulation_steps 8
```

**Solution 2:** Enable gradient checkpointing (already on)

**Solution 3:** Use 8-bit quantization during training
```bash
python finetune_smolvlm.py --load_in_8bit
```

### GPT-4V labeling errors

**Check API rate limits:**
```python
# In prepare_dataset.py, increase sleep time
time.sleep(1.0)  # From 0.5 to 1.0
```

**Reduce parallel workers:**
```bash
python prepare_dataset.py --num_workers 2  # From 4 to 2
```

### GGUF conversion fails

**Make sure llama.cpp is up to date:**
```bash
cd ../llama.cpp
git pull
make clean
make
```

**Check for mmproj extraction script:**
```bash
ls llama.cpp/examples/llava/convert_image_encoder_to_gguf.py
```

### Low validation accuracy (< 70%)

**Possible causes:**
1. **Dataset quality:** Check GPT-4V labels manually
2. **Class imbalance:** Ensure ~200 images per class
3. **Ambiguous images:** Increase `min_confidence` to 0.8
4. **Underfitting:** Increase `num_epochs` or `lora_r`

---

## File Structure

```
fine-tuning/
├── README.md                    # This file
├── requirements.txt             # Python dependencies
├── prepare_dataset.py           # Dataset collection & labeling
├── finetune_smolvlm.py         # LoRA fine-tuning script
├── merge_lora.py               # Merge LoRA into base model
├── convert_to_gguf.sh          # GGUF quantization script
├── validate_model.py           # Model evaluation
├── dataset/                    # Generated by prepare_dataset.py
│   ├── images/
│   ├── train.json
│   ├── val.json
│   └── metadata.json
└── models/                     # Generated by fine-tuning
    └── smolvlm-element-classifier/
        ├── final/              # Final HF model
        ├── lora_adapter/       # LoRA weights
        ├── merged/             # Merged model
        └── logs/               # Training logs
```

---

## Citation

If you use this pipeline, please cite:

```bibtex
@misc{smolvlm-element-classifier,
  title={SmolVLM Fine-tuning for Element Classification},
  author={Fortuna Android Team},
  year={2025},
  howpublished={\url{https://github.com/your-repo/fortuna_android}}
}
```

---

## License

This fine-tuning pipeline is MIT licensed. The base SmolVLM model follows its original license from HuggingFace.

---

## Support

For issues or questions:
1. Check [Troubleshooting](#troubleshooting) section
2. Review W&B training logs
3. Open an issue on GitHub

**Happy fine-tuning! 🚀**
