#!/usr/bin/env python3
"""
SmolVLM Element Classification Training Script
Quick POC for training on full images, with plans for cropped dataset enhancement

Usage:
    # Mac (testing)
    python train_smolvlm.py --config configs/mac.yaml

    # L40S server (training)
    python train_smolvlm.py --config configs/l40s.yaml

    # Colab
    python train_smolvlm.py --config configs/colab.yaml
"""

import argparse
import json
from pathlib import Path
from typing import Dict, List
import yaml

import torch
from PIL import Image
from transformers import (
    AutoProcessor,
    AutoModelForImageTextToText,
    TrainingArguments,
    Trainer,
    EarlyStoppingCallback,
)
from peft import LoraConfig, get_peft_model, prepare_model_for_kbit_training
import numpy as np


# Element classes (matching Android ElementMapper)
ELEMENTS = ["water", "land", "fire", "wood", "metal"]

# Android alignment constants
ANDROID_IMAGE_SIZE = 256  # SmolVLMManager.kt:35
ANDROID_IMAGE_MARKER = "<__media__>"  # SmolVLMManager.kt:32


def load_config(config_path: str) -> Dict:
    """Load YAML configuration file"""
    with open(config_path) as f:
        return yaml.safe_load(f)


def load_jsonl_dataset(jsonl_path: Path, images_dir: Path) -> List[Dict]:
    """Load dataset from JSONL file"""
    data = []
    with open(jsonl_path) as f:
        for line in f:
            item = json.loads(line.strip())
            # Verify image exists
            image_path = images_dir / item['image_path']
            if image_path.exists():
                data.append(item)
            else:
                print(f"Warning: Image not found: {image_path}")
    return data


def build_prompt(item: Dict, include_context: bool = True) -> tuple[str, str]:
    """
    Build prompt matching Android format

    Args:
        item: Dataset item with 'element' and 'reason' fields
        include_context: Whether to include reasoning context (helpful for training)

    Returns:
        (prompt, target) tuple
    """
    # Base prompt (matches Android inference)
    base_prompt = f"""{ANDROID_IMAGE_MARKER}
Classify this image into one of these elements: water, land, fire, wood, metal.

Element:"""

    # Add context for training (helps model learn reasoning)
    if include_context and 'reason' in item:
        prompt = f"""{ANDROID_IMAGE_MARKER}
Classify this image into one of these elements: water, land, fire, wood, metal.

Context: {item['reason']}

Element:"""
    else:
        prompt = base_prompt

    # Target: just the element label
    target = item['element']

    return prompt, target


class ElementDataset(torch.utils.data.Dataset):
    """Dataset for element classification"""

    def __init__(
        self,
        data: List[Dict],
        images_dir: Path,
        processor,
        max_length: int = 128,
        include_context: bool = True,
    ):
        self.data = data
        self.images_dir = images_dir
        self.processor = processor
        self.max_length = max_length
        self.include_context = include_context

        print(f"Loaded {len(self.data)} samples")

    def __len__(self):
        return len(self.data)

    def __getitem__(self, idx):
        item = self.data[idx]

        # Load image (already 256x256 from dataset prep)
        image_path = self.images_dir / item['image_path']
        image = Image.open(image_path).convert('RGB')

        # Build prompt and target
        prompt, target = build_prompt(item, self.include_context)

        # Process image + prompt
        inputs = self.processor(
            images=image,
            text=prompt,
            return_tensors="pt",
            padding="max_length",
            max_length=self.max_length,
            truncation=True,
        )

        # Tokenize target separately
        target_tokens = self.processor.tokenizer(
            target,
            add_special_tokens=False,
            return_tensors="pt",
        )

        # Concatenate prompt + target + eos
        input_ids = torch.cat([
            inputs["input_ids"].squeeze(0),
            target_tokens["input_ids"].squeeze(0),
            torch.tensor([self.processor.tokenizer.eos_token_id])
        ], dim=0)

        # Truncate if needed
        if len(input_ids) > self.max_length:
            input_ids = input_ids[:self.max_length]

        # Pad if needed
        if len(input_ids) < self.max_length:
            padding = torch.full(
                (self.max_length - len(input_ids),),
                self.processor.tokenizer.pad_token_id,
                dtype=torch.long
            )
            input_ids = torch.cat([input_ids, padding])

        # Create labels: -100 for prompt tokens, actual tokens for target
        labels = input_ids.clone()
        prompt_length = inputs["input_ids"].size(1)
        labels[:prompt_length] = -100  # Ignore prompt in loss

        # Create attention mask
        attention_mask = (input_ids != self.processor.tokenizer.pad_token_id).long()

        # Get pixel values
        pixel_values = inputs["pixel_values"].squeeze(0)

        return {
            "input_ids": input_ids,
            "attention_mask": attention_mask,
            "labels": labels,
            "pixel_values": pixel_values,
        }


def compute_metrics(eval_pred):
    """Compute accuracy metrics"""
    predictions, labels = eval_pred

    # predictions are logits, get the predicted token IDs
    if isinstance(predictions, tuple):
        predictions = predictions[0]

    # Get predicted tokens (argmax over vocab dimension)
    pred_tokens = np.argmax(predictions, axis=-1)

    # TODO: Decode tokens to text and compute element-level accuracy
    # For now, return token-level accuracy

    # Mask out -100 labels
    mask = labels != -100

    # Compute accuracy only on non-masked tokens
    correct = (pred_tokens == labels) & mask
    accuracy = correct.sum() / mask.sum()

    return {
        "accuracy": float(accuracy),
    }


