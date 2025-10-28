#!/usr/bin/env python3
"""
Validate fine-tuned SmolVLM model on test set

Usage:
    python validate_model.py \
        --model_path ./models/smolvlm-element-classifier/final \
        --dataset_dir ./dataset \
        --output_path ./validation_results.json
"""

import argparse
import json
from pathlib import Path
from typing import Dict, List
import torch
from PIL import Image
from transformers import AutoProcessor, AutoModelForVision2Seq
from tqdm import tqdm
import numpy as np
from sklearn.metrics import classification_report, confusion_matrix
import matplotlib.pyplot as plt
import seaborn as sns


ELEMENTS = ["water", "land", "fire", "wood", "metal"]


def predict_element(
    model,
    processor,
    image: Image.Image,
    device: str = "cuda",
) -> Dict[str, str]:
    """
    Predict element for a single image
    Returns: {"predicted": "fire", "raw_output": "fire"}
    """
    # Build inference prompt (same as training, but without context)
    prompt = "<image>Classify this image into one of these elements: water, land, fire, wood, metal.\n\nElement:"

    # Process image and text
    inputs = processor(
        images=image,
        text=prompt,
        return_tensors="pt",
    ).to(device)

    # Generate prediction
    with torch.no_grad():
        generated_ids = model.generate(
            pixel_values=inputs["pixel_values"],
            input_ids=inputs["input_ids"],
            max_new_tokens=10,  # Element names are short
            do_sample=False,    # Greedy decoding for deterministic output
            temperature=0.0,
        )

    # Decode output
    # Remove input prompt tokens
    output_ids = generated_ids[0][inputs["input_ids"].shape[1]:]
    raw_output = processor.decode(output_ids, skip_special_tokens=True).strip().lower()

    # Parse element from output
    predicted_element = None
    for elem in ELEMENTS:
        if elem in raw_output:
            predicted_element = elem
            break

    return {
        "predicted": predicted_element or "unknown",
        "raw_output": raw_output,
    }


def validate_model(
    model_path: Path,
    dataset_dir: Path,
    split: str = "validation",
    device: str = "cuda",
) -> Dict:
    """Run validation on dataset split"""

    print("=" * 80)
    print("SmolVLM Model Validation")
    print("=" * 80)
    print(f"📥 Model: {model_path}")
    print(f"📊 Dataset: {dataset_dir} ({split} split)")
    print(f"🔧 Device: {device}")
    print()

    # Load model and processor
    print("📥 Loading model...")
    processor = AutoProcessor.from_pretrained(str(model_path))
    model = AutoModelForVision2Seq.from_pretrained(
        str(model_path),
        torch_dtype=torch.bfloat16,
        device_map=device,
    )
    model.set_model_mode()
    print("✅ Model loaded")
    print()

    # Load validation data
    val_path = dataset_dir / f"{split}.json"
    with open(val_path) as f:
        val_data = json.load(f)

    print(f"📊 Validation set: {len(val_data)} images")
    print()

    # Run predictions
    print("🔍 Running predictions...")
    results = []
    correct = 0
    total = 0

    for item in tqdm(val_data, desc="Validating"):
        # Load image
        image_path = dataset_dir / item["image_path"]
        image = Image.open(image_path).convert("RGB")

        # Predict
        prediction = predict_element(model, processor, image, device)

        # Compare with ground truth
        true_element = item["element"]
        predicted_element = prediction["predicted"]

        is_correct = (predicted_element == true_element)
        if is_correct:
            correct += 1
        total += 1

        results.append({
            "image_id": item["image_id"],
            "true_label": true_element,
            "predicted_label": predicted_element,
            "raw_output": prediction["raw_output"],
            "correct": is_correct,
            "confidence": item.get("confidence", None),
        })

    accuracy = correct / total if total > 0 else 0

    print()
    print("=" * 80)
    print("📊 Validation Results")
    print("=" * 80)
    print(f"Overall Accuracy: {accuracy:.2%} ({correct}/{total})")
    print()

    # Compute per-class metrics
    y_true = [r["true_label"] for r in results]
    y_pred = [r["predicted_label"] for r in results]

    print("Per-Class Metrics:")
    print(classification_report(y_true, y_pred, target_names=ELEMENTS, zero_division=0))
    print()

    # Confusion matrix
    cm = confusion_matrix(y_true, y_pred, labels=ELEMENTS)
    print("Confusion Matrix:")
    print("(rows = true, cols = predicted)")
    print()
    for i, true_elem in enumerate(ELEMENTS):
        print(f"{true_elem:>6s}: {cm[i]}")
    print()

    # Error analysis
    print("Error Analysis:")
    errors = [r for r in results if not r["correct"]]
    print(f"Total errors: {len(errors)}")
    if errors:
        print("\nMost common misclassifications:")
        error_pairs = {}
        for err in errors:
            pair = (err["true_label"], err["predicted_label"])
            error_pairs[pair] = error_pairs.get(pair, 0) + 1

        for (true_l, pred_l), count in sorted(error_pairs.items(), key=lambda x: -x[1])[:5]:
            print(f"  {true_l} → {pred_l}: {count}")
    print()

    # Summary
    summary = {
        "accuracy": accuracy,
        "total": total,
        "correct": correct,
        "errors": len(errors),
        "per_class": {
            elem: {
                "precision": 0.0,
                "recall": 0.0,
                "f1_score": 0.0,
            }
            for elem in ELEMENTS
        },
        "confusion_matrix": cm.tolist(),
        "predictions": results,
    }

    # Compute per-class metrics
    for elem in ELEMENTS:
        true_positives = sum(1 for r in results if r["true_label"] == elem and r["predicted_label"] == elem)
        false_positives = sum(1 for r in results if r["true_label"] != elem and r["predicted_label"] == elem)
        false_negatives = sum(1 for r in results if r["true_label"] == elem and r["predicted_label"] != elem)

        precision = true_positives / (true_positives + false_positives) if (true_positives + false_positives) > 0 else 0
        recall = true_positives / (true_positives + false_negatives) if (true_positives + false_negatives) > 0 else 0
        f1 = 2 * precision * recall / (precision + recall) if (precision + recall) > 0 else 0

        summary["per_class"][elem] = {
            "precision": precision,
            "recall": recall,
            "f1_score": f1,
        }

    return summary


