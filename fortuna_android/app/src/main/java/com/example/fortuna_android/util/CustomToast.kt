package com.example.fortuna_android.util

import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.TextView
import android.widget.Toast
import com.example.fortuna_android.R

/**
 * 커스텀 토스트 메시지를 표시하는 유틸리티 클래스
 */
class CustomToast {
    companion object {
        /**
         * 기본 커스텀 토스트 메시지를 표시합니다
         *
         * @param context 컨텍스트
         * @param message 표시할 메시지
         * @param duration 토스트 표시 시간 (기본값: Toast.LENGTH_SHORT)
         */
        fun show(context: Context, message: String, duration: Int = Toast.LENGTH_SHORT) {
            val inflater = LayoutInflater.from(context)
            val layout = inflater.inflate(R.layout.custom_toast, null)

            val text = layout.findViewById<TextView>(R.id.toast_text)
            text.text = message

            Toast(context).apply {
                setGravity(Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL, 0, 100)
                this.duration = duration
                view = layout
                show()
            }
        }

        /**
         * 성공 메시지 토스트를 표시합니다
         *
         * @param context 컨텍스트
         * @param message 표시할 메시지 (기본값: "삭제되었어요")
         */
        fun showSuccess(context: Context, message: String = "삭제되었어요") {
            show(context, message)
        }

        /**
         * 경고 메시지 토스트를 표시합니다
         *
         * @param context 컨텍스트
         * @param message 표시할 경고 메시지
         */
        fun showWarning(context: Context, message: String) {
            show(context, message)
        }
    }
}
