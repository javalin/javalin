package io.javalin.core

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicInteger

object LoomUtil {

    @JvmField
    var useLoomThreadPool = true

    val loomAvailable = runCatching {
        val factoryMethod = Executors::class.java.getMethod("newVirtualThreadPerTaskExecutor") // this will not throw if preview is not enabled
        factoryMethod.invoke(Executors::class.java) as ExecutorService // this *will* throw if preview is not enabled
    }.isSuccess

    fun getExecutorService(name: String): ExecutorService {
        require(loomAvailable) { "Your Java version (${System.getProperty("java.version")}) doesn't support Loom" }
        val factoryMethod = Executors::class.java.getMethod("newThreadPerTaskExecutor", ThreadFactory::class.java)
        return factoryMethod.invoke(Executors::class.java, NamedVirtualThreadFactory(name)) as ExecutorService
    }

}

private class NamedVirtualThreadFactory(private val prefix: String) : ThreadFactory {

    private val threadCount = AtomicInteger(0)
    override fun newThread(runnable: Runnable): Thread = ReflectiveVirtualThreadBuilder()
        .name("$prefix-Virtual-${threadCount.getAndIncrement()}")
        .unstarted(runnable)

    private class ReflectiveVirtualThreadBuilder {
        private val builderClass = Class.forName("java.lang.Thread\$Builder\$OfVirtual")
        private var virtualBuilder = Thread::class.java.getMethod("ofVirtual").invoke(Thread::class.java)

        fun name(name: String): ReflectiveVirtualThreadBuilder = also {
            this.virtualBuilder = builderClass.getMethod("name", String::class.java).invoke(virtualBuilder, name)
        }

        fun unstarted(runnable: Runnable): Thread =
            builderClass.getMethod("unstarted", Runnable::class.java).invoke(virtualBuilder, runnable) as Thread
    }
}
