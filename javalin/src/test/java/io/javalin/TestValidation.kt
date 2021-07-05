/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.javalin.core.util.JavalinLogger
import io.javalin.core.validation.JavalinValidation
import io.javalin.core.validation.ValidationError
import io.javalin.core.validation.ValidationException
import io.javalin.core.validation.Validator
import io.javalin.core.validation.collectErrors
import io.javalin.plugin.json.JavalinJackson
import io.javalin.testing.SerializableObject
import io.javalin.testing.TestUtil
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.jetty.http.HttpStatus
import org.junit.Test
import java.text.MessageFormat
import java.time.Duration
import java.time.Instant

class TestValidation {

    @Test
    fun `pathParam gives correct error message`() = TestUtil.test { app, http ->
        app.get("/:param") { ctx -> ctx.pathParamAsClass<Int>("param").get() }
        assertThat(http.get("/abc").body).isEqualTo("""{"param":[{"message":"TYPE_CONVERSION_FAILED","args":{},"value":"abc"}]}""")
    }

    @Test
    fun `queryParam gives correct error message`() = TestUtil.test { app, http ->
        app.get("/") { ctx -> ctx.queryParamAsClass<Int>("param").get() }
        assertThat(http.get("/?param=abc").body).isEqualTo("""{"param":[{"message":"TYPE_CONVERSION_FAILED","args":{},"value":"abc"}]}""")
    }

    @Test
    fun `formParam gives correct error message`() = TestUtil.test { app, http ->
        app.post("/") { ctx -> ctx.formParamAsClass<Int>("param").get() }
        assertThat(http.post("/").body("param=abc").asString().body).isEqualTo("""{"param":[{"message":"TYPE_CONVERSION_FAILED","args":{},"value":"abc"}]}""")
        JavalinLogger.enabled = true
        val log = TestUtil.captureStdOut { http.post("/").body("param=abc").asString().body }
        assertThat(log).contains("Parameter 'param' with value 'abc' is not a valid Integer")

    }

    @Test
    fun `notNullOrEmpty works for Validator`() = TestUtil.test { app, http ->
        app.get("/") { ctx -> ctx.queryParamAsClass<String>("my-qp").get() }
        assertThat(http.get("/").body).isEqualTo("""{"my-qp":[{"message":"NULLCHECK_FAILED","args":{},"value":null}]}""")
        assertThat(http.get("/").status).isEqualTo(400)
    }

    @Test
    fun `notNullOrEmpty works for NullableValidator`() = TestUtil.test { app, http ->
        app.get("/") { ctx -> ctx.queryParamAsClass<String>("my-qp").allowNullable().get() }
        assertThat(http.get("/").body).isEqualTo("")
        assertThat(http.get("/").status).isEqualTo(200)
    }

    @Test
    fun `getAs clazz works`() = TestUtil.test { app, http ->
        app.get("/int") { ctx ->
            val myInt = ctx.queryParamAsClass<Int>("my-qp").get()
            ctx.result((myInt * 2).toString())
        }
        assertThat(http.get("/int").body).isEqualTo("""{"my-qp":[{"message":"NULLCHECK_FAILED","args":{},"value":null}]}""")
        assertThat(http.get("/int?my-qp=abc").body).isEqualTo("""{"my-qp":[{"message":"TYPE_CONVERSION_FAILED","args":{},"value":"abc"}]}""")
        assertThat(http.get("/int?my-qp=123").body).isEqualTo("246")
    }

    @Test
    fun `check works`() = TestUtil.test { app, http ->
        app.get("/") { ctx ->
            ctx.queryParamAsClass<String>("my-qp").check({ it.length > 5 }, "Length must be more than five").get()
        }
        assertThat(http.get("/?my-qp=1").body).isEqualTo("""{"my-qp":[{"message":"Length must be more than five","args":{},"value":"1"}]}""")
    }

    @Test
    fun `default query param values work`() = TestUtil.test { app, http ->
        app.get("/") { ctx ->
            val myInt = ctx.queryParamAsClass<Int>("my-qp").getOrDefault(788)
            ctx.result(myInt.toString())
        }
        assertThat(http.get("/?my-qp=a").body).isEqualTo("""{"my-qp":[{"message":"TYPE_CONVERSION_FAILED","args":{},"value":"a"}]}""")
        assertThat(http.get("/?my-qp=1").body).isEqualTo("1")
        assertThat(http.get("/").body).isEqualTo("788")
    }

    @Test
    fun `unregistered converter fails`() = TestUtil.test { app, http ->
        app.get("/duration") { it.queryParamAsClass<Duration>("from").get() }
        assertThat(http.get("/duration?from=abc").status).isEqualTo(500)
    }

