/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.javalin.core.validation.JavalinValidation
import io.javalin.core.validation.Validator
import io.javalin.core.validation.collectErrors
import io.javalin.http.BadRequestResponse
import io.javalin.plugin.json.JavalinJson
import io.javalin.testing.SerializeableObject
import io.javalin.testing.TestUtil
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.jetty.http.HttpStatus
import org.junit.Test
import java.time.Duration
import java.time.Instant

class TestValidation {

    @Test
    fun `pathParam gives correct error message`() = TestUtil.test { app, http ->
        app.get("/:param") { ctx -> ctx.pathParam<Int>("param").get() }
        assertThat(http.get("/abc").body).isEqualTo("Path parameter 'param' with value 'abc' is not a valid Integer")
    }

    @Test
    fun `queryParam gives correct error message`() = TestUtil.test { app, http ->
        app.get("/") { ctx -> ctx.queryParam<Int>("param").get() }
        assertThat(http.get("/?param=abc").body).isEqualTo("Query parameter 'param' with value 'abc' is not a valid Integer")
    }

    @Test
    fun `formParam gives correct error message`() = TestUtil.test { app, http ->
        app.post("/") { ctx -> ctx.formParam<Int>("param").get() }
        assertThat(http.post("/").body("param=abc").asString().body).isEqualTo("Form parameter 'param' with value 'abc' is not a valid Integer")
    }

    @Test
    fun `notNullOrEmpty works`() = TestUtil.test { app, http ->
        app.get("/") { ctx -> ctx.queryParam<String>("my-qp").get() }
        assertThat(http.get("/").body).isEqualTo("Query parameter 'my-qp' with value 'null' cannot be null or empty")
        assertThat(http.get("/").status).isEqualTo(400)
    }

    @Test
    fun `getAs clazz works`() = TestUtil.test { app, http ->
        app.get("/int") { ctx ->
            val myInt = ctx.queryParam<Int>("my-qp").get()
            ctx.result((myInt * 2).toString())
        }
        assertThat(http.get("/int").body).isEqualTo("Query parameter 'my-qp' with value 'null' cannot be null or empty")
        assertThat(http.get("/int?my-qp=abc").body).isEqualTo("Query parameter 'my-qp' with value 'abc' is not a valid Integer")
        assertThat(http.get("/int?my-qp=123").body).isEqualTo("246")
    }

    @Test
    fun `check works`() = TestUtil.test { app, http ->
        app.get("/") { ctx ->
            ctx.queryParam<String>("my-qp").check({ it.length > 5 }, "Length must be more than five").get()
        }
        assertThat(http.get("/?my-qp=1").body).isEqualTo("Query parameter 'my-qp' with value '1' invalid - Length must be more than five")
    }

    @Test
    fun `default query param values work`() = TestUtil.test { app, http ->
        app.get("/") { ctx ->
            val myInt = ctx.queryParam<Int>("my-qp", "788").get()
            ctx.result(myInt.toString())
        }
        assertThat(http.get("/?my-qp=a").body).isEqualTo("Query parameter 'my-qp' with value 'a' is not a valid Integer")
        assertThat(http.get("/?my-qp=1").body).isEqualTo("1")
        assertThat(http.get("/").body).isEqualTo("788")
    }

    @Test
    fun `unregistered converter fails`() = TestUtil.test { app, http ->
        app.get("/duration") { it.queryParam<Duration>("from").get() }
        assertThat(http.get("/duration?from=abc").status).isEqualTo(500)
    }

