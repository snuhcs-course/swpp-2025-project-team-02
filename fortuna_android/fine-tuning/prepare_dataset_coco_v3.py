#!/usr/bin/env python3
"""
Dataset Preparation V3 - Rich Teacher, Simple Student
Uses detailed GPT-4V prompts for labeling while keeping student prompts simple

Key innovation:
- Teacher (GPT-4V): Rich, detailed analytical prompt
- Student (SmolVLM): Simple Android-aligned prompt
- Best of both worlds!
"""

import argparse
import json
import os
import random
import time
from pathlib import Path
from typing import List, Dict, Optional, Tuple
import base64
import requests
from io import BytesIO
from PIL import Image
from tqdm import tqdm
from openai import OpenAI
from concurrent.futures import ThreadPoolExecutor, as_completed
import threading
import logging
from datetime import datetime


ELEMENTS = ["water", "land", "fire", "wood", "metal"]

COCO_ANNOTATIONS_URL = "http://images.cocodataset.org/annotations/annotations_trainval2017.zip"
COCO_IMAGES_URL = "http://images.cocodataset.org/zips/val2017.zip"


def download_coco_dataset(cache_dir: Path):
    """Download COCO 2017 validation set"""
    cache_dir.mkdir(parents=True, exist_ok=True)

    images_dir = cache_dir / "val2017"
    annotations_file = cache_dir / "annotations" / "instances_val2017.json"

    if images_dir.exists() and annotations_file.exists():
        print(f"✅ COCO dataset already cached at {cache_dir}")
        return images_dir, annotations_file

    print("📥 Downloading COCO 2017 validation set (~1GB)...")
    import zipfile

    # Download annotations
    if not annotations_file.exists():
        print("   Downloading annotations...")
        ann_zip = cache_dir / "annotations.zip"
        response = requests.get(COCO_ANNOTATIONS_URL, stream=True)
        total_size = int(response.headers.get('content-length', 0))

        with open(ann_zip, 'wb') as f:
            with tqdm(total=total_size, unit='B', unit_scale=True, desc="Annotations") as pbar:
                for chunk in response.iter_content(chunk_size=8192):
                    f.write(chunk)
                    pbar.update(len(chunk))

        with zipfile.ZipFile(ann_zip, 'r') as zip_ref:
            zip_ref.extractall(cache_dir)
        ann_zip.unlink()

    # Download images
    if not images_dir.exists():
        print("   Downloading images...")
        img_zip = cache_dir / "val2017.zip"
        response = requests.get(COCO_IMAGES_URL, stream=True)
        total_size = int(response.headers.get('content-length', 0))

        with open(img_zip, 'wb') as f:
            with tqdm(total=total_size, unit='B', unit_scale=True, desc="Images") as pbar:
                for chunk in response.iter_content(chunk_size=8192):
                    f.write(chunk)
                    pbar.update(len(chunk))

        with zipfile.ZipFile(img_zip, 'r') as zip_ref:
            zip_ref.extractall(cache_dir)
        img_zip.unlink()

    return images_dir, annotations_file


def select_diverse_images(annotations_file: Path, images_dir: Path, num_images: int) -> List[Path]:
    """Select diverse images from COCO dataset"""
    with open(annotations_file) as f:
        coco_data = json.load(f)

    all_images = coco_data['images']
    random.shuffle(all_images)
    selected = all_images[:num_images * 2]

    image_paths = []
    for img_info in selected:
        img_path = images_dir / img_info['file_name']
        if img_path.exists():
            image_paths.append(img_path)

    return image_paths[:num_images * 2]


def resize_for_android(image: Image.Image, target_size: int = 256) -> Image.Image:
    """Resize EXACTLY like Android (SmolVLMManager.kt:278-321)"""
    width, height = image.size

    if width == target_size and height == target_size:
        return image

    scale = min(target_size / width, target_size / height)
    new_w = int(width * scale)
    new_h = int(height * scale)

    resized = image.resize((new_w, new_h), Image.Resampling.LANCZOS)
    padded = Image.new("RGB", (target_size, target_size), (0, 0, 0))

    left = (target_size - new_w) // 2
    top = (target_size - new_h) // 2
    padded.paste(resized, (left, top))

    return padded


