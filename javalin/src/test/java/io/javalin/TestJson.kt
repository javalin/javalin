/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin

import com.fasterxml.jackson.annotation.JsonInclude
import com.google.gson.GsonBuilder
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import io.javalin.http.Header
import io.javalin.http.HttpStatus.BAD_REQUEST
import io.javalin.http.HttpStatus.INTERNAL_SERVER_ERROR
import io.javalin.http.bodyAsClass
import io.javalin.http.bodyStreamAsClass
import io.javalin.http.bodyValidator
import io.javalin.json.JavalinJackson
import io.javalin.json.JsonMapper
import io.javalin.json.fromJsonString
import io.javalin.json.toJsonString
import io.javalin.testing.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.io.InputStream
import java.lang.reflect.Type
import java.time.Instant

internal class TestJson {

    private val serializableObjectString = fasterJacksonMapper.toJsonString(SerializableObject())

    @Test
    fun `default mapper maps object to json`() = TestUtil.test { app, http ->
        app.get("/") { it.json(SerializableObject()) }
        val response = http.get("/")
        assertThat(response.headers.getFirst(Header.CONTENT_TYPE)).isEqualTo("application/json")
        assertThat(response.body).isEqualTo(serializableObjectString)
    }

    @Test
    fun `default mapper can serialize instant`() = TestUtil.test { app, http ->
        app.get("/") { it.json(Instant.EPOCH) }
        val response = http.get("/")
        assertThat(response.headers.getFirst(Header.CONTENT_TYPE)).isEqualTo("application/json")
        assertThat(response.body).isEqualTo("0.0")
    }

    @Test
    fun `default mapper treats strings as already being json`() = TestUtil.test { app, http ->
        app.get("/") { it.json("ok") }
        assertThat(http.getBody("/")).isEqualTo("ok")
    }

    @Test
    fun `default mapper doesn't deadlock when streaming large objects`() = TestUtil.test { app, http ->
        val big = mapOf("big" to "1".repeat(100_000))
        app.get("/") { it.jsonStream(big) }
        assertThat(http.getBody("/")).isEqualTo(fasterJacksonMapper.toJsonString(big))
    }

    @Test
    fun `default mapper throws when mapping unmappable object to json`() = TestUtil.test { app, http ->
        app.get("/streaming") { it.jsonStream(NonSerializableObject()) }
        http.get("/streaming").let {
            assertThat(it.httpCode()).isEqualTo(INTERNAL_SERVER_ERROR)
            assertThat(it.body).isEqualTo("") // error happens when writing the response, can't recover
        }
        app.get("/string") { it.json(NonSerializableObject()) }
        http.get("/string").let {
            assertThat(it.httpCode()).isEqualTo(INTERNAL_SERVER_ERROR)
            assertThat(it.body).contains(INTERNAL_SERVER_ERROR.message) // error happens when serializing, can recover
        }
    }

    @Test
    fun `default mapper mapper maps json to object`() = TestUtil.test { app, http ->
        app.post("/") { it.result(it.bodyAsClass<SerializableObject>().value1) }
        assertThat(http.post("/").body(serializableObjectString).asString().body).isEqualTo("FirstValue")
    }

    @Test
    fun `default mapper throws when mapping invalid json to class`() = TestUtil.test { app, http ->
        app.get("/") { it.bodyAsClass<NonSerializableObject>() }
        assertThat(http.get("/").httpCode()).isEqualTo(INTERNAL_SERVER_ERROR)
    }

    @Test
    fun `mapping invalid json to class can be handle by validator`() = TestUtil.test { app, http ->
        app.get("/") { it.bodyValidator<NonSerializableObject>().get() }
        assertThat(http.get("/").httpCode()).isEqualTo(BAD_REQUEST)
        assertThat(http.getBody("/")).isEqualTo("""{"REQUEST_BODY":[{"message":"DESERIALIZATION_FAILED","args":{},"value":""}]}""")
    }

    @Test
    fun `empty mapper throws error`() = TestUtil.test(Javalin.create { it.jsonMapper(object : JsonMapper {}) }) { app, http ->
        app.get("/") { it.json("Test") }
        assertThat(http.getBody("/")).contains("")
        assertThat(http.get("/").httpCode()).isEqualTo(INTERNAL_SERVER_ERROR)
    }

