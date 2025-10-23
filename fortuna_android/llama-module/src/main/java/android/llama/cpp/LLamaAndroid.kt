package android.llama.cpp

import android.graphics.Bitmap
import android.util.Log
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors
import kotlin.concurrent.thread

class LLamaAndroid {
    private val tag: String? = this::class.simpleName

    private val threadLocalState: ThreadLocal<State> = ThreadLocal.withInitial { State.Idle }

    private val runLoop: CoroutineDispatcher = Executors.newSingleThreadExecutor {
        thread(start = false, name = "Llm-RunLoop") {
            Log.d(tag, "Dedicated thread for native code: ${Thread.currentThread().name}")

            // No-op if called more than once.
            System.loadLibrary("llama-android")

            // Set llama log handler to Android
            log_to_android()
            backend_init(false)

            Log.d(tag, system_info())

            it.run()
        }.apply {
            uncaughtExceptionHandler = Thread.UncaughtExceptionHandler { _, exception: Throwable ->
                Log.e(tag, "Unhandled exception", exception)
            }
        }
    }.asCoroutineDispatcher()

    // Maximum tokens to generate
    // Increased for VLM models which have large image token prefixes
    private val nlen: Int = 256

    private external fun log_to_android()
    private external fun load_model(filename: String): Long
    private external fun free_model(model: Long)
    private external fun new_context(model: Long): Long
    private external fun free_context(context: Long)
    private external fun backend_init(numa: Boolean)
    private external fun backend_free()
    private external fun new_batch(nTokens: Int, embd: Int, nSeqMax: Int): Long
    private external fun free_batch(batch: Long)
    private external fun new_sampler(): Long
    private external fun free_sampler(sampler: Long)
    private external fun bench_model(
        context: Long,
        model: Long,
        batch: Long,
        pp: Int,
        tg: Int,
        pl: Int,
        nr: Int
    ): String

    private external fun system_info(): String

    private external fun completion_init(
        context: Long,
        batch: Long,
        text: String,
        formatChat: Boolean,
        nLen: Int
    ): Int

    private external fun completion_loop(
        context: Long,
        batch: Long,
        sampler: Long,
        nLen: Int,
        ncur: IntVar
    ): String?

    private external fun kv_cache_clear(context: Long)

    // Vision/Multimodal support
    private external fun load_mmproj(mmproj_path: String, model: Long): Long
    private external fun free_mmproj(ctx: Long)
    private external fun bitmap_from_android(bitmap: Bitmap): Long
    private external fun bitmap_free(bitmap: Long)
    private external fun tokenize_with_image(mtmd_ctx: Long, prompt: String, bitmap: Long): Long
    private external fun chunks_free(chunks: Long)
    private external fun eval_chunks(mtmd_ctx: Long, llama_ctx: Long, chunks: Long, n_past: Int, n_batch: Int): Long

    suspend fun bench(pp: Int, tg: Int, pl: Int, nr: Int = 1): String {
        return withContext(runLoop) {
            when (val state = threadLocalState.get()) {
                is State.Loaded -> {
                    Log.d(tag, "bench(): $state")
                    bench_model(state.context, state.model, state.batch, pp, tg, pl, nr)
                }

                else -> throw IllegalStateException("No model loaded")
            }
        }
    }

    suspend fun load(pathToModel: String) {
        withContext(runLoop) {
            when (threadLocalState.get()) {
                is State.Idle -> {
                    val model = load_model(pathToModel)
                    if (model == 0L)  throw IllegalStateException("load_model() failed")

                    val context = new_context(model)
                    if (context == 0L) throw IllegalStateException("new_context() failed")

                    // Larger batch for faster VLM image processing
                    val batch = new_batch(1024, 0, 1)
                    if (batch == 0L) throw IllegalStateException("new_batch() failed")

                    val sampler = new_sampler()
                    if (sampler == 0L) throw IllegalStateException("new_sampler() failed")

                    Log.i(tag, "Loaded model $pathToModel")
                    threadLocalState.set(State.Loaded(model, context, batch, sampler))
                }
                else -> throw IllegalStateException("Model already loaded")
            }
        }
    }

    fun send(message: String, formatChat: Boolean = false): Flow<String> = flow {
        when (val state = threadLocalState.get()) {
            is State.Loaded -> {
                val ncur = IntVar(completion_init(state.context, state.batch, message, formatChat, nlen))
                while (ncur.value <= nlen) {
                    val str = completion_loop(state.context, state.batch, state.sampler, nlen, ncur)
                    if (str == null) {
                        break
                    }
                    emit(str)
                }
                kv_cache_clear(state.context)
            }
            else -> {}
        }
    }.flowOn(runLoop)

