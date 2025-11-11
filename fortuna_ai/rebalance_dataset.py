"""
Re-balance existing dataset by:
1. Downsampling over-represented elements (metal, land, wood, water) to 140 each
2. Keeping all fire images (8)
3. Finding additional fire images from COCO and labeling with GPT-4V

Final target: 140 images per element = 700 total
"""

import json
import random
from pathlib import Path
from collections import Counter, defaultdict

def rebalance_existing_dataset(
    input_jsonl="dataset/train.jsonl",
    output_dir="dataset_balanced",
    target_per_element=140,
    seed=42
):
    """
    Step 1: Re-balance existing dataset by downsampling

    Args:
        input_jsonl: Path to existing train.jsonl
        output_dir: Output directory for balanced dataset
        target_per_element: Target number of samples per element
        seed: Random seed for reproducibility
    """
    random.seed(seed)

    # Read existing dataset
    print("📖 Reading existing dataset...")
    with open(input_jsonl, 'r') as f:
        data = [json.loads(line) for line in f]

    # Group by element
    element_data = defaultdict(list)
    for item in data:
        element_data[item['element']].append(item)

    # Print current distribution
    print("\n=== Current Distribution ===")
    for element in sorted(element_data.keys()):
        print(f"  {element:8s}: {len(element_data[element]):4d}")

    # Downsample each element
    balanced_data = []
    fire_count = 0

    print(f"\n=== Downsampling to {target_per_element} per element ===")
    for element in ["water", "land", "fire", "wood", "metal"]:
        samples = element_data.get(element, [])

        if element == "fire":
            # Keep ALL fire images (we have very few)
            selected = samples
            fire_count = len(selected)
            print(f"  {element:8s}: keeping all {fire_count} images (need {target_per_element - fire_count} more)")
        else:
            # Random downsample to target
            if len(samples) > target_per_element:
                selected = random.sample(samples, target_per_element)
                print(f"  {element:8s}: {len(samples):4d} → {len(selected):4d} (downsampled)")
            else:
                selected = samples
                print(f"  {element:8s}: {len(samples):4d} (kept all, need {target_per_element - len(samples)} more)")

        balanced_data.extend(selected)

    # Save balanced dataset
    output_path = Path(output_dir)
    output_path.mkdir(parents=True, exist_ok=True)

    output_file = output_path / "train_rebalanced.jsonl"
    with open(output_file, 'w') as f:
        for item in balanced_data:
            f.write(json.dumps(item) + '\n')

    print(f"\n✅ Saved {len(balanced_data)} balanced samples to {output_file}")

    # Print final distribution
    element_counts = Counter(item['element'] for item in balanced_data)
    print("\n=== Balanced Distribution ===")
    for element in sorted(element_counts.keys()):
        print(f"  {element:8s}: {element_counts[element]:4d}")

    print(f"\n⚠️  Still need {target_per_element - fire_count} fire images to reach {target_per_element}")

    return balanced_data, target_per_element - fire_count

if __name__ == "__main__":
    import argparse

    parser = argparse.ArgumentParser(description="Re-balance existing dataset")
    parser.add_argument("--input_jsonl", type=str, default="dataset/train.jsonl",
                        help="Input JSONL file")
    parser.add_argument("--output_dir", type=str, default="dataset_balanced",
                        help="Output directory")
    parser.add_argument("--target_per_element", type=int, default=140,
                        help="Target samples per element")
    parser.add_argument("--seed", type=int, default=42,
                        help="Random seed")

    args = parser.parse_args()

    print("🔄 Dataset Re-balancing")
    print("="*60)

    balanced_data, fire_needed = rebalance_existing_dataset(
        input_jsonl=args.input_jsonl,
        output_dir=args.output_dir,
        target_per_element=args.target_per_element,
        seed=args.seed
    )

    print("\n" + "="*60)
    print("📝 Next Steps:")
    print(f"   1. Collect {fire_needed} additional fire images")
    print(f"   2. Label them with GPT-4V")
    print(f"   3. Add to dataset_balanced/train_rebalanced.jsonl")
    print(f"   4. Convert to SmolVLM format and train!")
    print("="*60)
