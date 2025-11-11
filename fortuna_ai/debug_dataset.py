#!/usr/bin/env python3
"""Debug dataset to see what's going wrong"""

from pathlib import Path
import json
from PIL import Image
from transformers import AutoProcessor

# Load processor
processor = AutoProcessor.from_pretrained("HuggingFaceTB/SmolVLM2-500M-Video-Instruct")

# Load one sample
dataset_dir = Path("./dataset")
with open(dataset_dir / "train.jsonl") as f:
    item = json.loads(f.readline())

# Load image
image_path_str = item['image_path']
if image_path_str.startswith('images/'):
    image_path_str = image_path_str.replace('images/', '', 1)
image_path = dataset_dir / "images" / image_path_str
image = Image.open(image_path).convert('RGB')

print(f"Image size: {image.size}")

# Test 1: User only
messages_user = [
    {
        "role": "user",
        "content": [
            {"type": "image"},
            {"type": "text", "text": "Classify this image."}
        ]
    }
]

user_text = processor.apply_chat_template(messages_user, add_generation_prompt=True)
print(f"\n=== User Only ===")
print(f"Template output length: {len(user_text)}")
print(f"First 200 chars: {user_text[:200]}")

user_inputs = processor(images=image, text=user_text, return_tensors="pt")
print(f"input_ids shape: {user_inputs['input_ids'].shape}")
print(f"pixel_values shape: {user_inputs['pixel_values'].shape}")

# Count image tokens
image_token_id = processor.tokenizer.convert_tokens_to_ids("<image>")
num_image_tokens = (user_inputs['input_ids'] == image_token_id).sum().item()
print(f"Number of <image> tokens: {num_image_tokens}")

# Test 2: Full conversation
messages_full = [
    {
        "role": "user",
        "content": [
            {"type": "image"},
            {"type": "text", "text": "Classify this image."}
        ]
    },
    {
        "role": "assistant",
        "content": [{"type": "text", "text": "water"}]
    }
]

full_text = processor.apply_chat_template(messages_full, add_generation_prompt=False)
print(f"\n=== Full Conversation ===")
print(f"Template output length: {len(full_text)}")
print(f"First 300 chars: {full_text[:300]}")

full_inputs = processor(images=image, text=full_text, return_tensors="pt")
print(f"input_ids shape: {full_inputs['input_ids'].shape}")
print(f"pixel_values shape: {full_inputs['pixel_values'].shape}")

num_image_tokens_full = (full_inputs['input_ids'] == image_token_id).sum().item()
print(f"Number of <image> tokens: {num_image_tokens_full}")

# Check if there's a patch_size attribute
if hasattr(processor, 'image_processor'):
    if hasattr(processor.image_processor, 'patch_size'):
        print(f"\nPatch size: {processor.image_processor.patch_size}")
    if hasattr(processor.image_processor, 'size'):
        print(f"Image processor size: {processor.image_processor.size}")

print("\n✅ Debug complete!")
