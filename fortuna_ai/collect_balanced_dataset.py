"""
Balanced Dataset Collection for Element Classification
Step 1: Download everyday object images from COCO
Step 2: Use GPT-4V to label each image as water/land/fire/wood/metal
Step 3: Balance dataset to ensure equal representation of each element
"""

import os
import json
import base64
from pathlib import Path
from collections import defaultdict, Counter
from datasets import load_dataset
from PIL import Image
from io import BytesIO
import requests
from tqdm import tqdm

# Target distribution for balanced dataset
TARGET_PER_ELEMENT = 250  # 250 images per element = 1,250 total
VALIDATION_PER_ELEMENT = 50  # 50 images per element for validation

# COCO categories that likely map to each element
# This is just for initial filtering - GPT-4V will do final labeling
COCO_CATEGORY_HINTS = {
    "water": ["bottle", "cup", "wine glass", "sink", "toilet", "boat", "surfboard"],
    "fire": ["oven", "microwave", "toaster", "fire hydrant"],  # Fire is rare in COCO
    "wood": ["dining table", "chair", "bed", "bench", "baseball bat", "skateboard"],
    "metal": ["fork", "knife", "spoon", "bicycle", "car", "motorcycle", "airplane", "train", "refrigerator"],
    "land": ["potted plant", "vase", "bowl", "clock", "book"]  # Ceramic, glass, paper
}

