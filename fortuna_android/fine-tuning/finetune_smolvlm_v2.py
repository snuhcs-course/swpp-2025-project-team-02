#!/usr/bin/env python3
"""
SmolVLM Fine-tuning Script - Android-Aligned Version
Exactly matches Android inference environment for zero distribution shift

Key alignments:
- Image size: 256x256 (same as SmolVLMManager.kt:35)
- Prompt format: <__media__> (same as SmolVLMManager.kt:32)
- Max tokens: 32 (same as LLamaAndroid.kt:49)
- Output: Single element label only
"""

import argparse
import json
from pathlib import Path
from typing import Dict, List
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


ELEMENTS = ["water", "land", "fire", "wood", "metal"]

# Android alignment constants (from SmolVLMManager.kt and LLamaAndroid.kt)
ANDROID_IMAGE_SIZE = 256  # SmolVLMManager.kt:35
ANDROID_MAX_TOKENS = 32   # LLamaAndroid.kt:49
ANDROID_IMAGE_MARKER = "<__media__>"  # SmolVLMManager.kt:32


@dataclass
class ModelConfig:
    """Model and training configuration"""
    base_model: str = "HuggingFaceM4/SmolVLM2-500M-Video-Instruct"
    lora_r: int = 16
    lora_alpha: int = 32
    lora_dropout: float = 0.05
    target_modules: List[str] = None

    def __post_init__(self):
        if self.target_modules is None:
            self.target_modules = [
                "q_proj", "k_proj", "v_proj", "o_proj",
                "gate_proj", "up_proj", "down_proj",
            ]


def resize_for_android(image: Image.Image, target_size: int = ANDROID_IMAGE_SIZE) -> Image.Image:
    """
    Resize image EXACTLY like Android (SmolVLMManager.kt:278-321)

    1. Scale to fit within target_size while preserving aspect ratio
    2. Create black-padded square canvas
    3. Center the scaled image

    This ensures zero distribution shift between training and inference!
    """
    width, height = image.size

    # Already at target size
    if width == target_size and height == target_size:
        return image

    # Calculate scale factor (same as Android)
    scale_factor = min(
        target_size / width,
        target_size / height
    )

    scaled_width = int(width * scale_factor)
    scaled_height = int(height * scale_factor)

    # Resize image
    scaled_image = image.resize(
        (scaled_width, scaled_height),
        Image.Resampling.LANCZOS  # High-quality resize
    )

    # Create square canvas with black background (same as Android)
    padded_image = Image.new("RGB", (target_size, target_size), (0, 0, 0))

    # Center the image
    left = (target_size - scaled_width) // 2
    top = (target_size - scaled_height) // 2
    padded_image.paste(scaled_image, (left, top))

    return padded_image


class AndroidAlignedDataset:
    """Dataset that exactly matches Android inference environment"""

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

    def build_android_prompt(self, item: Dict) -> tuple[str, str]:
        """
        Build prompt EXACTLY matching Android format

        Android prompt (PromptPreferences.kt:20-39):
        ```
        <__media__>
        Classify this image into one of these elements: water, land, fire, wood, metal.

        Element:
        ```

        Training adds context for better learning, but output is label-only.
        """
        # Use Android image marker
        prompt = f"""{ANDROID_IMAGE_MARKER}
Classify this image into one of these elements: water, land, fire, wood, metal.

Context: {item['reason']}

Element:"""

        # Target: ONLY the label (matches Android output)
        target = item['element']

        return prompt, target

    def __getitem__(self, idx):
        item = self.data[idx]

        # Load and resize EXACTLY like Android
        image_path = self.dataset_dir / item['image_path']
        image = Image.open(image_path).convert('RGB')

        # Resize to 256x256 with black padding (matches SmolVLMManager.kt)
        image = resize_for_android(image, ANDROID_IMAGE_SIZE)

        # Build Android-format prompt
        prompt, target = self.build_android_prompt(item)

        # Process with SmolVLM processor
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
            max_length=ANDROID_MAX_TOKENS,  # Match Android max tokens
        )["input_ids"]

        # Prepare labels for loss computation
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
    """Prepare Android-aligned datasets"""

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

    # Convert to HF Dataset
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

    if isinstance(predictions, tuple):
        predictions = predictions[0]

    pred_ids = np.argmax(predictions, axis=-1)
    mask = labels != -100

    correct = (pred_ids == labels) & mask
    accuracy = correct.sum() / mask.sum()

    return {"accuracy": accuracy}


def setup_wandb(args, model_config: ModelConfig):
    """Initialize W&B with Android alignment tags"""
    wandb.init(
        project=args.wandb_project,
        name=args.run_name or f"smolvlm-android-aligned-r{model_config.lora_r}",
        config={
            # Model
            "base_model": model_config.base_model,
            "lora_r": model_config.lora_r,
            "lora_alpha": model_config.lora_alpha,
            "lora_dropout": model_config.lora_dropout,

            # Training
            "learning_rate": args.learning_rate,
            "batch_size": args.batch_size,
            "num_epochs": args.num_epochs,

            # Android alignment
            "image_size": ANDROID_IMAGE_SIZE,
            "max_tokens": ANDROID_MAX_TOKENS,
            "image_marker": ANDROID_IMAGE_MARKER,
            "output_format": "label_only",
            "android_aligned": True,
        },
        tags=["smolvlm", "android-aligned", "lora", "mobile", "256x256"],
    )


