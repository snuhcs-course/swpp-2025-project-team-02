# Remote GPU Server Setup Guide

Complete guide for running SmolVLM fine-tuning on a remote L40S server in background.

---

## Quick Start (TL;DR)

```bash
# On remote L40S server
cd fortuna_android/fine-tuning

# Set API keys (add to ~/.bashrc for persistence)
export OPENAI_API_KEY="sk-..."
export WANDB_API_KEY="..."

# Run entire pipeline in background
chmod +x run_remote.sh
nohup ./run_remote.sh > logs/training.log 2>&1 &

# Monitor progress
tail -f logs/training.log
```

---

## Detailed Setup

### 1. Initial Server Setup

#### SSH into your L40S server
```bash
ssh user@your-l40s-server.com
```

#### Check GPU availability
```bash
nvidia-smi
# Should show L40S with ~48GB memory
```

#### Install system dependencies
```bash
# Ubuntu/Debian
sudo apt update
sudo apt install -y python3.10 python3.10-venv python3-pip git

# Check Python version (need 3.10+)
python3 --version
```

---

### 2. Upload Fine-tuning Code

#### Option A: Git Clone (recommended)
```bash
git clone https://github.com/your-repo/fortuna_android.git
cd fortuna_android/fine-tuning
```

#### Option B: SCP from local machine
```bash
# On your local machine
cd fortuna_android
tar -czf fine-tuning.tar.gz fine-tuning/
scp fine-tuning.tar.gz user@server:~/

# On remote server
tar -xzf fine-tuning.tar.gz
cd fine-tuning
```

---

### 3. Configure API Keys

#### Set environment variables (temporary)
```bash
export OPENAI_API_KEY="sk-..."
export WANDB_API_KEY="..."  # Get from https://wandb.ai/authorize
```

#### Make persistent (recommended)
```bash
# Add to ~/.bashrc or ~/.zshrc
echo 'export OPENAI_API_KEY="sk-..."' >> ~/.bashrc
echo 'export WANDB_API_KEY="..."' >> ~/.bashrc

# Reload
source ~/.bashrc
```

#### Verify
```bash
echo $OPENAI_API_KEY  # Should print your key
```

---

### 4. Run Training Pipeline

#### Method 1: Background with nohup (recommended)
```bash
# Make script executable
chmod +x run_remote.sh

# Create logs directory
mkdir -p logs

# Run in background
nohup ./run_remote.sh > logs/training_$(date +%Y%m%d_%H%M%S).log 2>&1 &

# Save the process ID
echo $! > logs/training.pid

# Monitor progress
tail -f logs/training_*.log
```

#### Method 2: Screen session
```bash
# Start screen session
screen -S smolvlm-training

# Run script
./run_remote.sh

# Detach: Ctrl+A, then D
# Reattach: screen -r smolvlm-training
```

#### Method 3: Tmux session
```bash
# Start tmux session
tmux new -s training

# Run script
./run_remote.sh

# Detach: Ctrl+B, then D
# Reattach: tmux attach -t training
```

---

### 5. Monitor Progress

#### Watch logs in real-time
```bash
tail -f logs/training_*.log
```

#### Monitor GPU usage
```bash
# Real-time GPU monitoring
watch -n 1 nvidia-smi

# Or just check once
nvidia-smi
```

#### Check W&B dashboard
```
https://wandb.ai/your-username/smolvlm-element-classification/runs
```

#### Check disk space
```bash
df -h  # Should have ~30GB free
du -sh dataset/ models/  # Check sizes
```

---

### 6. Estimated Timeline

| Step | Duration (L40S) | Output |
|------|----------------|---------|
| 1. COCO download | 5-10 min (one-time) | ~/.cache/coco/ (~1GB) |
| 2. Dataset labeling | 30-60 min | dataset/ (~500MB) |
| 3. Fine-tuning | 4-8 hours | models/smolvlm-element-classifier/ (~3GB) |
| 4. LoRA merge | 5 min | models/.../merged/ (~1GB) |
| 5. GGUF conversion | 10 min | models/gguf/ (~521MB) |
| 6. Validation | 10 min | validation_results.json |
| **Total** | **5-10 hours** | **~5GB** |

---

### 7. Configuration Options

#### Environment variables you can customize:

```bash
# Dataset
export NUM_IMAGES=1000           # Number of images to label
export MIN_CONFIDENCE=0.75       # Minimum GPT-4V confidence
export USE_UNSPLASH=false        # Use COCO (faster) instead of Unsplash

# Training
export BATCH_SIZE=4              # Batch size (reduce if OOM)
export GRAD_ACCUM=4              # Gradient accumulation steps
export NUM_EPOCHS=3              # Training epochs
export LEARNING_RATE=2e-4        # Learning rate
export LORA_R=16                 # LoRA rank (higher = more params)

# Then run
./run_remote.sh
```

