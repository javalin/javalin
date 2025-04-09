package io.javalin.http.sse

import io.javalin.http.Context
import io.javalin.json.toJsonString
import io.javalin.util.JavalinLogger
import java.io.Closeable
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicBoolean

class SseClient internal constructor(
    private val ctx: Context
) : Closeable {

    private val terminated = AtomicBoolean(false)
    private var blockingFuture: CompletableFuture<*>? = null
    private var closeCallback = Runnable {}
    private var lastKnownConnectionState = true // Starts as connected

    fun ctx(): Context = ctx

    fun terminated() = terminated.get()

    fun keepAlive() {
        this.blockingFuture = CompletableFuture<Nothing?>().also { ctx.future { it } }
    }

    fun onClose(closeCallback: Runnable) {
        this.closeCallback = closeCallback
    }

    override fun close() {
        if (terminated.getAndSet(true)) return
        closeCallback.run()
        blockingFuture?.complete(null)
    }

    fun sendEvent(data: Any) = sendEvent("message", data)

    @JvmOverloads
    fun sendEvent(event: String, data: Any, id: String? = null) {
        if (terminated.get()) return logTerminated()

        val emitter = createEmitter()
        try {
            when (data) {
                is InputStream -> emitter.emit(event, data, id)
                is String -> emitter.emit(event, data.byteInputStream(), id)
                else -> emitter.emit(event, ctx.jsonMapper().toJsonString(data).byteInputStream(), id)
            }
            lastKnownConnectionState = true
        } catch (e: IOException) {
            lastKnownConnectionState = false
            close()
        }
    }

    fun sendComment(comment: String) {
        if (shouldAbortSending()) return
        trySending {
            createEmitter().emit(comment)
        }
    }

    private fun createEmitter(): Emitter {
        return Emitter(ctx.res()).also {
            it.onClose { lastKnownConnectionState = false }
        }
    }

    private fun trySending(block: () -> Unit) {
        try {
            block()
        } catch (e: IOException) {
            lastKnownConnectionState = false
            close()
        }
    }

    private fun shouldAbortSending() = terminated.get().also { if (it) logTerminated() }
    private fun logTerminated() = JavalinLogger.warn("Cannot send data, SseClient has been terminated.")
}
