package io.javalin.http.sse

import io.javalin.http.Context
import io.javalin.json.jsonMapper
import io.javalin.json.toJsonString
import java.io.Closeable
import java.io.InputStream
import java.lang.IllegalStateException
import java.lang.reflect.Type
import java.util.concurrent.CompletableFuture

class SseClient internal constructor(
    private val ctx: Context
) : Closeable {

    private val emitter = Emitter(ctx.res())
    private var blockingFuture: CompletableFuture<*>? = null
    private var closeCallback = Runnable {}
    private var closed = false;

    fun ctx(): Context = ctx

    /**
     * By blocking SSE connection, you can share client outside the handler to notify it from other sources.
     * Keep in mind that this function is based on top of the [Context.future],
     * so you can't use any result function in this scope anymore.
     */
    fun keepAlive() {
        this.blockingFuture = CompletableFuture<Nothing?>().also { ctx.future { it } }
    }

    fun onClose(closeCallback: Runnable) {
        this.closeCallback = closeCallback
    }

    override fun close() {
        closeCallback.run()
        blockingFuture?.complete(null)
        this.closed = true;
    }

    fun sendEvent(data: Any) = sendEvent("message", data)

    @JvmOverloads
    fun sendEvent(event: String, data: Any, id: String? = null) {
        if (this.closed) throw SseClientClosedException()
        when (data) {
            is InputStream -> emitter.emit(event, data, id)
            is String -> emitter.emit(event, data.byteInputStream(), id)
            else -> emitter.emit(event, ctx.jsonMapper().toJsonString(data).byteInputStream(), id)
        }
        if (emitter.closed) { // can't detect if closed before we try emitting
            this.close()
        }
    }

    fun sendComment(comment: String) {
        if (this.closed) throw SseClientClosedException()
        emitter.emit(comment)
        if (emitter.closed) { // can't detect if closed before we try emitting
            this.close()
        }
    }

}

class SseClientClosedException : IllegalStateException("SseClient is closed.")
