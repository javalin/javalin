package io.javalin.util

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

    @Suppress("MemberVisibilityCanBePrivate")
    /** Determines if Javalin should use Loom. By default true, set it to false to disable Loom integration. **/
    var useLoom = true

    @JvmStatic
    fun executorService(name: String): ExecutorService = when (useLoom && isLoomAvailable()) {
        true -> LoomUtil.getExecutorService(name)
        false -> Executors.newCachedThreadPool(NamedThreadFactory(name))
    }

    @JvmStatic
    fun newSingleThreadScheduledExecutor(name: String): ScheduledExecutorService =
        Executors.newSingleThreadScheduledExecutor(NamedThreadFactory(name))

    @JvmStatic
    fun jettyThreadPool(name: String, minThreads: Int, maxThreads: Int): ThreadPool = when (useLoom && isLoomAvailable()) {
        true -> LoomUtil.getThreadPool(name)
        false -> QueuedThreadPool(maxThreads, minThreads, 60_000).apply { this.name = name }
    }

    @JvmStatic
    fun isLoomAvailable(): Boolean =
        LoomUtil.loomAvailable

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

    private class LoomThreadPool(name: String) : ThreadPool {
        private val executorService = getExecutorService(name)
        override fun join() {}
        override fun getThreads() = 1
        override fun getIdleThreads() = 1
        override fun isLowOnThreads() = false
        override fun execute(command: Runnable) {
            executorService.submit(command)
        }
    }

    fun getThreadPool(name: String): ThreadPool =
        LoomThreadPool(name)

    fun isLoomThreadPool(threadPool: ThreadPool): Boolean =
        threadPool is LoomThreadPool

    fun logIfLoom(server: Server) {
        if (!isLoomThreadPool(server.threadPool)) return
        JavalinLogger.startup("Your JDK supports Loom. Javalin will prefer Virtual Threads by default. Disable with `ConcurrencyUtil.useLoom = false`.")
    }

}

open class NamedThreadFactory(protected val prefix: String) : ThreadFactory {
    protected val group: ThreadGroup? = Thread.currentThread().threadGroup
    protected val threadCount = AtomicInteger(0)

    override fun newThread(runnable: Runnable): Thread =
        Thread(group, runnable, "$prefix-${threadCount.getAndIncrement()}", 0)
}

open class NamedVirtualThreadFactory(prefix: String) : NamedThreadFactory(prefix) {
    override fun newThread(runnable: Runnable): Thread = ReflectiveVirtualThreadBuilder()
        .name("$prefix-Virtual-${threadCount.getAndIncrement()}")
        .unstarted(runnable)
}

open class ReflectiveVirtualThreadBuilder {

    protected companion object {
        val OF_VIRTUAL: MethodHandle
        val NAME: MethodHandle
        val UNSTARTED: MethodHandle

        init {
            val builderClass = Class.forName("java.lang.Thread\$Builder\$OfVirtual")
            val handles = MethodHandles.publicLookup()
            OF_VIRTUAL = handles.findStatic(Thread::class.java, "ofVirtual", MethodType.methodType(builderClass))
            NAME = handles.findVirtual(builderClass, "name", MethodType.methodType(builderClass, String::class.java))
            UNSTARTED = handles.findVirtual(builderClass, "unstarted", MethodType.methodType(Thread::class.java, Runnable::class.java))
        }
    }

    protected var virtualBuilder: Any = OF_VIRTUAL.invoke()

    fun name(name: String): ReflectiveVirtualThreadBuilder = also {
        this.virtualBuilder = NAME.invoke(virtualBuilder, name)
    }

    fun unstarted(runnable: Runnable): Thread =
        UNSTARTED.invoke(virtualBuilder, runnable) as Thread

}
