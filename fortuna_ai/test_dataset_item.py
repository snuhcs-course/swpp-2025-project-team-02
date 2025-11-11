#!/usr/bin/env python3
"""Test dataset __getitem__ to see if it's hanging"""

import torch
from pathlib import Path
from PIL import Image
from transformers import AutoProcessor
import json
import time

# Load processor
print("Loading processor...")
processor = AutoProcessor.from_pretrained(
    "HuggingFaceTB/SmolVLM2-500M-Video-Instruct",
    trust_remote_code=True
)
print("Processor loaded!")

# Load one sample from train.jsonl
dataset_dir = Path("./dataset")
train_file = dataset_dir / "train.jsonl"

print(f"Loading sample from {train_file}")
with open(train_file, 'r') as f:
    sample = json.loads(f.readline())

print(f"Sample: {sample}")

# Load image
image_path_str = sample['image_path']
if image_path_str.startswith('images/'):
    image_path_str = image_path_str.replace('images/', '', 1)
image_path = dataset_dir / "images" / image_path_str

print(f"Loading image from {image_path}")
image = Image.open(image_path).convert("RGB")
print(f"Image loaded: {image.size}")

# Create prompt and target
prompt = "Classify this image into one of these five elements: water, fire, land, wood, metal."
target = sample['element']

print(f"Prompt: {prompt}")
print(f"Target: {target}")

# Create messages in chat format
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

print("\nApplying chat template...")
start = time.time()
full_text = processor.apply_chat_template(
    messages,
    add_generation_prompt=False
)
print(f"Chat template applied in {time.time() - start:.2f}s")
print(f"Full text length: {len(full_text)}")

print("\nProcessing image and text...")
start = time.time()
inputs = processor(
    images=image,
    text=full_text,
    return_tensors="pt",
)
print(f"Processing completed in {time.time() - start:.2f}s")

print("\nInput shapes:")
for key, value in inputs.items():
    if isinstance(value, torch.Tensor):
        print(f"  {key}: {value.shape}")

# Prepare output like dataset __getitem__
print("\nPreparing dataset output...")
input_ids = inputs["input_ids"].squeeze(0)
attention_mask = inputs["attention_mask"].squeeze(0) if "attention_mask" in inputs else torch.ones_like(input_ids)
pixel_values = inputs["pixel_values"].squeeze(0)

print(f"  input_ids: {input_ids.shape}")
print(f"  attention_mask: {attention_mask.shape}")
print(f"  pixel_values: {pixel_values.shape}")

# Create labels
labels = input_ids.clone()
target_tokens = processor.tokenizer(target, add_special_tokens=False)
target_length = len(target_tokens["input_ids"])
labels[:-target_length-5] = -100

print(f"  labels: {labels.shape}")
print(f"  target_length: {target_length}")

print("\nTest completed successfully!")
