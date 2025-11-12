#!/usr/bin/env python3
"""
Validate SmolVLM fine-tuned model on validation set
"""

import argparse
import json
from pathlib import Path
from collections import Counter

import torch
from PIL import Image
from transformers import AutoProcessor, AutoModelForImageTextToText
from peft import PeftModel
from tqdm import tqdm


ELEMENTS = ["water", "land", "fire", "wood", "metal"]

INSTRUCTION_PROMPT = "Classify this image into one of these elements: water, land, fire, wood, metal.\n\nProvide your answer with a brief description."


def validate_model(
    model_dir: str,
    val_jsonl: str,
    images_dir: str,
    batch_size: int = 1,
    max_samples: int = None
):
    """
    Validate fine-tuned SmolVLM model

    Args:
        model_dir: Directory containing fine-tuned model
        val_jsonl: Path to validation JSONL file
        images_dir: Directory containing images
        batch_size: Batch size for inference
        max_samples: Maximum number of samples to validate (None = all)
    """

    print(f"Loading model from {model_dir}...")

    # Load processor and base model
    base_model_name = "HuggingFaceTB/SmolVLM2-500M-Video-Instruct"
    processor = AutoProcessor.from_pretrained(base_model_name)

    model = AutoModelForImageTextToText.from_pretrained(
        base_model_name,
        torch_dtype=torch.float32,
        device_map="auto",
        trust_remote_code=True,
    )

    # Load LoRA adapters
    model_path = Path(model_dir)
    if (model_path / "adapter_model.safetensors").exists() or (model_path / "adapter_model.bin").exists():
        print("Loading LoRA adapters...")
        model = PeftModel.from_pretrained(model, model_dir)
        model = model.merge_and_unload()

    model.eval()

    # Load validation data
    print(f"\nLoading validation data from {val_jsonl}...")
    with open(val_jsonl, 'r') as f:
        val_data = [json.loads(line) for line in f]

    if max_samples:
        val_data = val_data[:max_samples]

    print(f"Validating on {len(val_data)} samples...")

    # Run validation
    images_path = Path(images_dir)
    predictions = []
    ground_truths = []

    correct = 0
    total = 0

    element_correct = Counter()
    element_total = Counter()

    for item in tqdm(val_data, desc="Validating"):
        # Extract image path and ground truth
        if 'image_path' in item:
            img_filename = item['image_path']
        elif 'image' in item:
            img_filename = item['image']
        else:
            print(f"Warning: No image path in item: {item}")
            continue

        # Extract ground truth element
        if 'element' in item:
            gt_element = item['element']
        elif 'conversations' in item and len(item['conversations']) > 1:
            content = item['conversations'][1]['content']
            # Extract element from descriptive format: "...The element is fire."
            if "The element is " in content:
                gt_element = content.split("The element is ")[-1].rstrip(".").split()[0].lower()
            else:
                gt_element = content
        else:
            print(f"Warning: No element in item: {item}")
            continue

        # Load image
        if img_filename.startswith('images/'):
            img_filename = img_filename.replace('images/', '', 1)

        img_path = images_path / img_filename

        if not img_path.exists():
            print(f"Warning: Image not found: {img_path}")
            continue

        image = Image.open(img_path).convert('RGB')

        # Prepare prompt
        prompt = f"<image>\n{INSTRUCTION_PROMPT}"

        # Process inputs
        inputs = processor(images=[image], text=prompt, return_tensors="pt")
        inputs = {k: v.to(model.device) for k, v in inputs.items()}

        # Generate prediction
        with torch.no_grad():
            outputs = model.generate(
                **inputs,
                max_new_tokens=100,  # Increased for descriptive format
                do_sample=False,
            )

        # Decode prediction
        generated_tokens = outputs[0][inputs['input_ids'].shape[1]:]
        pred_text = processor.decode(generated_tokens, skip_special_tokens=True).strip().lower()

        # Check if ground truth element is in prediction
        is_correct = gt_element.lower() in pred_text

        predictions.append(pred_text)
        ground_truths.append(gt_element)

        if is_correct:
            correct += 1
            element_correct[gt_element] += 1

        total += 1
        element_total[gt_element] += 1

    # Print results
    print("\n" + "=" * 60)
    print("Validation Results")
    print("=" * 60)

    overall_acc = (correct / total * 100) if total > 0 else 0
    print(f"\nOverall Accuracy: {correct}/{total} ({overall_acc:.2f}%)")

    print("\nPer-Element Accuracy:")
    for elem in ELEMENTS:
        elem_total = element_total.get(elem, 0)
        elem_correct = element_correct.get(elem, 0)
        elem_acc = (elem_correct / elem_total * 100) if elem_total > 0 else 0
        print(f"  {elem:8s}: {elem_correct:3d}/{elem_total:3d} ({elem_acc:5.1f}%)")

    # Show sample predictions
    print("\nSample Predictions:")
    for i in range(min(10, len(predictions))):
        gt = ground_truths[i]
        pred = predictions[i]
        status = "✓" if gt.lower() in pred else "✗"
        print(f"  {status} GT: {gt:8s} | Pred: {pred}")

    print("=" * 60)

    return overall_acc


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Validate SmolVLM fine-tuned model")
    parser.add_argument("--model_dir", type=str, default="./output/final",
                        help="Directory containing fine-tuned model")
    parser.add_argument("--val_jsonl", type=str, default="./dataset_balanced_smolvlm/val.jsonl",
                        help="Path to validation JSONL")
    parser.add_argument("--images_dir", type=str, default="./dataset_balanced_smolvlm/images",
                        help="Directory containing images")
    parser.add_argument("--batch_size", type=int, default=1,
                        help="Batch size for inference")
    parser.add_argument("--max_samples", type=int, default=None,
                        help="Maximum number of samples to validate")

    args = parser.parse_args()

    validate_model(
        model_dir=args.model_dir,
        val_jsonl=args.val_jsonl,
        images_dir=args.images_dir,
        batch_size=args.batch_size,
        max_samples=args.max_samples
    )