    val timeModuleMapper by lazy { JavalinJackson(ObjectMapper().apply { registerModule(JavaTimeModule()) }) }

    @Test
    fun `custom converter works`() = TestUtil.test(Javalin.create { it.jsonMapper(timeModuleMapper) }) { app, http ->
        JavalinValidation.register(Instant::class.java) { Instant.ofEpochMilli(it.toLong()) }
        app.get("/instant") { ctx ->
            val fromDate = ctx.queryParamAsClass<Instant>("from").get()
            val toDate = ctx.queryParamAsClass<Instant>("to")
                .check({ it.isAfter(fromDate) }, "'to' has to be after 'from'")
                .get()
            ctx.json(toDate.isAfter(fromDate))
        }
        assertThat(http.get("/instant?from=1262347200000&to=1262347300000").body).isEqualTo("true")
        assertThat(http.get("/instant?from=1262347200000&to=1262347100000").body).isEqualTo("""{"to":[{"message":"'to' has to be after 'from'","args":{},"value":1262347100.000000000}]}""")
    }

    @Test
    fun `custom converter works for null when nullable`() = TestUtil.test { app, http ->
        JavalinValidation.register(Instant::class.java) { Instant.ofEpochMilli(it.toLong()) }
        app.get("/instant") { ctx ->
            val fromDate = ctx.queryParamAsClass<Instant>("from").get()
            val toDate = ctx.queryParamAsClass<Instant>("to")
                .allowNullable()
                .check({ it == null || it.isAfter(fromDate) }, "'to' has to null or after 'from'")
                .get()
            ctx.json(toDate == null || toDate.isAfter(fromDate))
        }
        assertThat(http.get("/instant?from=1262347200000").body).isEqualTo("true")
        assertThat(http.get("/instant?from=1262347200000&to=1262347300000").body).isEqualTo("true")
    }

    @Test
    fun `custom converter returns null`() = TestUtil.test { app, http ->
        JavalinValidation.register(Instant::class.java) { null }
        app.get("/instant") { it.queryParamAsClass<Instant>("from").get() }
        assertThat(http.get("/instant?from=1262347200000").status).isEqualTo(400)
    }

    @Test
    fun `default converters work`() {
        assertThat(Validator.create(Boolean::class.java, "true", "?").get() is Boolean).isTrue()
        assertThat(Validator.create(Double::class.java, "1.2", "?").get() is Double).isTrue()
        assertThat(Validator.create(Float::class.java, "1.2", "?").get() is Float).isTrue()
        assertThat(Validator.create(Int::class.java, "123", "?").get() is Int).isTrue()
        assertThat(Validator.create(Long::class.java, "123", "?").get() is Long).isTrue()
    }

    @Test
    fun `validatedBody works`() = TestUtil.test { app, http ->
        app.post("/json") { ctx ->
            val obj = ctx.bodyValidator<SerializableObject>()
                .check({ it.value1 == "Bananas" }, "value1 must be 'Bananas'")
                .get()
            ctx.result(obj.value1)
        }
        val invalidJson = JavalinJackson().toJsonString(SerializableObject())
        val validJson = JavalinJackson().toJsonString(SerializableObject().apply {
            value1 = "Bananas"
        })

        """{"SerializableObject":[{"message":"DESERIALIZATION_FAILED","args":{},"value":"not-json"}]}""".let { expected ->
            assertThat(http.post("/json").body("not-json").asString().body).isEqualTo(expected)
        }
        """{"SerializableObject":[{"message":"value1 must be 'Bananas'","args":{},"value":{"value1":"FirstValue","value2":"SecondValue"}}]}""".let { expected ->
            assertThat(http.post("/json").body(invalidJson).asString().body).isEqualTo(expected)
        }

        assertThat(http.post("/json").body(validJson).asString().body).isEqualTo("Bananas")
    }

    @Test
    fun `multiple checks and named fields work when validating class`() = TestUtil.test { app, http ->
        app.post("/json") { ctx ->
            val obj = ctx.bodyValidator<SerializableObject>()
                .check({ false }, "UnnamedFieldCheck1")
                .check({ false }, "UnnamedFieldCheck2")
                .check("named_field", { false }, "NamedFieldCheck3")
                .get()
        }
        val expected = """{"SerializableObject":[
            {"message":"UnnamedFieldCheck1","args":{},"value":{"value1":"FirstValue","value2":"SecondValue"}},
            {"message":"UnnamedFieldCheck2","args":{},"value":{"value1":"First Value","value2":"SecondValue"}}],
            "named_field":[{"message":"NamedFieldCheck3","args":{},"value":{"value1":"FirstValue","value2":"SecondValue"}}]}""".replace("\\s".toRegex(), "")
        val response = http.post("/json").body(JavalinJackson().toJsonString(SerializableObject())).asString().body
        assertThat(response).isEqualTo(expected)
    }

