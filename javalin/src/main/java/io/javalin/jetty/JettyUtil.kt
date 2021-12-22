package io.javalin.jetty

import io.javalin.core.LoomUtil
import io.javalin.core.LoomUtil.loomAvailable
import io.javalin.core.LoomUtil.useLoomThreadPool
import io.javalin.core.util.JavalinLogger
import org.eclipse.jetty.server.LowResourceMonitor
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.handler.StatisticsHandler
import org.eclipse.jetty.util.thread.QueuedThreadPool
import org.eclipse.jetty.util.thread.ThreadPool
import java.io.IOException
import java.util.concurrent.TimeoutException

private var defaultLogger: org.eclipse.jetty.util.log.Logger? = null

object JettyUtil {

    @JvmStatic
    fun getOrDefault(server: Server?) = server ?: Server(defaultThreadPool()).apply {
        addBean(LowResourceMonitor(this))
        insertHandler(StatisticsHandler())
        setAttribute("is-default-server", true)
    }

    private fun defaultThreadPool() = if (useLoomThreadPool && loomAvailable) {
        JavalinLogger.info("Loom is available, using Virtual ThreadPool... Neat!")
        LoomThreadPool()
    } else {
        QueuedThreadPool(250, 8, 60_000).apply { name = "JettyServerThreadPool" }
    }

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

    var logIfNotStarted = true

    @JvmStatic
    fun maybeLogIfServerNotStarted(jettyServer: JettyServer) = Thread {
        Thread.sleep(5000)
        if (logIfNotStarted && !jettyServer.started) {
            JavalinLogger.info("It looks like you created a Javalin instance, but you never started it.")
            JavalinLogger.info("Try: Javalin app = Javalin.create().start();")
            JavalinLogger.info("For more help, visit https://javalin.io/documentation#server-setup")
            JavalinLogger.info("To disable this message, do `JettyUtil.logIfNotStarted = false`")
        }
    }.start()

    // jetty throws if client aborts during response writing. testing name avoids hard dependency on jetty.
    fun isClientAbortException(t: Throwable) = t::class.java.name == "org.eclipse.jetty.io.EofException"

    // Jetty may timeout connections to avoid having broken connections that remain open forever
    // This is rare, but intended (see issues #163 and #1277)
    fun isJettyTimeoutException(t: Throwable) = t is IOException && t.cause is TimeoutException

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

class LoomThreadPool : ThreadPool {

    private val executorService = LoomUtil.getExecutorService()

    override fun execute(command: Runnable) {
        executorService.submit(command)
    }

    override fun join() {}
    override fun getThreads() = 1
    override fun getIdleThreads() = 1
    override fun isLowOnThreads() = false
}