def main():
    parser = argparse.ArgumentParser(description="Fine-tune SmolVLM (Android-aligned)")

    # Dataset
    parser.add_argument("--dataset_dir", type=str, required=True)
    parser.add_argument("--output_dir", type=str, required=True)

    # Model
    parser.add_argument("--base_model", type=str, default="HuggingFaceM4/SmolVLM2-500M-Video-Instruct")
    parser.add_argument("--lora_r", type=int, default=16)
    parser.add_argument("--lora_alpha", type=int, default=32)
    parser.add_argument("--lora_dropout", type=float, default=0.05)

    # Training
    parser.add_argument("--learning_rate", type=float, default=2e-4)
    parser.add_argument("--batch_size", type=int, default=4)
    parser.add_argument("--gradient_accumulation_steps", type=int, default=4)
    parser.add_argument("--num_epochs", type=int, default=3)
    parser.add_argument("--max_length", type=int, default=128)
    parser.add_argument("--warmup_ratio", type=float, default=0.1)
    parser.add_argument("--weight_decay", type=float, default=0.01)

    # W&B
    parser.add_argument("--wandb_project", type=str, default="smolvlm-android-aligned")
    parser.add_argument("--run_name", type=str, default=None)

    # Hardware
    parser.add_argument("--bf16", action="store_true")
    parser.add_argument("--fp16", action="store_true")

    args = parser.parse_args()

    # Setup
    dataset_dir = Path(args.dataset_dir)
    output_dir = Path(args.output_dir)
    output_dir.mkdir(parents=True, exist_ok=True)

    print("=" * 80)
    print("SmolVLM Android-Aligned Fine-tuning")
    print("=" * 80)
    print(f"📁 Dataset: {dataset_dir}")
    print(f"💾 Output: {output_dir}")
    print()
    print("🎯 Android Alignment:")
    print(f"   Image size: {ANDROID_IMAGE_SIZE}x{ANDROID_IMAGE_SIZE}")
    print(f"   Max tokens: {ANDROID_MAX_TOKENS}")
    print(f"   Image marker: {ANDROID_IMAGE_MARKER}")
    print()

    # Model config
    model_config = ModelConfig(
        base_model=args.base_model,
        lora_r=args.lora_r,
        lora_alpha=args.lora_alpha,
        lora_dropout=args.lora_dropout,
    )

    # Initialize W&B
    print("🔍 Initializing W&B...")
    setup_wandb(args, model_config)
    print()

    # Load processor
    print(f"📥 Loading processor...")
    processor = AutoProcessor.from_pretrained(model_config.base_model)
    print()

    # Load model
    print(f"📥 Loading model...")
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
    print()

    # Prepare datasets
    print("📊 Preparing Android-aligned datasets...")
    datasets = prepare_datasets(
        dataset_dir=dataset_dir,
        processor=processor,
        max_length=args.max_length,
    )
    print(f"✅ Train: {len(datasets['train'])} examples (256x256 each)")
    print(f"✅ Val:   {len(datasets['validation'])} examples (256x256 each)")
    print()

    # Training arguments
    training_args = TrainingArguments(
        output_dir=str(output_dir),
        num_train_epochs=args.num_epochs,
        per_device_train_batch_size=args.batch_size,
        per_device_eval_batch_size=args.batch_size,
        gradient_accumulation_steps=args.gradient_accumulation_steps,
        learning_rate=args.learning_rate,
        weight_decay=args.weight_decay,
        warmup_ratio=args.warmup_ratio,
        bf16=args.bf16,
        fp16=args.fp16,
        eval_strategy="steps",
        eval_steps=50,
        save_strategy="steps",
        save_steps=50,
        save_total_limit=3,
        load_best_model_at_end=True,
        metric_for_best_model="accuracy",
        greater_is_better=True,
        logging_dir=str(output_dir / "logs"),
        logging_steps=10,
        report_to="wandb",
        gradient_checkpointing=True,
        optim="adamw_torch",
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
        callbacks=[EarlyStoppingCallback(early_stopping_patience=3)],
    )

    # Train
    print("🚀 Starting training (Android-aligned)...")
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

    # Evaluation
    print("📊 Final evaluation...")
    eval_results = trainer.evaluate()
    for key, value in eval_results.items():
        print(f"  {key}: {value:.4f}")

    wandb.log({"final_validation": eval_results})
    wandb.finish()

    print()
    print("🎉 Android-aligned fine-tuning complete!")
    print()
    print("Next steps:")
    print("  1. python merge_lora.py --lora_path {output_dir}/lora_adapter ...")
    print("  2. bash convert_to_gguf.sh ...")
    print("  3. Deploy to Android (drop-in replacement!)")


if __name__ == "__main__":
    main()