    @Test
    fun `custom converter works`() = TestUtil.test { app, http ->
        JavalinValidation.register(Instant::class.java) { Instant.ofEpochMilli(it.toLong()) }
        app.get("/instant") { ctx ->
            val fromDate = ctx.queryParam<Instant>("from").get()
            val toDate = ctx.queryParam<Instant>("to")
                    .check({ it.isAfter(fromDate) }, "'to' has to be after 'from'")
                    .get()
            ctx.json(toDate.isAfter(fromDate))
        }
        assertThat(http.get("/instant?from=1262347200000&to=1262347300000").body).isEqualTo("true")
        assertThat(http.get("/instant?from=1262347200000&to=1262347100000").body).isEqualTo("Query parameter 'to' with value '1262347100000' invalid - 'to' has to be after 'from'")
    }

    @Test
    fun `custom converter returns null`() = TestUtil.test { app, http ->
        JavalinValidation.register(Instant::class.java) { null }
        app.get("/instant") { it.queryParam<Instant>("from").get() }
        assertThat(http.get("/instant?from=1262347200000").status).isEqualTo(400)
    }

    @Test
    fun `default converters work`() {
        assertThat(Validator.create(Boolean::class.java, "true").get() is Boolean).isTrue()
        assertThat(Validator.create(Double::class.java, "1.2").get() is Double).isTrue()
        assertThat(Validator.create(Float::class.java, "1.2").get() is Float).isTrue()
        assertThat(Validator.create(Int::class.java, "123").get() is Int).isTrue()
        assertThat(Validator.create(Long::class.java, "123").get() is Long).isTrue()
    }

