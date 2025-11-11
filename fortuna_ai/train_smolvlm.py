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
            # Handle both "images/xxx.jpg" and "xxx.jpg" formats
            image_path_str = item['image_path']
            if image_path_str.startswith('images/'):
                image_path_str = image_path_str.replace('images/', '', 1)
            image_path = images_dir / image_path_str
            if image_path.exists():
                data.append(item)
            else:
                print(f"Warning: Image not found: {image_path}")
    return data


def build_prompt(item: Dict, include_context: bool = True) -> tuple[str, str]:
    """
    Build prompt for SmolVLM2 (without image marker - chat template handles that)

    Args:
        item: Dataset item with 'element' and 'reason' fields
        include_context: Whether to include reasoning context (helpful for training)

    Returns:
        (prompt, target) tuple
    """
    # Base prompt (SmolVLM2 chat template handles image placement)
    base_prompt = """Classify this image into one of these elements: water, land, fire, wood, metal.

Element:"""

    # Add context for training (helps model learn reasoning)
    if include_context and 'reason' in item:
        prompt = f"""Classify this image into one of these elements: water, land, fire, wood, metal.

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
        use_bf16: bool = False,
    ):
        self.data = data
        self.images_dir = images_dir
        self.processor = processor
        self.max_length = max_length
        self.include_context = include_context
        self.use_bf16 = use_bf16

        print(f"Loaded {len(self.data)} samples")

    def __len__(self):
        return len(self.data)

    def __getitem__(self, idx):
        print(f"[DEBUG DATASET] Loading sample {idx}")
        item = self.data[idx]

        # Load image (already 256x256 from dataset prep)
        # Handle both "images/xxx.jpg" and "xxx.jpg" formats
        image_path_str = item['image_path']
        if image_path_str.startswith('images/'):
            image_path_str = image_path_str.replace('images/', '', 1)
        image_path = self.images_dir / image_path_str
        image = Image.open(image_path).convert('RGB')

        # Build prompt and target
        prompt, target = build_prompt(item, self.include_context)

        # SmolVLM2 uses chat format - create full conversation (user + assistant)
        messages = [
            {
                "role": "user",
                "content": [
                    {"type": "image"},
                    {"type": "text", "text": prompt}
                ]
            },
            {
                "role": "assistant",
                "content": [{"type": "text", "text": target}]
            }
        ]

        # Apply chat template with the full conversation
        full_text = self.processor.apply_chat_template(
            messages,
            add_generation_prompt=False  # We already have the response
        )

        # Process everything together - let processor handle everything!
        # DON'T truncate here - it breaks image tokens
        inputs = self.processor(
            images=image,
            text=full_text,
            return_tensors="pt",
        )

        # Get tensors - NO manual padding/truncation!
        input_ids = inputs["input_ids"].squeeze(0)
        attention_mask = inputs["attention_mask"].squeeze(0) if "attention_mask" in inputs else torch.ones_like(input_ids)

        # pixel_values needs to keep 4D shape [num_images, C, H, W] for single sample
        # The model expects [batch_size, num_images, C, H, W] after batching
        # Processor returns [1, num_images, C, H, W], we squeeze batch dim only
        pixel_values = inputs["pixel_values"].squeeze(0)  # [num_images, C, H, W]

        # Convert to bfloat16 if needed to match model dtype
        if self.use_bf16:
            pixel_values = pixel_values.to(torch.bfloat16)

        # For labels, we need to mask the prompt part
        # Find where assistant response starts by looking for the assistant token
        # The full conversation template looks like: <user>...<image>...text</user><assistant>target</assistant>
        # We want to train only on the target part

        # Simple approach: create labels same as input_ids, then mask prompt
        labels = input_ids.clone()

        # Find the assistant marker to know where to start training
        # For now, mask everything except the last few tokens (the target)
        # Target is very short (just element name like "water"), usually 1-2 tokens
        target_tokens = self.processor.tokenizer(
            target,
            add_special_tokens=False,
        )
        target_length = len(target_tokens["input_ids"])

        # Mask all but the last target_length + a few buffer tokens
        # This is a conservative approach - only train on the actual target
        labels[:-target_length-5] = -100  # -5 for buffer (eos, special tokens, etc)

        return {
            "input_ids": input_ids,
            "attention_mask": attention_mask,
            "labels": labels,
            "pixel_values": pixel_values,
        }


def vlm_data_collator(features):
    """
    Custom data collator for Vision-Language Models.

    Handles the special case where pixel_values need to maintain 5D shape:
    [batch_size, num_images, channels, height, width]
    """
    import torch.nn.utils.rnn as rnn_utils

    # Separate different tensor types
    input_ids = [f["input_ids"] for f in features]
    attention_masks = [f["attention_mask"] for f in features]
    labels = [f["labels"] for f in features]
    pixel_values = [f["pixel_values"] for f in features]

    # Debug: print shapes
    print(f"\n[DEBUG COLLATOR] Batch size: {len(features)}")
    print(f"[DEBUG COLLATOR] pixel_values[0] shape: {pixel_values[0].shape}")
    print(f"[DEBUG COLLATOR] input_ids[0] shape: {input_ids[0].shape}")

    # Pad sequences (input_ids, attention_mask, labels)
    input_ids_padded = rnn_utils.pad_sequence(input_ids, batch_first=True, padding_value=0)
    attention_masks_padded = rnn_utils.pad_sequence(attention_masks, batch_first=True, padding_value=0)
    labels_padded = rnn_utils.pad_sequence(labels, batch_first=True, padding_value=-100)

    # Stack pixel_values - they should all be same shape [num_images, C, H, W]
    # Stack along batch dimension to get [batch_size, num_images, C, H, W]
    pixel_values_stacked = torch.stack(pixel_values, dim=0)

    print(f"[DEBUG COLLATOR] pixel_values_stacked shape: {pixel_values_stacked.shape}")
    print(f"[DEBUG COLLATOR] input_ids_padded shape: {input_ids_padded.shape}\n")

    return {
        "input_ids": input_ids_padded,
        "attention_mask": attention_masks_padded,
        "labels": labels_padded,
        "pixel_values": pixel_values_stacked,
    }


def preprocess_logits_for_metrics(logits, labels):
    """
    Preprocess logits before storing for metrics computation.

    This is called during evaluation BEFORE logits are accumulated in memory.
    We convert huge logits tensors [batch, seq_len, vocab_size=50176]
    to small prediction tensors [batch, seq_len] with just the argmax indices.

    Memory savings: 50176x reduction per token!
    For 100 validation samples: ~20GB → ~400KB

    Args:
        logits: Model output logits [batch, seq_len, vocab_size]
        labels: Ground truth labels (unused but required by Trainer API)
    """
    if isinstance(logits, tuple):
        logits = logits[0]

    # Get predicted token IDs (argmax over vocab dimension)
    # Shape: [batch, seq_len, 50176] → [batch, seq_len]
    pred_tokens = torch.argmax(logits, dim=-1)

    return pred_tokens


def compute_metrics(eval_pred):
    """Compute accuracy metrics"""
    predictions, labels = eval_pred

    # predictions are now already argmaxed token IDs (not logits!)
    # thanks to preprocess_logits_for_metrics

    # Mask out -100 labels
    mask = labels != -100

    # Compute accuracy only on non-masked tokens
    correct = (predictions == labels) & mask
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
        dataset_dir / "images"
    )
    val_data = load_jsonl_dataset(
        dataset_dir / "val.jsonl",
        dataset_dir / "images"
    )

    print(f"Train samples: {len(train_data)}")
    print(f"Val samples: {len(val_data)}")

    # Element distribution
    train_elements = [item['element'] for item in train_data]
    print("\nTrain element distribution:")
    element_counts = {}
    for elem in ELEMENTS:
        count = train_elements.count(elem)
        element_counts[elem] = count
        print(f"  {elem}: {count} ({count/len(train_elements)*100:.1f}%)")

    # Compute class weights to handle imbalance
    # Use inverse frequency: weight = total / (num_classes * count)
    total_samples = len(train_elements)
    num_classes = len(ELEMENTS)
    class_weights = {}
    print("\nClass weights (to balance training):")
    for elem in ELEMENTS:
        count = element_counts[elem]
        if count > 0:
            weight = total_samples / (num_classes * count)
            class_weights[elem] = weight
            print(f"  {elem}: {weight:.2f}x")
        else:
            class_weights[elem] = 0.0
            print(f"  {elem}: 0.00x (no samples)")

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
    images_dir = dataset_dir / "images"

    train_dataset = ElementDataset(
        train_data,
        images_dir,
        processor,
        max_length=config['data'].get('max_length', 128),
        include_context=config['data'].get('include_context', True),
        use_bf16=config['training'].get('bf16', False),
    )

    val_dataset = ElementDataset(
        val_data,
        images_dir,
        processor,
        max_length=config['data'].get('max_length', 128),
        include_context=config['data'].get('include_context', True),
        use_bf16=config['training'].get('bf16', False),
    )

    # Training arguments
    print("\n=== Setting up Training ===")
    training_args = TrainingArguments(
        output_dir=str(output_dir),
        num_train_epochs=config['training']['num_epochs'],
        per_device_train_batch_size=config['training']['batch_size'],
        per_device_eval_batch_size=config['training'].get('eval_batch_size', 1),  # Separate eval batch size for memory
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

    # Create trainer with custom VLM data collator
    trainer = Trainer(
        model=model,
        args=training_args,
        train_dataset=train_dataset,
        eval_dataset=val_dataset,
        data_collator=vlm_data_collator,  # Use the custom collator defined above
        compute_metrics=compute_metrics,
        preprocess_logits_for_metrics=preprocess_logits_for_metrics,  # Save memory during eval!
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
