#!/usr/bin/env python3
"""
Mac-optimized version of finetune_smolvlm_v2.py
- MPS (Metal) support
- Lower memory usage
- FP16 only (no BF16)
"""

import sys
import torch

# Check if running on Mac
if not torch.backends.mps.is_available():
    print("⚠️  Warning: MPS (Metal) not available")
    print("   This script is optimized for Apple Silicon Macs")
    response = input("Continue anyway? (y/n): ")
    if response.lower() != 'y':
        sys.exit(1)

# Import rest of modules
import argparse
import json
from pathlib import Path
from typing import Dict, List
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


ELEMENTS = ["water", "land", "fire", "wood", "metal"]
ANDROID_IMAGE_SIZE = 256
ANDROID_MAX_TOKENS = 32
ANDROID_IMAGE_MARKER = "<__media__>"


@dataclass
class ModelConfig:
    base_model: str = "HuggingFaceM4/SmolVLM2-500M-Video-Instruct"
    lora_r: int = 8  # Reduced for Mac
    lora_alpha: int = 16  # Reduced for Mac
    lora_dropout: float = 0.05
    target_modules: List[str] = None

    def __post_init__(self):
        if self.target_modules is None:
            self.target_modules = [
                "q_proj", "k_proj", "v_proj", "o_proj",
                "gate_proj", "up_proj", "down_proj",
            ]


def resize_for_android(image: Image.Image, target_size: int = ANDROID_IMAGE_SIZE) -> Image.Image:
    """Resize EXACTLY like Android"""
    width, height = image.size
    if width == target_size and height == target_size:
        return image

    scale_factor = min(target_size / width, target_size / height)
    scaled_width = int(width * scale_factor)
    scaled_height = int(height * scale_factor)

    scaled_image = image.resize((scaled_width, scaled_height), Image.Resampling.LANCZOS)
    padded_image = Image.new("RGB", (target_size, target_size), (0, 0, 0))

    left = (target_size - scaled_width) // 2
    top = (target_size - scaled_height) // 2
    padded_image.paste(scaled_image, (left, top))

    return padded_image


class AndroidAlignedDataset:
    def __init__(self, data_path: Path, dataset_dir: Path, processor, max_length: int = 128):
        with open(data_path) as f:
            self.data = json.load(f)
        self.dataset_dir = dataset_dir
        self.processor = processor
        self.max_length = max_length
        print(f"Loaded {len(self.data)} examples from {data_path}")

    def __len__(self):
        return len(self.data)

    def build_android_prompt(self, item: Dict) -> tuple:
        prompt = f"""{ANDROID_IMAGE_MARKER}
Classify this image into one of these elements: water, land, fire, wood, metal.

Context: {item['reason']}

Element:"""
        target = item['element']
        return prompt, target

    def __getitem__(self, idx):
        item = self.data[idx]

        image_path = self.dataset_dir / item['image_path']
        image = Image.open(image_path).convert('RGB')
        image = resize_for_android(image, ANDROID_IMAGE_SIZE)

        prompt, target = self.build_android_prompt(item)

        inputs = self.processor(
            images=image,
            text=prompt,
            return_tensors="pt",
            padding="max_length",
            truncation=True,
            max_length=self.max_length,
        )

        target_ids = self.processor.tokenizer(
            target,
            return_tensors="pt",
            padding="max_length",
            truncation=True,
            max_length=ANDROID_MAX_TOKENS,
        )["input_ids"]

        labels = inputs["input_ids"].clone()
        prompt_length = inputs["input_ids"].size(1) - target_ids.size(1)
        labels[:, :prompt_length] = -100

        return {
            "pixel_values": inputs["pixel_values"].squeeze(0),
            "input_ids": inputs["input_ids"].squeeze(0),
            "attention_mask": inputs["attention_mask"].squeeze(0),
            "labels": labels.squeeze(0),
        }


def prepare_datasets(dataset_dir: Path, processor, max_length: int = 128) -> DatasetDict:
    train_dataset = AndroidAlignedDataset(
        data_path=dataset_dir / "train.json",
        dataset_dir=dataset_dir,
        processor=processor,
        max_length=max_length,
    )

    val_dataset = AndroidAlignedDataset(
        data_path=dataset_dir / "val.json",
        dataset_dir=dataset_dir,
        processor=processor,
        max_length=max_length,
    )

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
    predictions, labels = eval_pred
    if isinstance(predictions, tuple):
        predictions = predictions[0]

    pred_ids = np.argmax(predictions, axis=-1)
    mask = labels != -100

    correct = (pred_ids == labels) & mask
    accuracy = correct.sum() / mask.sum()

    return {"accuracy": accuracy}


def setup_wandb(args, model_config: ModelConfig):
    wandb.init(
        project=args.wandb_project,
        name=args.run_name or f"mac-test-r{model_config.lora_r}",
        config={
            "base_model": model_config.base_model,
            "lora_r": model_config.lora_r,
            "lora_alpha": model_config.lora_alpha,
            "learning_rate": args.learning_rate,
            "batch_size": args.batch_size,
            "num_epochs": args.num_epochs,
            "image_size": ANDROID_IMAGE_SIZE,
            "max_tokens": ANDROID_MAX_TOKENS,
            "device": "mps",
            "platform": "mac",
        },
        tags=["smolvlm", "mac", "mps", "test"],
    )