---

### 8. Troubleshooting

#### Out of Memory (OOM)
```bash
# Reduce batch size
export BATCH_SIZE=2
export GRAD_ACCUM=8
./run_remote.sh
```

#### OpenAI API rate limits
```python
# Edit prepare_dataset_coco.py line ~220
time.sleep(1.0)  # Increase from 0.5 to 1.0
```

#### CUDA not available
```bash
# Check CUDA installation
nvcc --version

# Check PyTorch CUDA
python3 -c "import torch; print(torch.cuda.is_available())"

# Reinstall PyTorch with CUDA
pip install torch --upgrade --index-url https://download.pytorch.org/whl/cu121
```

#### Disk full
```bash
# Check space
df -h

# Clean up
rm -rf ~/.cache/huggingface/hub/models--*  # Remove old HF models
rm -rf dataset/images/  # After training (keep JSON files)
```

---

### 9. Download Results

After training completes (~6-10 hours), download GGUF files:

```bash
# On your local machine
mkdir -p local-models

# Download GGUF files
scp user@server:~/fortuna_android/fine-tuning/models/gguf/*.gguf ./local-models/

# Download validation results
scp user@server:~/fortuna_android/fine-tuning/logs/validation_results_*.json ./local-models/
```

Files to download:
- `SmolVLM2-500M-Element-Q8_0.gguf` (~417MB)
- `mmproj-SmolVLM2-500M-Element-Q8_0.gguf` (~104MB)
- `validation_results_*.json` (~50KB)

---

### 10. Stop Training (if needed)

#### If using nohup
```bash
# Find process ID
cat logs/training.pid

# Kill process
kill $(cat logs/training.pid)

# Or find manually
ps aux | grep run_remote.sh
kill <PID>
```

#### If using screen
```bash
# Reattach
screen -r smolvlm-training

# Press Ctrl+C to stop

# Exit screen
exit
```

#### If using tmux
```bash
# Reattach
tmux attach -t training

# Press Ctrl+C to stop

# Exit tmux
exit
```

---

## Cost Estimates

### GPU Time
- **L40S**: ~$1-2/hour
- **Training duration**: 5-10 hours
- **Total GPU cost**: ~$5-20

### API Calls
- **GPT-4V**: ~$0.01/image
- **1000 images**: ~$10-20

### Total: ~$15-40 for complete pipeline

---

## Dataset Options

### Option 1: COCO (Default - FREE & FAST)
```bash
# Uses prepare_dataset_coco.py
export USE_UNSPLASH=false
./run_remote.sh
```

**Pros:**
- ✅ Free (public dataset)
- ✅ Fast download (~5-10 min one-time)
- ✅ Diverse everyday images
- ✅ 5000 images available

**Cons:**
- ❌ Less control over image selection

### Option 2: Unsplash (Slower)
```bash
# Uses prepare_dataset.py
export USE_UNSPLASH=true
export UNSPLASH_ACCESS_KEY="..."
./run_remote.sh
```

**Pros:**
- ✅ High-quality curated images
- ✅ More control over search queries

**Cons:**
- ❌ Rate limited (50 requests/hour)
- ❌ ~20 hours for 1000 images
- ❌ Requires API key

### Recommendation: **Use COCO (default)**

---

## Monitoring Commands Cheat Sheet

```bash
# Logs
tail -f logs/training_*.log           # Follow training log
tail -n 100 logs/training_*.log       # Last 100 lines
grep "Epoch" logs/training_*.log      # Show epoch progress
grep "accuracy" logs/training_*.log   # Show accuracy metrics

# GPU
watch -n 1 nvidia-smi                 # Real-time GPU usage
nvidia-smi --query-gpu=utilization.gpu,memory.used --format=csv -l 1

# Processes
ps aux | grep python                  # Find Python processes
htop                                  # Interactive process viewer

# Disk
df -h                                 # Disk space
du -sh dataset/ models/               # Directory sizes
ncdu                                  # Interactive disk usage (if installed)
```

---

## Next Steps After Training

1. **Verify accuracy** (>85% recommended):
   ```bash
   cat logs/validation_results_*.json | grep "accuracy"
   ```

2. **Download GGUF files** (see section 9)

3. **Copy to Android project**:
   ```bash
   cp local-models/*.gguf fortuna_android/app/src/main/assets/models/
   ```

4. **Update Android config** (see main README.md)

5. **Test on device**

---

## Support

For issues:
1. Check logs: `logs/training_*.log`
2. Check W&B dashboard
3. Verify GPU: `nvidia-smi`
4. Check this troubleshooting guide

**Happy training! 🚀**
