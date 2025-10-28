#!/usr/bin/env python3
"""
SmolVLM Fine-tuning Script for Element Classification
Uses LoRA for efficient training with W&B tracking

Usage:
    python finetune_smolvlm.py \
        --dataset_dir ./dataset \
        --output_dir ./models/smolvlm-element-classifier \
        --wandb_project smolvlm-element-classification
"""

import argparse
import json
import os
from pathlib import Path
from typing import Dict, List, Optional
import torch
from PIL import Image
from transformers import (
    AutoProcessor,
    AutoModelForVision2Seq,
    TrainingArguments,
    Trainer,
    EarlyStoppingCallback,
)
from peft import LoraConfig, get_peft_model, TaskType, prepare_model_for_kbit_training
from datasets import Dataset, DatasetDict
import wandb
from dataclasses import dataclass
import numpy as np
from tqdm import tqdm


# Element categories
ELEMENTS = ["water", "land", "fire", "wood", "metal"]


@dataclass
class ModelConfig:
    """Model and training configuration"""
    base_model: str = "HuggingFaceM4/SmolVLM2-500M-Video-Instruct"
    lora_r: int = 16
    lora_alpha: int = 32
    lora_dropout: float = 0.05
    target_modules: List[str] = None

    def __post_init__(self):
        # Target attention and MLP layers in vision and language models
        if self.target_modules is None:
            self.target_modules = [
                "q_proj", "k_proj", "v_proj", "o_proj",  # Attention
                "gate_proj", "up_proj", "down_proj",      # MLP
            ]


class ElementClassificationDataset:
    """Custom dataset for element classification"""

    def __init__(
        self,
        data_path: Path,
        dataset_dir: Path,
        processor,
        max_length: int = 128,
    ):
        with open(data_path) as f:
            self.data = json.load(f)

        self.dataset_dir = dataset_dir
        self.processor = processor
        self.max_length = max_length

        print(f"Loaded {len(self.data)} examples from {data_path}")

    def __len__(self):
        return len(self.data)

    def build_prompt(self, item: Dict) -> str:
        """
        Build training prompt with reason in input, but ONLY element label in output.
        This teaches the model to classify based on visual features (learned from reason),
        but output concisely.
        """
        # Input: Include image marker + task description + reason for context
        # Output: ONLY the element label (to save tokens during inference)

        prompt = f"""<image>Classify this image into one of these elements: water, land, fire, wood, metal.

Context: {item['reason']}

Element:"""

        # Target: ONLY the label (e.g., "fire")
        # No explanation, no JSON, just the single word
        target = item['element']

        return prompt, target

    def __getitem__(self, idx):
        item = self.data[idx]

        # Load image
        image_path = self.dataset_dir / item['image_path']
        image = Image.open(image_path).convert('RGB')

        # Build prompt and target
        prompt, target = self.build_prompt(item)

        # Process with SmolVLM processor
        # Processor handles image + text tokenization
        inputs = self.processor(
            images=image,
            text=prompt,
            return_tensors="pt",
            padding="max_length",
            truncation=True,
            max_length=self.max_length,
        )

        # Tokenize target (just the element label)
        target_ids = self.processor.tokenizer(
            target,
            return_tensors="pt",
            padding="max_length",
            truncation=True,
            max_length=10,  # Element names are short (max ~5 tokens)
        )["input_ids"]

        # Prepare labels for causal LM loss
        # Set prompt tokens to -100 (ignored in loss), only compute loss on target
        labels = inputs["input_ids"].clone()
        prompt_length = inputs["input_ids"].size(1) - target_ids.size(1)
        labels[:, :prompt_length] = -100  # Ignore prompt in loss

        return {
            "pixel_values": inputs["pixel_values"].squeeze(0),
            "input_ids": inputs["input_ids"].squeeze(0),
            "attention_mask": inputs["attention_mask"].squeeze(0),
            "labels": labels.squeeze(0),
        }


def prepare_datasets(
    dataset_dir: Path,
    processor,
    max_length: int = 128,
) -> DatasetDict:
    """Prepare train and validation datasets"""

    train_dataset = ElementClassificationDataset(
        data_path=dataset_dir / "train.json",
        dataset_dir=dataset_dir,
        processor=processor,
        max_length=max_length,
    )

    val_dataset = ElementClassificationDataset(
        data_path=dataset_dir / "val.json",
        dataset_dir=dataset_dir,
        processor=processor,
        max_length=max_length,
    )

    # Convert to HF Dataset format
    train_hf = Dataset.from_dict({
        k: [train_dataset[i][k] for i in range(len(train_dataset))]
        for k in train_dataset[0].keys()
    })

    val_hf = Dataset.from_dict({
        k: [val_dataset[i][k] for i in range(len(val_dataset))]
        for k in val_dataset[0].keys()
    })

    return DatasetDict({"train": train_hf, "validation": val_hf})


