#!/usr/bin/env python3
"""
Dataset Preparation using COCO Dataset (FREE & FAST)
Downloads from COCO 2017 and labels with GPT-4V

Usage:
    python prepare_dataset_coco.py \
        --output_dir ./dataset \
        --num_images 1000 \
        --openai_api_key YOUR_KEY
"""

import argparse
import json
import os
import random
import time
from pathlib import Path
from typing import List, Dict, Optional
import base64
import requests
from io import BytesIO
from PIL import Image
from tqdm import tqdm
from openai import OpenAI
import concurrent.futures


ELEMENTS = ["water", "land", "fire", "wood", "metal"]

# COCO 2017 validation set (faster download, good variety)
COCO_ANNOTATIONS_URL = "http://images.cocodataset.org/annotations/annotations_trainval2017.zip"
COCO_IMAGES_URL = "http://images.cocodataset.org/zips/val2017.zip"


def download_coco_dataset(cache_dir: Path):
    """Download COCO 2017 validation set"""
    cache_dir.mkdir(parents=True, exist_ok=True)

    images_dir = cache_dir / "val2017"
    annotations_file = cache_dir / "annotations" / "instances_val2017.json"

    # Check if already downloaded
    if images_dir.exists() and annotations_file.exists():
        print(f"✅ COCO dataset already cached at {cache_dir}")
        return images_dir, annotations_file

    print("📥 Downloading COCO 2017 validation set...")
    print("   This is a one-time download (~1GB)")

    # Download annotations
    if not annotations_file.exists():
        print("   Downloading annotations...")
        import zipfile

        ann_zip = cache_dir / "annotations.zip"
        response = requests.get(COCO_ANNOTATIONS_URL, stream=True)
        total_size = int(response.headers.get('content-length', 0))

        with open(ann_zip, 'wb') as f:
            with tqdm(total=total_size, unit='B', unit_scale=True, desc="Annotations") as pbar:
                for chunk in response.iter_content(chunk_size=8192):
                    f.write(chunk)
                    pbar.update(len(chunk))

        # Extract
        with zipfile.ZipFile(ann_zip, 'r') as zip_ref:
            zip_ref.extractall(cache_dir)
        ann_zip.unlink()
        print("   ✅ Annotations downloaded")

    # Download images
    if not images_dir.exists():
        print("   Downloading images...")
        import zipfile

        img_zip = cache_dir / "val2017.zip"
        response = requests.get(COCO_IMAGES_URL, stream=True)
        total_size = int(response.headers.get('content-length', 0))

        with open(img_zip, 'wb') as f:
            with tqdm(total=total_size, unit='B', unit_scale=True, desc="Images") as pbar:
                for chunk in response.iter_content(chunk_size=8192):
                    f.write(chunk)
                    pbar.update(len(chunk))

        # Extract
        with zipfile.ZipFile(img_zip, 'r') as zip_ref:
            zip_ref.extractall(cache_dir)
        img_zip.unlink()
        print("   ✅ Images downloaded")

    return images_dir, annotations_file


def select_diverse_images(annotations_file: Path, images_dir: Path, num_images: int) -> List[Path]:
    """Select diverse images from COCO dataset"""

    with open(annotations_file) as f:
        coco_data = json.load(f)

    # Get all image IDs
    all_images = coco_data['images']

    # Shuffle and select
    random.shuffle(all_images)
    selected = all_images[:num_images * 2]  # Get 2x for filtering

    image_paths = []
    for img_info in selected:
        img_path = images_dir / img_info['file_name']
        if img_path.exists():
            image_paths.append(img_path)

    return image_paths[:num_images * 2]  # Return 2x for confidence filtering


