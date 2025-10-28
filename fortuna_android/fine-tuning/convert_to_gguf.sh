#!/bin/bash
# Convert merged HuggingFace model to GGUF Q8_0 format for Android deployment
#
# Usage:
#   bash convert_to_gguf.sh <merged_model_path> <output_dir>
#
# Example:
#   bash convert_to_gguf.sh ./models/smolvlm-element-classifier/merged ./models/gguf

set -e  # Exit on error

# Check arguments
if [ $# -ne 2 ]; then
    echo "Usage: bash convert_to_gguf.sh <merged_model_path> <output_dir>"
    echo ""
    echo "Example:"
    echo "  bash convert_to_gguf.sh ./models/smolvlm-element-classifier/merged ./models/gguf"
    exit 1
fi

MERGED_MODEL_PATH="$1"
OUTPUT_DIR="$2"

# Validate input
if [ ! -d "$MERGED_MODEL_PATH" ]; then
    echo "❌ Error: Merged model directory not found: $MERGED_MODEL_PATH"
    exit 1
fi

# Check for llama.cpp
LLAMACPP_DIR="../llama.cpp"
if [ ! -d "$LLAMACPP_DIR" ]; then
    echo "❌ Error: llama.cpp directory not found at $LLAMACPP_DIR"
    echo "Please clone llama.cpp:"
    echo "  cd .."
    echo "  git clone https://github.com/ggerganov/llama.cpp"
    exit 1
fi

# Create output directory
mkdir -p "$OUTPUT_DIR"

echo "================================================================================"
echo "Convert SmolVLM to GGUF Q8_0 Format"
echo "================================================================================"
echo "📥 Input:  $MERGED_MODEL_PATH"
echo "📁 Output: $OUTPUT_DIR"
echo "🔧 Format: Q8_0 (8-bit quantization)"
echo ""

# Step 1: Convert to FP16 GGUF (intermediate format)
echo "Step 1/3: Converting to FP16 GGUF..."
python3 "$LLAMACPP_DIR/convert_hf_to_gguf.py" \
    "$MERGED_MODEL_PATH" \
    --outtype f16 \
    --outfile "$OUTPUT_DIR/smolvlm-element-fp16.gguf"

if [ $? -ne 0 ]; then
    echo "❌ Error: Failed to convert to FP16 GGUF"
    exit 1
fi
echo "✅ FP16 GGUF created"
echo ""

# Step 2: Quantize to Q8_0
echo "Step 2/3: Quantizing to Q8_0..."
"$LLAMACPP_DIR/llama-quantize" \
    "$OUTPUT_DIR/smolvlm-element-fp16.gguf" \
    "$OUTPUT_DIR/SmolVLM2-500M-Element-Q8_0.gguf" \
    Q8_0

if [ $? -ne 0 ]; then
    echo "❌ Error: Failed to quantize to Q8_0"
    exit 1
fi
echo "✅ Q8_0 quantization complete"
echo ""

# Step 3: Extract mmproj (vision projector)
echo "Step 3/3: Extracting mmproj (vision projector)..."

# Check if mmproj extraction script exists
MMPROJ_SCRIPT="$LLAMACPP_DIR/examples/llava/convert_image_encoder_to_gguf.py"
if [ ! -f "$MMPROJ_SCRIPT" ]; then
    echo "⚠️  Warning: mmproj extraction script not found"
    echo "   You may need to extract mmproj manually or use the original mmproj file"
    echo "   Script expected at: $MMPROJ_SCRIPT"
else
    python3 "$MMPROJ_SCRIPT" \
        --model-dir "$MERGED_MODEL_PATH" \
        --projector-type ldpv2 \
        --output-dir "$OUTPUT_DIR"

    # Rename to match Android naming convention
    if [ -f "$OUTPUT_DIR/mmproj-model.gguf" ]; then
        mv "$OUTPUT_DIR/mmproj-model.gguf" "$OUTPUT_DIR/mmproj-SmolVLM2-500M-Element-Q8_0.gguf"
        echo "✅ mmproj extracted and renamed"
    else
        echo "⚠️  Warning: mmproj file not found after extraction"
        echo "   You may need to use the original mmproj file from base model"
    fi
fi
echo ""

# Cleanup intermediate files
echo "🧹 Cleaning up intermediate files..."
rm -f "$OUTPUT_DIR/smolvlm-element-fp16.gguf"
echo "✅ Cleanup complete"
echo ""

# Summary
echo "================================================================================"
echo "🎉 GGUF Conversion Complete!"
echo "================================================================================"
echo "📁 Output directory: $OUTPUT_DIR"
echo ""
echo "Generated files:"
ls -lh "$OUTPUT_DIR"/*.gguf 2>/dev/null || echo "  (No .gguf files found)"
echo ""
echo "Next steps:"
echo "  1. Copy .gguf files to Android project:"
echo "     cp $OUTPUT_DIR/SmolVLM2-500M-Element-Q8_0.gguf ../app/src/main/assets/models/"
echo "     cp $OUTPUT_DIR/mmproj-SmolVLM2-500M-Element-Q8_0.gguf ../app/src/main/assets/models/"
echo ""
echo "  2. Update model filenames in app/build.gradle.kts:"
echo "     val VLM_MODEL_FILENAME = \"SmolVLM2-500M-Element-Q8_0.gguf\""
echo "     val VLM_MMPROJ_FILENAME = \"mmproj-SmolVLM2-500M-Element-Q8_0.gguf\""
echo ""
echo "  3. Update SmolVLMManager.kt constants to match"
echo ""
echo "  4. Rebuild and test on Android device"
echo "================================================================================"
