"""
Convert balanced dataset to SmolVLM training format
Reads dataset_balanced/train_final.jsonl and creates train.jsonl and val.jsonl
"""

import json
import random
from pathlib import Path
from collections import Counter

# Instruction prompt for element classification
INSTRUCTION_PROMPT = "Classify this image into one of these elements: water, land, fire, wood, metal.\n\nElement:"

def convert_to_smolvlm_format(
    input_jsonl="dataset_balanced/train_final.jsonl",
    output_dir="dataset_balanced_smolvlm",
    val_split=0.15,
    seed=42
):
    """
    Convert balanced JSONL to SmolVLM format

    Args:
        input_jsonl: Input balanced dataset
        output_dir: Output directory for SmolVLM format
        val_split: Validation split ratio (default: 0.15)
        seed: Random seed
    """
    random.seed(seed)

    # Read input dataset
    print(f"📖 Reading balanced dataset from {input_jsonl}...")
    with open(input_jsonl, 'r') as f:
        data = [json.loads(line) for line in f]

    print(f"   Found {len(data)} samples")

    # Group by element for stratified split
    element_data = {}
    for element in ["water", "land", "fire", "wood", "metal"]:
        element_data[element] = [item for item in data if item['element'] == element]
        print(f"   {element:8s}: {len(element_data[element]):4d} samples")

    # Stratified train/val split
    train_samples = []
    val_samples = []

    for element, items in element_data.items():
        # Shuffle
        random.shuffle(items)

        # Split
        num_val = max(1, int(len(items) * val_split))
        val_items = items[:num_val]
        train_items = items[num_val:]

        # Convert to SmolVLM format
        for item in train_items:
            # Extract just the filename from the path
            image_path = item["image_path"]
            if "images/" in image_path:
                # Extract filename after "images/"
                filename = image_path.split("images/")[-1]
            else:
                filename = Path(image_path).name

            train_samples.append({
                "image": filename,
                "conversations": [
                    {
                        "role": "user",
                        "content": f"<image>\n{INSTRUCTION_PROMPT}"
                    },
                    {
                        "role": "assistant",
                        "content": element
                    }
                ]
            })

        for item in val_items:
            # Extract just the filename from the path
            image_path = item["image_path"]
            if "images/" in image_path:
                # Extract filename after "images/"
                filename = image_path.split("images/")[-1]
            else:
                filename = Path(image_path).name

            val_samples.append({
                "image": filename,
                "conversations": [
                    {
                        "role": "user",
                        "content": f"<image>\n{INSTRUCTION_PROMPT}"
                    },
                    {
                        "role": "assistant",
                        "content": element
                    }
                ]
            })

    # Shuffle samples
    random.shuffle(train_samples)
    random.shuffle(val_samples)

    # Create output directory
    output_path = Path(output_dir)
    output_path.mkdir(parents=True, exist_ok=True)

    # Write JSONL files
    train_path = output_path / "train.jsonl"
    val_path = output_path / "val.jsonl"

    with open(train_path, "w") as f:
        for sample in train_samples:
            f.write(json.dumps(sample) + "\n")

    with open(val_path, "w") as f:
        for sample in val_samples:
            f.write(json.dumps(sample) + "\n")

    # Print statistics
    print(f"\n{'='*60}")
    print("📊 SmolVLM Dataset Statistics")
    print('='*60)

    train_element_counts = Counter(s["conversations"][1]["content"] for s in train_samples)
    val_element_counts = Counter(s["conversations"][1]["content"] for s in val_samples)

    print(f"\n🚂 Training Set ({len(train_samples)} samples):")
    for element in ["water", "land", "fire", "wood", "metal"]:
        count = train_element_counts.get(element, 0)
        percentage = (count / len(train_samples) * 100) if len(train_samples) > 0 else 0
        print(f"   {element:8s}: {count:4d} ({percentage:5.1f}%)")

    print(f"\n✅ Validation Set ({len(val_samples)} samples):")
    for element in ["water", "land", "fire", "wood", "metal"]:
        count = val_element_counts.get(element, 0)
        percentage = (count / len(val_samples) * 100) if len(val_samples) > 0 else 0
        print(f"   {element:8s}: {count:4d} ({percentage:5.1f}%)")

    # Check balance
    train_min = min(train_element_counts.values()) if train_element_counts else 0
    train_max = max(train_element_counts.values()) if train_element_counts else 0
    train_imbalance = train_max / train_min if train_min > 0 else float('inf')

    val_min = min(val_element_counts.values()) if val_element_counts else 0
    val_max = max(val_element_counts.values()) if val_element_counts else 0
    val_imbalance = val_max / val_min if val_min > 0 else float('inf')

    print(f"\n📈 Balance Metrics:")
    print(f"   Train imbalance ratio: {train_imbalance:.2f} (max/min)")
    print(f"   Val imbalance ratio: {val_imbalance:.2f} (max/min)")

    if train_imbalance <= 1.2 and val_imbalance <= 1.2:
        print(f"   ✅ Both splits are well-balanced!")
    else:
        print(f"   ⚠️  Some imbalance detected")

    print(f"\n💾 Files saved:")
    print(f"   {train_path}")
    print(f"   {val_path}")
    print('='*60)

    return train_samples, val_samples

if __name__ == "__main__":
    import argparse

    parser = argparse.ArgumentParser(description="Convert balanced dataset to SmolVLM format")
    parser.add_argument("--input_jsonl", type=str, default="dataset_balanced/train_final.jsonl",
                        help="Input balanced dataset")
    parser.add_argument("--output_dir", type=str, default="dataset_balanced_smolvlm",
                        help="Output directory")
    parser.add_argument("--val_split", type=float, default=0.15,
                        help="Validation split ratio")
    parser.add_argument("--seed", type=int, default=42,
                        help="Random seed")

    args = parser.parse_args()

    print("🔄 Converting Balanced Dataset to SmolVLM Format")
    print("="*60)

    train_samples, val_samples = convert_to_smolvlm_format(
        input_jsonl=args.input_jsonl,
        output_dir=args.output_dir,
        val_split=args.val_split,
        seed=args.seed
    )

    print(f"\n✅ Conversion complete!")
    print(f"\n📝 Next steps:")
    print(f"   1. Train SmolVLM: python train_smolvlm.py --config configs/l40s.yaml --dataset_dir {args.output_dir}")
    print(f"   2. Expected accuracy improvement: 41% → 70%+ due to balanced classes")
