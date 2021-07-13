package io.javalin.testtools

import io.javalin.Javalin
import io.javalin.core.util.JavalinLogger
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.util.*

object TestUtil {

    @JvmField
    var disableJavalinLogging = true

    @JvmField
    var clearCookies = true

    @JvmStatic
    fun test(app: Javalin, testCase: TestCase) {
        if (disableJavalinLogging) {
            JavalinLogger.enabled = false
        }
        app.start(0)
        val http = HttpClient(app)
        testCase.accept(app, http) // this is where the user's test happens
        if (clearCookies) {
            val endpointUrl = "/clear-cookies-${UUID.randomUUID()}"
            app.delete(endpointUrl) { it.cookieMap().forEach { (k, _) -> it.removeCookie(k) } }
            http.request(endpointUrl) { it.delete() }
        }
        app.stop()
        if (disableJavalinLogging) {
            JavalinLogger.enabled = true
        }
    }

    @JvmStatic
    fun test(testCase: TestCase) {
        test(Javalin.create(), testCase)
    }

    @JvmStatic
    fun captureStdOut(run: ThrowingRunnable): String {
        val out = ByteArrayOutputStream()
        val printStream = PrintStream(out)
        val oldOut = System.out
        val oldErr = System.err
        System.setOut(printStream)
        System.setErr(printStream)
        try {
            run.run()
        } finally {
            System.out.flush()
            System.setOut(oldOut)
            System.setErr(oldErr)
        }
        return out.toString()
    }

}
