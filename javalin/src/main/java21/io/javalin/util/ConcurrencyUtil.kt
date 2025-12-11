package io.javalin.util

import org.eclipse.jetty.util.thread.ThreadPool
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors.newThreadPerTaskExecutor
import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicInteger

internal object LoomUtil {

    @JvmStatic
    val loomAvailable: Boolean = true

    @JvmStatic
    fun executorService(name: String): ExecutorService =
        newThreadPerTaskExecutor(NamedVirtualThreadFactory(name))

    @JvmStatic
    fun threadPool(name: String): ThreadPool =
        LoomThreadPool(name)

    @JvmStatic
    fun isLoomThreadPool(threadPool: ThreadPool): Boolean =
        threadPool is LoomThreadPool

    private class LoomThreadPool(name: String) : ThreadPool {
        private val executorService = executorService(name)
        override fun join() {}
        override fun getThreads() = 1
        override fun getIdleThreads() = 1
        override fun isLowOnThreads() = false
        override fun execute(command: Runnable) {
            executorService.submit(command)
        }
    }
}

internal class NamedVirtualThreadFactory(private val prefix: String) : ThreadFactory {
    private val threadCount = AtomicInteger(0)

    override fun newThread(runnable: Runnable): Thread = Thread.ofVirtual()
            .name("$prefix-Virtual-${threadCount.getAndIncrement()}")
            .unstarted(runnable)
}
