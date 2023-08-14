package io.javalin.util

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
import java.util.concurrent.locks.ReentrantLock
import kotlin.LazyThreadSafetyMode.NONE
import kotlin.LazyThreadSafetyMode.SYNCHRONIZED
import kotlin.concurrent.withLock

object ConcurrencyUtil {

    @Suppress("MemberVisibilityCanBePrivate")
    // Determines if Javalin should use Loom. By default true, set it to false to disable Loom integration.
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
    SYNCHRONIZED -> if (ConcurrencyUtil.useLoom && ConcurrencyUtil.isLoomAvailable()) ReentrantLazy(initializer) else lazy(SYNCHRONIZED, initializer)
    else -> lazy(threadSafetyMode, initializer)
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