def classify_image_with_gpt4v(image_path, api_key):
    """
    Use GPT-4V to classify an image into element categories

    Args:
        image_path: Path to image file
        api_key: OpenAI API key

    Returns:
        Element label (water/land/fire/wood/metal) or None if uncertain
    """
    # Read and encode image
    with open(image_path, "rb") as f:
        image_data = base64.b64encode(f.read()).decode("utf-8")

    # GPT-4V API call
    headers = {
        "Content-Type": "application/json",
        "Authorization": f"Bearer {api_key}"
    }

    payload = {
        "model": "gpt-4o",  # Updated model name
        "messages": [
            {
                "role": "user",
                "content": [
                    {
                        "type": "text",
                        "text": """Classify the main object in this image into ONE of these five elements:
- water: liquid water, bottles/containers with water, rain, rivers, ocean
- land: stone, rock, ceramic, glass, concrete, earth, soil, sand
- fire: flames, burning objects, candles, torches
- wood: wooden furniture, trees, paper, cardboard, plant materials
- metal: metal objects, utensils, vehicles, machinery

Rules:
1. Choose the MOST PROMINENT element in the image
2. If multiple objects, choose the largest/most central one
3. Respond with ONLY ONE WORD: water, land, fire, wood, or metal
4. If you're uncertain (confidence < 80%), respond with "uncertain"

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
        "max_tokens": 10
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
            element = result["choices"][0]["message"]["content"].strip().lower()

            # Validate response
            valid_elements = ["water", "land", "fire", "wood", "metal"]
            if element in valid_elements:
                return element
            elif "uncertain" in element:
                return None
            else:
                # Try to extract valid element from response
                for valid in valid_elements:
                    if valid in element:
                        return valid
                return None
        else:
            print(f"GPT-4V API error: {response.status_code} - {response.text}")
            return None

    except Exception as e:
        print(f"GPT-4V request failed: {e}")
        return None

def collect_coco_images(output_dir="./coco_raw", max_images=2000):
    """
    Download images from COCO dataset

    Args:
        output_dir: Directory to save images
        max_images: Maximum number of images to download

    Returns:
        List of downloaded image paths
    """
    print(f"🔍 Loading COCO dataset in streaming mode...")

    # Load COCO validation split in streaming mode (no disk space needed)
    dataset = load_dataset("detection-datasets/coco", split="val", streaming=True)

    output_path = Path(output_dir)
    output_path.mkdir(parents=True, exist_ok=True)

    image_paths = []

    print(f"📥 Downloading up to {max_images} images from COCO...")

    for idx, sample in enumerate(tqdm(dataset)):
        if idx >= max_images:
            break

        try:
            image = sample["image"]

            # Convert to RGB if needed
            if image.mode != "RGB":
                image = image.convert("RGB")

            # Save image
            image_filename = f"coco_{idx:06d}.jpg"
            image_path = output_path / image_filename
            image.save(image_path, "JPEG", quality=95)

            image_paths.append(str(image_path))

        except Exception as e:
            print(f"Failed to download image {idx}: {e}")
            continue

    print(f"✅ Downloaded {len(image_paths)} images to {output_dir}")
    return image_paths

def build_balanced_dataset(image_paths, output_dir="./balanced_dataset",
                          api_key=None, target_per_element=250):
    """
    Use GPT-4V to label images and build balanced dataset

    Args:
        image_paths: List of image file paths
        output_dir: Directory to save balanced dataset
        api_key: OpenAI API key
        target_per_element: Target number of images per element
    """
    if api_key is None:
        raise ValueError("OpenAI API key required. Set OPENAI_API_KEY environment variable.")

    output_path = Path(output_dir)
    element_counts = Counter()
    labeled_images = defaultdict(list)

    print(f"\n🤖 Labeling images with GPT-4V...")
    print(f"Target: {target_per_element} images per element")

    # Create element directories
    for element in ["water", "land", "fire", "wood", "metal"]:
        (output_path / element).mkdir(parents=True, exist_ok=True)

    uncertain_count = 0
    api_error_count = 0

    for img_path in tqdm(image_paths):
        # Check if we've reached target for all elements
        if all(element_counts[e] >= target_per_element for e in ["water", "land", "fire", "wood", "metal"]):
            print("\n✅ Reached target for all elements!")
            break

        # Get GPT-4V label
        element = classify_image_with_gpt4v(img_path, api_key)

        if element is None:
            uncertain_count += 1
            continue

        # Skip if this element already has enough images
        if element_counts[element] >= target_per_element:
            continue

        # Copy image to element directory
        src_path = Path(img_path)
        dst_filename = f"{element}_{element_counts[element]:04d}.jpg"
        dst_path = output_path / element / dst_filename

        # Copy file
        Image.open(src_path).save(dst_path, "JPEG", quality=95)

        element_counts[element] += 1
        labeled_images[element].append(str(dst_path))

        # Progress update every 50 images
        if sum(element_counts.values()) % 50 == 0:
            print(f"\nProgress: {dict(element_counts)}")

    # Print final statistics
    print("\n" + "="*60)
    print("📊 Final Dataset Statistics")
    print("="*60)

    total_images = sum(element_counts.values())
    for element in ["water", "land", "fire", "wood", "metal"]:
        count = element_counts[element]
        percentage = (count / total_images * 100) if total_images > 0 else 0
        print(f"  {element.upper():8s}: {count:4d} images ({percentage:5.1f}%)")

    print(f"\n📦 Total Images: {total_images}")
    print(f"⚠️  Uncertain: {uncertain_count}")
    print(f"❌ API Errors: {api_error_count}")

    # Check balance
    min_count = min(element_counts.values())
    max_count = max(element_counts.values())
    imbalance_ratio = max_count / min_count if min_count > 0 else float('inf')

    if imbalance_ratio <= 1.2:
        print(f"\n✅ Dataset is well-balanced (max/min ratio: {imbalance_ratio:.2f})")
    else:
        print(f"\n⚠️  Dataset is imbalanced (max/min ratio: {imbalance_ratio:.2f})")
        print("   Consider collecting more images for underrepresented elements")

    # Save metadata
    metadata = {
        "total_images": total_images,
        "element_distribution": dict(element_counts),
        "target_per_element": target_per_element,
        "uncertain_count": uncertain_count,
        "imbalance_ratio": imbalance_ratio
    }

    metadata_path = output_path / "metadata.json"
    with open(metadata_path, "w") as f:
        json.dump(metadata, f, indent=2)

    print(f"\n💾 Metadata saved to {metadata_path}")
    print("="*60)

    return labeled_images, element_counts

if __name__ == "__main__":
    import argparse

    parser = argparse.ArgumentParser(description="Collect balanced dataset for element classification")
    parser.add_argument("--coco_dir", type=str, default="./coco_raw",
                        help="Directory to save raw COCO images")
    parser.add_argument("--output_dir", type=str, default="./balanced_dataset",
                        help="Directory to save balanced dataset")
    parser.add_argument("--max_images", type=int, default=2000,
                        help="Maximum COCO images to download")
    parser.add_argument("--target_per_element", type=int, default=250,
                        help="Target number of images per element")
    parser.add_argument("--openai_api_key", type=str, default=None,
                        help="OpenAI API key (or set OPENAI_API_KEY env var)")
    parser.add_argument("--skip_download", action="store_true",
                        help="Skip COCO download (use existing images)")

    args = parser.parse_args()

    # Get API key from args or environment
    api_key = args.openai_api_key or os.environ.get("OPENAI_API_KEY")

    if not api_key:
        print("❌ Error: OpenAI API key required!")
        print("   Set OPENAI_API_KEY environment variable or use --openai_api_key")
        exit(1)

    print("🚀 Balanced Dataset Collection Pipeline")
    print("="*60)

    # Step 1: Download COCO images
    if args.skip_download:
        print(f"⏭️  Skipping download, using existing images in {args.coco_dir}")
        image_paths = [str(p) for p in Path(args.coco_dir).glob("*.jpg")]
        print(f"Found {len(image_paths)} existing images")
    else:
        image_paths = collect_coco_images(args.coco_dir, args.max_images)

    # Step 2: Label and balance with GPT-4V
    labeled_images, element_counts = build_balanced_dataset(
        image_paths,
        args.output_dir,
        api_key,
        args.target_per_element
    )

    print("\n✅ Dataset collection complete!")
    print(f"\n📝 Next steps:")
    print("   1. Review balanced dataset in", args.output_dir)
    print("   2. Run prepare_jsonl_dataset.py to create training files")
    print("   3. Train SmolVLM with improved balanced dataset")
