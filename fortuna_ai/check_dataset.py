#!/usr/bin/env python3
"""
Dataset Analysis Script
Check dataset quality, class distribution, and potential issues
"""

import json
from pathlib import Path
from collections import Counter
import sys


def analyze_dataset(jsonl_path: Path, images_dir: Path, name: str):
    """Analyze a JSONL dataset"""
    print(f"\n=== {name} Dataset ===")

    # Load data
    data = []
    missing_images = []

    with open(jsonl_path) as f:
        for line_num, line in enumerate(f, 1):
            try:
                item = json.loads(line.strip())
                data.append(item)

                # Check if image exists
                image_path = images_dir / item['image_path']
                if not image_path.exists():
                    missing_images.append((line_num, item['image_path']))
            except json.JSONDecodeError as e:
                print(f"⚠ Warning: Invalid JSON at line {line_num}: {e}")

    # Basic stats
    print(f"Total samples: {len(data)}")

    if missing_images:
        print(f"\n⚠ Missing images: {len(missing_images)}")
        for line_num, path in missing_images[:5]:
            print(f"  Line {line_num}: {path}")
        if len(missing_images) > 5:
            print(f"  ... and {len(missing_images) - 5} more")

    # Element distribution
    elements = [item['element'] for item in data]
    element_counts = Counter(elements)

    print(f"\nElement distribution:")
    print(f"{'Element':<10} {'Count':>6} {'Percentage':>10}")
    print("-" * 30)

    for element in ["water", "land", "fire", "wood", "metal"]:
        count = element_counts.get(element, 0)
        pct = count / len(data) * 100 if data else 0
        status = ""

        # Flag imbalance
        if count < 50:
            status = " ⚠ TOO FEW"
        elif count > 400:
            status = " ⚠ TOO MANY"

        print(f"{element:<10} {count:>6} {pct:>9.1f}%{status}")

    # Check for severe imbalance
    counts = list(element_counts.values())
    if counts:
        min_count = min(counts)
        max_count = max(counts)
        imbalance_ratio = max_count / min_count if min_count > 0 else float('inf')

        print(f"\nClass balance:")
        print(f"  Min class size: {min_count}")
        print(f"  Max class size: {max_count}")
        print(f"  Imbalance ratio: {imbalance_ratio:.1f}x")

        if imbalance_ratio > 10:
            print("  ⚠ SEVERE IMBALANCE - Consider:")
            print("    1. Collecting more samples for underrepresented classes")
            print("    2. Using class weights during training")
            print("    3. Oversampling minority classes")
        elif imbalance_ratio > 3:
            print("  ⚠ MODERATE IMBALANCE - May need class weights")

    return data, element_counts


def main():
    dataset_dir = Path("dataset")

    if not dataset_dir.exists():
        print(f"Error: {dataset_dir} not found")
        sys.exit(1)

    print("="*60)
    print("SmolVLM Dataset Analysis")
    print("="*60)

    # Analyze training set
    train_data, train_counts = analyze_dataset(
        dataset_dir / "train.jsonl",
        dataset_dir / "images",
        "Training"
    )

    # Analyze validation set
    val_data, val_counts = analyze_dataset(
        dataset_dir / "val.jsonl",
        dataset_dir / "images",
        "Validation"
    )

    # Overall summary
    print("\n" + "="*60)
    print("SUMMARY")
    print("="*60)
    print(f"Total training samples: {len(train_data)}")
    print(f"Total validation samples: {len(val_data)}")
    print(f"Total samples: {len(train_data) + len(val_data)}")

    # Check if validation distribution matches training
    print("\nValidation vs Training distribution:")
    for element in ["water", "land", "fire", "wood", "metal"]:
        train_pct = train_counts.get(element, 0) / len(train_data) * 100 if train_data else 0
        val_pct = val_counts.get(element, 0) / len(val_data) * 100 if val_data else 0
        diff = abs(train_pct - val_pct)

        status = "✓" if diff < 5 else "⚠"
        print(f"  {status} {element:<10} Train: {train_pct:>5.1f}%  Val: {val_pct:>5.1f}%  Diff: {diff:>4.1f}%")

    print("\n" + "="*60)
    print("✅ Dataset analysis complete!")
    print("="*60)


if __name__ == "__main__":
    main()
