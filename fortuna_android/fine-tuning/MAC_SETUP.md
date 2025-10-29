# Mac Testing Guide - SmolVLM Fine-tuning

Quick validation on MacBook (M1/M2/M3) before deploying to L40S GPU server.

## 🎯 Purpose

Test the entire fine-tuning pipeline on your Mac with **10 images (~30-60 minutes)** to validate code works before committing **5-10 hours** on L40S for full 1000-image training.

**Why test on Mac first?**
- ✅ Catch bugs early (dataset prep, training, conversion)
- ✅ Validate Android alignment (256x256, 32 tokens, `<__media__>`)
- ✅ Test rich teacher → simple student prompts
- ✅ Ensure GGUF conversion works
- ✅ Save L40S time and money

---

## 💻 Hardware Requirements

### Minimum (Will work but slow)
- **CPU**: M1 (8-core)
- **RAM**: 16GB
- **Storage**: 10GB free
- **macOS**: 12.3+ (for MPS support)

### Recommended (Faster)
- **CPU**: M1 Pro/Max, M2, M3 (10+ cores)
- **RAM**: 32GB
- **Storage**: 20GB free
- **macOS**: Latest (Sonoma 14.0+)

### Your Mac (From conversation)
- **Model**: M1 Pro
- **RAM**: 16GB
- **Status**: ✅ Should work fine!

---

## 🚀 Quick Start (One-Liner)

```bash
# Export API keys
export OPENAI_API_KEY="sk-..."

# Run test (10 images, ~30-60 min)
chmod +x test_mac.sh
./test_mac.sh
```

**That's it!** The script handles everything automatically.

---

## 📋 Detailed Setup

### 1. Install Homebrew (if needed)

```bash
# Check if installed
brew --version

# If not installed:
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
```

### 2. Install Python 3.10+

```bash
# Install Python
brew install python@3.10

# Verify
python3 --version  # Should show 3.10.x or higher
```

### 3. Install PyTorch with MPS Support

```bash
# Create virtual environment
python3 -m venv venv
source venv/bin/activate

# Install PyTorch (Mac optimized)
pip install torch torchvision torchaudio

# Verify MPS is available
python3 -c "import torch; print('MPS available:', torch.backends.mps.is_available())"
# Should print: MPS available: True
```

**⚠️ If MPS not available:**
- Make sure macOS ≥ 12.3
- Update PyTorch: `pip install --upgrade torch`
- Restart terminal

### 4. Install Dependencies

```bash
cd fine-tuning
pip install -r requirements.txt
```

**Expected packages:**
```
torch>=2.1.0
transformers>=4.36.0
peft>=0.7.0
openai>=1.6.0
wandb>=0.16.0
Pillow>=10.0.0
tqdm>=4.66.0
scikit-learn>=1.3.0
```

### 5. Configure API Keys

```bash
# OpenAI (required for GPT-4V labeling)
export OPENAI_API_KEY="sk-..."

# W&B (optional but recommended)
wandb login
# Or: export WANDB_API_KEY="..."
```

**💡 Tip:** Add to `~/.zshrc` for persistence:
```bash
echo 'export OPENAI_API_KEY="sk-..."' >> ~/.zshrc
source ~/.zshrc
```

### 6. Clone llama.cpp (for GGUF conversion)

```bash
cd ..  # Go to parent directory
git clone https://github.com/ggerganov/llama.cpp
cd llama.cpp
make
cd ../fine-tuning
```

---

## 🧪 Running the Test

### Option 1: Automated Test (Recommended)

```bash
chmod +x test_mac.sh
./test_mac.sh
```

**What it does:**
1. ✅ Downloads COCO dataset (~1GB, one-time)
2. ✅ Labels 10 images with GPT-4V (~5-10 min)
3. ✅ Fine-tunes on Mac MPS (~20-40 min)
4. ✅ Merges LoRA weights (~2-5 min)
5. ✅ Validates accuracy (~5 min)

**Total time:** ~30-60 minutes