def plot_confusion_matrix(cm: np.ndarray, output_path: Path):
    """Plot and save confusion matrix"""
    plt.figure(figsize=(8, 6))
    sns.heatmap(
        cm,
        annot=True,
        fmt="d",
        cmap="Blues",
        xticklabels=ELEMENTS,
        yticklabels=ELEMENTS,
        cbar_kws={"label": "Count"},
    )
    plt.xlabel("Predicted")
    plt.ylabel("True")
    plt.title("Element Classification Confusion Matrix")
    plt.tight_layout()
    plt.savefig(output_path, dpi=150)
    print(f"📊 Confusion matrix saved to {output_path}")


def main():
    parser = argparse.ArgumentParser(description="Validate SmolVLM element classifier")
    parser.add_argument(
        "--model_path",
        type=str,
        required=True,
        help="Path to fine-tuned model"
    )
    parser.add_argument(
        "--dataset_dir",
        type=str,
        required=True,
        help="Path to dataset directory"
    )
    parser.add_argument(
        "--split",
        type=str,
        default="validation",
        choices=["train", "validation"],
        help="Dataset split to validate on"
    )
    parser.add_argument(
        "--output_path",
        type=str,
        default="./validation_results.json",
        help="Output path for validation results"
    )
    parser.add_argument(
        "--device",
        type=str,
        default="cuda",
        help="Device to use (cuda/cpu)"
    )
    parser.add_argument(
        "--plot_cm",
        action="store_true",
        help="Plot confusion matrix"
    )

    args = parser.parse_args()

    # Run validation
    summary = validate_model(
        model_path=Path(args.model_path),
        dataset_dir=Path(args.dataset_dir),
        split=args.split,
        device=args.device,
    )

    # Save results
    output_path = Path(args.output_path)
    output_path.parent.mkdir(parents=True, exist_ok=True)

    with open(output_path, "w") as f:
        json.dump(summary, f, indent=2)

    print(f"💾 Validation results saved to {output_path}")

    # Plot confusion matrix
    if args.plot_cm:
        cm_path = output_path.parent / "confusion_matrix.png"
        plot_confusion_matrix(np.array(summary["confusion_matrix"]), cm_path)

    print()
    print("=" * 80)
    print("✅ Validation complete!")
    print("=" * 80)


if __name__ == "__main__":
    main()