def main():
    parser = argparse.ArgumentParser(description="Train SmolVLM for element classification")
    parser.add_argument("--config", type=str, required=True, help="Path to config YAML")
    parser.add_argument("--dataset_dir", type=str, default="./dataset", help="Dataset directory")
    parser.add_argument("--output_dir", type=str, default="./output", help="Output directory")
    parser.add_argument("--wandb", action="store_true", help="Enable W&B logging")
    args = parser.parse_args()

    # Load config
    config = load_config(args.config)
    print(f"Loaded config from {args.config}")
    print(f"Config: {json.dumps(config, indent=2)}")

    # Setup paths
    dataset_dir = Path(args.dataset_dir)
    output_dir = Path(args.output_dir)
    output_dir.mkdir(parents=True, exist_ok=True)

    # Load dataset
    print("\n=== Loading Dataset ===")
    train_data = load_jsonl_dataset(
        dataset_dir / "train.jsonl",
        dataset_dir
    )
    val_data = load_jsonl_dataset(
        dataset_dir / "val.jsonl",
        dataset_dir
    )

    print(f"Train samples: {len(train_data)}")
    print(f"Val samples: {len(val_data)}")

    # Element distribution
    train_elements = [item['element'] for item in train_data]
    print("\nTrain element distribution:")
    for elem in ELEMENTS:
        count = train_elements.count(elem)
        print(f"  {elem}: {count} ({count/len(train_elements)*100:.1f}%)")

    # Load model and processor
    print("\n=== Loading Model ===")
    model_name = config['model']['base_model']
    print(f"Base model: {model_name}")

    processor = AutoProcessor.from_pretrained(model_name)

    # Load model with device map
    model = AutoModelForImageTextToText.from_pretrained(
        model_name,
        torch_dtype=torch.bfloat16 if config['training'].get('bf16', False) else torch.float32,
        device_map="auto",
        trust_remote_code=True,  # Required for SmolVLM
    )

    # Prepare for LoRA
    if config['training'].get('gradient_checkpointing', False):
        model.gradient_checkpointing_enable()

    model = prepare_model_for_kbit_training(model)

    # Setup LoRA
    print("\n=== Configuring LoRA ===")
    lora_config = LoraConfig(
        r=config['lora']['rank'],
        lora_alpha=config['lora']['alpha'],
        lora_dropout=config['lora']['dropout'],
        target_modules=config['lora']['target_modules'],
        task_type="CAUSAL_LM",
    )

    model = get_peft_model(model, lora_config)
    model.print_trainable_parameters()

    # Create datasets
    print("\n=== Creating PyTorch Datasets ===")
    train_dataset = ElementDataset(
        train_data,
        dataset_dir / "images",
        processor,
        max_length=config['data'].get('max_length', 128),
        include_context=config['data'].get('include_context', True),
    )

    val_dataset = ElementDataset(
        val_data,
        dataset_dir / "images",
        processor,
        max_length=config['data'].get('max_length', 128),
        include_context=config['data'].get('include_context', True),
    )

    # Training arguments
    print("\n=== Setting up Training ===")
    training_args = TrainingArguments(
        output_dir=str(output_dir),
        num_train_epochs=config['training']['num_epochs'],
        per_device_train_batch_size=config['training']['batch_size'],
        per_device_eval_batch_size=config['training']['batch_size'],
        gradient_accumulation_steps=config['training'].get('gradient_accumulation_steps', 1),
        learning_rate=config['training']['learning_rate'],
        warmup_steps=config['training'].get('warmup_steps', 100),
        logging_steps=config['training'].get('logging_steps', 10),
        eval_steps=config['training'].get('eval_steps', 50),
        save_steps=config['training'].get('save_steps', 100),
        eval_strategy="steps",
        save_strategy="steps",
        load_best_model_at_end=True,
        metric_for_best_model="eval_loss",
        greater_is_better=False,
        bf16=config['training'].get('bf16', False),
        gradient_checkpointing=config['training'].get('gradient_checkpointing', False),
        report_to="wandb" if args.wandb else "none",
        save_total_limit=3,
        dataloader_num_workers=config['training'].get('num_workers', 0),
    )

    # Create trainer
    trainer = Trainer(
        model=model,
        args=training_args,
        train_dataset=train_dataset,
        eval_dataset=val_dataset,
        compute_metrics=compute_metrics,
        callbacks=[EarlyStoppingCallback(early_stopping_patience=2)],
    )

    # Train
    print("\n=== Starting Training ===")
    trainer.train()

    # Save final model
    print("\n=== Saving Model ===")
    final_output_dir = output_dir / "final"
    trainer.save_model(str(final_output_dir))
    processor.save_pretrained(str(final_output_dir))

    print(f"\n✅ Training complete! Model saved to {final_output_dir}")
    print("\nNext steps:")
    print("1. Run validation: python validate.py --model_dir ./output/final")
    print("2. Merge LoRA adapters and convert to GGUF for Android deployment")


if __name__ == "__main__":
    main()
