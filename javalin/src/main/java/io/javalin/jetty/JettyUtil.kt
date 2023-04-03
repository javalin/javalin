package io.javalin.jetty

import io.javalin.http.HttpStatus
import io.javalin.util.ConcurrencyUtil
import io.javalin.util.JavalinLogger
import jakarta.servlet.http.HttpServletResponse
import org.eclipse.jetty.server.LowResourceMonitor
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.handler.StatisticsHandler
import java.io.IOException
import java.util.concurrent.CompletionException
import java.util.concurrent.TimeoutException

object JettyUtil {

    @JvmStatic
    fun getOrDefault(server: Server?) = server ?: Server(defaultThreadPool()).apply {
        addBean(LowResourceMonitor(this))
        insertHandler(StatisticsHandler())
        setAttribute("is-default-server", true)
    }

    private fun defaultThreadPool() = ConcurrencyUtil.jettyThreadPool("JettyServerThreadPool", 8, 250)

    @JvmStatic
    fun maybeLogIfServerNotStarted(jettyServer: JettyServer) = Thread {
        Thread.sleep(5000)
        if (!jettyServer.started) {
            JavalinLogger.startup("It looks like you created a Javalin instance, but you never started it.")
            JavalinLogger.startup("Try: Javalin app = Javalin.create().start();")
            JavalinLogger.startup("For more help, visit https://javalin.io/documentation#server-setup")
        }
    }.start()

    // jetty throws if client aborts during response writing. testing name avoids hard dependency on jetty.
    fun isClientAbortException(t: Throwable) = t::class.java.name == "org.eclipse.jetty.io.EofException"

    // Jetty may timeout connections to avoid having broken connections that remain open forever
    // This is rare, but intended (see issues #163 and #1277)
    fun isJettyTimeoutException(t: Throwable) = t is IOException && t.cause is TimeoutException

    fun isSomewhatExpectedException(t: Throwable): Boolean {
        val unwrapped = (t as? CompletionException)?.cause ?: t
        return isClientAbortException(unwrapped) || isJettyTimeoutException(unwrapped)
    }
    fun logDebugAndSetError(t: Throwable, res: HttpServletResponse) {
        JavalinLogger.debug("Client aborted or timed out", t)
        res.status = HttpStatus.INTERNAL_SERVER_ERROR.code
    }

}
