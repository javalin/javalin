package io.javalin.util

import io.javalin.util.LoomUtil.loomAvailable
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.util.thread.QueuedThreadPool
import org.eclipse.jetty.util.thread.ThreadPool
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType
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

    fun jettyThreadPool(name: String, minThreads: Int, maxThreads: Int): ThreadPool = when (useLoom && loomAvailable) {
        true -> LoomThreadPool(name)
        false -> QueuedThreadPool(maxThreads, minThreads, 60_000).apply { this.name = name }
    }
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

    val loomAvailable = runCatching {
        val factoryMethod = Executors::class.java.getMethod("newVirtualThreadPerTaskExecutor") // this will not throw if preview is not enabled
        factoryMethod.invoke(Executors::class.java) as ExecutorService // this *will* throw if preview is not enabled
    }.isSuccess

    fun getExecutorService(name: String): ExecutorService {
        require(loomAvailable) { "Your Java version (${System.getProperty("java.version")}) doesn't support Loom" }
        val factoryMethod = Executors::class.java.getMethod("newThreadPerTaskExecutor", ThreadFactory::class.java)
        return factoryMethod.invoke(Executors::class.java, NamedVirtualThreadFactory(name)) as ExecutorService
    }

    const val logMsg = "Your JDK supports Loom. Javalin will prefer Virtual Threads by default. Disable with `ConcurrencyUtil.useLoom = false`."

    fun logIfLoom(server: Server) {
        if (server.threadPool !is LoomThreadPool) return
        JavalinLogger.startup(logMsg)
    }

}

private class NamedThreadFactory(private val prefix: String) : ThreadFactory {
    private val group = Thread.currentThread().threadGroup
    private val threadCount = AtomicInteger(0)
    override fun newThread(runnable: Runnable): Thread =
        Thread(group, runnable, "$prefix-${threadCount.getAndIncrement()}", 0)
}

private class NamedVirtualThreadFactory(private val prefix: String) : ThreadFactory {

    private val threadCount = AtomicInteger(0)

    override fun newThread(runnable: Runnable): Thread = ReflectiveVirtualThreadBuilder()
        .name("$prefix-Virtual-${threadCount.getAndIncrement()}")
        .unstarted(runnable)

    private class ReflectiveVirtualThreadBuilder {
        private var virtualBuilder = OF_VIRTUAL.invoke()

        fun name(name: String): ReflectiveVirtualThreadBuilder = also {
            this.virtualBuilder = NAME.invoke(virtualBuilder, name)
        }

        fun unstarted(runnable: Runnable): Thread =
            UNSTARTED.invoke(virtualBuilder, runnable) as Thread
    }

    companion object {

        val OF_VIRTUAL: MethodHandle
        val NAME: MethodHandle
        val UNSTARTED: MethodHandle

        init {
            val threadClass = Thread::class.java
            val builderClass = Class.forName("java.lang.Thread\$Builder\$OfVirtual")
            val handles = MethodHandles.publicLookup()
            OF_VIRTUAL = handles.findStatic(threadClass, "ofVirtual", MethodType.methodType(builderClass))
            NAME = handles.findVirtual(builderClass, "name", MethodType.methodType(builderClass, String::class.java))
            UNSTARTED = handles.findVirtual(builderClass, "unstarted", MethodType.methodType(threadClass, Runnable::class.java))
        }
    }
}
