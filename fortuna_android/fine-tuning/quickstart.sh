#!/bin/bash
# Quick Start Script for SmolVLM Fine-tuning
# Runs the entire pipeline end-to-end

set -e  # Exit on error

echo "================================================================================"
echo "SmolVLM Element Classification - Quick Start"
echo "================================================================================"
echo ""

# Check environment variables
if [ -z "$OPENAI_API_KEY" ]; then
    echo "❌ Error: OPENAI_API_KEY not set"
    echo "   Please set it: export OPENAI_API_KEY='sk-...'"
    exit 1
fi

if [ -z "$WANDB_API_KEY" ]; then
    echo "⚠️  Warning: WANDB_API_KEY not set"
    echo "   W&B tracking will require manual login"
fi

# Configuration
NUM_IMAGES=${NUM_IMAGES:-1000}
BATCH_SIZE=${BATCH_SIZE:-4}
NUM_EPOCHS=${NUM_EPOCHS:-3}
DATASET_DIR="./dataset"
MODEL_DIR="./models/smolvlm-element-classifier"
GGUF_DIR="./models/gguf"

echo "Configuration:"
echo "  Images: $NUM_IMAGES"
echo "  Batch size: $BATCH_SIZE"
echo "  Epochs: $NUM_EPOCHS"
echo "  Dataset: $DATASET_DIR"
echo "  Model output: $MODEL_DIR"
echo ""

# Step 1: Prepare dataset
echo "================================================================================"
echo "Step 1/5: Preparing dataset ($NUM_IMAGES images)"
echo "================================================================================"
python prepare_dataset.py \
    --output_dir "$DATASET_DIR" \
    --num_images "$NUM_IMAGES" \
    --openai_api_key "$OPENAI_API_KEY" \
    --min_confidence 0.7

echo ""
echo "✅ Dataset prepared"
echo ""

# Step 2: Fine-tune model
echo "================================================================================"
echo "Step 2/5: Fine-tuning SmolVLM with LoRA"
echo "================================================================================"
python finetune_smolvlm.py \
    --dataset_dir "$DATASET_DIR" \
    --output_dir "$MODEL_DIR" \
    --wandb_project smolvlm-element-classification \
    --batch_size "$BATCH_SIZE" \
    --num_epochs "$NUM_EPOCHS" \
    --bf16

echo ""
echo "✅ Fine-tuning complete"
echo ""

# Step 3: Merge LoRA
echo "================================================================================"
echo "Step 3/5: Merging LoRA weights"
echo "================================================================================"
python merge_lora.py \
    --lora_path "$MODEL_DIR/lora_adapter" \
    --output_path "$MODEL_DIR/merged"

echo ""
echo "✅ LoRA merged"
echo ""

# Step 4: Convert to GGUF
echo "================================================================================"
echo "Step 4/5: Converting to GGUF Q8_0"
echo "================================================================================"
bash convert_to_gguf.sh "$MODEL_DIR/merged" "$GGUF_DIR"

echo ""
echo "✅ GGUF conversion complete"
echo ""

# Step 5: Validate
echo "================================================================================"
echo "Step 5/5: Validating model"
echo "================================================================================"
python validate_model.py \
    --model_path "$MODEL_DIR/final" \
    --dataset_dir "$DATASET_DIR" \
    --output_path "./validation_results.json" \
    --plot_cm

echo ""
echo "✅ Validation complete"
echo ""

# Summary
echo "================================================================================"
echo "🎉 Pipeline Complete!"
echo "================================================================================"
echo ""
echo "Results:"
echo "  📊 Validation: ./validation_results.json"
echo "  📁 GGUF models: $GGUF_DIR/"
echo ""
echo "Next steps:"
echo "  1. Review validation accuracy in validation_results.json"
echo "  2. Copy GGUF files to Android:"
echo "     cp $GGUF_DIR/*.gguf ../app/src/main/assets/models/"
echo ""
echo "  3. Update Android config:"
echo "     - app/build.gradle.kts: Update VLM_MODEL_FILENAME and VLM_MMPROJ_FILENAME"
echo "     - app/.../SmolVLMManager.kt: Update MODEL_FILENAME and MMPROJ_FILENAME"
echo ""
echo "  4. Rebuild APK:"
echo "     cd .. && ./gradlew assembleDebug"
echo ""
echo "Happy classifying! 🚀"
echo "================================================================================"
