package io.javalin

import io.jaegertracing.Configuration
import io.jaegertracing.internal.reporters.InMemoryReporter
import io.jaegertracing.internal.samplers.ConstSampler
import io.javalin.plugin.tracing.JaegerPlugin
import io.javalin.testing.HttpUtil
import org.assertj.core.api.Assertions
import org.junit.After
import org.junit.Test

class TestJaegerPlugin {
    private val inMemoryReporter: InMemoryReporter = InMemoryReporter()
    val app: Javalin = Javalin.create { config ->
        config.registerPlugin(JaegerPlugin(Configuration("jaeger-plugin-test")
                .tracerBuilder
                .withSampler(ConstSampler(true))
                .withReporter(inMemoryReporter)
                .build())
        )
    }.start(0)
    val http = HttpUtil(app.port())

    @After
    fun after() {
        app.stop()
    }

    @Test
    fun `root span created with tags`() {
        app.get("/hello/:name") { it.result("Hello: ${it.pathParam("name")}") }
        http.get("/hello/jon")

        val spans = inMemoryReporter.spans
        Assertions.assertThat(spans.size).isEqualTo(1)

        val rootSpan = spans.first()
        Assertions.assertThat(rootSpan.tags.containsKey("method"))
        Assertions.assertThat(rootSpan.tags.containsKey("path"))

        Assertions.assertThat(rootSpan.tags.getOrDefault("method", "")).isEqualTo("GET")
        Assertions.assertThat(rootSpan.tags.getOrDefault("path", "")).isEqualTo("/hello/jon")

        Assertions.assertThat(rootSpan.isFinished)
    }
}
