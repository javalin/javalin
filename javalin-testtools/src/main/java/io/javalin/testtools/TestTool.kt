package io.javalin.testtools

import io.javalin.Javalin
import io.javalin.config.Key
import io.javalin.http.HandlerType
import io.javalin.router.Endpoint
import io.javalin.util.JavalinLogger
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.net.CookieManager
import java.net.CookiePolicy
import java.net.http.HttpClient as JdkHttpClient
import java.time.Duration
import java.util.*

object DefaultTestConfig {
    @JvmStatic var clearCookies: Boolean = true
    @JvmStatic var captureLogs: Boolean = true
    @JvmStatic var httpClient: JdkHttpClient = JdkHttpClient.newBuilder()
        .cookieHandler(CookieManager(null, CookiePolicy.ACCEPT_ALL))
        .connectTimeout(Duration.ofSeconds(10))
        .build()
}

data class TestConfig @JvmOverloads constructor(
    val clearCookies: Boolean = DefaultTestConfig.clearCookies,
    val captureLogs: Boolean = DefaultTestConfig.captureLogs,
    val httpClient: JdkHttpClient = DefaultTestConfig.httpClient,
    val defaultHeaders: Map<String, String> = emptyMap()
)

class TestTool(private val testConfig: TestConfig = TestConfig()) {

    companion object {
        @JvmField val TestLogsKey: Key<String?> = Key("testlogs")
    }

    class RunResult(val logs: String?, val exception: Throwable?)

    @JvmOverloads
    fun test(app: Javalin = Javalin.create(), config: TestConfig = this.testConfig, testCase: TestCase) {
        val result: RunResult = runAndCaptureLogs(config) {
            app.start(0)
            val http = HttpClient(app, config.httpClient, config.defaultHeaders)
            testCase.accept(app, http) // this is where the user's test happens
            if (config.clearCookies) {
                val endpointUrl = "/clear-cookies-${UUID.randomUUID()}"
                app.unsafeConfig().pvt.internalRouter.addHttpEndpoint(
                    Endpoint(HandlerType.DELETE, endpointUrl) { it.cookieMap().forEach { (k, _) -> it.removeCookie(k) } }
                )
                http.request(endpointUrl) { it.delete() }
            }
            app.stop()
        }
        app.unsafeConfig().appData(TestLogsKey, result.logs)
        if (result.exception != null) {
            JavalinLogger.error("JavalinTest#test failed - full log output below:\n" + result.logs)
            throw result.exception
        }
    }

    fun captureStdOut(runnable: Runnable): String? = runAndCaptureLogs(this.testConfig, runnable).logs

    private fun runAndCaptureLogs(testConfig: TestConfig = this.testConfig, testCode: Runnable): RunResult {
        var exception: Throwable? = null
        val out = ByteArrayOutputStream()
        val printStream = PrintStream(out)
        val oldOut = System.out
        val oldErr = System.err
        if (testConfig.captureLogs) {
            System.setOut(printStream)
            System.setErr(printStream)
        }
        try {
            testCode.run()
        } catch (t: Throwable) {
            exception = when (t) {
                is Exception -> t
                is AssertionError -> t
                else -> Exception("Unexpected Throwable in test. Message: '${t.message}'", t)
            }
        } finally {
            System.out.flush()
            System.err.flush()
            System.setOut(oldOut)
            System.setErr(oldErr)
        }
        return RunResult(out.toString(), exception)
    }

}