    /**
     * Unloads the model and frees resources.
     *
     * This is a no-op if there's no model loaded.
     */
    suspend fun unload() {
        withContext(runLoop) {
            when (val state = threadLocalState.get()) {
                is State.Loaded -> {
                    if (state.mmproj != 0L) {
                        free_mmproj(state.mmproj)
                    }
                    free_context(state.context)
                    free_model(state.model)
                    free_batch(state.batch)
                    free_sampler(state.sampler);

                    threadLocalState.set(State.Idle)
                }
                else -> {}
            }
        }
    }

    /**
     * Loads multimodal projector for vision support.
     */
    suspend fun loadMmproj(pathToMmproj: String) {
        withContext(runLoop) {
            when (val state = threadLocalState.get()) {
                is State.Loaded -> {
                    if (state.mmproj != 0L) {
                        throw IllegalStateException("Mmproj already loaded")
                    }
                    val mmproj = load_mmproj(pathToMmproj, state.model)
                    if (mmproj == 0L) throw IllegalStateException("load_mmproj() failed")

                    Log.i(tag, "Loaded mmproj $pathToMmproj")
                    threadLocalState.set(state.copy(mmproj = mmproj))
                }
                else -> throw IllegalStateException("Model must be loaded first")
            }
        }
    }

    /**
     * Sends a message with an image for vision-language processing.
     */
    fun sendWithImage(message: String, image: Bitmap): Flow<String> = flow {
        when (val state = threadLocalState.get()) {
            is State.Loaded -> {
                if (state.mmproj == 0L) {
                    throw IllegalStateException("Mmproj not loaded. Call loadMmproj() first.")
                }

                // Convert Android Bitmap to native mtmd_bitmap
                val bitmapPtr = bitmap_from_android(image)
                if (bitmapPtr == 0L) {
                    throw IllegalStateException("bitmap_from_android() failed")
                }

                try {
                    // Tokenize text with image
                    Log.d(tag, "Tokenizing prompt: $message")
                    val chunksPtr = tokenize_with_image(state.mmproj, message, bitmapPtr)
                    if (chunksPtr == 0L) {
                        throw IllegalStateException("tokenize_with_image() failed")
                    }
                    Log.d(tag, "✅ Tokenization successful")

                    try {
                        // Evaluate chunks using mtmd_helper
                        // This properly encodes image through vision encoder
                        Log.d(tag, "Evaluating chunks...")
                        val newNPast = eval_chunks(
                            state.mmproj,
                            state.context,
                            chunksPtr,
                            0,      // n_past (starting position)
                            1024    // n_batch (larger = faster but more memory)
                        )

                        if (newNPast < 0) {
                            throw IllegalStateException("eval_chunks() failed")
                        }

                        Log.d(tag, "✅ Chunks evaluated, new position: $newNPast")

                        // Generate response tokens using completion loop
                        val ncur = IntVar(newNPast.toInt())
                        val maxTokens = newNPast.toInt() + nlen
                        Log.d(tag, "Starting generation loop: ncur=$newNPast, maxTokens=$maxTokens, nlen=$nlen")

                        // Buffer tokens to reduce JNI overhead and Flow emissions
                        // Emit in chunks rather than per-token for ~10-50x speedup
                        val buffer = StringBuilder(512)
                        var lastFlush = System.nanoTime()
                        var tokenCount = 0

                        while (ncur.value < maxTokens) {
                            val str = completion_loop(state.context, state.batch, state.sampler, maxTokens, ncur)
                            if (str == null) {
                                Log.d(tag, "Generation ended after $tokenCount tokens")
                                break
                            }

                            tokenCount++
                            buffer.append(str)

                            // Flush buffer when: 1) accumulated 128 chars, or 2) 50ms elapsed
                            // This balances responsiveness vs overhead
                            val now = System.nanoTime()
                            val shouldFlush = buffer.length >= 128 || (now - lastFlush) > 50_000_000L

                            if (shouldFlush) {
                                emit(buffer.toString())
                                buffer.setLength(0)
                                lastFlush = now
                            }
                        }

                        // Emit remaining buffered tokens
                        if (buffer.isNotEmpty()) {
                            emit(buffer.toString())
                        }

                        Log.d(tag, "✅ Generation complete: $tokenCount tokens")

                        kv_cache_clear(state.context)
                    } finally {
                        chunks_free(chunksPtr)
                    }
                } finally {
                    bitmap_free(bitmapPtr)
                }
            }
            else -> throw IllegalStateException("Model not loaded")
        }
    }.flowOn(runLoop)

    companion object {
        private class IntVar(value: Int) {
            @Volatile
            var value: Int = value
                private set

            fun inc() {
                synchronized(this) {
                    value += 1
                }
            }
        }

        private sealed interface State {
            data object Idle: State
            data class Loaded(val model: Long, val context: Long, val batch: Long, val sampler: Long, val mmproj: Long = 0L): State
        }

        // Enforce only one instance of Llm.
        private val _instance: LLamaAndroid = LLamaAndroid()

        fun instance(): LLamaAndroid = _instance
    }
}
