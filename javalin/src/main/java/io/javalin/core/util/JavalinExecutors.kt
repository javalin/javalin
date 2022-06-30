package io.javalin.core.util

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicInteger

object JavalinExecutors {
    @JvmStatic
    fun newCachedThreadPool(name: String): ExecutorService =
        Executors.newCachedThreadPool(NamedThreadFactory(name))

    fun newSingleThreadScheduledExecutor(name: String): ScheduledExecutorService =
        Executors.newSingleThreadScheduledExecutor(NamedThreadFactory(name))
}

class NamedThreadFactory(private val prefix: String) : ThreadFactory {
    private val group = Thread.currentThread().threadGroup
    private val threadCount = AtomicInteger(0)
    override fun newThread(runnable: Runnable): Thread =
        Thread(group, runnable, "$prefix-${threadCount.getAndIncrement()}", 0)

}
