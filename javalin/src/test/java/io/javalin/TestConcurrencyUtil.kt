package io.javalin

import io.javalin.util.ConcurrencyUtil
import io.javalin.util.NamedThreadFactory
import io.javalin.util.ReentrantLazy
import io.javalin.util.javalinLazy
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.jetty.util.thread.QueuedThreadPool
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledForJreRange
import org.junit.jupiter.api.condition.JRE
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.LazyThreadSafetyMode

internal class TestConcurrencyUtil {

    @Test
    fun `reentrant lazy should work like synchronized lazy`() {
        val threads = Runtime.getRuntime().availableProcessors() * 2
        val await = CountDownLatch(threads)
        val counter = AtomicInteger(0)

        val lazy = ReentrantLazy {
            Thread.sleep(1000)
            counter.incrementAndGet()
        }

        val fixedExecutor = Executors.newFixedThreadPool(threads)

        for (thread in 0..threads) {
            fixedExecutor.execute {
                lazy.value
                await.countDown()
            }
        }

        await.await()
        assertThat(lazy.isInitialized())
        assertThat(lazy.value).isEqualTo(1)
        assertThat(counter.get()).isEqualTo(1)
    }

    @Test
    fun `reentrant lazy should work like other kotlin lazy`() {
        val lazy: Lazy<Int> = ReentrantLazy { 1 }
        val lazyBy by lazy
        assertThat(lazyBy).isEqualTo(1)
    }

    @Test
    fun `executorService creates cached thread pool when loom is disabled`() {
        val executor = ConcurrencyUtil.executorService("test-executor", false)
        assertThat(executor).isNotNull()
        
        var executed = false
        executor.submit { executed = true }.get()
        assertThat(executed).isTrue()
        
        executor.shutdown()
    }

    @Test
    @EnabledForJreRange(min = JRE.JAVA_21)
    fun `executorService creates virtual thread pool when loom is enabled`() {
        if (!ConcurrencyUtil.isLoomAvailable()) return
        
        val executor = ConcurrencyUtil.executorService("test-virtual-executor", true)
        assertThat(executor).isNotNull()
        
        var executed = false
        executor.submit { executed = true }.get()
        assertThat(executed).isTrue()
        
        executor.shutdown()
    }

    @Test
    fun `newSingleThreadScheduledExecutor creates scheduled executor`() {
        val executor = ConcurrencyUtil.newSingleThreadScheduledExecutor("test-scheduled")
        assertThat(executor).isNotNull()
        
        val latch = CountDownLatch(1)
        executor.schedule({ latch.countDown() }, 10, TimeUnit.MILLISECONDS)
        
        assertThat(latch.await(1, TimeUnit.SECONDS)).isTrue()
        executor.shutdown()
    }

    @Test
    fun `jettyThreadPool creates QueuedThreadPool when loom is disabled`() {
        val threadPool = ConcurrencyUtil.jettyThreadPool("test-jetty", 2, 10, false)
        assertThat(threadPool).isInstanceOf(QueuedThreadPool::class.java)
        assertThat((threadPool as QueuedThreadPool).name).isEqualTo("test-jetty")
        assertThat(threadPool.minThreads).isEqualTo(2)
        assertThat(threadPool.maxThreads).isEqualTo(10)
    }

    @Test
    @EnabledForJreRange(min = JRE.JAVA_21)
    fun `jettyThreadPool creates LoomThreadPool when loom is enabled`() {
        if (!ConcurrencyUtil.isLoomAvailable()) return
        
        val threadPool = ConcurrencyUtil.jettyThreadPool("test-loom-jetty", 2, 10, true)
        assertThat(threadPool).isNotNull()
        assertThat(threadPool).isNotInstanceOf(QueuedThreadPool::class.java)
    }

    @Test
    fun `javalinLazy with NONE mode uses standard lazy`() {
        var initCount = 0
        val lazy = javalinLazy(LazyThreadSafetyMode.NONE) { 
            initCount++
            "value" 
        }
        
        assertThat(lazy.value).isEqualTo("value")
        assertThat(lazy.value).isEqualTo("value")
        assertThat(initCount).isEqualTo(1)
    }

    @Test
    fun `javalinLazy with SYNCHRONIZED mode uses ReentrantLazy when Loom available`() {
        var initCount = 0
        val lazy = javalinLazy(LazyThreadSafetyMode.SYNCHRONIZED) { 
            initCount++
            "value" 
        }
        
        assertThat(lazy.value).isEqualTo("value")
        assertThat(initCount).isEqualTo(1)
        
        if (ConcurrencyUtil.isLoomAvailable()) {
            assertThat(lazy).isInstanceOf(ReentrantLazy::class.java)
        }
    }

    @Test
    fun `javalinLazy with PUBLICATION mode uses standard lazy`() {
        var initCount = 0
        val lazy = javalinLazy(LazyThreadSafetyMode.PUBLICATION) { 
            initCount++
            "value" 
        }
        
        assertThat(lazy.value).isEqualTo("value")
        assertThat(initCount).isEqualTo(1)
    }

    @Test
    fun `NamedThreadFactory creates thread with sequential names across multiple instances`() {
        val factory1 = NamedThreadFactory("worker")
        val factory2 = NamedThreadFactory("worker")
        
        val thread1 = factory1.newThread {}
        val thread2 = factory2.newThread {}
        
        // Each factory maintains its own counter
        assertThat(thread1.name).isEqualTo("worker-0")
        assertThat(thread2.name).isEqualTo("worker-0")
    }

}

