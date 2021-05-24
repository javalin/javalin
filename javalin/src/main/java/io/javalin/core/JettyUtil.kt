package io.javalin.core

import io.javalin.core.LoomUtil.loomAvailable
import io.javalin.core.LoomUtil.useLoomThreadPool
import io.javalin.core.util.JavalinLogger
import org.eclipse.jetty.server.LowResourceMonitor
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.handler.StatisticsHandler
import org.eclipse.jetty.util.thread.QueuedThreadPool
import org.eclipse.jetty.util.thread.ThreadPool
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

object JettyUtil {

    private var defaultLogger: org.eclipse.jetty.util.log.Logger? = null

    @JvmStatic
    fun getOrDefault(server: Server?) = server ?: Server(defaultThreadPool()).apply {
        addBean(LowResourceMonitor(this))
        insertHandler(StatisticsHandler())
        setAttribute("is-default-server", true)
    }

    private fun defaultThreadPool() = if (useLoomThreadPool && loomAvailable) LoomThreadPool() else QueuedThreadPool(250, 8, 60_000)

    @JvmField
    var logDuringStartup = false

    @JvmStatic
    fun disableJettyLogger() {
        if (logDuringStartup) return
        defaultLogger = defaultLogger ?: org.eclipse.jetty.util.log.Log.getLog()
        org.eclipse.jetty.util.log.Log.setLog(NoopLogger())
    }

    fun reEnableJettyLogger() {
        if (logDuringStartup) return
        org.eclipse.jetty.util.log.Log.setLog(defaultLogger)
    }


    class NoopLogger : org.eclipse.jetty.util.log.Logger {
        override fun getName() = "noop"
        override fun getLogger(name: String) = this
        override fun setDebugEnabled(enabled: Boolean) {}
        override fun isDebugEnabled() = false
        override fun ignore(ignored: Throwable) {}
        override fun warn(msg: String, vararg args: Any) {}
        override fun warn(thrown: Throwable) {}
        override fun warn(msg: String, thrown: Throwable) {}
        override fun info(msg: String, vararg args: Any) {}
        override fun info(thrown: Throwable) {}
        override fun info(msg: String, thrown: Throwable) {}
        override fun debug(msg: String, vararg args: Any) {}
        override fun debug(s: String, l: Long) {}
        override fun debug(thrown: Throwable) {}
        override fun debug(msg: String, thrown: Throwable) {}
    }

}

object LoomUtil {

    @JvmField
    var useLoomThreadPool = true
    val loomAvailable = System.getProperty("java.version").contains("loom", ignoreCase = true) || try {
        Thread::class.java.getDeclaredMethod("startVirtualThread", Runnable::class.java)
        true
    } catch (e: Exception) {
        false
    }

    fun getExecutor(): ExecutorService {
        if (!loomAvailable) {
            throw IllegalStateException("Your Java version (${System.getProperty("java.version")}) doesn't support Loom")
        }
        JavalinLogger.info("Loom is available, using Virtual ThreadPool... Neat!")
        return Executors::class.java.getMethod("newVirtualThreadExecutor").invoke(Executors::class.java) as ExecutorService
    }
}

class LoomThreadPool : ThreadPool {

    private val executorService: ExecutorService = LoomUtil.getExecutor()

    override fun execute(command: Runnable) {
        executorService.submit(command)
    }

    override fun join() {}
    override fun getThreads() = 1
    override fun getIdleThreads() = 1
    override fun isLowOnThreads() = false
}
