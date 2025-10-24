package io.javalin.benchmark

import io.javalin.Javalin
import io.javalin.json.JavalinJackson
import org.openjdk.jmh.annotations.*
import org.openjdk.jmh.infra.Blackhole
import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit

/**
 * Benchmarks for common Context method operations.
 * These methods are called frequently in request handlers and their performance
 * directly impacts application throughput.
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
@Fork(value = 2, jvmArgs = ["-Xms2G", "-Xmx2G"])
@Warmup(iterations = 3, time = 2)
@Measurement(iterations = 5, time = 2)
open class ContextMethodsBenchmark {

    private lateinit var app: Javalin
    private lateinit var jsonMapper: JavalinJackson
    private lateinit var testData: TestData

    data class TestData(
        val name: String = "",
        val age: Int = 0,
        val email: String = "",
        val tags: List<String> = emptyList()
    )

    @Setup
    fun setup() {
        val jacksonMapper = JavalinJackson()
        app = Javalin.create { config ->
            config.jsonMapper(jacksonMapper)
        }

        jsonMapper = jacksonMapper
        testData = TestData("John Doe", 30, "john@example.com", listOf("tag1", "tag2", "tag3"))
    }

    @TearDown
    fun teardown() {
        app.stop()
    }

    /**
     * Benchmark JSON serialization - converting objects to JSON strings.
     * This is one of the most common operations in REST APIs.
     */
    @Benchmark
    fun jsonSerialization(blackhole: Blackhole) {
        val json = jsonMapper.toJsonString(testData, TestData::class.java)
        blackhole.consume(json)
    }

    /**
     * Benchmark JSON deserialization - parsing JSON strings to objects.
     * Critical for POST/PUT request handling.
     */
    @Benchmark
    fun jsonDeserialization(blackhole: Blackhole) {
        val json = """{"name":"John Doe","age":30,"email":"john@example.com","tags":["tag1","tag2","tag3"]}"""
        val data: TestData = jsonMapper.fromJsonString(json, TestData::class.java)
        blackhole.consume(data)
    }

    /**
     * Benchmark JSON stream serialization.
     * Used for large responses where streaming is more efficient.
     */
    @Benchmark
    fun jsonStreamSerialization(blackhole: Blackhole) {
        val stream = jsonMapper.toJsonStream(testData, TestData::class.java)
        blackhole.consume(stream)
    }

    /**
     * Benchmark JSON stream deserialization.
     * Used for large request bodies.
     */
    @Benchmark
    fun jsonStreamDeserialization(blackhole: Blackhole) {
        val json = """{"name":"John Doe","age":30,"email":"john@example.com","tags":["tag1","tag2","tag3"]}"""
        val stream = ByteArrayInputStream(json.toByteArray(StandardCharsets.UTF_8))
        val data: TestData = jsonMapper.fromJsonStream(stream, TestData::class.java)
        blackhole.consume(data)
    }

    /**
     * Benchmark for map serialization - common for dynamic responses.
     */
    @Benchmark
    fun mapSerialization(blackhole: Blackhole) {
        val map: Map<String, Any> = mapOf(
            "status" to "success",
            "count" to 42,
            "items" to listOf("item1", "item2", "item3")
        )

        val json = jsonMapper.toJsonString(map, Map::class.java)
        blackhole.consume(json)
    }

    /**
     * Benchmark for list serialization - common for collection endpoints.
     */
    @Benchmark
    fun listSerialization(blackhole: Blackhole) {
        val list = listOf(
            TestData("User1", 25, "user1@example.com", listOf("a", "b")),
            TestData("User2", 30, "user2@example.com", listOf("c", "d")),
            TestData("User3", 35, "user3@example.com", listOf("e", "f"))
        )

        val json = jsonMapper.toJsonString(list, List::class.java)
        blackhole.consume(json)
    }

    /**
     * Benchmark for nested object serialization.
     */
    @Benchmark
    fun nestedObjectSerialization(blackhole: Blackhole) {
        val nested: Map<String, Any> = mapOf(
            "user" to testData,
            "metadata" to mapOf("timestamp" to System.currentTimeMillis(), "version" to "1.0"),
            "permissions" to listOf("read", "write", "delete")
        )

        val json = jsonMapper.toJsonString(nested, Map::class.java)
        blackhole.consume(json)
    }

    /**
     * Benchmark for small JSON payload (typical for simple responses).
     */
    @Benchmark
    fun smallJsonPayload(blackhole: Blackhole) {
        val simple = mapOf("status" to "ok", "message" to "Success")
        val json = jsonMapper.toJsonString(simple, Map::class.java)
        blackhole.consume(json)
    }

    /**
     * Benchmark for medium JSON payload (typical for single entity responses).
     */
    @Benchmark
    fun mediumJsonPayload(blackhole: Blackhole) {
        val json = jsonMapper.toJsonString(testData, TestData::class.java)
        blackhole.consume(json)
    }

    /**
     * Benchmark for large JSON payload (typical for collection responses).
     */
    @Benchmark
    fun largeJsonPayload(blackhole: Blackhole) {
        val largeList = (0 until 100).map { i ->
            TestData("User$i", 20 + i, "user$i@example.com", listOf("tag1", "tag2"))
        }

        val json = jsonMapper.toJsonString(largeList, List::class.java)
        blackhole.consume(json)
    }
}