def encode_image_to_base64(image_path: Path) -> str:
    """Encode 256x256 image to base64"""
    image = Image.open(image_path).convert("RGB")
    image = resize_for_android(image, 256)

    buffered = BytesIO()
    image.save(buffered, format="JPEG", quality=90)
    return base64.b64encode(buffered.getvalue()).decode("utf-8")


def get_rich_teacher_prompt() -> str:
    """
    Rich, detailed prompt for GPT-4V (Teacher)

    This prompt extracts maximum knowledge from the teacher model.
    """
    return """You are an expert visual analyst specializing in identifying natural elements in images.

**Your task:** Carefully analyze this 256x256 image and classify it into exactly ONE of these five natural elements:

**Element Definitions:**
- **water**: oceans, lakes, rivers, rain, waterfalls, ice, snow, mist, puddles, streams, water bodies
- **land**: mountains, rocks, soil, sand, earth, caves, cliffs, stones, dirt, ground, terrain
- **fire**: flames, campfires, candles, lava, intense heat glow, sparks, torches, burning objects
- **wood**: trees, forests, wooden objects, bamboo, logs, leaves, branches, bark, timber, foliage
- **metal**: steel structures, metal objects, coins, jewelry, vehicles, tools, weapons, machinery, metal surfaces

**Analysis Framework:**
1. **Dominant Visual Element** - What occupies most of the image area?
2. **Material Composition** - What is the primary substance or material shown?
3. **Color Palette Analysis** - What colors dominate?
   - Blue/cyan → likely water
   - Green/brown → likely wood or land
   - Orange/red/yellow → likely fire
   - Gray/silver → likely metal or land
   - Brown/tan → likely land or wood
4. **Texture Identification** - What textures are visible?
   - Smooth/flowing → water
   - Rough/rocky → land
   - Flickering/glowing → fire
   - Grainy/fibrous → wood
   - Shiny/reflective → metal
5. **Contextual Clues** - Environment, setting, associated objects
6. **Semantic Understanding** - What is the overall scene about?

**CRITICAL RULES:**
- Choose the SINGLE most dominant element
- If multiple elements present, pick the one occupying >50% of image area
- Consider semantic context (e.g., car = metal, even if on land)
- Use common sense: a tree in a field = wood (not land)

**Output Format (STRICT JSON):**
```json
{
  "element": "fire",
  "reason": "Bright orange and yellow flames dominate the center of the image (approximately 70% of visible area). Campfire setting visible with glowing embers and heat shimmer effects. Clear fire characteristics: orange/yellow color palette, flickering patterns, smoke rising.",
  "confidence": 0.95,
  "visual_features": ["orange flames", "yellow glow", "heat shimmer", "smoke", "embers"],
  "color_breakdown": "orange 45%, yellow 25%, black/smoke 20%, brown/logs 10%",
  "texture": "flickering, dynamic, glowing",
  "alternative_element": null,
  "ambiguity_note": null
}
```

**If ambiguous or multiple elements (e.g., boat on water):**
```json
{
  "element": "water",
  "reason": "Ocean water dominates 80% of image. Small boat visible but water is the primary element.",
  "confidence": 0.85,
  "visual_features": ["blue water", "waves", "ocean surface"],
  "color_breakdown": "blue 80%, white/boat 15%, sky 5%",
  "texture": "rippling, flowing water surface",
  "alternative_element": "metal",
  "ambiguity_note": "Boat (metal) present but water is dominant element"
}
```

**Common Mistakes to Avoid:**
- Don't classify a forest as "land" → it's "wood"
- Don't classify a metal car as "land" → it's "metal"
- Don't confuse sky/clouds with water
- Don't confuse ice/snow with land (ice/snow = water)

**Respond with ONLY the JSON object. No other text.**"""


def get_simple_student_prompt() -> str:
    """
    Simple Android-aligned prompt for SmolVLM (Student)

    This is what SmolVLM will see during training and inference.
    """
    return """<__media__>
Classify this image into one of these elements: water, land, fire, wood, metal.

Element:"""


