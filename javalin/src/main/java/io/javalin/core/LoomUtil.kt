package io.javalin.core

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicInteger

internal class NamedThreadFactory(private val prefix: String) : ThreadFactory {
    private val group = Thread.currentThread().threadGroup
    private val threadCount = AtomicInteger(0)
    override fun newThread(runnable: Runnable): Thread =
        Thread(group, runnable, "$prefix-${threadCount.getAndIncrement()}", 0)

}

object LoomUtil {

    @JvmField
    var useLoomThreadPool = true

    val loomAvailable = try {
        Thread::class.java.getMethod("startVirtualThread", Runnable::class.java)
        Executors::class.java.getMethod("newThreadPerTaskExecutor", ThreadFactory::class.java)
        true
    } catch (e: Exception) {
        false
    }

    fun getExecutorService(name: String): ExecutorService {
        require(loomAvailable) { "Your Java version (${System.getProperty("java.version")}) doesn't support Loom" }
        val factoryMethod = Executors::class.java.getMethod("newThreadPerTaskExecutor", ThreadFactory::class.java)
        return factoryMethod.invoke(Executors::class.java, NamedThreadFactory(name)) as ExecutorService
    }

}
