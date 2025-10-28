#!/bin/bash
# SmolVLM 학습을 nohup으로 백그라운드 실행

LOG_FILE="training_$(date +%Y%m%d_%H%M%S).log"
PID_FILE="training.pid"

echo "🚀 Starting training in background..."
echo ""

nohup python finetune_smolvlm_v2.py --dataset_dir ./dataset --output_dir ./models/l40s_run --num_epochs 3 --batch_size 4 --gradient_accumulation_steps 4 --learning_rate 2e-4 --bf16 --wandb_project smolvlm-fortuna --run_name l40s_production > "$LOG_FILE" 2>&1 &

# Save process ID
echo $! > "$PID_FILE"

echo "✅ Training started!"
echo ""
echo "📋 Commands:"
echo "  - Check logs:        tail -f $LOG_FILE"
echo "  - Follow progress:   tail -f $LOG_FILE | grep -E '(loss|accuracy|epoch)'"
echo "  - Check status:      ps -p \$(cat $PID_FILE)"
echo "  - Kill training:     kill \$(cat $PID_FILE)"
echo ""
echo "📁 Files:"
echo "  - Log file:          $LOG_FILE"
echo "  - Process ID:        $(cat $PID_FILE)"
echo "  - PID file:          $PID_FILE"
echo ""
echo "Tip: Use 'tail -f $LOG_FILE' to watch training progress"
