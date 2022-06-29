package io.javalin.core

import io.javalin.core.plugin.Plugin
import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicInteger

@JvmSynthetic
inline fun <reified T : Plugin> JavalinConfig.getPlugin(): T = getPlugin(T::class.java)

class NamedThreadFactory(private val prefix: String) : ThreadFactory {

    private val group = Thread.currentThread().threadGroup
    private val threadCount = AtomicInteger(0)

    override fun newThread(runnalbe: Runnable): Thread =
        Thread(group, runnalbe, "$prefix - ${threadCount.getAndIncrement()}", 0)

}
