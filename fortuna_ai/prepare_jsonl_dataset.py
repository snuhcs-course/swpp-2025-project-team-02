"""
Prepare JSONL dataset from balanced element images
Converts balanced_dataset/ to train.jsonl and val.jsonl for SmolVLM fine-tuning
"""

import json
from pathlib import Path
from collections import Counter
import random

# Instruction prompt for element classification
INSTRUCTION_PROMPT = "Classify this image into one of these elements: water, land, fire, wood, metal.\n\nElement:"

def create_jsonl_dataset(dataset_dir="./balanced_dataset",
                         output_dir="./dataset",
                         val_split=0.15):
    """
    Create train.jsonl and val.jsonl from balanced dataset

    Args:
        dataset_dir: Directory with balanced element images
        output_dir: Directory to save JSONL files
        val_split: Validation split ratio (default: 0.15 = 15%)
    """
    dataset_path = Path(dataset_dir)
    output_path = Path(output_dir)
    output_path.mkdir(parents=True, exist_ok=True)

    # Collect all images by element
    element_images = {}
    for element in ["water", "land", "fire", "wood", "metal"]:
        element_dir = dataset_path / element
        if not element_dir.exists():
            print(f"⚠️  Warning: {element_dir} not found, skipping {element}")
            continue

        images = list(element_dir.glob("*.jpg"))
        element_images[element] = images
        print(f"Found {len(images)} images for {element}")

    # Split into train/val for each element (stratified split)
    train_samples = []
    val_samples = []

    for element, images in element_images.items():
        # Shuffle images
        random.shuffle(images)

        # Split
        num_val = max(1, int(len(images) * val_split))
        val_images = images[:num_val]
        train_images = images[num_val:]

        # Create samples
        for img_path in train_images:
            train_samples.append({
                "image": str(img_path.absolute()),
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

        for img_path in val_images:
            val_samples.append({
                "image": str(img_path.absolute()),
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
    print("\n" + "="*60)
    print("📊 Dataset Statistics")
    print("="*60)

    train_element_counts = Counter(s["conversations"][1]["content"] for s in train_samples)
    val_element_counts = Counter(s["conversations"][1]["content"] for s in val_samples)

    print(f"\n🚂 Training Set ({len(train_samples)} samples):")
    for element in ["water", "land", "fire", "wood", "metal"]:
        count = train_element_counts.get(element, 0)
        percentage = (count / len(train_samples) * 100) if len(train_samples) > 0 else 0
        print(f"  {element:8s}: {count:4d} ({percentage:5.1f}%)")

    print(f"\n✅ Validation Set ({len(val_samples)} samples):")
    for element in ["water", "land", "fire", "wood", "metal"]:
        count = val_element_counts.get(element, 0)
        percentage = (count / len(val_samples) * 100) if len(val_samples) > 0 else 0
        print(f"  {element:8s}: {count:4d} ({percentage:5.1f}%)")

    # Check balance
    train_min = min(train_element_counts.values()) if train_element_counts else 0
    train_max = max(train_element_counts.values()) if train_element_counts else 0
    train_imbalance = train_max / train_min if train_min > 0 else float('inf')

    val_min = min(val_element_counts.values()) if val_element_counts else 0
    val_max = max(val_element_counts.values()) if val_element_counts else 0
    val_imbalance = val_max / val_min if val_min > 0 else float('inf')

    print(f"\n📈 Balance Metrics:")
    print(f"  Train imbalance ratio: {train_imbalance:.2f} (max/min)")
    print(f"  Val imbalance ratio: {val_imbalance:.2f} (max/min)")

    if train_imbalance <= 1.2 and val_imbalance <= 1.2:
        print(f"  ✅ Both splits are well-balanced!")
    else:
        print(f"  ⚠️  Some imbalance detected - consider rebalancing")

    print(f"\n💾 Files saved:")
    print(f"  {train_path}")
    print(f"  {val_path}")
    print("="*60)

if __name__ == "__main__":
    import argparse

    parser = argparse.ArgumentParser(description="Prepare JSONL dataset for SmolVLM training")
    parser.add_argument("--dataset_dir", type=str, default="./balanced_dataset",
                        help="Directory with balanced element images")
    parser.add_argument("--output_dir", type=str, default="./dataset",
                        help="Directory to save JSONL files")
    parser.add_argument("--val_split", type=float, default=0.15,
                        help="Validation split ratio (default: 0.15)")
    parser.add_argument("--seed", type=int, default=42,
                        help="Random seed for reproducibility")

    args = parser.parse_args()

    # Set random seed
    random.seed(args.seed)

    print("🚀 JSONL Dataset Preparation")
    print("="*60)

    create_jsonl_dataset(
        dataset_dir=args.dataset_dir,
        output_dir=args.output_dir,
        val_split=args.val_split
    )

    print("\n✅ JSONL dataset preparation complete!")
    print(f"\n📝 Next steps:")
    print("   1. Review train.jsonl and val.jsonl")
    print("   2. Train SmolVLM: python train_smolvlm.py --config configs/l40s.yaml")
    print("   3. Monitor training progress and element-wise accuracy")
