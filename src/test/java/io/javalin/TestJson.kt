/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin

import com.google.gson.GsonBuilder
import com.mashape.unirest.http.Unirest
import io.javalin.json.FromJsonMapper
import io.javalin.json.JavalinJackson
import io.javalin.json.JavalinJson
import io.javalin.json.ToJsonMapper
import io.javalin.misc.CustomMapper
import io.javalin.misc.NonSerializableObject
import io.javalin.misc.SerializeableObject
import io.javalin.util.TestUtil
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.junit.Before
import org.junit.Test

class TestJson {

    @Before
    fun resetObjectMapper() = TestUtil.test { app, http ->
        JavalinJackson.configure(CustomMapper()) // reset for every test
    }

    @Test
    fun `json-mapper maps object to json`() = TestUtil.test { app, http ->
        app.get("/hello") { ctx -> ctx.json(SerializeableObject()) }
        assertThat(http.getBody("/hello"), `is`(CustomMapper().writeValueAsString(SerializeableObject())))
    }

    @Test
    fun `json-mapper maps String to json`() = TestUtil.test { app, http ->
        app.get("/hello") { ctx -> ctx.json("\"ok\"") }
        assertThat(http.getBody("/hello"), `is`("\"\\\"ok\\\"\""))
    }

    @Test
    fun `json-mapper throws when mapping unmappable object to json`() = TestUtil.test { app, http ->
        app.get("/hello") { ctx -> ctx.json(NonSerializableObject()) }
        assertThat(http.get("/hello").status, `is`(500))
        assertThat(http.getBody("/hello"), `is`("Internal server error"))
    }

    @Test
    fun `json-mapper maps json to object`() = TestUtil.test { app, http ->
        app.post("/hello") { ctx ->
            ctx.body<SerializeableObject>()
            ctx.result("success")
        }
        val jsonString = JavalinJackson.toJson(SerializeableObject())
        assertThat(http.post("/hello").body(jsonString).asString().body, `is`("success"))
    }

    @Test
    fun `json-mapper throws when mapping json to unmappable object`() = TestUtil.test { app, http ->
        app.get("/hello") { ctx -> ctx.json(ctx.body<NonSerializableObject>().javaClass.simpleName) }
        assertThat(http.get("/hello").status, `is`(500))
        assertThat(http.getBody("/hello"), `is`("Internal server error"))
    }

    @Test
    fun `custom silly toJsonMapper works`() = TestUtil.test { app, http ->
        JavalinJson.toJsonMapper = object : ToJsonMapper {
            override fun map(obj: Any) = "Silly mapper"
        }
        app.get("/") { ctx -> ctx.json("Test") }
        assertThat(http.getBody("/"), `is`("Silly mapper"))
    }

    @Test
    fun `custom gson toJsonMapper mapper works`() = TestUtil.test { app, http ->
        val gson = GsonBuilder().create()
        JavalinJson.toJsonMapper = object : ToJsonMapper {
            override fun map(obj: Any) = gson.toJson(obj)
        }
        app.get("/") { ctx -> ctx.json(SerializeableObject()) }
        assertThat(http.getBody("/"), `is`(gson.toJson(SerializeableObject())))
    }

    @Test
    fun `custom silly fromJsonMapper works`() = TestUtil.test { app, http ->
        val sillyString = "Silly string"
        JavalinJson.fromJsonMapper = object : FromJsonMapper {
            override fun <T> map(json: String, targetClass: Class<T>) = sillyString as T
        }
        app.post("/") { ctx ->
            if (sillyString == ctx.bodyAsClass(String::class.java)) {
                ctx.result(sillyString)
            }
        }
        assertThat(Unirest.post("${http.origin}/").body("{}").asString().body, `is`(sillyString))
    }

    @Test
    fun `custom gson fromJsonMapper works`() = TestUtil.test { app, http ->
        val gson = GsonBuilder().create()
        JavalinJson.fromJsonMapper = object : FromJsonMapper {
            override fun <T> map(json: String, targetClass: Class<T>) = gson.fromJson(json, targetClass)
        }
        app.post("/") { ctx ->
            ctx.bodyAsClass(SerializeableObject::class.java)
            ctx.result("success")
        }
        assertThat(http.post("/").body(gson.toJson(SerializeableObject())).asString().body, `is`("success"))
    }

    @Test
    fun `can use JavalinJson as an object-mapper`() {
        val mapped = JavalinJson.toJson(SerializeableObject())
        val mappedBack = JavalinJson.fromJson(mapped, SerializeableObject::class.java)
        assertThat(SerializeableObject().value1, `is`(mappedBack.value1))
        assertThat(SerializeableObject().value2, `is`(mappedBack.value2))
    }

}