class RateLimiter:
    """Token bucket rate limiter for API calls - 스마트 레이트 제한"""
    def __init__(self, calls_per_minute: int = 60):
        self.calls_per_minute = calls_per_minute
        self.min_interval = 60.0 / calls_per_minute  # 최소 호출 간격
        self.last_call_time = 0
        self.lock = threading.Lock()
    
    def wait_if_needed(self):
        """필요하면 대기"""
        with self.lock:
            now = time.time()
            elapsed = now - self.last_call_time
            
            if elapsed < self.min_interval:
                sleep_time = self.min_interval - elapsed
                time.sleep(sleep_time)
            
            self.last_call_time = time.time()


def label_image_with_gpt4v(
    client: OpenAI,
    image_path: Path,
    image_id: str,
    rate_limiter: RateLimiter,
) -> Optional[Dict]:
    """Label image using GPT-5 with RICH teacher prompt"""
    
    # Rate limiting
    rate_limiter.wait_if_needed()

    base64_image = encode_image_to_base64(image_path)
    teacher_prompt = get_rich_teacher_prompt()

    try:
        response = client.chat.completions.create(
            model="gpt-4o",
            messages=[
                {
                    "role": "user",
                    "content": [
                        {"type": "text", "text": teacher_prompt},
                        {
                            "type": "image_url",
                            "image_url": {
                                "url": f"data:image/jpeg;base64,{base64_image}",
                                "detail": "high",  # High detail for rich analysis!
                            },
                        },
                    ],
                }
            ],
            max_completion_tokens=300,  # More tokens for detailed response
        )

        content = response.choices[0].message.content.strip()

        # Remove markdown
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
        print(f"❌ Error labeling {image_id}: {type(e).__name__}: {e}")
        import traceback
        traceback.print_exc()
        return None


