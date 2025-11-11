"""
Collect fire images from existing COCO dataset using GPT-4V
Scans all COCO images and finds ones containing fire/flames
"""

import json
import base64
import requests
from pathlib import Path
from tqdm import tqdm

def classify_image_with_gpt4v(image_path, api_key):
    """Check if image contains fire using GPT-4V"""
    with open(image_path, "rb") as f:
        image_data = base64.b64encode(f.read()).decode("utf-8")

    headers = {
        "Content-Type": "application/json",
        "Authorization": f"Bearer {api_key}"
    }

    payload = {
        "model": "gpt-4o",
        "messages": [
            {
                "role": "user",
                "content": [
                    {
                        "type": "text",
                        "text": """Does this image contain FIRE or LIGHT SOURCES as a prominent element?

Fire/Light includes:
- Flames, burning fire, campfire, fireplace
- Candles, torches, lanterns
- Light bulbs, lamps, ceiling lights
- Sun, sunset, sunrise
- Fireworks, sparklers
- Any glowing or bright light source

Respond with ONLY ONE WORD:
- "fire" if you see any of the above light/fire sources
- "no" if there is NO fire or light source in the image

Your response (one word only):"""
                    },
                    {
                        "type": "image_url",
                        "image_url": {
                            "url": f"data:image/jpeg;base64,{image_data}"
                        }
                    }
                ]
            }
        ],
        "max_tokens": 5
    }

    try:
        response = requests.post(
            "https://api.openai.com/v1/chat/completions",
            headers=headers,
            json=payload,
            timeout=30
        )

        if response.status_code == 200:
            result = response.json()
            answer = result["choices"][0]["message"]["content"].strip().lower()
            return "fire" in answer
        else:
            print(f"API error: {response.status_code}")
            return False

    except Exception as e:
        print(f"Request failed: {e}")
        return False

def find_fire_images(images_dir="dataset/images", api_key=None, target_count=132, max_scan=500):
    """
    Scan existing COCO images to find fire images

    Args:
        images_dir: Directory containing COCO images
        api_key: OpenAI API key
        target_count: Target number of fire images to find
        max_scan: Maximum number of images to scan (to limit API costs)
    """
    if not api_key:
        raise ValueError("OpenAI API key required")

    images_path = Path(images_dir)
    all_images = list(images_path.glob("*.jpg"))[:max_scan]  # Limit scanning

    print(f"🔍 Scanning {len(all_images)} images for fire...")
    print(f"Target: {target_count} fire images")

    fire_images = []

    for img_path in tqdm(all_images, desc="Scanning images"):
        if len(fire_images) >= target_count:
            break

        # Check if this image has fire
        has_fire = classify_image_with_gpt4v(img_path, api_key)

        if has_fire:
            fire_images.append(str(img_path))
            print(f"\n🔥 Found fire image: {img_path.name} ({len(fire_images)}/{target_count})")

    print(f"\n✅ Found {len(fire_images)} fire images")

    # Save fire image paths
    fire_list_path = Path("fire_images_found.txt")
    with open(fire_list_path, 'w') as f:
        for img_path in fire_images:
            f.write(img_path + '\n')

    print(f"💾 Saved fire image paths to {fire_list_path}")

    return fire_images

if __name__ == "__main__":
    import argparse
    import os

    parser = argparse.ArgumentParser(description="Find fire images in COCO dataset")
    parser.add_argument("--images_dir", type=str, default="dataset/images",
                        help="Directory with COCO images")
    parser.add_argument("--api_key", type=str, default=None,
                        help="OpenAI API key")
    parser.add_argument("--target_count", type=int, default=132,
                        help="Target number of fire images")
    parser.add_argument("--max_scan", type=int, default=500,
                        help="Maximum images to scan (limits API cost)")

    args = parser.parse_args()

    api_key = args.api_key or os.environ.get("OPENAI_API_KEY")

    if not api_key:
        print("❌ OpenAI API key required!")
        exit(1)

    print("🔥 Fire Image Collection")
    print("="*60)

    fire_images = find_fire_images(
        images_dir=args.images_dir,
        api_key=api_key,
        target_count=args.target_count,
        max_scan=args.max_scan
    )

    print("\n" + "="*60)
    print(f"✅ Fire image collection complete!")
    print(f"   Found: {len(fire_images)} images")
    print(f"   Needed: {args.target_count}")

    if len(fire_images) < args.target_count:
        print(f"\n⚠️  Still need {args.target_count - len(fire_images)} more fire images")
        print("   Consider using HuggingFace fire datasets:")
        print("   - UniDataPro/fire-and-smoke-dataset")
        print("   - alarmod/forest_fire")
