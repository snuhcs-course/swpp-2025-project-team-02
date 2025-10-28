#!/bin/bash
# Remote GPU Server - Background Fine-tuning Script
# Runs entire pipeline in background with logging and error handling
#
# Usage on remote server:
#   chmod +x run_remote.sh
#   ./run_remote.sh > logs/training.log 2>&1 &
#
# Monitor progress:
#   tail -f logs/training.log
#   watch -n 1 nvidia-smi

export PATH="$HOME/miniconda3/bin:$PATH"

set -e  # Exit on error
set -o pipefail  # Catch errors in pipes

# ============================================================================
# Configuration
# ============================================================================

# Check required environment variables
if [ -z "$OPENAI_API_KEY" ]; then
    echo "❌ Error: OPENAI_API_KEY not set"
    echo "   Export it in your shell rc file (~/.bashrc or ~/.zshrc):"
    echo "   export OPENAI_API_KEY='sk-...'"
    exit 1
fi

# Optional: Check W&B (will prompt for login if not set)
if [ -z "$WANDB_API_KEY" ]; then
    echo "⚠️  Warning: WANDB_API_KEY not set"
    echo "   Run: wandb login"
fi

# Dataset configuration
NUM_IMAGES=${NUM_IMAGES:-1000}
MIN_CONFIDENCE=${MIN_CONFIDENCE:-0.75}
USE_UNSPLASH=${USE_UNSPLASH:-false}  # Default: don't use Unsplash (slow)

# Training configuration
BATCH_SIZE=${BATCH_SIZE:-4}
GRAD_ACCUM=${GRAD_ACCUM:-4}
NUM_EPOCHS=${NUM_EPOCHS:-3}
LEARNING_RATE=${LEARNING_RATE:-2e-4}
LORA_R=${LORA_R:-16}

# Paths
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
DATASET_DIR="${SCRIPT_DIR}/dataset"
MODEL_DIR="${SCRIPT_DIR}/models/smolvlm-element-classifier"
GGUF_DIR="${SCRIPT_DIR}/models/gguf"
LOG_DIR="${SCRIPT_DIR}/logs"

# Create directories
mkdir -p "$LOG_DIR"
mkdir -p "$DATASET_DIR"
mkdir -p "$MODEL_DIR"

# ============================================================================
# Logging setup
# ============================================================================

TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
LOG_FILE="${LOG_DIR}/training_${TIMESTAMP}.log"
exec > >(tee -a "$LOG_FILE")
exec 2>&1

echo "================================================================================"
echo "SmolVLM Element Classification - Remote Training"
echo "================================================================================"
echo "Started at: $(date)"
echo "Log file: $LOG_FILE"
echo ""
echo "Configuration:"
echo "  GPU: $(nvidia-smi --query-gpu=name --format=csv,noheader | head -n1)"
echo "  Images: $NUM_IMAGES"
echo "  Min confidence: $MIN_CONFIDENCE"
echo "  Batch size: $BATCH_SIZE"
echo "  Grad accumulation: $GRAD_ACCUM"
echo "  Epochs: $NUM_EPOCHS"
echo "  Learning rate: $LEARNING_RATE"
echo "  LoRA rank: $LORA_R"
echo ""

# ============================================================================
# Python environment setup
# ============================================================================

echo "================================================================================"
echo "Setting up Python environment"
echo "================================================================================"

# Check if venv exists
if [ ! -d "${SCRIPT_DIR}/venv" ]; then
    echo "Creating virtual environment..."
    python3 -m venv "${SCRIPT_DIR}/venv"
fi

# Activate venv
source "${SCRIPT_DIR}/venv/bin/activate"

# Upgrade pip
pip install --upgrade pip > /dev/null

# Install requirements
echo "Installing dependencies..."
pip install -r "${SCRIPT_DIR}/requirements.txt"

echo "✅ Environment ready"
echo ""

# ============================================================================
# Step 1: Prepare Dataset
# ============================================================================

if [ -f "${DATASET_DIR}/train.json" ] && [ -f "${DATASET_DIR}/val.json" ]; then
    echo "================================================================================"
    echo "Dataset already exists, skipping preparation"
    echo "================================================================================"
    echo "Train: ${DATASET_DIR}/train.json"
    echo "Val:   ${DATASET_DIR}/val.json"
    echo ""
    echo "To regenerate dataset, run:"
    echo "  rm -rf ${DATASET_DIR}"
    echo ""
