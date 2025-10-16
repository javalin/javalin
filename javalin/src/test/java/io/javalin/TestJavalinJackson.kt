/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin

import com.fasterxml.jackson.annotation.JsonInclude
import io.javalin.json.JavalinJackson
import io.javalin.json.fromJsonString
import io.javalin.json.toJsonString
import io.javalin.testing.TestUtil
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import kotlin.streams.asStream

internal class TestJavalinJackson {

    @Test
    fun `JavalinJackson can convert a small Stream to JSON`() {
        TestJsonMapper.convertSmallStreamToJson(JavalinJackson())
    }

    @Test
    fun `JavalinJackson can convert a large Stream to JSON`() {
        TestJsonMapper.convertLargeStreamToJson(JavalinJackson())
    }

    data class SerializableDataClass(val value1: String = "Default1", val value2: String)

    @Test
    fun `can use JavalinJackson with a custom object-mapper on a kotlin data class`() {
        val mapped = JavalinJackson().toJsonString(SerializableDataClass("First value", "Second value"))
        val mappedBack = JavalinJackson().fromJsonString<SerializableDataClass>(mapped)
        assertThat("First value").isEqualTo(mappedBack.value1)
        assertThat("Second value").isEqualTo(mappedBack.value2)
    }

    @Test
    fun `default JavalinJackson includes nulls`() = TestUtil.test { app, http ->
        data class TestClass(val one: String? = null, val two: String? = null)
        app.unsafe.routes.get("/") { it.json(TestClass()) }
        assertThat(http.getBody("/")).isEqualTo("""{"one":null,"two":null}""")
    }

    @Test
    fun `can update ObjectMapper of JavalinJackson`() = TestUtil.test(Javalin.create {
        it.jsonMapper(JavalinJackson().updateMapper { mapper ->
            mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL)
        })
    }) { app, http ->
        data class TestClass(val one: String? = null, val two: String? = null)
        app.unsafe.routes.get("/") { it.json(TestClass()) }
        assertThat(http.getBody("/")).isEqualTo("{}")
    }

    @Test
    fun `can write a JSON stream with JavalinJackson`() =
        TestUtil.test(Javalin.create { it.jsonMapper(JavalinJackson()) }) { app, http ->
            data class Hello(val greet: String, val value: Long)

            var value = 0L
            val take = 100
            val seq = generateSequence { Hello("hi", value++) }
            app.unsafe.routes.get("/json-stream") { it.writeJsonStream(seq.take(take).asStream()) }
            val expectedResponse = List(take) { """{"greet":"hi","value":${it}}""" }.joinToString(",", "[", "]")
            assertThat(http.jsonGet("/json-stream").body).isEqualTo(expectedResponse)
        }

    @Test
    fun `toJsonStream treats Strings as already being json`() = TestUtil.test { app, http ->
        app.unsafe.routes.get("/") { it.jsonStream("{a:b}") }
        assertThat(http.getBody("/")).isEqualTo("{a:b}")
    }

}
