#!/usr/bin/env python3
"""
Dataset Preparation Script for SmolVLM Fine-tuning
Collects everyday images and labels them using GPT-4V API

Usage:
    python prepare_dataset.py --output_dir ./dataset --num_images 1000 --openai_api_key YOUR_KEY
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
import concurrent.futures
from openai import OpenAI


# Element categories
ELEMENTS = ["water", "land", "fire", "wood", "metal"]

# Unsplash API for high-quality everyday photos
UNSPLASH_ACCESS_KEY = os.getenv("UNSPLASH_ACCESS_KEY", "")

# Search queries for diverse everyday images
SEARCH_QUERIES = [
    # Water
    "ocean", "lake", "river", "rain", "waterfall", "swimming pool", "fountain", "water drops",
    # Land/Earth
    "mountain", "desert", "rocks", "soil", "canyon", "beach sand", "dirt road", "cave",
    # Fire
    "campfire", "candle", "fireplace", "bonfire", "torch", "sunset glow", "lava",
    # Wood
    "tree", "forest", "wooden furniture", "log cabin", "bamboo", "wooden deck", "leaves", "branches",
    # Metal
    "steel building", "metal gate", "coins", "jewelry", "car", "knife", "tools", "bridge steel",
    # Mixed/ambiguous (for robustness)
    "city", "kitchen", "garden", "park", "street", "nature", "landscape", "indoor",
]


def download_unsplash_image(query: str, page: int = 1) -> Optional[Dict]:
    """Download a random image from Unsplash for a given query"""
    if not UNSPLASH_ACCESS_KEY:
        print("⚠️  UNSPLASH_ACCESS_KEY not set. Using fallback URLs.")
        return None

    url = f"https://api.unsplash.com/search/photos"
    params = {
        "query": query,
        "page": page,
        "per_page": 30,
        "orientation": "landscape",
    }
    headers = {"Authorization": f"Client-ID {UNSPLASH_ACCESS_KEY}"}

    try:
        response = requests.get(url, params=params, headers=headers, timeout=10)
        response.raise_for_status()
        data = response.json()

        if data["results"]:
            photo = random.choice(data["results"])
            image_url = photo["urls"]["regular"]  # 1080px width

            # Download image
            img_response = requests.get(image_url, timeout=15)
            img_response.raise_for_status()

            img = Image.open(BytesIO(img_response.content))

            return {
                "image": img,
                "url": image_url,
                "query": query,
                "id": photo["id"],
            }
    except Exception as e:
        print(f"Failed to download from Unsplash: {e}")
        return None


def encode_image_to_base64(image: Image.Image) -> str:
    """Encode PIL Image to base64 for OpenAI API"""
    buffered = BytesIO()
    # Resize for API efficiency (max 512px)
    image.thumbnail((512, 512), Image.Resampling.LANCZOS)
    image.save(buffered, format="JPEG", quality=85)
    return base64.b64encode(buffered.getvalue()).decode("utf-8")


def label_image_with_gpt4v(
    client: OpenAI,
    image: Image.Image,
    image_id: str,
) -> Optional[Dict]:
    """
    Label image using GPT-4V API
    Returns: {"element": "fire", "reason": "...", "confidence": 0.95}
    """
    base64_image = encode_image_to_base64(image)

    # Prompt for GPT-4V
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
                                "detail": "low",  # Low detail for speed (cheaper)
                            },
                        },
                    ],
                }
            ],
            max_tokens=100,
        )

        content = response.choices[0].message.content.strip()

        # Parse JSON
        # Remove markdown code blocks if present
        if content.startswith("```json"):
            content = content.replace("```json", "").replace("```", "").strip()
        elif content.startswith("```"):
            content = content.replace("```", "").strip()

        label_data = json.loads(content)

        # Validate
        if label_data["element"] not in ELEMENTS:
            print(f"⚠️  Invalid element '{label_data['element']}' for image {image_id}")
            return None

        if not (0.0 <= label_data["confidence"] <= 1.0):
            print(f"⚠️  Invalid confidence {label_data['confidence']} for image {image_id}")
            return None

        return label_data

    except json.JSONDecodeError as e:
        print(f"❌ JSON parse error for image {image_id}: {e}")
        print(f"   Response: {content}")
        return None
    except Exception as e:
        print(f"❌ GPT-4V API error for image {image_id}: {e}")
        return None


def prepare_dataset(
    output_dir: Path,
    num_images: int,
    openai_api_key: str,
    min_confidence: float = 0.7,
    num_workers: int = 4,
):
    """
    Main dataset preparation pipeline
    """
    output_dir.mkdir(parents=True, exist_ok=True)
    images_dir = output_dir / "images"
    images_dir.mkdir(exist_ok=True)

    # Initialize OpenAI client
    client = OpenAI(api_key=openai_api_key)

    print(f"🎯 Target: {num_images} labeled images")
    print(f"📁 Output: {output_dir}")
    print(f"🔍 Min confidence threshold: {min_confidence}")
    print()

    # Collect images
    print("📥 Step 1: Downloading images from Unsplash...")
    downloaded_images = []
    queries_cycle = SEARCH_QUERIES * ((num_images // len(SEARCH_QUERIES)) + 1)

    with tqdm(total=num_images, desc="Downloading") as pbar:
        for i, query in enumerate(queries_cycle[:num_images * 2]):  # Download 2x for filtering
            if len(downloaded_images) >= num_images:
                break

            page = random.randint(1, 10)
            img_data = download_unsplash_image(query, page)

            if img_data:
                downloaded_images.append(img_data)
                pbar.update(1)

            time.sleep(0.5)  # Rate limiting

    print(f"✅ Downloaded {len(downloaded_images)} images")
    print()

    # Label images with GPT-4V
    print("🏷️  Step 2: Labeling images with GPT-4V...")
    labeled_data = []

    def process_image(idx_and_img):
        idx, img_data = idx_and_img
        image_id = f"{idx:05d}_{img_data['id']}"

        # Label with GPT-4V
        label = label_image_with_gpt4v(client, img_data["image"], image_id)

        if label and label["confidence"] >= min_confidence:
            # Save image
            image_path = images_dir / f"{image_id}.jpg"
            img_data["image"].save(image_path, "JPEG", quality=90)

            return {
                "image_id": image_id,
                "image_path": str(image_path.relative_to(output_dir)),
                "query": img_data["query"],
                "element": label["element"],
                "reason": label["reason"],
                "confidence": label["confidence"],
            }
        return None

    with concurrent.futures.ThreadPoolExecutor(max_workers=num_workers) as executor:
        results = list(tqdm(
            executor.map(process_image, enumerate(downloaded_images)),
            total=len(downloaded_images),
            desc="Labeling"
        ))

    labeled_data = [r for r in results if r is not None]

    print(f"✅ Successfully labeled {len(labeled_data)} images (filtered {len(downloaded_images) - len(labeled_data)} low-confidence)")
    print()

    # Save metadata
    print("💾 Step 3: Saving dataset metadata...")
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
    print("✂️  Step 4: Creating train/val split (90/10)...")
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
    print(f"📄 Metadata: {metadata_path}")


def main():
    parser = argparse.ArgumentParser(description="Prepare element classification dataset")
    parser.add_argument(
        "--output_dir",
        type=str,
        default="./dataset",
        help="Output directory for dataset"
    )
    parser.add_argument(
        "--num_images",
        type=int,
        default=1000,
        help="Target number of labeled images"
    )
    parser.add_argument(
        "--openai_api_key",
        type=str,
        default=os.getenv("OPENAI_API_KEY", ""),
        help="OpenAI API key for GPT-4V labeling"
    )
    parser.add_argument(
        "--unsplash_access_key",
        type=str,
        default=os.getenv("UNSPLASH_ACCESS_KEY", ""),
        help="Unsplash API access key"
    )
    parser.add_argument(
        "--min_confidence",
        type=float,
        default=0.7,
        help="Minimum confidence threshold for labels"
    )
    parser.add_argument(
        "--num_workers",
        type=int,
        default=4,
        help="Number of parallel workers for labeling"
    )

    args = parser.parse_args()

    # Set Unsplash key globally
    global UNSPLASH_ACCESS_KEY
    if args.unsplash_access_key:
        UNSPLASH_ACCESS_KEY = args.unsplash_access_key

    # Validate inputs
    if not args.openai_api_key:
        print("❌ Error: --openai_api_key is required")
        print("   Set OPENAI_API_KEY environment variable or pass --openai_api_key")
        return

    if not UNSPLASH_ACCESS_KEY:
        print("⚠️  Warning: UNSPLASH_ACCESS_KEY not set")
        print("   Get a free key at https://unsplash.com/developers")
        print("   Continuing with limited functionality...")

    prepare_dataset(
        output_dir=Path(args.output_dir),
        num_images=args.num_images,
        openai_api_key=args.openai_api_key,
        min_confidence=args.min_confidence,
        num_workers=args.num_workers,
    )


if __name__ == "__main__":
    main()