### Option 2: Manual Step-by-Step (For debugging)

#### Step 1: Prepare Dataset (10 images)

```bash
python3 prepare_dataset_coco_v3.py \
    --output_dir ./test_dataset_mac \
    --num_images 10 \
    --openai_api_key "$OPENAI_API_KEY" \
    --min_confidence 0.7
```

**Expected output:**
```
Dataset Preparation V3: Rich Teacher, Simple Student
🎯 Target: 10 labeled images
🎓 Teacher Prompt: Rich & Detailed (300+ tokens)
🎒 Student Prompt: Simple & Android-aligned (20 tokens)

📥 Downloading COCO dataset...
✅ COCO dataset already cached

🎲 Selecting diverse images...
✅ Selected 20 candidate images

🏷️  Labeling with GPT-4V (rich analysis)...
Labeling: 100%|████████████████| 20/20 [05:30<00:00]
✅ Labeled 10 images with rich analysis

📊 Dataset Statistics:
   Total: 10
   water: 2 (20.0%)
   land:  3 (30.0%)
   fire:  1 (10.0%)
   wood:  2 (20.0%)
   metal: 2 (20.0%)

✅ Train: 9 images
✅ Val:   1 images
```

**💰 Cost:** ~$0.10-0.20 (GPT-4V API)

#### Step 2: Fine-tune (Mac MPS)

```bash
python3 finetune_mac.py \
    --dataset_dir ./test_dataset_mac \
    --output_dir ./test_model_mac \
    --batch_size 1 \
    --gradient_accumulation_steps 4 \
    --num_epochs 1 \
    --learning_rate 2e-4 \
    --lora_r 8 \
    --wandb_project smolvlm-mac-test
```