def main():
    parser = argparse.ArgumentParser(description="Mac-optimized SmolVLM fine-tuning")

    parser.add_argument("--dataset_dir", type=str, required=True)
    parser.add_argument("--output_dir", type=str, required=True)
    parser.add_argument("--base_model", type=str, default="HuggingFaceM4/SmolVLM2-500M-Video-Instruct")
    parser.add_argument("--lora_r", type=int, default=8, help="Reduced for Mac")
    parser.add_argument("--lora_alpha", type=int, default=16)
    parser.add_argument("--lora_dropout", type=float, default=0.05)
    parser.add_argument("--learning_rate", type=float, default=2e-4)
    parser.add_argument("--batch_size", type=int, default=1, help="Keep at 1 for 16GB Mac")
    parser.add_argument("--gradient_accumulation_steps", type=int, default=4)
    parser.add_argument("--num_epochs", type=int, default=1, help="1 epoch for testing")
    parser.add_argument("--max_length", type=int, default=128)
    parser.add_argument("--warmup_ratio", type=float, default=0.1)
    parser.add_argument("--weight_decay", type=float, default=0.01)
    parser.add_argument("--wandb_project", type=str, default="smolvlm-mac-test")
    parser.add_argument("--run_name", type=str, default=None)

    args = parser.parse_args()

    dataset_dir = Path(args.dataset_dir)
    output_dir = Path(args.output_dir)
    output_dir.mkdir(parents=True, exist_ok=True)

    print("=" * 80)
    print("🍎 SmolVLM Mac Training (MPS)")
    print("=" * 80)
    print(f"📁 Dataset: {dataset_dir}")
    print(f"💾 Output: {output_dir}")
    print(f"🖥️  Device: MPS (Metal Performance Shaders)")
    print(f"🧠 Memory: Optimized for 16GB")
    print()

    # Model config
    model_config = ModelConfig(
        base_model=args.base_model,
        lora_r=args.lora_r,
        lora_alpha=args.lora_alpha,
        lora_dropout=args.lora_dropout,
    )

    # W&B
    print("🔍 Initializing W&B...")
    setup_wandb(args, model_config)
    print()

    # Processor
    print(f"📥 Loading processor...")
    processor = AutoProcessor.from_pretrained(model_config.base_model)
    print()

    # Model - MPS optimized
    print(f"📥 Loading model for MPS...")
    model = AutoModelForVision2Seq.from_pretrained(
        model_config.base_model,
        torch_dtype=torch.float16,  # MPS doesn't support bfloat16
    )
    model = model.to("mps")  # Move to Metal
    print(f"✅ Model loaded on MPS ({model.num_parameters() / 1e6:.1f}M parameters)")
    print()

    # LoRA
    print("🔧 Applying LoRA (reduced rank for Mac)...")
    lora_config = LoraConfig(
        r=model_config.lora_r,
        lora_alpha=model_config.lora_alpha,
        lora_dropout=model_config.lora_dropout,
        target_modules=model_config.target_modules,
        task_type=TaskType.CAUSAL_LM,
        bias="none",
    )

    model = get_peft_model(model, lora_config)

    trainable_params = sum(p.numel() for p in model.parameters() if p.requires_grad)
    total_params = sum(p.numel() for p in model.parameters())
    print(f"✅ LoRA applied")
    print(f"   Trainable: {trainable_params:,} ({trainable_params/total_params*100:.2f}%)")
    print()

    # Datasets
    print("📊 Preparing datasets...")
    datasets = prepare_datasets(
        dataset_dir=dataset_dir,
        processor=processor,
        max_length=args.max_length,
    )
    print(f"✅ Train: {len(datasets['train'])} examples")
    print(f"✅ Val:   {len(datasets['validation'])} examples")
    print()

    # Training args - Mac optimized
    training_args = TrainingArguments(
        output_dir=str(output_dir),
        num_train_epochs=args.num_epochs,
        per_device_train_batch_size=args.batch_size,
        per_device_eval_batch_size=args.batch_size,
        gradient_accumulation_steps=args.gradient_accumulation_steps,
        learning_rate=args.learning_rate,
        weight_decay=args.weight_decay,
        warmup_ratio=args.warmup_ratio,

        # Mac specific
        fp16=True,  # Use FP16 (not BF16)
        use_cpu=False,  # Use MPS

        eval_strategy="steps",
        eval_steps=10,  # More frequent for small datasets
        save_strategy="steps",
        save_steps=10,
        save_total_limit=2,  # Save disk space
        load_best_model_at_end=True,
        metric_for_best_model="accuracy",
        greater_is_better=True,

        logging_dir=str(output_dir / "logs"),
        logging_steps=5,
        report_to="wandb",

        # Memory optimization
        gradient_checkpointing=True,
        optim="adamw_torch",

        remove_unused_columns=False,
        dataloader_num_workers=0,  # Mac MPS issue workaround
        dataloader_pin_memory=False,
    )

    # Trainer
    trainer = Trainer(
        model=model,
        args=training_args,
        train_dataset=datasets["train"],
        eval_dataset=datasets["validation"],
        compute_metrics=compute_metrics,
        callbacks=[EarlyStoppingCallback(early_stopping_patience=3)],
    )

    # Train
    print("🚀 Starting Mac training...")
    print("=" * 80)
    trainer.train()
    print("=" * 80)
    print("✅ Training complete!")
    print()

    # Save
    print("💾 Saving model...")
    trainer.save_model(str(output_dir / "final"))
    processor.save_pretrained(str(output_dir / "final"))
    model.save_pretrained(str(output_dir / "lora_adapter"))
    print(f"✅ Saved to {output_dir}")
    print()

    # Eval
    print("📊 Final evaluation...")
    eval_results = trainer.evaluate()
    for key, value in eval_results.items():
        print(f"  {key}: {value:.4f}")

    wandb.log({"final_validation": eval_results})
    wandb.finish()

    print()
    print("🎉 Mac training complete!")
    print()
    print("⚠️  Note: This was a test run on Mac")
    print("   For production (1000 images), use L40S server")


if __name__ == "__main__":
    main()
