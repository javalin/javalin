package io.javalin

import io.javalin.util.ReentrantLazy
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

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

}