**Mac-specific settings:**
- `--batch_size 1` - For 16GB RAM
- `--lora_r 8` - Reduced LoRA rank (16 on L40S)
- `--num_epochs 1` - Quick test (3 on L40S)
- Uses FP16 (not BF16 - MPS doesn't support it)

**Expected output:**
```
🍎 SmolVLM Mac Training (MPS)
📁 Dataset: ./test_dataset_mac
💾 Output: ./test_model_mac
🖥️  Device: MPS (Metal Performance Shaders)
🧠 Memory: Optimized for 16GB

📥 Loading model for MPS...
✅ Model loaded on MPS (500.3M parameters)

🔧 Applying LoRA (reduced rank for Mac)...
✅ LoRA applied
   Trainable: 2,097,152 (0.42%)

📊 Preparing datasets...
✅ Train: 9 examples
✅ Val:   1 examples

🚀 Starting Mac training...
Epoch 1/1: 100%|████████████| 9/9 [20:15<00:00]
✅ Training complete!

📊 Final evaluation...
  eval_loss: 0.2341
  eval_accuracy: 0.9000
```

**⏱️ Duration:** ~20-40 minutes (depends on Mac model)

#### Step 3: Merge LoRA

```bash
python3 merge_lora.py \
    --base_model HuggingFaceM4/SmolVLM2-500M-Video-Instruct \
    --lora_path ./test_model_mac/lora_adapter \
    --output_path ./test_model_mac/merged \
    --device mps
```

**Expected output:**
```
🔧 Merging LoRA weights (MPS)
✅ Base model loaded
✅ LoRA weights loaded
✅ Merged successfully
💾 Saved to ./test_model_mac/merged
```

**⏱️ Duration:** ~2-5 minutes

#### Step 4: Validate

```bash
python3 validate_model.py \
    --model_path ./test_model_mac/final \
    --dataset_dir ./test_dataset_mac \
    --output_path ./logs/mac_test_results.json \
    --device mps
```

**Expected output:**
```
📊 Validation Results:
   Accuracy: 90.0% (9/10 correct)

Per-element accuracy:
   water: 100.0% (2/2)
   land:  66.7% (2/3)
   fire:  100.0% (1/1)
   wood:  100.0% (2/2)
   metal: 100.0% (2/2)

✅ Results saved to ./logs/mac_test_results.json
```

**⏱️ Duration:** ~5 minutes

---

## 📊 Performance Expectations

### Mac Test (10 images, 1 epoch)

| Step | Mac Mini M1 (16GB) | MacBook Pro M1 Pro (16GB) | MacBook Pro M3 Max (64GB) |
|------|-------------------|---------------------------|--------------------------|
| Dataset prep | 5-10 min | 5-10 min | 5-10 min |
| Training | 30-40 min | 20-30 min | 10-15 min |
| Merge LoRA | 3-5 min | 2-4 min | 1-2 min |
| Validate | 5-8 min | 3-5 min | 2-3 min |
| **Total** | **45-63 min** | **30-49 min** | **18-30 min** |

### L40S Full Training (1000 images, 3 epochs)

| Step | Duration | Notes |
|------|----------|-------|
| Dataset prep | 30-60 min | GPT-4V labeling |
| Training | 4-8 hours | Much faster than Mac! |
| Merge LoRA | 5 min | |
| GGUF conversion | 10 min | |
| Validate | 10 min | |
| **Total** | **5-10 hours** | |

**💡 Mac is ~10-20x slower than L40S for training!**

---

## ⚠️ Common Issues & Fixes

### Issue 1: MPS Not Available

**Symptom:**
```
⚠️  Warning: MPS (Metal) not available
```

**Causes & Fixes:**
1. **macOS too old** → Upgrade to macOS 12.3+
2. **PyTorch too old** → `pip install --upgrade torch`
3. **Intel Mac** → Use CPU mode: remove `--device mps` flag

**Verify:**
```bash
python3 -c "import torch; print(torch.backends.mps.is_available())"
```

### Issue 2: Out of Memory (OOM)

**Symptom:**
```
RuntimeError: MPS backend out of memory
```

**Fixes:**
```bash
# 1. Reduce batch size (already at 1)
# 2. Reduce LoRA rank
python3 finetune_mac.py --lora_r 4  # Instead of 8

# 3. Reduce image size (not recommended - breaks Android alignment)
# 4. Close other apps to free RAM
```

**Memory usage:**
- Mac (16GB): Batch size 1, LoRA rank 8 → ~12-14GB used ✅
- Mac (8GB): Will likely OOM ❌

### Issue 3: BF16 Not Supported

**Symptom:**
```
RuntimeError: MPS does not support BF16
```

**Fix:** Already handled in `finetune_mac.py`
- Uses FP16 (not BF16)
- If you see this, make sure you're using `finetune_mac.py` (not `finetune_smolvlm_v2.py`)

### Issue 4: GPT-4V Rate Limits

**Symptom:**
```
❌ Error: 429 Too Many Requests
```

**Fix:** Increase sleep in `prepare_dataset_coco_v3.py:366`
```python
time.sleep(1.0)  # Increase from 0.5
```

### Issue 5: llama.cpp Not Found

**Symptom:**
```
❌ Error: ../llama.cpp/convert_hf_to_gguf.py not found
```

**Fix:**
```bash
cd ..
git clone https://github.com/ggerganov/llama.cpp
cd llama.cpp && make
cd ../fine-tuning
```

### Issue 6: Slow Training (>1 hour for 10 images)

**Normal if:**
- Mac Mini M1 (8-core)
- 16GB RAM
- First time (downloading models)

**Speed up:**
- Close background apps
- Plug into power
- Enable "High Performance" mode (if available)
- Reduce LoRA rank: `--lora_r 4`

### Issue 7: W&B Login Failed

**Symptom:**
```
wandb: ERROR Failed to login
```

**Fix:**
```bash
# Option 1: Login interactively
wandb login

# Option 2: Set API key
export WANDB_API_KEY="..."

# Option 3: Disable W&B (not recommended)
export WANDB_MODE=disabled
```

---

## ✅ Success Criteria

After Mac test completes, verify:

### 1. Dataset Quality
```bash
# Check dataset stats
jq '. | length' test_dataset_mac/train.json  # Should be ~9
jq '. | length' test_dataset_mac/val.json    # Should be ~1

# Check labels
jq '.[0] | {element, reason, confidence}' test_dataset_mac/train.json
```

**Expected:**
- All images have `element` in [water, land, fire, wood, metal]
- `reason` is detailed (100+ chars)
- `confidence` ≥ 0.7

### 2. Training Metrics
```bash
# Check final accuracy
cat logs/mac_test_results.json | jq '.accuracy'
```

**Expected:**
- Accuracy ≥ 70% (on 10 images)
- No NaN or inf values
- Loss decreasing over epochs

### 3. Model Output Format
```bash
# Test inference
python3 -c "
from transformers import AutoProcessor, AutoModelForVision2Seq
from PIL import Image

model = AutoModelForVision2Seq.from_pretrained('./test_model_mac/final')
processor = AutoProcessor.from_pretrained('./test_model_mac/final')

img = Image.open('test_dataset_mac/images/00000_something.jpg')
inputs = processor(images=img, text='<__media__>\nClassify this image into one of these elements: water, land, fire, wood, metal.\n\nElement:', return_tensors='pt')

output = model.generate(**inputs, max_new_tokens=32)
print(processor.decode(output[0], skip_special_tokens=True))
"
```

**Expected output:**
```
water
```

**NOT:**
```
The image shows water with blue color...  # ❌ Too verbose
```

### 4. File Structure
```bash
tree test_model_mac
```

**Expected:**
```
test_model_mac/
├── final/              # Merged model
│   ├── config.json
│   ├── model.safetensors
│   └── ...
└── lora_adapter/       # LoRA weights
    ├── adapter_config.json
    └── adapter_model.safetensors
```

---

## 🚀 Mac → L40S Workflow

Once Mac test passes (accuracy ≥70%, outputs single words):

### 1. Clean Up Mac Test Data

```bash
# Remove test files (optional)
rm -rf test_dataset_mac test_model_mac
```

### 2. Deploy to L40S

```bash
# On L40S server
cd fine-tuning
export OPENAI_API_KEY="sk-..."
export WANDB_API_KEY="..."

# Run full pipeline (1000 images, 3 epochs)
chmod +x run_remote.sh
nohup ./run_remote.sh > logs/training.log 2>&1 &

# Monitor
tail -f logs/training.log
watch -n 1 nvidia-smi
```

### 3. Expected L40S Results

**After 5-10 hours:**
- ✅ 1000 labeled images (90% train, 10% val)
- ✅ Validation accuracy ≥85%
- ✅ GGUF files (~521MB total)
- ✅ Model outputs single word labels

### 4. Download Models from L40S

```bash
# On local Mac
scp user@l40s-server:/path/to/fine-tuning/models/gguf/*.gguf ./local-models/
```

**Files to download:**
- `smolvlm-element-classifier-q8_0.gguf` (~260MB)
- `smolvlm-element-classifier-mmproj-q8_0.gguf` (~261MB)

### 5. Deploy to Android

```bash
# Copy to Android assets
cp local-models/*.gguf ../app/src/main/assets/models/

# Update config (if needed)
# Edit: app/src/main/assets/models/config.json

# Rebuild APK
cd ../
./gradlew assembleDebug
```

---

## 🔍 Debugging

### Enable Verbose Logging

```bash
# Mac training
python3 finetune_mac.py \
    --dataset_dir ./test_dataset_mac \
    --output_dir ./test_model_mac \
    --logging_steps 1  # Log every step
```

### Check GPU Usage

```bash
# Monitor MPS usage
sudo powermetrics --samplers gpu_power -i 1000
```

### Profile Memory

```bash
# Add to finetune_mac.py
import torch.mps
print(f"MPS memory allocated: {torch.mps.current_allocated_memory() / 1e9:.2f} GB")
```

### Save Intermediate Checkpoints

```bash
python3 finetune_mac.py \
    --dataset_dir ./test_dataset_mac \
    --output_dir ./test_model_mac \
    --save_steps 5  # Save every 5 steps
```

---

## 📈 Performance Tuning

### Faster Mac Training (if you have 32GB+ RAM)

```bash
python3 finetune_mac.py \
    --batch_size 2 \           # Increase batch size
    --gradient_accumulation_steps 2 \  # Reduce grad accum
    --lora_r 16 \              # Increase LoRA rank
    --num_epochs 2             # More epochs
```

### Lower Memory Usage (if OOM)

```bash
python3 finetune_mac.py \
    --batch_size 1 \           # Keep at 1
    --gradient_accumulation_steps 8 \  # Increase grad accum
    --lora_r 4 \               # Reduce LoRA rank
    --max_length 64            # Reduce sequence length
```

---

## 📝 Checklist

Before running on Mac:

- [ ] macOS ≥ 12.3
- [ ] MPS available (`python3 -c "import torch; print(torch.backends.mps.is_available())"`)
- [ ] Python 3.10+
- [ ] `pip install -r requirements.txt`
- [ ] `export OPENAI_API_KEY="sk-..."`
- [ ] `wandb login` (optional)
- [ ] llama.cpp cloned in `../llama.cpp` (for GGUF conversion)
- [ ] ~10GB disk space free

After Mac test completes:

- [ ] Accuracy ≥70% on 10 images
- [ ] Model outputs single word labels (not explanations)
- [ ] No OOM errors
- [ ] GGUF conversion works
- [ ] Ready to deploy to L40S for full training

After L40S training:

- [ ] Validation accuracy ≥85% on 1000 images
- [ ] GGUF files created (~521MB)
- [ ] Downloaded to local machine
- [ ] Copied to Android assets
- [ ] APK rebuilt and tested

---

## 🆘 Getting Help

### Common Questions

**Q: Can I use Intel Mac?**
A: No MPS support. Use CPU mode (will be 5-10x slower):
```bash
python3 finetune_mac.py --device cpu
```

**Q: Can I test with 100 images instead of 10?**
A: Yes! Edit `test_mac.sh:38`:
```bash
NUM_IMAGES=100  # Will take ~5-9 hours
```

**Q: Can I skip W&B?**
A: Yes:
```bash
export WANDB_MODE=disabled
```

**Q: What if accuracy is <70% on Mac test?**
A: Normal for 10 images! If <50%, check:
- Dataset labels are correct
- Model outputs single words (not explanations)
- No errors in training logs

**Q: Should I run GGUF conversion on Mac?**
A: Not required for testing. But if you want to:
```bash
bash convert_to_gguf.sh ./test_model_mac/merged ./test_gguf_mac
```

---

## 🎯 Summary

**Mac Test Flow:**
1. ✅ Run `./test_mac.sh` (30-60 min)
2. ✅ Verify accuracy ≥70%
3. ✅ Check model outputs single words
4. ✅ Deploy to L40S: `./run_remote.sh`

**L40S Full Training:**
1. ✅ 1000 images, 3 epochs (5-10 hours)
2. ✅ Validation accuracy ≥85%
3. ✅ Download GGUF files
4. ✅ Deploy to Android

**Success Metrics:**
- Mac: 70%+ accuracy (10 images) = code validated ✅
- L40S: 85%+ accuracy (1000 images) = ready for production ✅

---

## 📚 Next Steps

After successful Mac test:

1. **Review results:**
   ```bash
   cat logs/mac_test_results.json
   ```

2. **If good (≥70% accuracy):**
   - Deploy to L40S
   - Run full training (1000 images)

3. **If bad (<70% accuracy):**
   - Check dataset labels
   - Review training logs
   - Test with different images
   - Ask for help!

4. **After L40S training:**
   - Download GGUF models
   - Test on Android device
   - Measure inference time (should be 5-8s per image)
   - Celebrate! 🎉

---

**Ready to test?**

```bash
chmod +x test_mac.sh
./test_mac.sh
```

Good luck! 🍀
