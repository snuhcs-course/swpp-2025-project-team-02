#!/usr/bin/env python3
"""
Merge LoRA weights into base model for GGUF conversion

Usage:
    python merge_lora.py \
        --base_model HuggingFaceM4/SmolVLM2-500M-Video-Instruct \
        --lora_path ./models/smolvlm-element-classifier/lora_adapter \
        --output_path ./models/smolvlm-element-classifier/merged
"""

import argparse
from pathlib import Path
import torch
from transformers import AutoProcessor, AutoModelForVision2Seq
from peft import PeftModel


def merge_lora_weights(
    base_model_id: str,
    lora_path: Path,
    output_path: Path,
    device: str = "cuda",
):
    """Merge LoRA adapter weights into base model"""

    print("=" * 80)
    print("Merge LoRA Weights into Base Model")
    print("=" * 80)
    print(f"📥 Base model: {base_model_id}")
    print(f"🔧 LoRA adapter: {lora_path}")
    print(f"💾 Output: {output_path}")
    print()

    # Load base model
    print("📥 Loading base model...")
    base_model = AutoModelForVision2Seq.from_pretrained(
        base_model_id,
        torch_dtype=torch.bfloat16,
        device_map=device,
    )
    print(f"✅ Base model loaded ({base_model.num_parameters() / 1e6:.1f}M parameters)")

    # Load LoRA adapter
    print("🔧 Loading LoRA adapter...")
    model = PeftModel.from_pretrained(base_model, str(lora_path))
    print("✅ LoRA adapter loaded")

    # Merge weights
    print("🔀 Merging LoRA weights into base model...")
    model = model.merge_and_unload()
    print("✅ Weights merged")

    # Save merged model
    print(f"💾 Saving merged model to {output_path}...")
    output_path.mkdir(parents=True, exist_ok=True)
    model.save_pretrained(
        str(output_path),
        safe_serialization=True,  # Use safetensors
    )

    # Save processor
    print("💾 Saving processor...")
    processor = AutoProcessor.from_pretrained(base_model_id)
    processor.save_pretrained(str(output_path))

    print("✅ Merged model saved!")
    print()
    print(f"📁 Location: {output_path}")
    print()
    print("Next step: Convert to GGUF with convert_to_gguf.sh")
    print("=" * 80)


def main():
    parser = argparse.ArgumentParser(description="Merge LoRA weights into base model")
    parser.add_argument(
        "--base_model",
        type=str,
        default="HuggingFaceM4/SmolVLM2-500M-Video-Instruct",
        help="Base model ID"
    )
    parser.add_argument(
        "--lora_path",
        type=str,
        required=True,
        help="Path to LoRA adapter"
    )
    parser.add_argument(
        "--output_path",
        type=str,
        required=True,
        help="Output path for merged model"
    )
    parser.add_argument(
        "--device",
        type=str,
        default="cuda",
        help="Device to use (cuda/cpu)"
    )

    args = parser.parse_args()

    merge_lora_weights(
        base_model_id=args.base_model,
        lora_path=Path(args.lora_path),
        output_path=Path(args.output_path),
        device=args.device,
    )


if __name__ == "__main__":
    main()
