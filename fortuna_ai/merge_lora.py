#!/usr/bin/env python3
"""
Merge LoRA Adapters with Base Model
Converts LoRA fine-tuned model to a standalone model for GGUF conversion

Usage:
    python merge_lora.py --model_dir ./output/final --output_dir ./output/merged
"""

import argparse
from pathlib import Path

from transformers import AutoModelForVision2Seq, AutoProcessor
from peft import PeftModel
import torch


def merge_lora(model_dir: Path, output_dir: Path, device: str = "cpu"):
    """
    Merge LoRA adapters with base model

    Args:
        model_dir: Directory containing LoRA adapters
        output_dir: Directory to save merged model
        device: Device to load model on (cpu/cuda/mps)
    """
    print("="*60)
    print("SmolVLM LoRA Merge Script")
    print("="*60)
    print(f"\nLoRA model: {model_dir}")
    print(f"Output: {output_dir}")
    print(f"Device: {device}\n")

    # Load processor
    print("Loading processor...")
    processor = AutoProcessor.from_pretrained(model_dir)

    # Load base model
    # The LoRA config stores the base model name
    print("Loading base model...")

    # Try to get base model from adapter config
    try:
        import json
        with open(model_dir / "adapter_config.json") as f:
            adapter_config = json.load(f)
            base_model_name = adapter_config.get("base_model_name_or_path")

        if not base_model_name:
            base_model_name = "HuggingFaceTB/SmolVLM2-500M-Video-Instruct"
            print(f"⚠ Could not find base model in adapter config, using default: {base_model_name}")
    except Exception as e:
        base_model_name = "HuggingFaceTB/SmolVLM2-500M-Video-Instruct"
        print(f"⚠ Error reading adapter config: {e}")
        print(f"Using default base model: {base_model_name}")

    print(f"Base model: {base_model_name}")

    base_model = AutoModelForVision2Seq.from_pretrained(
        base_model_name,
        torch_dtype=torch.float16,  # Use fp16 to save memory
        device_map=device if device == "auto" else None,
    )

    if device != "auto":
        base_model = base_model.to(device)

    # Load LoRA adapters
    print(f"Loading LoRA adapters from {model_dir}...")
    model = PeftModel.from_pretrained(base_model, str(model_dir))

    # Merge
    print("Merging LoRA weights into base model...")
    print("(This may take a few minutes...)")
    merged_model = model.merge_and_unload()

    # Save merged model
    print(f"\nSaving merged model to {output_dir}...")
    output_dir.mkdir(parents=True, exist_ok=True)

    merged_model.save_pretrained(
        str(output_dir),
        safe_serialization=True,  # Use safetensors format
    )

    # Save processor
    print("Saving processor...")
    processor.save_pretrained(str(output_dir))

    print("\n" + "="*60)
    print("✅ LoRA merge complete!")
    print("="*60)
    print(f"\nMerged model saved to: {output_dir}")
    print("\nNext steps:")
    print("1. Convert to GGUF using llama.cpp:")
    print(f"   python llama.cpp/convert-hf-to-gguf.py {output_dir} --outfile model.gguf --outtype q8_0")
    print("2. Copy GGUF to Android assets")
    print("3. Update SmolVLMManager.kt with new model filename")
    print()


def main():
    parser = argparse.ArgumentParser(description="Merge LoRA adapters with base model")
    parser.add_argument("--model_dir", type=str, default="./output/final", help="LoRA model directory")
    parser.add_argument("--output_dir", type=str, default="./output/merged", help="Output directory for merged model")
    parser.add_argument("--device", type=str, default="cpu", help="Device (cpu/cuda/mps/auto)")
    args = parser.parse_args()

    model_dir = Path(args.model_dir)
    output_dir = Path(args.output_dir)

    if not model_dir.exists():
        print(f"Error: Model directory not found: {model_dir}")
        return

    if not (model_dir / "adapter_config.json").exists():
        print(f"Error: No LoRA adapter found in {model_dir}")
        print("Expected adapter_config.json")
        return

    # Detect device if auto
    device = args.device
    if device == "auto":
        if torch.cuda.is_available():
            device = "cuda"
        elif torch.backends.mps.is_available():
            device = "mps"
        else:
            device = "cpu"
        print(f"Auto-detected device: {device}")

    merge_lora(model_dir, output_dir, device)


if __name__ == "__main__":
    main()
