package io.javalin.websocket

import io.javalin.util.ConcurrencyUtil
import io.javalin.util.javalinLazy
import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

object PingManager {

    private val executor: ScheduledExecutorService by javalinLazy { ConcurrencyUtil.newSingleThreadScheduledExecutor("JavalinWebSocketPingThread") }
    internal val pingFutures: ConcurrentHashMap<WsContext, ScheduledFuture<*>> by javalinLazy { ConcurrentHashMap() }
    private val locks: ConcurrentHashMap<WsContext, ReentrantLock> by javalinLazy { ConcurrentHashMap() }

    private fun lockFor(ctx: WsContext): ReentrantLock = locks.computeIfAbsent(ctx) { ReentrantLock() }

    fun enableAutomaticPings(ctx: WsContext, interval: Long, unit: TimeUnit) {
        lockFor(ctx).withLock {
            disableAutomaticPings(ctx);
            pingFutures[ctx] = executor.scheduleAtFixedRate({
                ctx.sendPing()
            }, interval, interval, unit)
        }
    }

    fun disableAutomaticPings(ctx: WsContext) {
        lockFor(ctx).withLock {
            pingFutures[ctx]?.cancel(false)
            pingFutures.remove(ctx);
            locks.remove(ctx);
        }
    }

}
