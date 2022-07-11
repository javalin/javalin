package io.javalin.testtools

import io.javalin.Javalin

object JavalinTest {

    private val testConfig = TestConfig()
    private val testTool = TestTool(testConfig)

    @JvmStatic
    @JvmOverloads
    fun test(app: Javalin = Javalin.create(), config: TestConfig = this.testConfig, testCase: TestCase) =
        testTool.test(app, config, testCase)

    @JvmStatic
    fun captureStdOut(runnable: Runnable) = testTool.captureStdOut(runnable)

}
