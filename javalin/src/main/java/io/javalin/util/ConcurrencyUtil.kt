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

internal open class LoomUtil {
    companion object {

        @JvmStatic
        val loomAvailable: Boolean = false

        @JvmStatic
        fun executorService(name: String): ExecutorService =
            throw UnsupportedOperationException("Loom not available")

        @JvmStatic
        fun threadPool(name: String): ThreadPool =
            throw UnsupportedOperationException("Loom not available")

        @JvmStatic
        fun isLoomThreadPool(threadPool: ThreadPool): Boolean = false
    }
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
