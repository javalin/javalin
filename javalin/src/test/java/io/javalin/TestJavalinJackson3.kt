/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin

import com.fasterxml.jackson.annotation.JsonInclude
import io.javalin.http.HttpStatus
import io.javalin.http.bodyStreamAsClass
import io.javalin.json.JavalinJackson3
import io.javalin.json.fromJsonString
import io.javalin.json.toJsonString
import io.javalin.testing.TestUtil
import io.javalin.util.CoreDependency
import io.javalin.util.Util
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkObject
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import kotlin.streams.asStream

internal class TestJavalinJackson3 {

    @Test
    fun `JavalinJackson3 can convert a small Stream to JSON`() {
        TestJsonMapper.convertSmallStreamToJson(JavalinJackson3())
    }

    @Test
    fun `JavalinJackson3 can convert a large Stream to JSON`() {
        TestJsonMapper.convertLargeStreamToJson(JavalinJackson3())
    }

    data class SerializableDataClass(val value1: String = "Default1", val value2: String)

    @Test
    fun `can use JavalinJackson3 with a custom json-mapper on a kotlin data class`() {
        val mapped = JavalinJackson3().toJsonString(SerializableDataClass("First value", "Second value"))
        val mappedBack = JavalinJackson3().fromJsonString<SerializableDataClass>(mapped)
        assertThat("First value").isEqualTo(mappedBack.value1)
        assertThat("Second value").isEqualTo(mappedBack.value2)
    }

    @Test
    fun `default JavalinJackson3 includes nulls`() = TestUtil.test(appWithJackson3()) { app, http ->
        data class TestClass(val one: String? = null, val two: String? = null)
        app.unsafe.routes.get("/") { it.json(TestClass()) }
        assertThat(http.getBody("/")).isEqualTo("""{"one":null,"two":null}""")
    }

    @Test
    fun `can update JsonMapper of JavalinJackson3`() = TestUtil.test(Javalin.create {
        it.jsonMapper(JavalinJackson3().updateMapper { builder ->
            builder.changeDefaultPropertyInclusion { incl -> incl.withValueInclusion(JsonInclude.Include.NON_NULL) }
        })
    }) { app, http ->
        data class TestClass(val one: String? = null, val two: String? = null)
        app.unsafe.routes.get("/") { it.json(TestClass()) }
        assertThat(http.getBody("/")).isEqualTo("{}")
    }

    @Test
    fun `can write a JSON stream with JavalinJackson3`() = TestUtil.test(appWithJackson3()) { app, http ->
        data class Hello(val greet: String, val value: Long)

        var value = 0L
        val take = 100
        val seq = generateSequence { Hello("hi", value++) }
        app.unsafe.routes.get("/json-stream") { it.writeJsonStream(seq.take(take).asStream()) }
        val expectedResponse = List(take) { """{"greet":"hi","value":${it}}""" }.joinToString(",", "[", "]")
        assertThat(http.jsonGet("/json-stream").body).isEqualTo(expectedResponse)
    }

    @Test
    fun `toJsonStream treats Strings as already being json`() = TestUtil.test(appWithJackson3()) { app, http ->
        app.unsafe.routes.get("/") { it.jsonStream("{a:b}") }
        assertThat(http.getBody("/")).isEqualTo("{a:b}")
    }

    @Test
    fun `can convert InputStream to JSON`() = TestUtil.test(appWithJackson3()) { app, http ->
        val expected = SerializableDataClass("First value", "Second value")
        app.unsafe.routes.post("/") { it.json(it.bodyStreamAsClass<SerializableDataClass>()) }
        assertThat(http.post("/").body(expected).asObject(SerializableDataClass::class.java).body).isEqualTo(expected)
    }

    @Test
    fun `can convert JSON to InputStream`() = TestUtil.test(appWithJackson3()) { app, http ->
        data class Hello(val greet: String)
        app.unsafe.routes.get("/") { it.jsonStream(Hello("hi")) }
        assertThat(http.get("/").body).isEqualTo("""{"greet":"hi"}""")
    }

    @Test
    fun `returns internal server error when Jackson3 dependency is not on classpath`() =
        TestUtil.test(appWithJackson3()) { app, http ->
            try {
                mockkObject(Util)
                every { Util.classExists(CoreDependency.JACKSON3.testClass) } returns false
                app.unsafe.routes.get("/") { it.json(mapOf("ok" to true)) }
                assertThat(http.getStatus("/")).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
                assertThat(http.getBody("/")).contains(CoreDependency.JACKSON3.artifactId)
            } finally {
                unmockkObject(Util)
            }
        }

    fun appWithJackson3() = Javalin.create {
        it.jsonMapper(JavalinJackson3())
    }
}
