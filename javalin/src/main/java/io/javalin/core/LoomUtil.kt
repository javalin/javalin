package io.javalin.core

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

object LoomUtil {

    @JvmField
    var useLoomThreadPool = true

    val loomAvailable = System.getProperty("java.version").contains("loom", ignoreCase = true) || try {
        Thread::class.java.getDeclaredMethod("startVirtualThread", Runnable::class.java)
        true
    } catch (e: Exception) {
        false
    }

    fun getExecutorService(): ExecutorService {
        if (!loomAvailable) {
            throw IllegalStateException("Your Java version (${System.getProperty("java.version")}) doesn't support Loom")
        }
        return Executors::class.java.getMethod("newVirtualThreadExecutor").invoke(Executors::class.java) as ExecutorService
    }

}
