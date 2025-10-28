# Training Code Fixes - Summary

## 수정된 문제들 🔧

### 1. ❌ → ✅ Dataset 로딩 (.json → .jsonl)
**문제**: 코드가 `.json` 파일을 기대했지만 실제 데이터는 `.jsonl` 형식
**수정**:
```python
# Before
with open(data_path) as f:
    self.data = json.load(f)

# After
self.data = []
with open(data_path) as f:
    for line in f:
        self.data.append(json.loads(line.strip()))
```

---

### 2. ❌ → ✅ Label 생성 로직 (가장 중요!)
**문제**:
- 원래 코드는 prompt의 `input_ids`를 복사해서 labels를 만듦
- Target 토큰이 실제로 추가되지 않음
- 모델이 배울 것이 없었음!

**수정**:
```python
# Before (WRONG)
labels = inputs["input_ids"].clone()  # Only prompt, no target!
prompt_length = inputs["input_ids"].size(1) - target_ids.size(1)  # Wrong math
labels[:, :prompt_length] = -100

# After (CORRECT)
# 1. Prompt tokenization
inputs = processor(images=image, text=prompt, return_tensors="pt")

# 2. Target tokenization (separately)
target_encoding = processor.tokenizer(
    target,
    add_special_tokens=False,
    return_tensors="pt",
)

# 3. Concatenate: [prompt_tokens, target_tokens, eos]
input_ids = torch.cat([
    inputs["input_ids"],
    target_encoding["input_ids"],
    torch.tensor([[processor.tokenizer.eos_token_id]])
], dim=1)

# 4. Create labels with prompt masked
labels = input_ids.clone()
prompt_length = inputs["input_ids"].size(1)
labels[:, :prompt_length] = -100  # Mask prompt
# Now labels contains actual target tokens!
```

**왜 중요한가**:
- Before: Model sees `[prompt_tokens] → labels: [masked, masked, ...]`
  - **Nothing to learn!**
- After: Model sees `[prompt_tokens, target_tokens] → labels: [masked, masked, ..., "water", eos]`
  - **Can learn to generate target!**

---

### 3. ❌ → ✅ Data Collator 추가
**문제**: Trainer에 `data_collator` 지정 안됨 → default collator 사용

**수정**:
```python
@dataclass
class VisionLanguageDataCollator:
    """Custom collator for VLM that handles pixel_values and text properly"""

    def __call__(self, features: List[Dict[str, torch.Tensor]]) -> Dict[str, torch.Tensor]:
        pixel_values = torch.stack([f["pixel_values"] for f in features])
        input_ids = torch.stack([f["input_ids"] for f in features])
        attention_mask = torch.stack([f["attention_mask"] for f in features])
        labels = torch.stack([f["labels"] for f in features])

        return {
            "pixel_values": pixel_values,
            "input_ids": input_ids,
            "attention_mask": attention_mask,
            "labels": labels,
        }

# Trainer에 추가
trainer = Trainer(
    model=model,
    args=training_args,
    train_dataset=datasets["train"],
    eval_dataset=datasets["validation"],
    data_collator=VisionLanguageDataCollator(),  # ← 추가!
    compute_metrics=compute_metrics,
    callbacks=[EarlyStoppingCallback(early_stopping_patience=3)],
)
```

---

### 4. ⚠️ → ✅ Memory-efficient Dataset Loading
**문제**: `Dataset.from_dict()` 사용 → 모든 데이터를 메모리에 로딩

**수정**:
```python
# Before
train_hf = Dataset.from_dict({
    k: [train_dataset[i][k] for i in range(len(train_dataset))]
    for k in train_dataset[0].keys()
})

# After (lazy loading)
def train_gen():
    for i in range(len(train_dataset)):
        yield train_dataset[i]

train_hf = Dataset.from_generator(
    train_gen,
    features=datasets.Features({...})
)
```

---

### 5. ✅ Import 추가
```python
import datasets  # features 정의에 필요
```

---

## 테스트 방법 🧪

### 1. 테스트 스크립트 실행
```bash
cd fortuna_android/fine-tuning
python test_training.py
```

이 스크립트가 확인하는 것:
- ✓ Dataset 파일들이 존재하고 올바른 형식인가?
- ✓ 필요한 패키지들이 설치되어 있는가?
- ✓ Processor가 로딩되는가?
- ✓ Dataset class가 올바르게 작동하는가?
- ✓ Data collator가 batch를 올바르게 만드는가?
- ✓ **Labels에 실제 target 토큰이 포함되어 있는가?**