    @Test
    fun `validatedBody works`() = TestUtil.test { app, http ->
        app.post("/json") { ctx ->
            val obj = ctx.bodyValidator<SerializeableObject>()
                    .check({ it.value1 == "Bananas" }, "value1 must be 'Bananas'")
                    .get()
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
    fun `custom treatment for BadRequestResponse exception response works`() = TestUtil.test { app, http ->
        app.get("/") { ctx ->
            val myString = ctx.queryParam<String>("my-qp").get()
        }
        app.exception(BadRequestResponse::class.java) { e, ctx ->
            ctx.status(HttpStatus.EXPECTATION_FAILED_417)
            ctx.result("Error Expected!")
        }
        assertThat(http.get("/").body).isEqualTo("Error Expected!")
        assertThat(http.get("/").status).isEqualTo(HttpStatus.EXPECTATION_FAILED_417)
    }

    @Test
    fun `optional query param value works`() = TestUtil.test { app, http ->
        app.get("/") { ctx ->
            val myInt: Int? = ctx.queryParam<Int>("my-qp").getOrNull()
            assertThat(myInt).isEqualTo(null)
        }
        assertThat(http.get("/").status).isEqualTo(200)
    }

    @Test
    fun `optional query param value with check works`() = TestUtil.test { app, http ->
        app.get("/") { ctx ->
            val id: Int? = ctx.queryParam<Int>("id")
                    .check({ it > 10 }, "id was not greater than 10")
                    .getOrNull()

            if (id != null) {
                ctx.result(id.toString())
            }
        }

        // Test valid param
        http.get("/?id=20").apply {
            assertThat(status).isEqualTo(200)
            assertThat(body).isEqualTo("20")
        }

        // Test invalid param
        http.get("/?id=4").apply {
            assertThat(status).isEqualTo(400)
            assertThat(body).isEqualTo("Query parameter 'id' with value '4' invalid - id was not greater than 10")
        }

        // test valid missing param
        http.get("/").apply {
            assertThat(status).isEqualTo(200)
            assertThat(body).isEqualTo("")
        }
    }

    @Test
    fun `All errors can be collected from multiple validators`() = TestUtil.test { app, http ->

        app.get("/") { ctx ->
            val numberValidator = ctx.queryParam<Int>("number")
                    .check({ it > 12 }, "must be greater than 12.")
                    .check({ it.rem(2) == 0}, "must be even.")

            val stringValidator = ctx.queryParam<String>("first_name")
                    .check({ !it.contains("-") }, "cannot contain hyphens.")
                    .check({ it.length < 10 }, "cannot be longer than 10 characters.")

            ctx.json(listOf(numberValidator, stringValidator).collectErrors())
        }

        http.get("/?number=7&first_name=my-overly-long-first-name").apply {
            assertThat(status).isEqualTo(200)

            val errors = jacksonObjectMapper().readValue<Map<String, List<String>>>(body)

            assertThat(errors).size().isEqualTo(2)
            assertThat(errors.keys).contains("number", "first_name")

            assertThat(errors["number"]).size().isEqualTo(2)
            assertThat(errors["number"]).contains("must be greater than 12.", "must be even.")

            assertThat(errors["first_name"]).size().isEqualTo(2)
            assertThat(errors["first_name"]).contains("cannot contain hyphens.", "cannot be longer than 10 characters.")
        }
    }

    @Test
    fun `body validator with check and retrieve errors`() = TestUtil.test { app, http ->
        app.post("/") { ctx ->
            val errors = ctx.bodyValidator<Map<String, String>>()
                    .check("first_name", { it.containsKey("first_name") }, "This field is mandatory")
                    .check("first_name", { (it["first_name"]?.length ?: 0) < 6 }, "Too long")
                    .errors()

            ctx.json(errors)
        }

        // Test valid param
        http.post("/").body("{\"first_name\":\"John\"}").asString().apply {
            assertThat(status).isEqualTo(200)
            assertThat(body).isEqualTo("{}")
        }

        // Test invalid param
        http.post("/").body("{\"first_name\":\"Mathilde\"}").asString().apply {
            assertThat(status).isEqualTo(200)
            assertThat(body).isEqualTo("{\"first_name\":[\"Too long\"]}")
        }

        // Test invalid empty param
        http.post("/").body("{}").asString().apply {
            assertThat(status).isEqualTo(200)
            assertThat(body).isEqualTo("{\"first_name\":[\"This field is mandatory\"]}")
        }
    }

    @Test
    fun `body validator with check and isValid`() = TestUtil.test { app, http ->
        app.post("/") { ctx ->
            val isValid = ctx.bodyValidator<Map<String, String>>()
                    .check("first_name", { it.containsKey("first_name") }, "This field is mandatory")
                    .check("first_name", { (it["first_name"]?.length ?: 0) < 6 }, "Too long")
                    .isValid()

            ctx.result(isValid.toString())
        }

        // Test valid param
        http.post("/").body("{\"first_name\":\"John\"}").asString().apply {
            assertThat(status).isEqualTo(200)
            assertThat(body).isEqualTo("true")
        }

        // Test invalid param
        http.post("/").body("{\"first_name\":\"Mathilde\"}").asString().apply {
            assertThat(status).isEqualTo(200)
            assertThat(body).isEqualTo("false")
        }

        // Test invalid empty param
        http.post("/").body("{}").asString().apply {
            assertThat(status).isEqualTo(200)
            assertThat(body).isEqualTo("false")
        }
    }

    @Test
    fun `body validator with check and hasError`() = TestUtil.test { app, http ->
        app.post("/") { ctx ->
            val hasError = ctx.bodyValidator<Map<String, String>>()
                    .check("first_name", { it.containsKey("first_name") }, "This field is mandatory")
                    .check("first_name", { (it["first_name"]?.length ?: 0) < 6 }, "Too long")
                    .hasError()

            ctx.result(hasError.toString())
        }

        // Test valid param
        http.post("/").body("{\"first_name\":\"John\"}").asString().apply {
            assertThat(status).isEqualTo(200)
            assertThat(body).isEqualTo("false")
        }

        // Test invalid param
        http.post("/").body("{\"first_name\":\"Mathilde\"}").asString().apply {
            assertThat(status).isEqualTo(200)
            assertThat(body).isEqualTo("true")
        }

        // Test invalid empty param
        http.post("/").body("{}").asString().apply {
            assertThat(status).isEqualTo(200)
            assertThat(body).isEqualTo("true")
        }
    }
}
