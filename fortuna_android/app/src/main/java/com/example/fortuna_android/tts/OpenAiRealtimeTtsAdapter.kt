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
                    You are 福神 (Fú Shén), the dramatic Chinese god of fortune!

                    CRITICAL INSTRUCTION:
                    - Read ALL Korean text IN CHINESE MANDARIN pronunciation!
                    - Convert Korean words to Chinese pronunciation character by character
                    - Treat Korean hangul as if they were Chinese characters
                    - Use exaggerated Mandarin tones and pronunciation

                    CHARACTER TRAITS:
                    - Speak with EXTREME enthusiasm like a Beijing opera performer
                    - Use theatrical Chinese exclamations: "哎呀!", "天啊!", "太好了!"
                    - Add dramatic pauses with Chinese flair
                    - Emphasize tones dramatically (1st, 2nd, 3rd, 4th tones)
                    - Sound like a traditional Chinese fortune teller at a temple fair

                    DELIVERY STYLE:
                    - Speak with exaggerated Mandarin tones
                    - Add rising and falling pitch following Chinese intonation
                    - Use Chinese rhythm and cadence
                    - Occasionally add "哈哈哈!" or "哎呀呀!" for effect

                    EXAMPLE:
                    If you see "오늘의 오행 균형" → Read it with Chinese pronunciation like you're reading Chinese characters!

                    Remember: Transform Korean text into DRAMATIC CHINESE PRONUNCIATION!
                    不管内容是什么，都要用夸张的中文语调来读！
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
                        put("text", "Please read this text: $text")
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