    @Test
    fun `custom treatment for ValidationException exception response works`() = TestUtil.test { app, http ->
        app.get("/") { ctx ->
            val myString = ctx.queryParamAsClass<String>("my-qp").get()
        }
        app.exception(ValidationException::class.java) { e, ctx ->
            ctx.status(HttpStatus.EXPECTATION_FAILED_417)
            ctx.result("Error Expected!")
        }
        assertThat(http.get("/").body).isEqualTo("Error Expected!")
        assertThat(http.get("/").status).isEqualTo(HttpStatus.EXPECTATION_FAILED_417)
    }

    @Test
    fun `allowNullable throws if called after check`() = TestUtil.test { app, http ->
        app.get("/") { it.queryParamAsClass<Int>("my-qp").check({ false }, "Irrelevant").allowNullable() }
        assertThat(http.get("/").status).isEqualTo(500)
    }

    @Test
    fun `optional query param value works`() = TestUtil.test { app, http ->
        app.get("/") { ctx ->
            val myInt: Int? = ctx.queryParamAsClass<Int>("my-qp").allowNullable().get()
            assertThat(myInt).isEqualTo(null)
        }
        assertThat(http.get("/").status).isEqualTo(200)
    }

    @Test
    fun `optional query param value with check works`() = TestUtil.test { app, http ->
        app.get("/") { ctx ->
            val id: Int? = ctx.queryParamAsClass<Int>("id")
                .allowNullable()
                .check({ if (it != null) it > 10 else true }, "id was not greater than 10")
                .get()

            if (id != null) {
                ctx.result(id.toString())
            }
        }

        // Test valid param
        http.get("/?id=20").apply {
            assertThat(body).isEqualTo("20")
            assertThat(status).isEqualTo(200)
        }

        // Test invalid param
        http.get("/?id=4").apply {
            assertThat(body).isEqualTo("""{"id":[{"message":"id was not greater than 10","args":{},"value":4}]}""")
            assertThat(status).isEqualTo(400)
        }

        // test valid missing param
        http.get("/").apply {
            assertThat(body).isEqualTo("")
            assertThat(status).isEqualTo(200)
        }
    }

    @Test
    fun `All errors can be collected from multiple validators`() = TestUtil.test { app, http ->

        app.get("/") { ctx ->
            val numberValidator = ctx.queryParamAsClass<Int>("number")
                .check({ it > 12 }, "must be greater than 12.")
                .check({ it.rem(2) == 0 }, "must be even.")

            val stringValidator = ctx.queryParamAsClass<String>("first_name")
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
            assertThat(body).isEqualTo("""{"first_name":[{"message":"Too long","args":{},"value":{"first_name":"Mathilde"}}]}""")
        }

        // Test invalid empty param
        http.post("/").body("{}").asString().apply {
            assertThat(status).isEqualTo(200)
            assertThat(body).isEqualTo("""{"first_name":[{"message":"This field is mandatory","args":{},"value":{}}]}""")
        }
    }

    @Test
    fun `error args work`() = TestUtil.test { app, http ->
        app.get("/args") { ctx ->
            ctx.queryParamAsClass<Int>("my-qp")
                .check({ it > 5 }, ValidationError("OVER_LIMIT", args = mapOf("limit" to 5)))
                .get()
        }
        assertThat(http.get("/args").body).isEqualTo("""{"my-qp":[{"message":"NULLCHECK_FAILED","args":{},"value":null}]}""")
    }

    @Test
    fun `localization is easy`() = TestUtil.test { app, http ->
        app.get("/") { ctx ->
            ctx.queryParamAsClass<Int>("number")
                .check({ it in 6..9 }, ValidationError("NUMBER_NOT_IN_RANGE", args = mapOf("min" to 6, "max" to 9)))
                .get()
        }
        app.exception(ValidationException::class.java) { e, ctx ->
            val msgBundle = mapOf(
                "NUMBER_NOT_IN_RANGE" to mapOf(
                    "en" to "The value has to at least {0} and at most {1}",
                    "fr" to "La valeur doit au moins {0} et au plus {1}",
                )
            )
            val error = e.errors["number"]!!.first()
            val locale = ctx.queryParam("locale")!!
            val messageTemplate = msgBundle[error.message]!![locale]!!
            ctx.result(MessageFormat.format(messageTemplate, *error.args.values.toTypedArray()))
        }
        assertThat(http.getBody("/?number=20&locale=en")).contains("The value has to at least 6 and at most 9")
        assertThat(http.getBody("/?number=20&locale=fr")).contains("La valeur doit au moins 6 et au plus 9")
    }
}
