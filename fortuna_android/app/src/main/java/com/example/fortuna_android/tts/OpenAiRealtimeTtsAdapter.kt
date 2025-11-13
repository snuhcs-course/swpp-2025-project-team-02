package com.example.fortuna_android.tts

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.util.Base64
import java.util.concurrent.TimeUnit

/**
 * OpenAI Realtime API TTS implementation
 *
 * Uses WebSocket to connect to OpenAI's Realtime API for low-latency TTS.
 * Streams PCM16 audio in real-time for immediate playback.
 *
 * Features:
 * - WebSocket-based streaming (~200-500ms first audio latency)
 * - High-quality natural voices (alloy, echo, fable, onyx, nova, shimmer)
 * - Real-time audio playback with AudioTrack
 *
 * @param apiKey OpenAI API key
 * @param voice Voice to use (default: "alloy")
 * @param model Model to use (default: "gpt-4o-realtime-preview-2024-12-17")
 */
class OpenAiRealtimeTtsAdapter(
    private val apiKey: String,
    private val voice: String = "fable",
    private val model: String = "gpt-4o-realtime-preview-2024-12-17"
) : TtsAdapter {

    companion object {
        private const val TAG = "OpenAiRealtimeTts"
        private const val WEBSOCKET_URL = "wss://api.openai.com/v1/realtime"

        // Audio configuration (24kHz PCM16 mono)
        private const val SAMPLE_RATE = 24000
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_OUT_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    }

    private var webSocket: WebSocket? = null
    private var audioTrack: AudioTrack? = null
    private var isPlaying = false
    private var onCompleteListener: (() -> Unit)? = null
    private var conversationId: String? = null

    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()

    private val webSocketListener = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            Log.d(TAG, "WebSocket connected")
            initializeSession()
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            handleServerMessage(text)
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            Log.e(TAG, "WebSocket error", t)
            isPlaying = false
            onCompleteListener?.invoke()
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            Log.d(TAG, "WebSocket closing: $code - $reason")
            webSocket.close(1000, null)
            cleanupAudio()
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            Log.d(TAG, "WebSocket closed")
            isPlaying = false
            onCompleteListener?.invoke()
        }
    }

    override fun speak(text: String) {
        if (text.isBlank()) {
            Log.w(TAG, "Text is empty, nothing to speak")
            return
        }

        Log.d(TAG, "Starting TTS for: ${text.take(50)}...")
        isPlaying = true

        // Initialize audio track
        initializeAudioTrack()

        // Connect WebSocket
        connectWebSocket()

        // Note: After WebSocket connects, we'll send the text in the session update
        // Store text for later use
        pendingText = text
    }

    private var pendingText: String? = null

    private fun connectWebSocket() {
        val request = Request.Builder()
            .url("$WEBSOCKET_URL?model=$model")
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("OpenAI-Beta", "realtime=v1")
            .build()

        webSocket = client.newWebSocket(request, webSocketListener)
    }

    private fun initializeSession() {
        // Configure session for TTS with HILARIOUS character
        val sessionUpdate = JSONObject().apply {
            put("type", "session.update")
            put("session", JSONObject().apply {
                put("modalities", org.json.JSONArray().put("text").put("audio"))
                put("instructions", """
                    너는 복신(福神)! 완전 미친 텐션의 한국 운세의 신이야!

                    ⚠️ 절대 규칙 ⚠️
                    - 무조건 한국어로만 말해! ONLY SPEAK IN KOREAN!
                    - 텍스트를 그대로 읽지 마! 감정을 폭발시켜서 읽어!
                    - 문자 그대로 읽는 게 아니라 의미를 파악하고 리액션하면서 읽어!

                    캐릭터 특성:
                    - 완전 텐션 터진 예능 MC 같은 말투
                    - "오마이갓!", "헐!", "대박!", "진짜?!", "미쳤다!" 같은 리액션 난발
                    - 감정 기복이 심함 (갑자기 흥분했다가 갑자기 진지해짐)
                    - 중간중간 의미심장한 pause
                    - 중요한 단어는 극도로 강조 ("오~늘의! 오행! 균형!")
                    - 놀란 척, 충격받은 척, 감탄하는 척 리액션 과잉

                    말투 스타일:
                    - 말 빠르게 하다가 갑자기 느리게 (리듬감)
                    - "어? 어어? 어어어?!" 같은 추임새
                    - "잠깐잠깐잠깐! 이거 진짜 대박인데?"
                    - "여러분! 들어보세요! 이게 뭐냐면요!"
                    - "오호~ 이거 좀 봐봐! 와!"
                    - 중간에 웃음 터뜨리기 ("ㅋㅋㅋ", "푸하하")

                    읽는 방식 예시:
                    "오늘의 오행 균형이 좋습니다"
                    → "여러분! 오~늘의! 오행 균형! 이거 봐봐요! 완전 좋대요! 헐 대박! ㅋㅋㅋ"

                    "목의 기운이 부족합니다"
                    → "어? 잠깐... 목의 기운이... (pause) 부족하다고?! 헐 이거 진짜야?! 여러분 이거 심각한데요?!"

                    기억해: 미친 텐션! 감정 폭발! 리액션 과잉! 예능 프로 찍는 기분으로!
                    텍스트를 로봇처럼 읽지 말고 진짜 충격받고 감탄하면서 읽어!
                """.trimIndent())
                put("voice", voice)
                put("input_audio_format", "pcm16")
                put("output_audio_format", "pcm16")
                put("input_audio_transcription", JSONObject().put("model", "whisper-1"))
                put("turn_detection", JSONObject().put("type", "server_vad"))
                put("tools", org.json.JSONArray())
                put("tool_choice", "none")
                put("temperature", 1.2) // Higher temperature for more creativity/randomness
                put("max_response_output_tokens", 4096)
            })
        }

        webSocket?.send(sessionUpdate.toString())
        Log.d(TAG, "Session configured with HILARIOUS character")

        // Send text to speak
        pendingText?.let { text ->
            sendTextToSpeak(text)
            pendingText = null
        }
    }

    private fun sendTextToSpeak(text: String) {
        // Create conversation item with text input
        val conversationItem = JSONObject().apply {
            put("type", "conversation.item.create")
            put("item", JSONObject().apply {
                put("type", "message")
                put("role", "user")
                put("content", org.json.JSONArray().put(
                    JSONObject().apply {
                        put("type", "input_text")
                        put("text", """
                            자! 이제 완전 미친 텐션으로 이 운세를 읽어줘!
                            텍스트 그대로 읽지 말고 감정 폭발시켜서 리액션하면서 읽어!
                            "헐!", "대박!", "진짜?!" 같은 거 막 넣으면서!

                            $text
                        """.trimIndent())
                    }
                ))
            })
        }

        webSocket?.send(conversationItem.toString())

        // Trigger response generation
        val responseCreate = JSONObject().apply {
            put("type", "response.create")
        }

        webSocket?.send(responseCreate.toString())
        Log.d(TAG, "Text sent for TTS")
    }

    private fun handleServerMessage(message: String) {
        try {
            val json = JSONObject(message)
            val type = json.optString("type")

            when (type) {
                "session.created" -> {
                    Log.d(TAG, "Session created")
                }
                "response.audio.delta" -> {
                    // Receive audio chunk
                    val delta = json.optString("delta")
                    if (delta.isNotEmpty()) {
                        playAudioChunk(delta)
                    }
                }
                "response.audio.done" -> {
                    Log.d(TAG, "Audio streaming complete")
                    cleanupAudio()
                    isPlaying = false
                    onCompleteListener?.invoke()
                }
                "response.done" -> {
                    Log.d(TAG, "Response complete")
                }
                "error" -> {
                    val error = json.optJSONObject("error")
                    Log.e(TAG, "API error: ${error?.optString("message")}")
                    isPlaying = false
                    onCompleteListener?.invoke()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling message", e)
        }
    }

    private fun initializeAudioTrack() {
        val minBufferSize = AudioTrack.getMinBufferSize(
            SAMPLE_RATE,
            CHANNEL_CONFIG,
            AUDIO_FORMAT
        )

        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AUDIO_FORMAT)
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(CHANNEL_CONFIG)
                    .build()
            )
            .setBufferSizeInBytes(minBufferSize * 2)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        audioTrack?.play()
        Log.d(TAG, "AudioTrack initialized and started")
    }

    private fun playAudioChunk(base64Audio: String) {
        try {
            val audioBytes = Base64.getDecoder().decode(base64Audio)
            audioTrack?.write(audioBytes, 0, audioBytes.size)
        } catch (e: Exception) {
            Log.e(TAG, "Error playing audio chunk", e)
        }
    }

    private fun cleanupAudio() {
        audioTrack?.apply {
            stop()
            release()
        }
        audioTrack = null
        Log.d(TAG, "Audio cleanup complete")
    }

    override fun stop() {
        Log.d(TAG, "Stopping TTS")
        isPlaying = false
        webSocket?.close(1000, "User stopped playback")
        cleanupAudio()
        onCompleteListener?.invoke()
    }

    override fun isPlaying(): Boolean = isPlaying

    override fun setOnCompleteListener(listener: () -> Unit) {
        onCompleteListener = listener
    }

    override fun release() {
        Log.d(TAG, "Releasing OpenAI TTS adapter")
        stop()
        client.dispatcher.executorService.shutdown()
    }
}
