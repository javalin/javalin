/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.validation

import io.javalin.BadRequestResponse
import io.javalin.json.JavalinJson
import io.javalin.misc.SerializeableObject
import io.javalin.util.TestUtil
import io.javalin.validation.JavalinValidation.validate
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.jetty.http.HttpStatus
import org.junit.Test
import java.time.Duration
import java.time.Instant

class TestValidation {

    @Test
    fun `test notNullOrBlank()`() = TestUtil.test { app, http ->
        app.get("/") { ctx ->
            ctx.validatedQueryParam("my-qp")
                    .notNullOrEmpty()
                    .getOrThrow()
        }
        assertThat(http.get("/").body).isEqualTo("Query parameter 'my-qp' with value 'null' cannot be null or empty")
        assertThat(http.get("/").status).isEqualTo(400)
    }

    @Test
    fun `test getAs(clazz)`() = TestUtil.test { app, http ->
        app.get("/int") { ctx ->
            val myInt = ctx.validatedQueryParam("my-qp").asInt().getOrThrow()
            ctx.result((myInt * 2).toString())
        }
        assertThat(http.get("/int").body).isEqualTo("Query parameter 'my-qp' with value 'null' cannot be null or empty")
        assertThat(http.get("/int?my-qp=abc").body).isEqualTo("Query parameter 'my-qp' with value 'abc' is not a valid int")
        assertThat(http.get("/int?my-qp=123").body).isEqualTo("246")
    }

    @Test
    fun `test check()`() = TestUtil.test { app, http ->
        app.get("/") { ctx ->
            ctx.validatedQueryParam("my-qp")
                    .check({ it.length > 5 }, "Length must be more than five")
                    .getOrThrow()
        }
        assertThat(http.get("/?my-qp=1").body).isEqualTo("Query parameter 'my-qp' with value '1' invalid - Length must be more than five")
    }

    @Test
    fun `test matches()`() = TestUtil.test { app, http ->
        app.get("/") { ctx ->
            val myLong = ctx.validatedQueryParam("my-qp")
                    .matches("[0-9]")
                    .asLong()
                    .getOrThrow()
            ctx.result(myLong.toString())
        }
        assertThat(http.get("/?my-qp=a").body).isEqualTo("Query parameter 'my-qp' with value 'a' invalid - does not match '[0-9]'")
        assertThat(http.get("/?my-qp=1").body).isEqualTo("1")
    }

    @Test
    fun `test default()`() = TestUtil.test { app, http ->
        app.get("/") { ctx ->
            val myInt = ctx.validatedQueryParam("my-qp", "788")
                    .asInt()
                    .getOrThrow()
            ctx.result(myInt.toString())
        }
        assertThat(http.get("/?my-qp=a").body).isEqualTo("Query parameter 'my-qp' with value 'a' is not a valid int")
        assertThat(http.get("/?my-qp=1").body).isEqualTo("1")
        assertThat(http.get("/").body).isEqualTo("788")
    }

    @Test
    fun `test self-instantiated validator`() {
        try {
            validate(null).notNullOrEmpty().getOrThrow()
        } catch (e: BadRequestResponse) {
            assertThat(e.msg).isEqualTo("Value cannot be null or empty")
            assertThat(e.message).isEqualTo("Value cannot be null or empty")
        }
        try {
            val jsonProp = ""
            validate(jsonProp, "jsonProp").notNullOrEmpty().getOrThrow()
        } catch (e: BadRequestResponse) {
            assertThat(e.msg).isEqualTo("jsonProp cannot be null or empty")
            assertThat(e.message).isEqualTo("jsonProp cannot be null or empty")
        }
    }

    @Test
    fun `test unregistered converter`() = TestUtil.test { app, http ->
        app.get("/duration") { it.validatedQueryParam("from").asClass<Duration>().getOrThrow() }
        assertThat(http.get("/duration?from=abc").status).isEqualTo(500)
    }

    @Test
    fun `test custom converter`() = TestUtil.test { app, http ->
        JavalinValidation.register(Instant::class.java) { Instant.ofEpochMilli(it.toLong()) }
        app.get("/instant") { ctx ->
            val fromDate = ctx.validatedQueryParam("from")
                    .asClass<Instant>()
                    .getOrThrow()
            val toDate = ctx.validatedQueryParam("to")
                    .asClass<Instant>()
                    .check({ it.isAfter(fromDate) }, "'to' has to be after 'from'")
                    .getOrThrow()
            ctx.json(toDate.isAfter(fromDate))
        }
        assertThat(http.get("/instant?from=1262347200000&to=1262347300000").body).isEqualTo("true")
        assertThat(http.get("/instant?from=1262347200000&to=1262347100000").body).isEqualTo("Query parameter 'to' with value '1262347100000' invalid - 'to' has to be after 'from'")
    }

    @Test
    fun `test default converters`() {
        assertThat(validate("true").asBoolean().getOrThrow() is Boolean).isTrue()
        assertThat(validate("1.2").asDouble().getOrThrow() is Double).isTrue()
        assertThat(validate("1.2").asFloat().getOrThrow() is Float).isTrue()
        assertThat(validate("123").asInt().getOrThrow() is Int).isTrue()
        assertThat(validate("123").asLong().getOrThrow() is Long).isTrue()
    }

    @Test
    fun `test validatedBody()`() = TestUtil.test { app, http ->
        app.post("/json") { ctx ->
            val obj = ctx.validatedBody<SerializeableObject>()
                    .check({ it.value1 == "Bananas" }, "value1 must be 'Bananas'")
                    .getOrThrow()
            ctx.result(obj.value1)
        }
        val invalidJson = JavalinJson.toJson(SerializeableObject())
        val validJson = JavalinJson.toJson(SerializeableObject().apply {
            value1 = "Bananas"
        })
        assertThat(http.post("/json").body("not-json").asString().body).isEqualTo("Couldn't deserialize body to SerializeableObject")
        assertThat(http.post("/json").body(invalidJson).asString().body).isEqualTo("Request body as SerializeableObject invalid - value1 must be 'Bananas'")
        assertThat(http.post("/json").body(validJson).asString().body).isEqualTo("Bananas")
    }

    @Test
    fun `test custom treatment for BadRequestResponse exception response`() = TestUtil.test { app, http ->
        app.get("/") { ctx ->
            val myString = ctx.validatedQueryParam("my-qp")
                    .notNullOrEmpty()
                    .getOrThrow()
        }
        app.exception(BadRequestResponse::class.java) { e, ctx ->
            ctx.status(HttpStatus.EXPECTATION_FAILED_417)
            ctx.result("Error Expected!")
        }
        assertThat(http.get("/").body).isEqualTo("Error Expected!")
        assertThat(http.get("/").status).isEqualTo(HttpStatus.EXPECTATION_FAILED_417)
    }

}