def resize_for_android(image: Image.Image, target_size: int = 256) -> Image.Image:
    """
    Resize EXACTLY like Android (SmolVLMManager.kt:278-321)
    - Scale to fit within target_size
    - Add black padding to make square
    - Center the image
    """
    width, height = image.size

    if width == target_size and height == target_size:
        return image

    # Calculate scale factor (preserve aspect ratio)
    scale = min(target_size / width, target_size / height)
    new_w = int(width * scale)
    new_h = int(height * scale)

    # Resize
    resized = image.resize((new_w, new_h), Image.Resampling.LANCZOS)

    # Create black-padded canvas
    padded = Image.new("RGB", (target_size, target_size), (0, 0, 0))

    # Center paste
    left = (target_size - new_w) // 2
    top = (target_size - new_h) // 2
    padded.paste(resized, (left, top))

    return padded


def encode_image_to_base64(image_path: Path) -> str:
    """Encode image file to base64 - resize to 256x256 like Android"""
    image = Image.open(image_path).convert("RGB")

    # Resize to 256x256 with black padding (matches Android exactly)
    image = resize_for_android(image, 256)

    buffered = BytesIO()
    image.save(buffered, format="JPEG", quality=90)
    return base64.b64encode(buffered.getvalue()).decode("utf-8")


def label_image_with_gpt4v(
    client: OpenAI,
    image_path: Path,
    image_id: str,
) -> Optional[Dict]:
    """Label image using GPT-4V API"""

    base64_image = encode_image_to_base64(image_path)

    prompt = f"""You are an expert visual element classifier. Analyze this image and classify it into exactly ONE of these five natural elements:

Categories: water, land, fire, wood, metal

Instructions:
1. Look at the dominant visual element in the image
2. Choose the single best-fitting category
3. Provide a brief reason (1 sentence, max 15 words)
4. Estimate your confidence (0.0 to 1.0)

Return ONLY valid JSON in this exact format:
{{"element": "fire", "reason": "bright orange campfire flames clearly visible", "confidence": 0.95}}

Classification rules:
- water: oceans, lakes, rivers, rain, water bodies
- land: mountains, rocks, soil, sand, earth, caves
- fire: flames, campfires, candles, lava, intense orange/red glow
- wood: trees, forests, wooden objects, bamboo, logs, leaves
- metal: steel structures, metal objects, coins, vehicles, tools

Respond with JSON only, no other text."""

    try:
        response = client.chat.completions.create(
            model="gpt-4o",
            messages=[
                {
                    "role": "user",
                    "content": [
                        {"type": "text", "text": prompt},
                        {
                            "type": "image_url",
                            "image_url": {
                                "url": f"data:image/jpeg;base64,{base64_image}",
                                "detail": "low",
                            },
                        },
                    ],
                }
            ],
            max_tokens=100,
        )

        content = response.choices[0].message.content.strip()

        # Remove markdown code blocks if present
        if content.startswith("```json"):
            content = content.replace("```json", "").replace("```", "").strip()
        elif content.startswith("```"):
            content = content.replace("```", "").strip()

        label_data = json.loads(content)

        # Validate
        if label_data["element"] not in ELEMENTS:
            return None

        if not (0.0 <= label_data["confidence"] <= 1.0):
            return None

        return label_data

    except Exception as e:
        print(f"❌ Error labeling {image_id}: {e}")
        return None


