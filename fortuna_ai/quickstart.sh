#!/bin/bash
# QuickStart Script for SmolVLM Training
# Runs a minimal training test to verify everything works

set -e  # Exit on error

echo "🚀 SmolVLM Training QuickStart"
echo "=============================="
echo ""

# Check Python version
echo "Checking Python version..."
python3 --version || { echo "Error: Python 3 not found"; exit 1; }

# Check if dataset exists
echo "Checking dataset..."
if [ ! -d "dataset" ]; then
    echo "Error: dataset/ directory not found"
    echo "Please ensure dataset/ exists with images/, train.jsonl, and val.jsonl"
    exit 1
fi

if [ ! -f "dataset/train.jsonl" ] || [ ! -f "dataset/val.jsonl" ]; then
    echo "Error: train.jsonl or val.jsonl not found in dataset/"
    exit 1
fi

echo "✓ Dataset found"

# Check dataset size
TRAIN_COUNT=$(wc -l < dataset/train.jsonl)
VAL_COUNT=$(wc -l < dataset/val.jsonl)
echo "  Train samples: $TRAIN_COUNT"
echo "  Val samples: $VAL_COUNT"
echo ""

# Install dependencies
echo "Installing dependencies..."
if [ ! -f "requirements.txt" ]; then
    echo "Error: requirements.txt not found"
    exit 1
fi

pip install -q -r requirements.txt
echo "✓ Dependencies installed"
echo ""

# Detect platform
echo "Detecting platform..."
if python3 -c "import torch; print(torch.cuda.is_available())" 2>/dev/null | grep -q "True"; then
    PLATFORM="cuda"
    CONFIG="configs/l40s.yaml"
    echo "✓ CUDA detected - using L40S config"
elif python3 -c "import torch; print(torch.backends.mps.is_available())" 2>/dev/null | grep -q "True"; then
    PLATFORM="mps"
    CONFIG="configs/mac.yaml"
    echo "✓ MPS detected - using Mac config"
else
    PLATFORM="cpu"
    CONFIG="configs/mac.yaml"
    echo "⚠ No GPU detected - using CPU (will be slow)"
fi
echo ""

# Run quick test (1 epoch, small batch)
echo "Running quick training test (1 epoch)..."
echo "This will take 5-15 minutes depending on your hardware"
echo ""

python3 train_smolvlm.py \
    --config "$CONFIG" \
    --dataset_dir ./dataset \
    --output_dir ./output_test

echo ""
echo "✅ Training test completed!"
echo ""

# Run validation
echo "Running validation on test model..."
python3 validate.py \
    --model_dir ./output_test/final \
    --dataset_dir ./dataset \
    --max_samples 20

echo ""
echo "=============================="
echo "✅ QuickStart Complete!"
echo ""
echo "Next steps:"
echo "1. Check validation results above"
echo "2. If accuracy looks good, run full training:"
echo "   python train_smolvlm.py --config $CONFIG --dataset_dir ./dataset --output_dir ./output"
echo "3. After training, validate:"
echo "   python validate.py --model_dir ./output/final --dataset_dir ./dataset"
echo "4. See README.md for deployment instructions"
echo ""
