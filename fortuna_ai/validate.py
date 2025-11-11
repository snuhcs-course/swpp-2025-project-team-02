#!/usr/bin/env python3
"""
SmolVLM Validation Script
Evaluate fine-tuned model on validation set

Usage:
    python validate.py --model_dir ./output/final --dataset_dir ./dataset
"""

import argparse
import json
from pathlib import Path
from typing import Dict, List
from collections import defaultdict

import torch
from PIL import Image
from transformers import AutoProcessor, AutoModelForImageTextToText
from peft import PeftModel
from tqdm import tqdm
from sklearn.metrics import classification_report, confusion_matrix
import numpy as np


ELEMENTS = ["water", "land", "fire", "wood", "metal"]
ANDROID_IMAGE_MARKER = "<__media__>"


def load_jsonl_dataset(jsonl_path: Path, images_dir: Path) -> List[Dict]:
    """Load dataset from JSONL file"""
    data = []
    with open(jsonl_path) as f:
        for line in f:
            item = json.loads(line.strip())
            # Handle both "images/xxx.jpg" and "xxx.jpg" formats
            image_path_str = item['image_path']
            if image_path_str.startswith('images/'):
                image_path_str = image_path_str.replace('images/', '', 1)
            image_path = images_dir / image_path_str
            if image_path.exists():
                data.append(item)
    return data


def build_inference_prompt() -> str:
    """Build prompt for inference (without context)"""
    return f"""{ANDROID_IMAGE_MARKER}
Classify this image into one of these elements: water, land, fire, wood, metal.

Element:"""


def extract_element_from_output(output_text: str) -> str:
    """Extract element label from model output"""
    output_text = output_text.strip().lower()
    
    for element in ELEMENTS:
        if element in output_text:
            return element
    
    first_word = output_text.split()[0] if output_text else ""
    return first_word


def validate_model(
    model,
    processor,
    val_data: List[Dict],
    images_dir: Path,
    device: str = "cuda",
    max_samples: int = None,
) -> Dict:
    """Validate model on validation set"""
    model.eval()
    
    prompt = build_inference_prompt()
    predictions = []
    ground_truth = []
    element_correct = defaultdict(int)
    element_total = defaultdict(int)
    
    if max_samples:
        val_data = val_data[:max_samples]
    
    print(f"\nValidating on {len(val_data)} samples...")
    
    with torch.no_grad():
        for item in tqdm(val_data, desc="Validating"):
            # Handle both "images/xxx.jpg" and "xxx.jpg" formats
            image_path_str = item['image_path']
            if image_path_str.startswith('images/'):
                image_path_str = image_path_str.replace('images/', '', 1)
            image_path = images_dir / image_path_str
            image = Image.open(image_path).convert('RGB')
            
            inputs = processor(images=image, text=prompt, return_tensors="pt").to(device)
            generated_ids = model.generate(**inputs, max_new_tokens=32, do_sample=False)
            generated_text = processor.batch_decode(generated_ids, skip_special_tokens=True)[0]
            generated_text = generated_text.replace(prompt, "").strip()
            predicted_element = extract_element_from_output(generated_text)
            true_element = item['element']
            
            predictions.append(predicted_element)
            ground_truth.append(true_element)
            element_total[true_element] += 1
            if predicted_element == true_element:
                element_correct[true_element] += 1
    
    correct = sum(p == t for p, t in zip(predictions, ground_truth))
    accuracy = correct / len(predictions) if predictions else 0
    
    element_metrics = {}
    for element in ELEMENTS:
        total = element_total[element]
        if total > 0:
            element_metrics[element] = {
                'accuracy': element_correct[element] / total,
                'correct': element_correct[element],
                'total': total,
            }
        else:
            element_metrics[element] = {'accuracy': 0.0, 'correct': 0, 'total': 0}
    
    cm = confusion_matrix(ground_truth, predictions, labels=ELEMENTS)
    report = classification_report(ground_truth, predictions, labels=ELEMENTS, target_names=ELEMENTS, zero_division=0)
    
    return {
        'accuracy': accuracy,
        'correct': correct,
        'total': len(predictions),
        'element_metrics': element_metrics,
        'confusion_matrix': cm,
        'classification_report': report,
        'predictions': predictions,
        'ground_truth': ground_truth,
    }


def print_results(metrics: Dict):
    """Pretty print validation results"""
    print("\n" + "="*60)
    print("VALIDATION RESULTS")
    print("="*60)
    print(f"\nOverall Accuracy: {metrics['accuracy']:.2%} ({metrics['correct']}/{metrics['total']})")
    print("\nPer-Element Accuracy:")
    print("-" * 60)
    for element in ELEMENTS:
        m = metrics['element_metrics'][element]
        print(f"  {element:8s}: {m['accuracy']:6.2%} ({m['correct']:3d}/{m['total']:3d})")
    print("\nClassification Report:")
    print("-" * 60)
    print(metrics['classification_report'])
    print("\nConfusion Matrix:")
    print("-" * 60)
    print("Rows: True, Columns: Predicted")
    print(f"{'':8s}", end='')
    for elem in ELEMENTS:
        print(f"{elem[:6]:8s}", end='')
    print()
    cm = metrics['confusion_matrix']
    for i, true_elem in enumerate(ELEMENTS):
        print(f"{true_elem[:6]:8s}", end='')
        for j in range(len(ELEMENTS)):
            print(f"{cm[i][j]:8d}", end='')
        print()
    print("="*60)


def main():
    parser = argparse.ArgumentParser(description="Validate SmolVLM element classifier")
    parser.add_argument("--model_dir", type=str, required=True)
    parser.add_argument("--dataset_dir", type=str, default="./dataset")
    parser.add_argument("--max_samples", type=int, default=None)
    parser.add_argument("--device", type=str, default=None)
    parser.add_argument("--output", type=str, default=None)
    args = parser.parse_args()
    
    if args.device:
        device = args.device
    elif torch.cuda.is_available():
        device = "cuda"
    elif torch.backends.mps.is_available():
        device = "mps"
    else:
        device = "cpu"
    
    print(f"Using device: {device}")
    print(f"\nLoading model from {args.model_dir}...")
    processor = AutoProcessor.from_pretrained(args.model_dir)
    model = AutoModelForImageTextToText.from_pretrained(
        args.model_dir,
        torch_dtype=torch.bfloat16 if device == "cuda" else torch.float32,
        trust_remote_code=True,
    )
    print("Model loaded successfully!")
    
    dataset_dir = Path(args.dataset_dir)
    print(f"\nLoading validation data from {dataset_dir}...")
    val_data = load_jsonl_dataset(dataset_dir / "val.jsonl", dataset_dir / "images")
    print(f"Loaded {len(val_data)} validation samples")
    
    metrics = validate_model(model, processor, val_data, dataset_dir / "images", device=device, max_samples=args.max_samples)
    print_results(metrics)
    
    if args.output:
        output_path = Path(args.output)
        save_metrics = {
            'accuracy': metrics['accuracy'],
            'correct': metrics['correct'],
            'total': metrics['total'],
            'element_metrics': metrics['element_metrics'],
            'confusion_matrix': metrics['confusion_matrix'].tolist(),
            'classification_report': metrics['classification_report'],
        }
        output_path.parent.mkdir(parents=True, exist_ok=True)
        with open(output_path, 'w') as f:
            json.dump(save_metrics, f, indent=2)
        print(f"\n✅ Results saved to {output_path}")


if __name__ == "__main__":
    main()
