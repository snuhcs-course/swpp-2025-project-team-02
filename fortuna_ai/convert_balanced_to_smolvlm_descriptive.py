"""
Convert balanced dataset to SmolVLM training format with DESCRIPTIVE responses
This leverages SmolVLM's natural description ability instead of forcing single-word outputs
"""

import json
import random
from pathlib import Path
from collections import Counter

# Instruction prompt for element classification
INSTRUCTION_PROMPT = "Classify this image into one of these elements: water, land, fire, wood, metal.\n\nProvide your answer with a brief description."

def convert_to_smolvlm_descriptive(
    input_jsonl="dataset_balanced/train_final.jsonl",
    output_dir="dataset_balanced_smolvlm_descriptive",
    val_split=0.15,
    seed=42
):
    """
    Convert balanced JSONL to SmolVLM format with descriptive responses

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

        # Convert to SmolVLM format with DESCRIPTIVE responses
        for item in train_items:
            # Extract just the filename from the path
            image_path = item["image_path"]
            if "images/" in image_path:
                filename = image_path.split("images/")[-1]
            else:
                filename = Path(image_path).name

            # Create descriptive response
            # Format: "{reason} The element is {element}."
            reason = item.get("reason", f"This image represents the {element} element.")

            # Ensure the response ends with "The element is {element}."
            if not reason.endswith("."):
                reason += "."
            descriptive_response = f"{reason} The element is {element}."

            train_samples.append({
                "image": filename,
                "conversations": [
                    {
                        "role": "user",
                        "content": f"<image>\n{INSTRUCTION_PROMPT}"
                    },
                    {
                        "role": "assistant",
                        "content": descriptive_response
                    }
                ]
            })

        for item in val_items:
            # Extract just the filename from the path
            image_path = item["image_path"]
            if "images/" in image_path:
                filename = image_path.split("images/")[-1]
            else:
                filename = Path(image_path).name

            # Create descriptive response
            reason = item.get("reason", f"This image represents the {element} element.")
            if not reason.endswith("."):
                reason += "."
            descriptive_response = f"{reason} The element is {element}."

            val_samples.append({
                "image": filename,
                "conversations": [
                    {
                        "role": "user",
                        "content": f"<image>\n{INSTRUCTION_PROMPT}"
                    },
                    {
                        "role": "assistant",
                        "content": descriptive_response
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
    print(f"\n{'='*80}")
    print("📊 SmolVLM Descriptive Dataset Statistics")
    print('='*80)

    train_element_counts = Counter(
        s["conversations"][1]["content"].split("The element is ")[-1].rstrip(".")
        for s in train_samples
    )
    val_element_counts = Counter(
        s["conversations"][1]["content"].split("The element is ")[-1].rstrip(".")
        for s in val_samples
    )

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

    # Show sample responses
    print(f"\n📝 Sample Descriptive Responses:")
    for i in range(min(3, len(train_samples))):
        response = train_samples[i]["conversations"][1]["content"]
        # Truncate if too long
        if len(response) > 150:
            response = response[:147] + "..."
        print(f"\n   Example {i+1}:")
        print(f"   {response}")

    print(f"\n💾 Files saved:")
    print(f"   {train_path}")
    print(f"   {val_path}")
    print('='*80)

    print(f"\n💡 Key Improvement:")
    print(f"   ✓ Leverages SmolVLM's natural description ability")
    print(f"   ✓ Avoids single-word output that breaks generation patterns")
    print(f"   ✓ Should eliminate 'being being...' token repetition issue")
    print(f"   ✓ Expected accuracy improvement: 33% → 60%+")

    return train_samples, val_samples


if __name__ == "__main__":
    import argparse

    parser = argparse.ArgumentParser(
        description="Convert balanced dataset to SmolVLM format with descriptive responses"
    )
    parser.add_argument("--input_jsonl", type=str, default="dataset_balanced/train_final.jsonl",
                        help="Input balanced dataset")
    parser.add_argument("--output_dir", type=str, default="dataset_balanced_smolvlm_descriptive",
                        help="Output directory")
    parser.add_argument("--val_split", type=float, default=0.15,
                        help="Validation split ratio")
    parser.add_argument("--seed", type=int, default=42,
                        help="Random seed")

    args = parser.parse_args()

    print("🔄 Converting to Descriptive SmolVLM Format")
    print("="*80)

    train_samples, val_samples = convert_to_smolvlm_descriptive(
        input_jsonl=args.input_jsonl,
        output_dir=args.output_dir,
        val_split=args.val_split,
        seed=args.seed
    )

    print(f"\n✅ Conversion complete!")
    print(f"\n📝 Next steps:")
    print(f"   1. Train SmolVLM: python train_smolvlm.py --config configs/l40s.yaml --dataset_dir {args.output_dir}")
    print(f"   2. This should fix the token repetition issue and improve accuracy!")