    @Test
    fun `empty mapper logs proper error messages`() = TestUtil.test(Javalin.create { it.jsonMapper(object : JsonMapper {}) }) { app, http ->
        var log = TestUtil.captureStdOut { app.get("/write-string") { it.json("") }.also { http.getBody("/write-string") } }
        assertThat(log).contains("JsonMapper#toJsonString not implemented")

        log = TestUtil.captureStdOut { app.get("/write-stream") { it.jsonStream("") }.also { http.getBody("/write-stream") } }
        assertThat(log).contains("JsonMapper#toJsonStream not implemented")

        log = TestUtil.captureStdOut { app.get("/read-string") { it.bodyAsClass<String>() }.also { http.getBody("/read-string") } }
        assertThat(log).contains("JsonMapper#fromJsonString not implemented")

        log = TestUtil.captureStdOut { app.get("/read-stream") { it.bodyStreamAsClass<String>() }.also { http.getBody("/read-stream") } }
        assertThat(log).contains("JsonMapper#fromJsonStream not implemented")
    }


    @Test
    fun `user can configure custom toJsonString`() {
        val sillyMapper = object : JsonMapper {
            override fun toJsonString(obj: Any, type: Type): String = "toJsonString"
        }
        TestUtil.test(Javalin.create { it.jsonMapper(sillyMapper) }) { app, http ->
            app.get("/") { it.json(SerializableObject()) }
            assertThat(http.get("/").body).isEqualTo("toJsonString")
        }
    }

    @Test
    fun `user can configure custom toJsonStream`() {
        val sillyMapper = object : JsonMapper {
            override fun toJsonStream(obj: Any, type: Type): InputStream = "toJsonStream".byteInputStream()
        }
        TestUtil.test(Javalin.create { it.jsonMapper(sillyMapper) }) { app, http ->
            app.get("/") { it.jsonStream(SerializableObject()) }
            assertThat(http.get("/").body).isEqualTo("toJsonStream")
        }
    }

    @Test
    fun `user can configure custom fromJsonString`() {
        val sillyMapper = object : JsonMapper {
            override fun <T : Any> fromJsonString(json: String, targetType: Type): T = "fromJsonString" as T
        }
        TestUtil.test(Javalin.create { it.jsonMapper(sillyMapper) }) { app, http ->
            app.get("/") { it.result(it.bodyAsClass<String>()) }
            assertThat(http.get("/").body).isEqualTo("fromJsonString")
        }
    }

    @Test
    fun `user can configure custom fromJsonStream`() {
        val sillyMapper = object : JsonMapper {
            override fun <T : Any> fromJsonStream(json: InputStream, targetType: Type): T = "fromJsonStream" as T
        }
        TestUtil.test(Javalin.create { it.jsonMapper(sillyMapper) }) { app, http ->
            app.get("/") { it.result(it.bodyStreamAsClass<String>()) }
            assertThat(http.get("/").body).isEqualTo("fromJsonStream")
        }
    }

    @Test
    fun `user can use GSON`() {
        val gson = GsonBuilder()
            .create()

        val gsonMapper = object : JsonMapper {
            override fun <T : Any> fromJsonString(json: String, targetType: Type): T = gson.fromJson(json, targetType)
            override fun toJsonString(obj: Any, type: Type) = gson.toJson(obj, type)
        }

        TestUtil.test(Javalin.create { it.jsonMapper(gsonMapper) }) { app, http ->
            app.get("/") { it.json(SerializableObject()) }
            assertThat(http.getBody("/")).isEqualTo(gson.toJson(SerializableObject()))
            app.post("/") { ctx ->
                ctx.bodyAsClass<SerializableObject>()
                ctx.result("success")
            }
            assertThat(http.post("/").body(gson.toJson(SerializableObject())).asString().body).isEqualTo("success")
        }
    }

    @Test
    fun `user can use Moshi as mapper`() {
        val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()

        val moshiMapper = object : JsonMapper {
            override fun toJsonString(obj: Any, type: Type): String = moshi.adapter<Any>(type).toJson(obj)
            override fun <T : Any> fromJsonString(json: String, targetType: Type): T = moshi.adapter<Any>(targetType).fromJson(json) as T
        }

        val asText = moshiMapper.toJsonString(arrayOf("use", "jackson"))
        val asObject = moshiMapper.fromJsonString<Array<String>>(asText)

        assertThat(asObject).isEqualTo(arrayOf("use", "jackson"))
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
        app.get("/") { it.json(TestClass()) }
        assertThat(http.getBody("/")).isEqualTo("""{"one":null,"two":null}""")
    }

    @Test
    fun `can update ObjectMapper of JavalinJackson`() = TestUtil.test(Javalin.create {
        it.jsonMapper(JavalinJackson().updateMapper {
            it.setSerializationInclusion(JsonInclude.Include.NON_NULL)
        })
    }) { app, http ->
        data class TestClass(val one: String? = null, val two: String? = null)
        app.get("/") { it.json(TestClass()) }
        assertThat(http.getBody("/")).isEqualTo("{}")
    }

}