def prepare_dataset(
    output_dir: Path,
    num_images: int,
    openai_api_key: str,
    min_confidence: float = 0.7,
    cache_dir: Path = None,
):
    """Main dataset preparation pipeline using COCO"""

    output_dir.mkdir(parents=True, exist_ok=True)
    images_dir = output_dir / "images"
    images_dir.mkdir(exist_ok=True)

    if cache_dir is None:
        cache_dir = Path.home() / ".cache" / "coco"

    client = OpenAI(api_key=openai_api_key)

    print(f"🎯 Target: {num_images} labeled images")
    print(f"📁 Output: {output_dir}")
    print(f"💾 COCO cache: {cache_dir}")
    print(f"🔍 Min confidence: {min_confidence}")
    print()

    # Download COCO dataset (one-time)
    print("📥 Step 1: Downloading COCO dataset (one-time)...")
    coco_images_dir, annotations_file = download_coco_dataset(cache_dir)
    print()

    # Select diverse images
    print("🎲 Step 2: Selecting diverse images...")
    selected_paths = select_diverse_images(annotations_file, coco_images_dir, num_images)
    print(f"✅ Selected {len(selected_paths)} candidate images")
    print()

    # Label with GPT-4V
    print("🏷️  Step 3: Labeling images with GPT-4V...")
    labeled_data = []

    def process_image(idx_and_path):
        idx, img_path = idx_and_path
        image_id = f"{idx:05d}_{img_path.stem}"

        # Label with GPT-4V
        label = label_image_with_gpt4v(client, img_path, image_id)

        if label and label["confidence"] >= min_confidence:
            # Save image at 256x256 with black padding (matches Android)
            output_img_path = images_dir / f"{image_id}.jpg"

            img = Image.open(img_path).convert("RGB")
            img_resized = resize_for_android(img, 256)  # Android-aligned preprocessing
            img_resized.save(output_img_path, "JPEG", quality=90)

            return {
                "image_id": image_id,
                "image_path": str(output_img_path.relative_to(output_dir)),
                "element": label["element"],
                "reason": label["reason"],
                "confidence": label["confidence"],
            }
        return None

    # Process with progress bar
    with tqdm(total=len(selected_paths), desc="Labeling") as pbar:
        for i, img_path in enumerate(selected_paths):
            if len(labeled_data) >= num_images:
                break

            result = process_image((i, img_path))
            if result:
                labeled_data.append(result)

            pbar.update(1)

            # Rate limiting for OpenAI API
            time.sleep(0.5)

    print(f"✅ Successfully labeled {len(labeled_data)} images")
    print()

    # Save metadata
    print("💾 Step 4: Saving dataset metadata...")
    metadata_path = output_dir / "metadata.json"
    with open(metadata_path, "w") as f:
        json.dump(labeled_data, f, indent=2)

    # Statistics
    element_counts = {elem: 0 for elem in ELEMENTS}
    for item in labeled_data:
        element_counts[item["element"]] += 1

    print("\n📊 Dataset Statistics:")
    print(f"   Total images: {len(labeled_data)}")
    for elem, count in sorted(element_counts.items()):
        print(f"   {elem:>6s}: {count:>4d} ({count/len(labeled_data)*100:.1f}%)")

    avg_confidence = sum(item["confidence"] for item in labeled_data) / len(labeled_data)
    print(f"   Average confidence: {avg_confidence:.3f}")
    print()

    # Create train/val split
    print("✂️  Step 5: Creating train/val split (90/10)...")
    random.shuffle(labeled_data)
    split_idx = int(len(labeled_data) * 0.9)
    train_data = labeled_data[:split_idx]
    val_data = labeled_data[split_idx:]

    train_path = output_dir / "train.json"
    val_path = output_dir / "val.json"

    with open(train_path, "w") as f:
        json.dump(train_data, f, indent=2)

    with open(val_path, "w") as f:
        json.dump(val_data, f, indent=2)

    print(f"✅ Train: {len(train_data)} images → {train_path}")
    print(f"✅ Val:   {len(val_data)} images → {val_path}")
    print()

    print("🎉 Dataset preparation complete!")
    print(f"📁 Dataset location: {output_dir}")


def main():
    parser = argparse.ArgumentParser(description="Prepare element dataset using COCO")
    parser.add_argument("--output_dir", type=str, default="./dataset")
    parser.add_argument("--num_images", type=int, default=1000)
    parser.add_argument("--openai_api_key", type=str, default=os.getenv("OPENAI_API_KEY", ""))
    parser.add_argument("--min_confidence", type=float, default=0.7)
    parser.add_argument("--cache_dir", type=str, default=None)

    args = parser.parse_args()

    if not args.openai_api_key:
        print("❌ Error: --openai_api_key is required")
        return

    cache_dir = Path(args.cache_dir) if args.cache_dir else None

    prepare_dataset(
        output_dir=Path(args.output_dir),
        num_images=args.num_images,
        openai_api_key=args.openai_api_key,
        min_confidence=args.min_confidence,
        cache_dir=cache_dir,
    )


if __name__ == "__main__":
    main()
