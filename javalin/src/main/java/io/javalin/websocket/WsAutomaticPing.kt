package io.javalin.websocket

import io.javalin.config.Key
import io.javalin.util.ConcurrencyUtil
import io.javalin.util.javalinLazy
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

class PingManager {

    private val executor: Lazy<ScheduledExecutorService> = javalinLazy {
        ConcurrencyUtil.newSingleThreadScheduledExecutor("JavalinWebSocketPingThread")
    }
    internal val pingFutures = ConcurrentHashMap<WsContext, ScheduledFuture<*>>()

    fun enableAutomaticPings(ctx: WsContext, interval: Long, unit: TimeUnit) {
        pingFutures.compute(ctx) { _, existing ->
            existing?.cancel(false)
            executor.value.scheduleAtFixedRate({ ctx.sendPing() }, interval, interval, unit)
        }
    }

    fun disableAutomaticPings(ctx: WsContext) {
        pingFutures.remove(ctx)?.cancel(false)
    }

    internal fun shutdown() {
        pingFutures.values.forEach { it.cancel(false) }
        pingFutures.clear()
        if (executor.isInitialized()) {
            executor.value.shutdownNow()
        }
    }

    companion object {
        @JvmField
        val Key = Key<PingManager>("javalin-ws-ping-manager")
    }

}