def compute_metrics(eval_pred):
    """Compute accuracy metrics"""
    predictions, labels = eval_pred

    # Get predicted token IDs (argmax over logits)
    if isinstance(predictions, tuple):
        predictions = predictions[0]

    pred_ids = np.argmax(predictions, axis=-1)

    # Only compute accuracy on non-ignored tokens
    mask = labels != -100

    # Element-wise accuracy
    correct = (pred_ids == labels) & mask
    accuracy = correct.sum() / mask.sum()

    return {"accuracy": accuracy}


def setup_wandb(args, model_config: ModelConfig):
    """Initialize Weights & Biases tracking"""
    wandb.init(
        project=args.wandb_project,
        name=args.run_name or f"smolvlm-element-lora-r{model_config.lora_r}",
        config={
            "base_model": model_config.base_model,
            "lora_r": model_config.lora_r,
            "lora_alpha": model_config.lora_alpha,
            "lora_dropout": model_config.lora_dropout,
            "target_modules": model_config.target_modules,
            "learning_rate": args.learning_rate,
            "batch_size": args.batch_size,
            "num_epochs": args.num_epochs,
            "max_length": args.max_length,
            "output_format": "label_only",  # Key optimization
        },
        tags=["smolvlm", "element-classification", "lora", "mobile"],
    )


