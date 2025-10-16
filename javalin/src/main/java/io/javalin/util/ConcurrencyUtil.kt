package io.javalin.util

import org.eclipse.jetty.util.thread.QueuedThreadPool
import org.eclipse.jetty.util.thread.ThreadPool
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantLock
import kotlin.LazyThreadSafetyMode.NONE
import kotlin.LazyThreadSafetyMode.SYNCHRONIZED
import kotlin.concurrent.withLock

object ConcurrencyUtil {

    @JvmStatic
    fun executorService(name: String, useLoom: Boolean): ExecutorService = when (useLoom && isLoomAvailable()) {
        true -> LoomUtil.executorService(name)
        false -> Executors.newCachedThreadPool(NamedThreadFactory(name))
    }

    @JvmStatic
    fun newSingleThreadScheduledExecutor(name: String): ScheduledExecutorService =
        Executors.newSingleThreadScheduledExecutor(NamedThreadFactory(name))

    @JvmStatic
    fun jettyThreadPool(name: String, minThreads: Int, maxThreads: Int, useLoom: Boolean): ThreadPool = when (useLoom && isLoomAvailable()) {
        true -> LoomUtil.threadPool(name)
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

    fun executorService(name: String): ExecutorService {
        require(loomAvailable) { "Your Java version (${System.getProperty("java.version")}) doesn't support Loom" }
        val factoryMethod = Executors::class.java.getMethod("newThreadPerTaskExecutor", ThreadFactory::class.java)
        return factoryMethod.invoke(Executors::class.java, NamedVirtualThreadFactory(name)) as ExecutorService
    }

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

    fun threadPool(name: String): ThreadPool =
        LoomThreadPool(name)

    fun isLoomThreadPool(threadPool: ThreadPool): Boolean =
        threadPool is LoomThreadPool

}

/**
 * Loom-friendly [kotlin.Lazy] implementation
 */
internal class ReentrantLazy<T : Any?>(initializer: () -> T) : Lazy<T> {
    private companion object {
        private object UNINITIALIZED_VALUE
    }

    private var initializer: (() -> T)? = initializer

    @Volatile
    private var lock: ReentrantLock? = ReentrantLock()

    @Volatile
    private var _value: Any? = UNINITIALIZED_VALUE

    override val value: T
        get() {
            lock?.withLock {
                if (_value === UNINITIALIZED_VALUE) {
                    this._value = initializer!!.invoke()
                    this.lock = null
                    this.initializer = null
                }
            }
            @Suppress("UNCHECKED_CAST")
            return _value as T
        }

    override fun isInitialized(): Boolean = _value !== UNINITIALIZED_VALUE
}


/**
 * Loom-friendly [kotlin.lazy] implementation
 *
 * By default, [kotlin.lazy] uses [SynchronizedLazyImpl] which is not Loom-friendly.
 * We instead use LazyThreadSafetyMode = NONE by default, and use [ReentrantLazy] when
 * [LazyThreadSafetyMode.SYNCHRONIZED] is requested.
 */
fun <T : Any?> javalinLazy(
    threadSafetyMode: LazyThreadSafetyMode = NONE,
    initializer: () -> T
): Lazy<T> = when (threadSafetyMode) {
    SYNCHRONIZED -> if (ConcurrencyUtil.isLoomAvailable()) ReentrantLazy(initializer) else lazy(SYNCHRONIZED, initializer)
    else -> lazy(threadSafetyMode, initializer)
}

open class NamedThreadFactory(protected val prefix: String) : ThreadFactory {
    protected val group: ThreadGroup? = Thread.currentThread().threadGroup
    protected val threadCount = AtomicInteger(0)

    override fun newThread(runnable: Runnable): Thread =
        Thread(group, runnable, "$prefix-${threadCount.getAndIncrement()}", 0)
}

open class NamedVirtualThreadFactory(prefix: String) : NamedThreadFactory(prefix) {
    override fun newThread(runnable: Runnable): Thread = VirtualThreadBuilder.create()
        .name("$prefix-Virtual-${threadCount.getAndIncrement()}")
        .unstarted(runnable)
}

object VirtualThreadBuilder {
    private val builderClass = Class.forName("java.lang.Thread\$Builder\$OfVirtual")
    private val nameMethod = builderClass.getMethod("name", String::class.java)
    private val unstartedMethod = builderClass.getMethod("unstarted", Runnable::class.java)
    private val ofVirtualMethod = Thread::class.java.getMethod("ofVirtual")

    interface Builder {
        fun name(name: String): Builder
        fun unstarted(runnable: Runnable): Thread
    }

    private class BuilderImpl : Builder {
        private val ofVirtual = ofVirtualMethod.invoke(null)

        override fun name(name: String): Builder {
            nameMethod.invoke(ofVirtual, name)
            return this
        }

        override fun unstarted(runnable: Runnable): Thread {
            return unstartedMethod.invoke(ofVirtual, runnable) as Thread
        }
    }

    fun create(): Builder = BuilderImpl()
}
