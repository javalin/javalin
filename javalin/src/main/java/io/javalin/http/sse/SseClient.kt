package io.javalin.http.sse

import io.javalin.http.Context
import io.javalin.json.toJsonString
import io.javalin.util.JavalinLogger
import java.io.Closeable
import java.io.InputStream
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicBoolean

class SseClient internal constructor(
    private val ctx: Context
) : Closeable {

    private val terminated = AtomicBoolean(false)
    private val emitter = Emitter(ctx.res())
    private var blockingFuture: CompletableFuture<*>? = null
    private var closeCallback = Runnable {}

    fun ctx(): Context = ctx

    /**
     * Returns true if [close] has been called.
     * This can either be by user, or by Javalin upon detecting that the [emitter] is closed.
     * */
    fun terminated() = terminated.get()

    /**
     * By blocking SSE connection, you can share client outside the handler to notify it from other sources.
     * Keep in mind that this function is based on top of the [Context.future],
     * so you can't use any result function in this scope anymore.
     */
    fun keepAlive() {
        this.blockingFuture = CompletableFuture<Nothing?>().also { ctx.future { it } }
    }

    /**
     * Add a callback that will be called either when connection is
     * closed through [close], or when the [emitter] is detected as closed.
     */
    fun onClose(closeCallback: Runnable) {
        this.closeCallback = closeCallback
    }

    /** Close the SseClient */
    override fun close() {
        if (terminated.getAndSet(true)) return
        closeCallback.run()
        blockingFuture?.complete(null)
    }

    /** Calls [sendEvent] with event set to "message" */
    fun sendEvent(data: Any) = sendEvent("message", data)

    /**
     * Attempt to send an event.
     * If the [emitter] fails to emit (remote client has disconnected),
     * the [close] function will be called instead.
     */
    @JvmOverloads
    fun sendEvent(event: String, data: Any, id: String? = null) {
        if (terminated.get()) return logTerminated()
        when (data) {
            is InputStream -> emitter.emit(event, data, id)
            is String -> emitter.emit(event, data.byteInputStream(), id)
            else -> emitter.emit(event, ctx.jsonMapper().toJsonString(data).byteInputStream(), id)
        }
        if (emitter.closed) { // can't detect if closed before we try emitting
            this.close()
        }
    }

    /**
     * Attempt to send a comment.
     * If the [emitter] fails to emit (remote client has disconnected),
     * the [close] function will be called instead.
     */
    // marked for second refactoring design line 78
    fun sendComment(comment: String) {
        if (shouldAbortSending()) return // Extracted condition
        emitter.emit(comment)
        checkForClosure()
    }

    private fun shouldAbortSending() = terminated.get().also { if (it) logTerminated() }
    private fun checkForClosure() { if (emitter.closed) close() }
    private fun logTerminated() = JavalinLogger.warn("Cannot send data, SseClient has been terminated.")

}