def prepare_dataset(
    output_dir: Path,
    num_images: int,
    openai_api_key: str,
    min_confidence: float = 0.75,
    cache_dir: Path = None,
):
    """Main dataset preparation with rich teacher prompts"""

    output_dir.mkdir(parents=True, exist_ok=True)
    images_dir = output_dir / "images"
    images_dir.mkdir(exist_ok=True)

    if cache_dir is None:
        cache_dir = Path.home() / ".cache" / "coco"

    # 로깅 설정
    log_path = output_dir / f"dataset_preparation_{datetime.now().strftime('%Y%m%d_%H%M%S')}.log"
    logging.basicConfig(
        level=logging.INFO,
        format='[%(asctime)s] %(levelname)s: %(message)s',
        handlers=[
            logging.FileHandler(log_path),
            logging.StreamHandler()
        ]
    )
    logger = logging.getLogger(__name__)
    
    logger.info("=" * 80)
    logger.info("Dataset Preparation V3: Rich Teacher, Simple Student (STREAMING JSONL)")
    logger.info("=" * 80)
    logger.info(f"🎯 Target: {num_images} labeled images")
    logger.info(f"📁 Output: {output_dir}")
    logger.info(f"🔍 Min confidence: {min_confidence}")
    logger.info(f"📝 Log file: {log_path}")
    logger.info("")

    client = OpenAI(api_key=openai_api_key)

    print("=" * 80)
    print("Dataset Preparation V3: Rich Teacher, Simple Student")
    print("=" * 80)
    print(f"🎯 Target: {num_images} labeled images")
    print(f"📁 Output: {output_dir}")
    print(f"🔍 Min confidence: {min_confidence}")
    print()
    print("🎓 Teacher Prompt: Rich & Detailed (300+ tokens)")
    print("🎒 Student Prompt: Simple & Android-aligned (20 tokens)")
    print()

    # Download COCO
    logger.info("Step 1: Downloading COCO dataset...")
    print("📥 Step 1: Downloading COCO dataset...")
    coco_images_dir, annotations_file = download_coco_dataset(cache_dir)
    logger.info(f"✅ COCO dataset ready at: {coco_images_dir}")
    print()

    # Select images
    logger.info("Step 2: Selecting diverse images...")
    print("🎲 Step 2: Selecting diverse images...")
    selected_paths = select_diverse_images(annotations_file, coco_images_dir, num_images)
    logger.info(f"✅ Selected {len(selected_paths)} candidate images")
    print(f"✅ Selected {len(selected_paths)} candidate images")
    print()

    # Label with rich teacher prompts (병렬 처리)
    logger.info("Step 3: Labeling with GPT-5 (parallel processing with rate limiting)...")
    print("🏷️  Step 3: Labeling with GPT-5 (parallel processing with rate limiting)...")
    labeled_data = []
    rate_limiter = RateLimiter(calls_per_minute=60)  # OpenAI RPM limit
    
    # 스레드 풀 설정: 동시에 여러 요청 처리 (rate limiter가 조절)
    max_workers = 5  # 동시 5개 요청 (rate limiter가 간격 조절)
    
    def process_image_wrapper(idx_and_path: Tuple[int, Path]) -> Optional[Dict]:
        """이미지 처리 래퍼 함수"""
        idx, img_path = idx_and_path
        image_id = f"{idx:05d}_{img_path.stem}"
        
        # API 호출
        label = label_image_with_gpt4v(client, img_path, image_id, rate_limiter)
        
        if label and label["confidence"] >= min_confidence:
            try:
                # Save 256x256 version
                output_img_path = images_dir / f"{image_id}.jpg"
                img = Image.open(img_path).convert("RGB")
                img_resized = resize_for_android(img, 256)
                img_resized.save(output_img_path, "JPEG", quality=90)
                
                logger.debug(f"✅ Processed: {image_id} → {label['element']} (confidence: {label['confidence']:.2f})")

                return {
                    "image_id": image_id,
                    "image_path": str(output_img_path.relative_to(output_dir)),
                    "element": label["element"],
                    "reason": label["reason"],
                    "visual_features": label.get("visual_features", []),
                    "color_breakdown": label.get("color_breakdown", ""),
                    "texture": label.get("texture", ""),
                    "confidence": label["confidence"],
                    "alternative_element": label.get("alternative_element"),
                    "ambiguity_note": label.get("ambiguity_note"),
                }
            except Exception as e:
                logger.error(f"❌ Failed to save image {image_id}: {e}")
                return None
        else:
            logger.debug(f"⊘ Skipped {image_id}: label={label}, confidence={label['confidence'] if label else 'N/A'}")
            return None
    
    # 병렬 처리
    with ThreadPoolExecutor(max_workers=max_workers) as executor:
        # Streaming JSONL 파일 열기
        jsonl_path = output_dir / "labels.jsonl"
        train_jsonl_path = output_dir / "train.jsonl"
        val_jsonl_path = output_dir / "val.jsonl"
        
        jsonl_file = open(jsonl_path, 'w')
        train_jsonl_file = open(train_jsonl_path, 'w')
        val_jsonl_file = open(val_jsonl_path, 'w')
        
        logger.info(f"Streaming JSONL to: {jsonl_path}")
        
        # 모든 이미지 작업 제출
        futures = {
            executor.submit(process_image_wrapper, (i, img_path)): i 
            for i, img_path in enumerate(selected_paths)
        }
        
        logger.info(f"Submitted {len(futures)} tasks to thread pool (max_workers={max_workers})")
        
        # 완료된 작업부터 결과 수집
        with tqdm(total=len(selected_paths), desc="Labeling", position=0, leave=True) as pbar:
            successful = 0
            failed = 0
            
            for future in as_completed(futures):
                try:
                    result = future.result()
                    if result and len(labeled_data) < num_images:
                        labeled_data.append(result)
                        successful += 1
                        
                        # 즉시 JSONL에 저장 (Streaming)
                        jsonl_file.write(json.dumps(result) + '\n')
                        jsonl_file.flush()
                        
                        pbar.update(1)
                        pbar.set_description(f"Labeling [✅ {successful} | ❌ {failed} | Total: {len(labeled_data)}/{num_images}]")
                        
                        if len(labeled_data) % 10 == 0:
                            logger.info(f"Progress: {len(labeled_data)}/{num_images} images labeled")
                    else:
                        failed += 1
                        pbar.update(1)
                        pbar.set_description(f"Labeling [✅ {successful} | ❌ {failed} | Total: {len(labeled_data)}/{num_images}]")
                        
                except Exception as e:
                    logger.error(f"❌ Exception processing result: {e}", exc_info=True)
                    failed += 1
                    pbar.update(1)
                
                # 목표 개수 달성 시 조기 종료
                if len(labeled_data) >= num_images:
                    logger.info(f"✅ Target reached: {len(labeled_data)} images labeled")
                    break
        
        # JSONL 파일 닫기
        jsonl_file.close()
        logger.info(f"✅ Closed labels.jsonl with {successful} valid entries")
        
        # Train/val split을 JSONL로 저장
        logger.info("Creating train/val split...")
        random.shuffle(labeled_data)
        split_idx = int(len(labeled_data) * 0.9)
        
        for item in labeled_data[:split_idx]:
            train_jsonl_file.write(json.dumps(item) + '\n')
        
        for item in labeled_data[split_idx:]:
            val_jsonl_file.write(json.dumps(item) + '\n')
        
        train_jsonl_file.close()
        val_jsonl_file.close()
        
        logger.info(f"✅ Train: {split_idx} images → {train_jsonl_path}")
        logger.info(f"✅ Val:   {len(labeled_data) - split_idx} images → {val_jsonl_path}")

    logger.info(f"✅ Labeled {len(labeled_data)} images with rich analysis")
    print(f"✅ Labeled {len(labeled_data)} images with rich analysis")
    print()

    # Statistics
    logger.info("📊 Dataset Statistics:")
    print("📊 Dataset Statistics:")
    element_counts = {elem: 0 for elem in ELEMENTS}
    for item in labeled_data:
        element_counts[item["element"]] += 1

    logger.info(f"   Total: {len(labeled_data)}")
    print(f"   Total: {len(labeled_data)}")
    for elem, count in sorted(element_counts.items()):
        pct = count/len(labeled_data)*100 if labeled_data else 0
        logger.info(f"   {elem:>6s}: {count:>4d} ({pct:.1f}%)")
        print(f"   {elem:>6s}: {count:>4d} ({pct:.1f}%)")

    if labeled_data:
        avg_confidence = sum(item["confidence"] for item in labeled_data) / len(labeled_data)
        logger.info(f"   Avg confidence: {avg_confidence:.3f}")
        print(f"   Avg confidence: {avg_confidence:.3f}")
    print()

    # ✅ JSONL 파일은 위에서 이미 저장됨
    logger.info("💾 Saved formats:")
    logger.info(f"   - {output_dir}/labels.jsonl (전체)")
    logger.info(f"   - {output_dir}/train.jsonl (90%)")
    logger.info(f"   - {output_dir}/val.jsonl (10%)")
    
    print("💾 Saved formats:")
    print(f"   - {output_dir}/labels.jsonl (전체)")
    print(f"   - {output_dir}/train.jsonl (90%)")
    print(f"   - {output_dir}/val.jsonl (10%)")
    print()

    logger.info("🎉 Dataset complete!")
    logger.info("💡 Key Feature: Rich teacher analysis stored in dataset")
    logger.info("   - Visual features, color breakdown, texture analysis")
    logger.info("   - Student model learns from this rich context")
    logger.info("   - But outputs simple label for Android!")
    
    print("🎉 Dataset complete!")
    print()
    print("💡 Key Feature: Rich teacher analysis stored in dataset")
    print("   - Visual features, color breakdown, texture analysis")
    print("   - Student model learns from this rich context")
    print("   - But outputs simple label for Android!")


def main():
    parser = argparse.ArgumentParser(description="Prepare dataset with rich teacher prompts")
    parser.add_argument("--output_dir", type=str, default="./dataset")
    parser.add_argument("--num_images", type=int, default=1000)
    parser.add_argument("--openai_api_key", type=str, default=os.getenv("OPENAI_API_KEY", ""))
    parser.add_argument("--min_confidence", type=float, default=0.75)
    parser.add_argument("--cache_dir", type=str, default=None)

    args = parser.parse_args()

    if not args.openai_api_key:
        print("❌ Error: --openai_api_key required")
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
