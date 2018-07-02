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
import io.javalin.util.BaseTest
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.junit.Before
import org.junit.Test

class TestJson : BaseTest() {

    @Before
    fun setObjectMapper() {
        JavalinJackson.configure(CustomMapper()) // reset for every test
    }

    @Test
    fun test_json_jacksonMapsObjectToJson() {
        app.get("/hello") { ctx -> ctx.json(SerializeableObject()) }
        val expected = CustomMapper().writeValueAsString(SerializeableObject())
        assertThat(http.getBody("/hello"), `is`(expected))
    }

    @Test
    fun test_json_jacksonMapsStringsToJson() {
        app.get("/hello") { ctx -> ctx.json("\"ok\"") }
        assertThat(http.getBody("/hello"), `is`("\"\\\"ok\\\"\""))
    }

    @Test
    fun test_json_customMapper_works() {
        app.get("/hello") { ctx -> ctx.json(SerializeableObject()) }
        assertThat(http.getBody("/hello"), `is`(CustomMapper().writeValueAsString(SerializeableObject())))
    }

    @Test
    fun test_json_jackson_throwsForBadObject() {
        app.get("/hello") { ctx -> ctx.json(NonSerializableObject()) }
        assertThat(http.get("/hello").code(), `is`(500))
        assertThat(http.getBody("/hello"), `is`("Internal server error"))
    }

    @Test
    fun test_json_jacksonMapsJsonToObject() {
        app.post("/hello") { ctx ->
            ctx.bodyAsClass(SerializeableObject::class.java)
            ctx.result("success")
        }
        val jsonString = JavalinJackson.toJson(SerializeableObject())
        assertThat(http.post("/hello").body(jsonString).asString().body, `is`("success"))
    }

    @Test
    fun test_json_jacksonMapsJsonToObject_throwsForBadObject() {
        app.get("/hello") { ctx -> ctx.json(ctx.bodyAsClass(NonSerializableObject::class.java).javaClass.simpleName) }
        assertThat(http.get("/hello").code(), `is`(500))
        assertThat(http.getBody("/hello"), `is`("Internal server error"))
    }

    @Test
    fun test_customToJsonMapper_sillyImplementation_works() {
        JavalinJson.toJsonMapper = object : ToJsonMapper {
            override fun map(obj: Any) = "Silly mapper"
        }
        app.get("/") { ctx -> ctx.json("Test") }
        assertThat(http.getBody("/"), `is`("Silly mapper"))
    }

    @Test
    fun test_customToJsonMapper_normalImplementation_works() {
        val gson = GsonBuilder().create()
        JavalinJson.toJsonMapper = object : ToJsonMapper {
            override fun map(obj: Any) = gson.toJson(obj)
        }
        app.get("/") { ctx -> ctx.json(SerializeableObject()) }
        assertThat(http.getBody_withCookies("/"), `is`(gson.toJson(SerializeableObject())))
    }

    @Test
    fun test_customFromJsonMapper_sillyImplementation_works() {
        val sillyString = "Silly string"
        JavalinJson.fromJsonMapper = object : FromJsonMapper {
            override fun <T> map(json: String, targetClass: Class<T>) = sillyString as T
        }
        app.post("/") { ctx ->
            if (sillyString == ctx.bodyAsClass(String::class.java)) {
                ctx.result(sillyString)
            }
        }
        assertThat(Unirest.post("$origin/").body("{}").asString().body, `is`(sillyString))
    }

    @Test
    fun test_customFromJsonMapper_normalImplementation_works() {
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

}
