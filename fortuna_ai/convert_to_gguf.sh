#!/bin/bash
# Convert SmolVLM merged model to GGUF format
#
# Prerequisites:
#   1. Merged model (run merge_lora.py first)
#   2. llama.cpp installed and compiled
#
# Usage:
#   ./convert_to_gguf.sh [merged_model_dir] [output_gguf_path] [quantization]
#
# Example:
#   ./convert_to_gguf.sh ./output_balanced_merged ./smolvlm_element.gguf q4_k_m

set -e  # Exit on error

# Default values
MERGED_MODEL_DIR="${1:-./output_balanced_merged}"
OUTPUT_GGUF="${2:-./smolvlm_element.gguf}"
QUANTIZATION="${3:-q4_k_m}"  # q4_k_m is good balance for mobile
LLAMA_CPP_DIR="${LLAMA_CPP_DIR:-../llama.cpp}"

echo "============================================"
echo "SmolVLM to GGUF Conversion Script"
echo "============================================"
echo "Merged model: $MERGED_MODEL_DIR"
echo "Output GGUF: $OUTPUT_GGUF"
echo "Quantization: $QUANTIZATION"
echo "llama.cpp: $LLAMA_CPP_DIR"
echo ""

# Check if merged model exists
if [ ! -d "$MERGED_MODEL_DIR" ]; then
    echo "❌ Error: Merged model directory not found: $MERGED_MODEL_DIR"
    echo ""
    echo "Run merge first:"
    echo "  python merge_lora.py --model_dir ./output_balanced --output_dir ./output_balanced_merged"
    exit 1
fi

# Check if llama.cpp exists
if [ ! -d "$LLAMA_CPP_DIR" ]; then
    echo "❌ Error: llama.cpp not found at: $LLAMA_CPP_DIR"
    echo ""
    echo "Clone llama.cpp:"
    echo "  cd .."
    echo "  git clone https://github.com/ggerganov/llama.cpp"
    echo "  cd llama.cpp"
    echo "  make"
    exit 1
fi

# Check if conversion script exists
CONVERT_SCRIPT="$LLAMA_CPP_DIR/convert_hf_to_gguf.py"
if [ ! -f "$CONVERT_SCRIPT" ]; then
    echo "❌ Error: Conversion script not found: $CONVERT_SCRIPT"
    echo ""
    echo "Update llama.cpp to latest version:"
    echo "  cd $LLAMA_CPP_DIR && git pull"
    exit 1
fi

echo "Step 1: Converting HF model to GGUF (FP16)..."
echo "----------------------------------------"
TEMP_GGUF="${OUTPUT_GGUF%.gguf}_fp16.gguf"

python "$CONVERT_SCRIPT" \
    "$MERGED_MODEL_DIR" \
    --outfile "$TEMP_GGUF" \
    --outtype f16

echo ""
echo "✓ FP16 GGUF created: $TEMP_GGUF"
echo ""

# If quantization is not fp16, quantize
if [ "$QUANTIZATION" != "f16" ]; then
    echo "Step 2: Quantizing to $QUANTIZATION..."
    echo "----------------------------------------"

    # Check if quantize tool exists
    QUANTIZE_TOOL="$LLAMA_CPP_DIR/llama-quantize"
    if [ ! -f "$QUANTIZE_TOOL" ]; then
        echo "❌ Error: llama-quantize not found. Compile llama.cpp first:"
        echo "  cd $LLAMA_CPP_DIR && make"
        exit 1
    fi

    "$QUANTIZE_TOOL" "$TEMP_GGUF" "$OUTPUT_GGUF" "$QUANTIZATION"

    echo ""
    echo "✓ Quantized GGUF created: $OUTPUT_GGUF"
    echo ""

    # Remove temporary FP16 file
    echo "Cleaning up temporary FP16 file..."
    rm -f "$TEMP_GGUF"
else
    # Just rename FP16 file
    mv "$TEMP_GGUF" "$OUTPUT_GGUF"
    echo "✓ FP16 GGUF saved as: $OUTPUT_GGUF"
fi

echo ""
echo "============================================"
echo "✅ Conversion Complete!"
echo "============================================"
echo "GGUF model: $OUTPUT_GGUF"
echo ""

# Show file size
if command -v du &> /dev/null; then
    FILE_SIZE=$(du -h "$OUTPUT_GGUF" | cut -f1)
    echo "File size: $FILE_SIZE"
    echo ""
fi

echo "Quantization options for mobile:"
echo "  q4_k_m  - Recommended (good balance)"
echo "  q4_k_s  - Smaller, slightly lower quality"
echo "  q5_k_m  - Better quality, larger file"
echo "  q8_0    - High quality, much larger"
echo ""
echo "Next steps:"
echo "  1. Test GGUF with llama.cpp:"
echo "     $LLAMA_CPP_DIR/llama-cli -m $OUTPUT_GGUF -p 'Test prompt'"
echo ""
echo "  2. Copy to Android project:"
echo "     cp $OUTPUT_GGUF /path/to/android/app/src/main/assets/"
echo ""
echo "  3. Update Android code to load new model"
echo ""
