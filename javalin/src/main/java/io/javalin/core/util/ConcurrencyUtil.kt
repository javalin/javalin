package io.javalin.core.util

import io.javalin.core.util.LoomUtil.loomAvailable
import org.eclipse.jetty.util.thread.QueuedThreadPool
import org.eclipse.jetty.util.thread.ThreadPool
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicInteger

object ConcurrencyUtil {

    var useLoom = true

    @JvmStatic
    fun executorService(name: String): ExecutorService = when (useLoom && loomAvailable) {
        true -> LoomUtil.getExecutorService(name)
        false -> Executors.newCachedThreadPool(NamedThreadFactory(name))
    }

    fun newSingleThreadScheduledExecutor(name: String): ScheduledExecutorService =
        Executors.newSingleThreadScheduledExecutor(NamedThreadFactory(name))

    fun jettyThreadPool(name: String): ThreadPool = when (useLoom && loomAvailable) {
        true -> LoomThreadPool(name)
        false -> QueuedThreadPool(250, 8, 60_000).apply { this.name = name }
    }
}

internal class NamedThreadFactory(private val prefix: String) : ThreadFactory {
    private val group = Thread.currentThread().threadGroup
    private val threadCount = AtomicInteger(0)
    override fun newThread(runnable: Runnable): Thread =
        Thread(group, runnable, "$prefix-${threadCount.getAndIncrement()}", 0)

}

internal class LoomThreadPool(name: String) : ThreadPool {
    private val executorService = LoomUtil.getExecutorService(name)
    override fun join() {}
    override fun getThreads() = 1
    override fun getIdleThreads() = 1
    override fun isLowOnThreads() = false
    override fun execute(command: Runnable) {
        executorService.submit(command)
    }
}

internal object LoomUtil {

    val loomAvailable = System.getProperty("java.version").contains("loom", ignoreCase = true) || try {
        Thread::class.java.getDeclaredMethod("startVirtualThread", Runnable::class.java)
        true
    } catch (e: Exception) {
        false
    }

    fun getExecutorService(name: String): ExecutorService {
        require(loomAvailable) { "Your Java version (${System.getProperty("java.version")}) doesn't support Loom" }
        val factoryMethod = Executors::class.java.getMethod("newVirtualThreadPerTaskExecutor")
        return factoryMethod.invoke(Executors::class.java, NamedThreadFactory(name)) as ExecutorService
    }

}
