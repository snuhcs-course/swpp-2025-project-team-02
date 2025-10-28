#!/bin/bash
# Mac Test Script - Quick validation before L40S deployment
# Tests with 10 images (~30-60 minutes total)

set -e

echo "================================================================================"
echo "🍎 Mac Test Pipeline (10 images)"
echo "================================================================================"
echo "Purpose: Validate code before deploying to L40S"
echo "Duration: ~30-60 minutes"
echo "Hardware: M1/M2/M3 Mac with 16GB+ RAM"
echo ""

# Check if on Mac
if [[ ! $(uname) == "Darwin" ]]; then
    echo "❌ Error: This script is for macOS only"
    exit 1
fi

# Check MPS availability
python3 -c "import torch; assert torch.backends.mps.is_available(), 'MPS not available'" 2>/dev/null || {
    echo "❌ Error: MPS (Metal) not available"
    echo "   Make sure you have:"
    echo "   - macOS 12.3+"
    echo "   - PyTorch with MPS support"
    exit 1
}

# Check API keys
if [ -z "$OPENAI_API_KEY" ]; then
    echo "❌ Error: OPENAI_API_KEY not set"
    echo "   Run: export OPENAI_API_KEY='sk-...'"
    exit 1
fi

# Configuration
NUM_IMAGES=10
TEST_DATASET_DIR="./test_dataset_mac"
TEST_MODEL_DIR="./test_model_mac"
LOG_DIR="./logs"

mkdir -p "$LOG_DIR"

echo "Configuration:"
echo "  Images: $NUM_IMAGES"
echo "  Dataset: $TEST_DATASET_DIR"
echo "  Model: $TEST_MODEL_DIR"
echo ""

# Clean up previous test
if [ -d "$TEST_DATASET_DIR" ] || [ -d "$TEST_MODEL_DIR" ]; then
    echo "⚠️  Previous test data found"
    read -p "   Delete and start fresh? (y/n): " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        rm -rf "$TEST_DATASET_DIR" "$TEST_MODEL_DIR"
        echo "✅ Cleaned up"
    fi
    echo ""
fi

# Step 1: Dataset preparation
echo "================================================================================"
echo "Step 1/4: Dataset Preparation (${NUM_IMAGES} images)"
echo "================================================================================"
echo "Using COCO dataset + GPT-4V labeling"
echo "Expected duration: ~5-10 minutes"
echo ""

python3 prepare_dataset_coco_v3.py \
    --output_dir "$TEST_DATASET_DIR" \
    --num_images "$NUM_IMAGES" \
    --openai_api_key "$OPENAI_API_KEY" \
    --min_confidence 0.7

echo ""
echo "✅ Dataset prepared"
echo ""

# Check dataset
TRAIN_COUNT=$(jq '. | length' "$TEST_DATASET_DIR/train.json")
VAL_COUNT=$(jq '. | length' "$TEST_DATASET_DIR/val.json")
echo "Dataset stats:"
echo "  Train: $TRAIN_COUNT images"
echo "  Val:   $VAL_COUNT images"
echo ""

# Step 2: Fine-tuning (Mac optimized)
echo "================================================================================"
echo "Step 2/4: Fine-tuning (Mac MPS)"
echo "================================================================================"
echo "Using Mac-optimized settings:"
echo "  - Batch size: 1 (for 16GB RAM)"
echo "  - LoRA rank: 8 (reduced)"
echo "  - FP16 (MPS doesn't support BF16)"
echo "  - 1 epoch (test only)"
echo ""
echo "Expected duration: ~20-40 minutes"
echo ""

python3 finetune_mac.py \
    --dataset_dir "$TEST_DATASET_DIR" \
    --output_dir "$TEST_MODEL_DIR" \
    --batch_size 1 \
    --gradient_accumulation_steps 4 \
    --num_epochs 1 \
    --learning_rate 2e-4 \
    --lora_r 8 \
    --wandb_project smolvlm-mac-test

echo ""
echo "✅ Fine-tuning complete"
echo ""

# Step 3: Merge LoRA
echo "================================================================================"
echo "Step 3/4: Merging LoRA Weights"
echo "================================================================================"
echo "Expected duration: ~2-5 minutes"
echo ""

python3 merge_lora.py \
    --base_model HuggingFaceM4/SmolVLM2-500M-Video-Instruct \
    --lora_path "$TEST_MODEL_DIR/lora_adapter" \
    --output_path "$TEST_MODEL_DIR/merged" \
    --device mps

echo ""
echo "✅ LoRA merged"
echo ""

# Step 4: Validation
echo "================================================================================"
echo "Step 4/4: Model Validation"
echo "================================================================================"
echo "Expected duration: ~5 minutes"
echo ""

python3 validate_model.py \
    --model_path "$TEST_MODEL_DIR/final" \
    --dataset_dir "$TEST_DATASET_DIR" \
    --output_path "$LOG_DIR/mac_test_results.json" \
    --device mps

echo ""
echo "✅ Validation complete"
echo ""

# Summary
echo "================================================================================"
echo "🎉 Mac Test Complete!"
echo "================================================================================"
echo ""
echo "Results:"
echo "  Dataset: $TEST_DATASET_DIR"
echo "  Model:   $TEST_MODEL_DIR"
echo "  Metrics: $LOG_DIR/mac_test_results.json"
echo ""

# Show validation results
if [ -f "$LOG_DIR/mac_test_results.json" ]; then
    echo "Validation Accuracy:"
    python3 -c "import json; data=json.load(open('$LOG_DIR/mac_test_results.json')); print(f\"  {data['accuracy']:.1%}\")"
    echo ""
fi

echo "⚠️  IMPORTANT: This is a TEST run (${NUM_IMAGES} images only)"
echo ""
echo "Next steps:"
echo "  1. Review results above"
echo "  2. If accuracy looks good (>70% on 10 images),"
echo "     deploy to L40S for full training (1000 images)"
echo "  3. On L40S: ./run_remote.sh"
echo ""
echo "Cleanup (optional):"
echo "  rm -rf $TEST_DATASET_DIR $TEST_MODEL_DIR"
echo ""
echo "================================================================================"
