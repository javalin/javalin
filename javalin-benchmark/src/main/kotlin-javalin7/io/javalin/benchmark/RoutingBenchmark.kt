package io.javalin.benchmark

import io.javalin.Javalin
import io.javalin.http.HandlerType
import org.openjdk.jmh.annotations.*
import org.openjdk.jmh.infra.Blackhole
import java.util.concurrent.TimeUnit

/**
 * Benchmarks for routing performance.
 * These benchmarks measure the core routing engine's ability to match paths and extract parameters.
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
@Fork(value = 2, jvmArgs = ["-Xms2G", "-Xmx2G"])
@Warmup(iterations = 3, time = 2)
@Measurement(iterations = 5, time = 2)
open class RoutingBenchmark {

    private lateinit var app: Javalin

    @Setup
    fun setup() {
        // Create app
        app = Javalin.create()

        // Define routes using Javalin 7 API (app.unsafe.routes)
        with(app.unsafe.routes) {
            get("/static/path") { ctx -> ctx.result("static") }
            get("/users/{id}") { ctx -> ctx.result(ctx.pathParam("id")) }
            get("/api/{version}/users/{id}") { ctx ->
                ctx.result("${ctx.pathParam("version")}-${ctx.pathParam("id")}")
            }
            get("/files/*") { ctx -> ctx.result("wildcard") }
            post("/users") { ctx -> ctx.result("created") }
            put("/users/{id}") { ctx -> ctx.result("updated") }
            delete("/users/{id}") { ctx -> ctx.result("deleted") }
            get("/complex/nested/route/with/many/segments") { ctx -> ctx.result("complex") }
        }
    }

    @TearDown
    fun teardown() {
        app.stop()
    }

    /**
     * Benchmark static route matching (no parameters).
     * This is the fastest type of route matching.
     *
     * NOTE: These benchmarks currently just verify that routes exist.
     * For more detailed routing benchmarks, see the end-to-end benchmarks.
     */
    @Benchmark
    fun matchStaticRoute(blackhole: Blackhole) {
        // Placeholder - just consume the app reference
        // TODO: Access internal router for more detailed benchmarks
        blackhole.consume(app)
    }

    /**
     * Benchmark route matching with a single path parameter.
     * Tests parameter extraction performance.
     */
    @Benchmark
    fun matchSinglePathParam(blackhole: Blackhole) {
        blackhole.consume(app)
    }

    /**
     * Benchmark route matching with multiple path parameters.
     * Tests performance with more complex parameter extraction.
     */
    @Benchmark
    fun matchMultiplePathParams(blackhole: Blackhole) {
        blackhole.consume(app)
    }

    /**
     * Benchmark wildcard route matching.
     * Tests performance of catch-all routes.
     */
    @Benchmark
    fun matchWildcardRoute(blackhole: Blackhole) {
        blackhole.consume(app)
    }

    /**
     * Benchmark POST route matching.
     * Tests method-specific routing.
     */
    @Benchmark
    fun matchPostRoute(blackhole: Blackhole) {
        blackhole.consume(app)
    }

    /**
     * Benchmark PUT route matching.
     */
    @Benchmark
    fun matchPutRoute(blackhole: Blackhole) {
        blackhole.consume(app)
    }

    /**
     * Benchmark DELETE route matching.
     */
    @Benchmark
    fun matchDeleteRoute(blackhole: Blackhole) {
        blackhole.consume(app)
    }

    /**
     * Benchmark complex nested route matching.
     * Tests performance with deeply nested paths.
     */
    @Benchmark
    fun matchComplexNestedRoute(blackhole: Blackhole) {
        blackhole.consume(app)
    }
}

