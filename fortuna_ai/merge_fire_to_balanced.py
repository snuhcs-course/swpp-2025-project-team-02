"""
Merge collected fire images into balanced dataset
Reads fire_images_found.txt and adds them to dataset_balanced/train_rebalanced.jsonl
"""

import json
from pathlib import Path

def merge_fire_images(
    fire_list="fire_images_found.txt",
    input_jsonl="dataset_balanced/train_rebalanced.jsonl",
    output_jsonl="dataset_balanced/train_final.jsonl"
):
    """
    Merge fire images into balanced dataset

    Args:
        fire_list: Path to file containing fire image paths
        input_jsonl: Existing balanced dataset
        output_jsonl: Output path for final merged dataset
    """

    # Read existing balanced dataset
    print(f"📖 Reading existing dataset from {input_jsonl}...")
    with open(input_jsonl, 'r') as f:
        existing_data = [json.loads(line) for line in f]

    print(f"   Found {len(existing_data)} existing samples")

    # Count existing elements
    from collections import Counter
    element_counts = Counter(item['element'] for item in existing_data)
    print(f"\n   Current distribution:")
    for element in sorted(element_counts.keys()):
        print(f"     {element:8s}: {element_counts[element]:4d}")

    # Read fire image paths
    print(f"\n🔥 Reading fire images from {fire_list}...")
    fire_list_path = Path(fire_list)

    if not fire_list_path.exists():
        print(f"❌ Error: {fire_list} not found!")
        print(f"   Run collect_fire_images.py first")
        return

    with open(fire_list_path, 'r') as f:
        fire_image_paths = [line.strip() for line in f if line.strip()]

    print(f"   Found {len(fire_image_paths)} fire images")

    # Create fire samples in same format as existing data
    fire_samples = []
    for img_path in fire_image_paths:
        # Extract image_id from path (e.g., "dataset/images/00589_000000415194.jpg")
        img_name = Path(img_path).stem  # "00589_000000415194"

        fire_sample = {
            "image_id": img_name,
            "image_path": img_path,
            "element": "fire",
            "reason": "Image contains fire or light sources (lamps, bulbs, sun, etc.)",
            "confidence": 0.85
        }
        fire_samples.append(fire_sample)

    # Merge datasets
    merged_data = existing_data + fire_samples

    # Write merged dataset
    output_path = Path(output_jsonl)
    output_path.parent.mkdir(parents=True, exist_ok=True)

    with open(output_path, 'w') as f:
        for item in merged_data:
            f.write(json.dumps(item) + '\n')

    # Print final statistics
    final_counts = Counter(item['element'] for item in merged_data)

    print(f"\n{'='*60}")
    print("📊 Final Dataset Statistics")
    print('='*60)
    print(f"\n   Total samples: {len(merged_data)}")
    print(f"\n   Element distribution:")
    for element in sorted(final_counts.keys()):
        count = final_counts[element]
        percentage = (count / len(merged_data) * 100) if len(merged_data) > 0 else 0
        print(f"     {element:8s}: {count:4d} ({percentage:5.1f}%)")

    # Check balance
    min_count = min(final_counts.values())
    max_count = max(final_counts.values())
    imbalance_ratio = max_count / min_count if min_count > 0 else float('inf')

    print(f"\n   Imbalance ratio: {imbalance_ratio:.2f} (max/min)")

    if imbalance_ratio <= 1.2:
        print(f"   ✅ Dataset is well-balanced!")
    else:
        print(f"   ⚠️  Dataset has some imbalance")

    print(f"\n💾 Saved merged dataset to {output_path}")
    print('='*60)

    return merged_data

if __name__ == "__main__":
    import argparse

    parser = argparse.ArgumentParser(description="Merge fire images into balanced dataset")
    parser.add_argument("--fire_list", type=str, default="fire_images_found.txt",
                        help="File containing fire image paths")
    parser.add_argument("--input_jsonl", type=str, default="dataset_balanced/train_rebalanced.jsonl",
                        help="Existing balanced dataset")
    parser.add_argument("--output_jsonl", type=str, default="dataset_balanced/train_final.jsonl",
                        help="Output path for merged dataset")

    args = parser.parse_args()

    print("🔄 Merging Fire Images into Balanced Dataset")
    print("="*60)

    merged_data = merge_fire_images(
        fire_list=args.fire_list,
        input_jsonl=args.input_jsonl,
        output_jsonl=args.output_jsonl
    )

    if merged_data:
        print("\n✅ Merge complete!")
        print(f"\n📝 Next steps:")
        print(f"   1. Convert to SmolVLM format: python prepare_jsonl_dataset.py")
        print(f"   2. Train model: python train_smolvlm.py --config configs/l40s.yaml")
