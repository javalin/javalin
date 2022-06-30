package io.javalin.core.util

import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicInteger

object JavalinExecutors {
    @JvmStatic
    fun newCachedThreadPool(name: String) = Executors.newCachedThreadPool(NamedThreadFactory(name))
}

class NamedThreadFactory(private val prefix: String) : ThreadFactory {
    private val group = Thread.currentThread().threadGroup
    private val threadCount = AtomicInteger(0)
    override fun newThread(runnalbe: Runnable): Thread =
        Thread(group, runnalbe, "$prefix - ${threadCount.getAndIncrement()}", 0)

}
