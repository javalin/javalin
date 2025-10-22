package io.javalin.benchmark

import io.javalin.Javalin
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.openjdk.jmh.annotations.*
import org.openjdk.jmh.infra.Blackhole
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * End-to-end benchmarks simulating real-world request scenarios.
 * These benchmarks measure the complete request lifecycle including:
 * - Network overhead
 * - Request parsing
 * - Routing
 * - Handler execution
 * - Response serialization
 * - Response writing
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Fork(value = 1, jvmArgs = ["-Xms2G", "-Xmx2G"])
@Warmup(iterations = 2, time = 3)
@Measurement(iterations = 3, time = 5)
open class EndToEndBenchmark {

    private lateinit var app: Javalin
    private lateinit var client: OkHttpClient
    private lateinit var baseUrl: String

    data class User(
        var id: Long? = null,
        val name: String = "",
        val email: String = "",
        val age: Int = 0
    )

    @Setup
    fun setup() {
        // Create a Javalin app with realistic configuration
        app = Javalin.create { config ->
            config.http.generateEtags = true
            config.http.prefer405over404 = true
        }

        // Define routes using standard API (works for Javalin 5 and 6)
        // NOTE: For Javalin 7, you need to use app.unsafe.routes.get() instead
        app.get("/api/health") { ctx ->
            ctx.json(mapOf("status" to "healthy"))
        }

        app.get("/api/users/{id}") { ctx ->
            val id = ctx.pathParam("id")
            ctx.json(User(id.toLong(), "User $id", "user$id@example.com", 25))
        }

        app.get("/api/users") { ctx ->
            val page = ctx.queryParam("page")
            val limit = ctx.queryParam("limit")

            val users = listOf(
                User(1L, "Alice", "alice@example.com", 28),
                User(2L, "Bob", "bob@example.com", 32),
                User(3L, "Charlie", "charlie@example.com", 25)
            )

            ctx.json(mapOf(
                "users" to users,
                "page" to (page ?: "1"),
                "limit" to (limit ?: "10")
            ))
        }

        app.post("/api/users") { ctx ->
            val user = ctx.bodyAsClass(User::class.java)
            user.id = 123L
            ctx.status(201).json(user)
        }

        app.put("/api/users/{id}") { ctx ->
            val id = ctx.pathParam("id")
            val user = ctx.bodyAsClass(User::class.java)
            user.id = id.toLong()
            ctx.json(user)
        }

        app.delete("/api/users/{id}") { ctx ->
            ctx.status(204)
        }

        app.get("/api/organizations/{org-id}/projects/{project-id}/tasks") { ctx ->
            val orgId = ctx.pathParam("org-id")
            val projectId = ctx.pathParam("project-id")

            ctx.json(mapOf(
                "organizationId" to orgId,
                "projectId" to projectId,
                "tasks" to listOf(
                    mapOf("id" to 1, "title" to "Task 1"),
                    mapOf("id" to 2, "title" to "Task 2")
                )
            ))
        }

        app.get("/api/search") { ctx ->
            val query = ctx.queryParam("q")
            val category = ctx.queryParam("category")
            val sort = ctx.queryParam("sort")

            ctx.json(mapOf(
                "query" to (query ?: ""),
                "category" to (category ?: "all"),
                "sort" to (sort ?: "relevance"),
                "results" to emptyList<Any>()
            ))
        }

        app.start(0)
        baseUrl = "http://localhost:${app.port()}"

        client = OkHttpClient.Builder()
            .connectionPool(ConnectionPool(50, 5, TimeUnit.MINUTES))
            .build()
    }

    @TearDown
    fun teardown() {
        client.dispatcher.executorService.shutdown()
        client.connectionPool.evictAll()
        app.stop()
    }

    @Throws(IOException::class)
    private fun executeRequest(request: Request): Response {
        return client.newCall(request).execute()
    }

    /**
     * Benchmark simple GET request.
     * Tests basic request/response cycle.
     */
    @Benchmark
    @Throws(IOException::class)
    fun simpleGetRequest(blackhole: Blackhole) {
        val request = Request.Builder()
            .url("$baseUrl/api/health")
            .get()
            .build()

        executeRequest(request).use { response ->
            blackhole.consume(response.body?.string())
        }
    }

    /**
     * Benchmark GET with path parameter.
     * Tests parameter extraction in real requests.
     */
    @Benchmark
    @Throws(IOException::class)
    fun getWithPathParam(blackhole: Blackhole) {
        val request = Request.Builder()
            .url("$baseUrl/api/users/123")
            .get()
            .build()

        executeRequest(request).use { response ->
            blackhole.consume(response.body?.string())
        }
    }

    /**
     * Benchmark GET with query parameters.
     * Tests query parameter parsing.
     */
    @Benchmark
    @Throws(IOException::class)
    fun getWithQueryParams(blackhole: Blackhole) {
        val request = Request.Builder()
            .url("$baseUrl/api/users?page=1&limit=10")
            .get()
            .build()

        executeRequest(request).use { response ->
            blackhole.consume(response.body?.string())
        }
    }

    /**
     * Benchmark POST with JSON body.
     * Tests JSON deserialization and serialization.
     */
    @Benchmark
    @Throws(IOException::class)
    fun postWithJsonBody(blackhole: Blackhole) {
        val json = """{"name":"John Doe","email":"john@example.com","age":30}"""
        val body = json.toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url("$baseUrl/api/users")
            .post(body)
            .build()

        executeRequest(request).use { response ->
            blackhole.consume(response.body?.string())
        }
    }

    /**
     * Benchmark PUT with path param and JSON body.
     * Tests combined parameter extraction and JSON handling.
     */
    @Benchmark
    @Throws(IOException::class)
    fun putWithPathParamAndBody(blackhole: Blackhole) {
        val json = """{"name":"Jane Doe","email":"jane@example.com","age":28}"""
        val body = json.toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url("$baseUrl/api/users/456")
            .put(body)
            .build()

        executeRequest(request).use { response ->
            blackhole.consume(response.body?.string())
        }
    }

    /**
     * Benchmark DELETE request.
     * Tests simple DELETE operations.
     */
    @Benchmark
    @Throws(IOException::class)
    fun deleteRequest(blackhole: Blackhole) {
        val request = Request.Builder()
            .url("$baseUrl/api/users/789")
            .delete()
            .build()

        executeRequest(request).use { response ->
            blackhole.consume(response.code)
        }
    }

    /**
     * Benchmark complex nested route.
     * Tests deeply nested path matching.
     */
    @Benchmark
    @Throws(IOException::class)
    fun complexNestedRoute(blackhole: Blackhole) {
        val request = Request.Builder()
            .url("$baseUrl/api/organizations/org1/projects/proj1/tasks")
            .get()
            .build()

        executeRequest(request).use { response ->
            blackhole.consume(response.body?.string())
        }
    }

    /**
     * Benchmark search with multiple query parameters.
     * Tests complex query parameter handling.
     */
    @Benchmark
    @Throws(IOException::class)
    fun searchWithMultipleParams(blackhole: Blackhole) {
        val request = Request.Builder()
            .url("$baseUrl/api/search?q=test&category=books&sort=date")
            .get()
            .build()

        executeRequest(request).use { response ->
            blackhole.consume(response.body?.string())
        }
    }
}

