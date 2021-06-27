/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin

import com.google.gson.GsonBuilder
import io.javalin.http.context.body
import io.javalin.plugin.json.JavalinJackson
import io.javalin.plugin.json.JsonMapper
import io.javalin.testing.NonSerializableObject
import io.javalin.testing.SerializableObject
import io.javalin.testing.TestUtil
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class TestJson {

    @Test
    fun `default mapper maps object to json`() = TestUtil.test { app, http ->
        app.get("/hello") { ctx -> ctx.json(SerializableObject()) }
        assertThat(http.getBody("/hello")).isEqualTo("""{"value1":"FirstValue","value2":"SecondValue"}""")
    }

    @Test
    fun `default mapper treats strings as already being json`() = TestUtil.test { app, http ->
        app.get("/hello") { ctx -> ctx.json("ok") }
        assertThat(http.getBody("/hello")).isEqualTo("ok")
    }

    @Test
    fun `json-mapper throws when mapping unmappable object to json`() = TestUtil.test { app, http ->
        app.get("/hello") { ctx -> ctx.json(NonSerializableObject()) }
        assertThat(http.get("/hello").status).isEqualTo(500)
        assertThat(http.getBody("/hello")).contains(""""title": "Internal server error"""")
    }

    @Test
    fun `json-mapper maps json to object`() = TestUtil.test { app, http ->
        app.post("/hello") { it.result(it.body<SerializableObject>().value1) }
        val jsonString = JavalinJackson().toJson(SerializableObject())
        assertThat(http.post("/hello").body(jsonString).asString().body).isEqualTo("FirstValue")
    }

    @Test
    fun `invalid json is handled by Validator`() = TestUtil.test { app, http ->
        app.get("/hello") { it.body<NonSerializableObject>() }
        assertThat(http.get("/hello").status).isEqualTo(400)
        val response = http.getBody("/hello").replace("\\s".toRegex(), "")
        assertThat(response).isEqualTo("""{"NonSerializableObject":[{"message":"DESERIALIZATION_FAILED","args":{},"value":""}]}""")
    }

    @Test
    fun `custom silly JSON mapper works`() {
        val sillyMapper = object : JsonMapper {
            override fun <T> fromJson(json: String, targetClass: Class<T>): T = TODO("Not yet implemented")
            override fun toJson(obj: Any): String = "Silly mapper"
        }
        TestUtil.test(Javalin.create { it.jsonMapper(sillyMapper) }) { app, http ->
            app.get("/") { ctx -> ctx.json("Test") }
            assertThat(http.getBody("/")).isEqualTo("Silly mapper")
        }
    }

    @Test
    fun `custom GSON mapper works`() {
        val gson = GsonBuilder().create()
        val gsonMapper = object : JsonMapper {
            override fun <T> fromJson(json: String, targetClass: Class<T>): T = gson.fromJson(json, targetClass)
            override fun toJson(obj: Any) = gson.toJson(obj)
        }
        TestUtil.test(Javalin.create { it.jsonMapper(gsonMapper) }) { app, http ->
            app.get("/") { ctx -> ctx.json(SerializableObject()) }
            assertThat(http.getBody("/")).isEqualTo(gson.toJson(SerializableObject()))
            app.post("/") { ctx ->
                ctx.bodyAsClass(SerializableObject::class.java)
                ctx.result("success")
            }
            assertThat(http.post("/").body(gson.toJson(SerializableObject())).asString().body).isEqualTo("success")
        }
    }

    data class SerializableDataClass(val value1: String = "Default1", val value2: String)

    @Test
    fun `can use JavalinJackson with a custom object-mapper on a kotlin data class`() {
        val mapped = JavalinJackson().toJson(SerializableDataClass("First value", "Second value"))
        val mappedBack = JavalinJackson().fromJson(mapped, SerializableDataClass::class.java)
        assertThat("First value").isEqualTo(mappedBack.value1)
        assertThat("Second value").isEqualTo(mappedBack.value2)
    }

}