def main():
    parser = argparse.ArgumentParser(description="Fine-tune SmolVLM for element classification")

    # Dataset
    parser.add_argument("--dataset_dir", type=str, required=True, help="Path to dataset directory")
    parser.add_argument("--output_dir", type=str, required=True, help="Output directory for model")

    # Model
    parser.add_argument("--base_model", type=str, default="HuggingFaceM4/SmolVLM2-500M-Video-Instruct")
    parser.add_argument("--lora_r", type=int, default=16, help="LoRA rank")
    parser.add_argument("--lora_alpha", type=int, default=32, help="LoRA alpha")
    parser.add_argument("--lora_dropout", type=float, default=0.05, help="LoRA dropout")

    # Training
    parser.add_argument("--learning_rate", type=float, default=2e-4, help="Learning rate")
    parser.add_argument("--batch_size", type=int, default=4, help="Batch size per device")
    parser.add_argument("--gradient_accumulation_steps", type=int, default=4, help="Gradient accumulation")
    parser.add_argument("--num_epochs", type=int, default=3, help="Number of epochs")
    parser.add_argument("--max_length", type=int, default=128, help="Max sequence length")
    parser.add_argument("--warmup_ratio", type=float, default=0.1, help="Warmup ratio")
    parser.add_argument("--weight_decay", type=float, default=0.01, help="Weight decay")

    # W&B
    parser.add_argument("--wandb_project", type=str, default="smolvlm-element-classification")
    parser.add_argument("--run_name", type=str, default=None, help="W&B run name")

    # Hardware
    parser.add_argument("--bf16", action="store_true", help="Use bfloat16 precision")
    parser.add_argument("--fp16", action="store_true", help="Use float16 precision")

    args = parser.parse_args()

    # Setup
    dataset_dir = Path(args.dataset_dir)
    output_dir = Path(args.output_dir)
    output_dir.mkdir(parents=True, exist_ok=True)

    print("=" * 80)
    print("SmolVLM Element Classification Fine-tuning")
    print("=" * 80)
    print(f"📁 Dataset: {dataset_dir}")
    print(f"💾 Output: {output_dir}")
    print(f"🎯 Task: 5-class element classification (label-only output)")
    print()

    # Model config
    model_config = ModelConfig(
        base_model=args.base_model,
        lora_r=args.lora_r,
        lora_alpha=args.lora_alpha,
        lora_dropout=args.lora_dropout,
    )

    # Initialize W&B
    print("🔍 Initializing Weights & Biases...")
    setup_wandb(args, model_config)
    print()

    # Load processor
    print(f"📥 Loading processor from {model_config.base_model}...")
    processor = AutoProcessor.from_pretrained(model_config.base_model)
    print()

    # Load base model
    print(f"📥 Loading base model {model_config.base_model}...")
    model = AutoModelForVision2Seq.from_pretrained(
        model_config.base_model,
        torch_dtype=torch.bfloat16 if args.bf16 else (torch.float16 if args.fp16 else torch.float32),
        device_map="auto",
    )
    print(f"✅ Model loaded ({model.num_parameters() / 1e6:.1f}M parameters)")
    print()

    # Apply LoRA
    print("🔧 Applying LoRA...")
    lora_config = LoraConfig(
        r=model_config.lora_r,
        lora_alpha=model_config.lora_alpha,
        lora_dropout=model_config.lora_dropout,
        target_modules=model_config.target_modules,
        task_type=TaskType.CAUSAL_LM,
        bias="none",
    )

    model = prepare_model_for_kbit_training(model)
    model = get_peft_model(model, lora_config)

    trainable_params = sum(p.numel() for p in model.parameters() if p.requires_grad)
    total_params = sum(p.numel() for p in model.parameters())
    print(f"✅ LoRA applied")
    print(f"   Trainable: {trainable_params:,} ({trainable_params/total_params*100:.2f}%)")
    print(f"   Total: {total_params:,}")
    print()

    # Prepare datasets
    print("📊 Preparing datasets...")
    datasets = prepare_datasets(
        dataset_dir=dataset_dir,
        processor=processor,
        max_length=args.max_length,
    )
    print(f"✅ Train: {len(datasets['train'])} examples")
    print(f"✅ Val:   {len(datasets['validation'])} examples")
    print()

    # Training arguments
    effective_batch_size = args.batch_size * args.gradient_accumulation_steps
    print(f"🎯 Effective batch size: {effective_batch_size}")
    print(f"🎯 Warmup steps: {int(len(datasets['train']) / effective_batch_size * args.warmup_ratio)}")
    print()

    training_args = TrainingArguments(
        output_dir=str(output_dir),

        # Training
        num_train_epochs=args.num_epochs,
        per_device_train_batch_size=args.batch_size,
        per_device_eval_batch_size=args.batch_size,
        gradient_accumulation_steps=args.gradient_accumulation_steps,
        learning_rate=args.learning_rate,
        weight_decay=args.weight_decay,
        warmup_ratio=args.warmup_ratio,

        # Precision
        bf16=args.bf16,
        fp16=args.fp16,

        # Evaluation
        eval_strategy="steps",
        eval_steps=50,
        save_strategy="steps",
        save_steps=50,
        save_total_limit=3,
        load_best_model_at_end=True,
        metric_for_best_model="accuracy",
        greater_is_better=True,

        # Logging
        logging_dir=str(output_dir / "logs"),
        logging_steps=10,
        report_to="wandb",

        # Optimization
        gradient_checkpointing=True,
        optim="adamw_torch",

        # Misc
        remove_unused_columns=False,
        dataloader_num_workers=4,
        dataloader_pin_memory=True,
    )

    # Trainer
    trainer = Trainer(
        model=model,
        args=training_args,
        train_dataset=datasets["train"],
        eval_dataset=datasets["validation"],
        compute_metrics=compute_metrics,
        callbacks=[
            EarlyStoppingCallback(early_stopping_patience=3),
        ],
    )

    # Train
    print("🚀 Starting training...")
    print("=" * 80)
    trainer.train()
    print("=" * 80)
    print("✅ Training complete!")
    print()

    # Save final model
    print("💾 Saving final model...")
    trainer.save_model(str(output_dir / "final"))
    processor.save_pretrained(str(output_dir / "final"))

    # Save LoRA adapter separately (for easy merging)
    model.save_pretrained(str(output_dir / "lora_adapter"))
    print(f"✅ Model saved to {output_dir / 'final'}")
    print(f"✅ LoRA adapter saved to {output_dir / 'lora_adapter'}")
    print()

    # Evaluation on validation set
    print("📊 Final evaluation on validation set...")
    eval_results = trainer.evaluate()
    print("Validation Results:")
    for key, value in eval_results.items():
        print(f"  {key}: {value:.4f}")

    # Log final results to W&B
    wandb.log({"final_validation": eval_results})

    print()
    print("🎉 Fine-tuning complete!")
    print(f"📁 Model location: {output_dir / 'final'}")
    print()
    print("Next steps:")
    print("  1. Merge LoRA weights: python merge_lora.py")
    print("  2. Convert to GGUF: bash convert_to_gguf.sh")
    print("  3. Test on Android device")

    wandb.finish()


if __name__ == "__main__":
    main()