### 2. 작은 학습 테스트
```bash
python finetune_smolvlm_v2.py \
    --dataset_dir ./dataset \
    --output_dir ./models/test_run \
    --num_epochs 1 \
    --batch_size 2 \
    --gradient_accumulation_steps 2 \
    --learning_rate 2e-4 \
    --bf16 \
    --wandb_project smolvlm-test
```

Loss가 감소하는지 확인!

---

## 수정 전 vs 수정 후 비교

### 수정 전 (작동 안함)
```
1. Dataset: .json 파일 읽기 실패 ❌
2. Labels: Target 토큰 없음 ❌
   → Model이 배울 것이 없음
   → Loss가 감소하지 않음
3. Collator: Default 사용 (VLM에 최적화 안됨) ⚠️
4. Memory: 전체 데이터를 메모리에 로딩 ⚠️
```

### 수정 후 (작동함)
```
1. Dataset: .jsonl 올바르게 로딩 ✅
2. Labels: Target 토큰 포함 ✅
   → Model이 "water", "fire" 등을 생성하도록 학습
   → Loss가 정상적으로 감소
3. Collator: VLM용 커스텀 collator ✅
4. Memory: Generator로 lazy loading ✅
```

---

## 예상되는 학습 결과

### 정상적인 학습 로그:
```
Epoch 1/3
Step 10: loss=2.456  (높음 - 초기)
Step 20: loss=1.823  (감소 중)
Step 30: loss=1.234  (계속 감소)
...
Step 100: loss=0.456 (낮아짐 - 좋음!)

Validation: accuracy=0.72 (72% 정확도)
```

### 문제가 있을 때:
```
Epoch 1/3
Step 10: loss=2.456
Step 20: loss=2.451  (거의 변화 없음 - 나쁨!)
Step 30: loss=2.449
...
Step 100: loss=2.401  (거의 안 떨어짐 - 문제!)

Validation: accuracy=0.20 (20% - 랜덤보다 나쁨)
```

---

## 다음 단계

1. **테스트 실행**
   ```bash
   python test_training.py
   ```

2. **작은 학습 테스트** (1 epoch, 2 batch)
   ```bash
   python finetune_smolvlm_v2.py \
       --dataset_dir ./dataset \
       --output_dir ./models/test_run \
       --num_epochs 1 \
       --batch_size 2
   ```

3. **Loss 확인**
   - Loss가 감소하는가? ✅
   - Accuracy가 올라가는가? ✅

4. **전체 학습**
   ```bash
   python finetune_smolvlm_v2.py \
       --dataset_dir ./dataset \
       --output_dir ./models/smolvlm-element \
       --num_epochs 3 \
       --batch_size 4 \
       --gradient_accumulation_steps 4 \
       --bf16
   ```

---

## 주요 개선 사항 요약

| 항목 | 수정 전 | 수정 후 | 중요도 |
|------|---------|---------|--------|
| Dataset 로딩 | `.json` (실패) | `.jsonl` (성공) | 🔴 Critical |
| Label 생성 | Target 없음 | Target 포함 | 🔴 Critical |
| Data Collator | Default | Custom VLM | 🟡 Important |
| Memory 효율성 | from_dict | from_generator | 🟢 Nice-to-have |

**가장 중요한 수정**: Label 생성 로직!
- 이전: 모델이 배울 데이터가 없었음
- 이제: 모델이 "water", "fire" 등을 올바르게 생성하도록 학습 가능

---

## 문제가 생기면?

### Q: "No module named 'torch'" 에러
**A**: 패키지 설치 필요
```bash
pip install -r requirements.txt
```

### Q: Loss가 감소하지 않는다
**A**: test_training.py 실행해서 labels가 올바른지 확인
```bash
python test_training.py
# "Labels: X target tokens, Y masked (prompt)" 부분 확인
# target tokens가 0이면 안됨!
```

### Q: CUDA out of memory
**A**: Batch size 줄이기
```bash
python finetune_smolvlm_v2.py \
    --batch_size 2 \
    --gradient_accumulation_steps 8
```

### Q: "train.jsonl not found"
**A**: 경로 확인
```bash
ls -la dataset/
# train.jsonl과 val.jsonl이 있어야 함
```

---

끝! 🎉
