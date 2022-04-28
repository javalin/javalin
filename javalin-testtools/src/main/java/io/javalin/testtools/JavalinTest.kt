package io.javalin.testtools

import io.javalin.Javalin
import io.javalin.core.util.JavalinLogger
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.util.*

object JavalinTest {

    class RunResult(val logs: String?, val exception: Exception?)

    @JvmField
    var clearCookies = true

    @JvmStatic
    fun test(app: Javalin, testCase: TestCase) {
        val result: RunResult = runAndCaptureLogs {
            app.start(0)
            val http = HttpClient(app)
            testCase.accept(app, http) // this is where the user's test happens
            if (clearCookies) {
                val endpointUrl = "/clear-cookies-${UUID.randomUUID()}"
                app.delete(endpointUrl) { it.cookieMap().forEach { (k, _) -> it.removeCookie(k) } }
                http.request(endpointUrl) { it.delete() }
            }
            app.stop()
        }
        app.attribute("testlogs", result.logs)
        if (result.exception != null) {
            JavalinLogger.error("There were non-assertion errors in test code.\n" + result.logs);
            throw RuntimeException(result.exception)
        }
    }

    @JvmStatic
    fun test(testCase: TestCase) = test(Javalin.create(), testCase)

    @JvmStatic
    fun runAndCaptureLogs(testCode: Runnable): RunResult {
        var exception: Exception? = null
        val out = ByteArrayOutputStream()
        val printStream = PrintStream(out)
        val oldOut = System.out
        val oldErr = System.err
        System.setOut(printStream)
        System.setErr(printStream)
        try {
            testCode.run()
        } catch (e: Exception) {
            exception = e
        } finally {
            System.out.flush()
            System.setOut(oldOut)
            System.setErr(oldErr)
        }
        return RunResult(out.toString(), exception)
    }

    @JvmStatic
    fun runLogLess(run: Runnable) {
        val result: RunResult = runAndCaptureLogs(run)
        if (result.exception != null) {
            JavalinLogger.error("There were non-assertion errors in test code:\n" + result.logs)
            throw RuntimeException(result.exception)
        }
    }

    @JvmStatic
    fun captureStdOut(run: Runnable): String? = runAndCaptureLogs(run).logs

}
