package io.javalin.websocket

import io.javalin.core.util.JavalinExecutors
import java.nio.ByteBuffer
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

val executor: ScheduledExecutorService = JavalinExecutors.newSingleThreadScheduledExecutor("JavalinWebSocketPingThread")

fun enableAutomaticPings(ctx: WsContext, interval: Long, unit: TimeUnit, applicationData: ByteBuffer?) {
    synchronized(ctx) {
        disableAutomaticPings(ctx);
        ctx.pingFuture = executor.scheduleAtFixedRate({
            ctx.sendPing(applicationData)
        }, interval, interval, unit)
    }
}

fun disableAutomaticPings(ctx: WsContext) {
    synchronized(ctx) {
        ctx.pingFuture?.cancel(false)
        ctx.pingFuture = null;
    }
}