else
    echo "================================================================================"
    echo "Step 1/5: Preparing Dataset ($NUM_IMAGES images)"
    echo "================================================================================"

    # Use V3 dataset preparation (Rich Teacher, Simple Student)
    if [ "$USE_UNSPLASH" = "true" ]; then
        echo "⚠️  Using Unsplash (slow due to rate limits)"
        python "${SCRIPT_DIR}/prepare_dataset.py" \
            --output_dir "$DATASET_DIR" \
            --num_images "$NUM_IMAGES" \
            --openai_api_key "$OPENAI_API_KEY" \
            --min_confidence "$MIN_CONFIDENCE" \
            --num_workers 2
    else
        echo "💡 Using COCO dataset with rich teacher prompts (V3)"
        python "${SCRIPT_DIR}/prepare_dataset_coco_v3.py" \
            --output_dir "$DATASET_DIR" \
            --num_images "$NUM_IMAGES" \
            --openai_api_key "$OPENAI_API_KEY" \
            --min_confidence "$MIN_CONFIDENCE"
    fi

    echo ""
    echo "✅ Dataset prepared"
    echo ""
fi

# ============================================================================
# Step 2: Fine-tune Model
# ============================================================================

echo "================================================================================"
echo "Step 2/5: Fine-tuning SmolVLM with LoRA (Android-aligned)"
echo "================================================================================"

# Use v2 script (Android-aligned: 256x256 images, 32 max tokens)
python "${SCRIPT_DIR}/finetune_smolvlm_v2.py" \
    --dataset_dir "$DATASET_DIR" \
    --output_dir "$MODEL_DIR" \
    --wandb_project smolvlm-android-aligned \
    --run_name "run_${TIMESTAMP}" \
    --batch_size "$BATCH_SIZE" \
    --gradient_accumulation_steps "$GRAD_ACCUM" \
    --num_epochs "$NUM_EPOCHS" \
    --learning_rate "$LEARNING_RATE" \
    --lora_r "$LORA_R" \
    --bf16

echo ""
echo "✅ Fine-tuning complete"
echo ""

# ============================================================================
# Step 3: Merge LoRA
# ============================================================================

echo "================================================================================"
echo "Step 3/5: Merging LoRA weights"
echo "================================================================================"

python "${SCRIPT_DIR}/merge_lora.py" \
    --lora_path "${MODEL_DIR}/lora_adapter" \
    --output_path "${MODEL_DIR}/merged"

echo ""
echo "✅ LoRA merged"
echo ""

# ============================================================================
# Step 4: Convert to GGUF
# ============================================================================

echo "================================================================================"
echo "Step 4/5: Converting to GGUF Q8_0"
echo "================================================================================"

bash "${SCRIPT_DIR}/convert_to_gguf.sh" \
    "${MODEL_DIR}/merged" \
    "$GGUF_DIR"

echo ""
echo "✅ GGUF conversion complete"
echo ""

# ============================================================================
# Step 5: Validate
# ============================================================================

echo "================================================================================"
echo "Step 5/5: Validating model"
echo "================================================================================"

python "${SCRIPT_DIR}/validate_model.py" \
    --model_path "${MODEL_DIR}/final" \
    --dataset_dir "$DATASET_DIR" \
    --output_path "${LOG_DIR}/validation_results_${TIMESTAMP}.json" \
    --plot_cm

echo ""
echo "✅ Validation complete"
echo ""

# ============================================================================
# Summary
# ============================================================================

echo "================================================================================"
echo "🎉 Training Pipeline Complete!"
echo "================================================================================"
echo "Finished at: $(date)"
echo ""
echo "Results:"
echo "  📊 Validation: ${LOG_DIR}/validation_results_${TIMESTAMP}.json"
echo "  📁 GGUF models: ${GGUF_DIR}/"
echo "  📝 Full log: ${LOG_FILE}"
echo ""
echo "GGUF files:"
ls -lh "${GGUF_DIR}/"*.gguf 2>/dev/null || echo "  (No files found)"
echo ""
echo "To download GGUF files to your local machine:"
echo "  scp user@server:${GGUF_DIR}/*.gguf ./local-models/"
echo ""
echo "Next steps on local machine:"
echo "  1. Copy to Android project:"
echo "     cp local-models/*.gguf fortuna_android/app/src/main/assets/models/"
echo ""
echo "  2. Update config files and rebuild APK"
echo ""
echo "================================================================================"

# Send completion notification (optional)
if command -v notify-send &> /dev/null; then
    notify-send "SmolVLM Training Complete" "Check ${LOG_FILE} for results"
fi

# Keep process alive for a bit (helps with job monitoring)
sleep 5
