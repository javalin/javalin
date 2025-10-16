/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.javalin.config.ValidationConfig
import io.javalin.http.HttpStatus.BAD_REQUEST
import io.javalin.http.HttpStatus.EXPECTATION_FAILED
import io.javalin.http.HttpStatus.INTERNAL_SERVER_ERROR
import io.javalin.http.HttpStatus.OK
import io.javalin.http.bodyValidator
import io.javalin.http.formParamAsClass
import io.javalin.http.pathParamAsClass
import io.javalin.http.queryParamAsClass
import io.javalin.json.JavalinJackson
import io.javalin.json.toJsonString
import io.javalin.testing.SerializableObject
import io.javalin.testing.TestUtil
import io.javalin.testing.fasterJacksonMapper
import io.javalin.testing.httpCode
import io.javalin.validation.MissingConverterException
import io.javalin.validation.NullableValidator
import io.javalin.validation.Params
import io.javalin.validation.Validation
import io.javalin.validation.ValidationError
import io.javalin.validation.ValidationException
import io.javalin.validation.collectErrors
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.text.MessageFormat
import java.time.Duration
import java.time.Instant
import java.util.*

class TestValidation {

    enum class MyEnum { CAT, DOG }

    @Test
    fun `pathParam gives correct error message`() = TestUtil.test { app, http ->
        app.unsafe.routes.get("/{param}") { it.pathParamAsClass<Int>("param").get() }
        assertThat(http.get("/abc").body).isEqualTo("""{"param":[{"message":"TYPE_CONVERSION_FAILED","args":{},"value":"abc"}]}""")
    }

    @Test
    fun `queryParam gives correct error message`() = TestUtil.test { app, http ->
        app.unsafe.routes.get("/") { it.queryParamAsClass<Int>("param").get() }
        assertThat(http.get("/?param=abc").body).isEqualTo("""{"param":[{"message":"TYPE_CONVERSION_FAILED","args":{},"value":"abc"}]}""")
    }

    @Test
    fun `formParam gives correct error message`() = TestUtil.test { app, http ->
        app.unsafe.routes.post("/") { it.formParamAsClass<Int>("param").get() }
        assertThat(http.post("/").body("param=abc").asString().body).isEqualTo("""{"param":[{"message":"TYPE_CONVERSION_FAILED","args":{},"value":"abc"}]}""")
        val log = TestUtil.captureStdOut { http.post("/").body("param=abc").asString().body }
        assertThat(log).contains("Couldn't convert param 'param' with value 'abc' to Integer")

    }

    @Test
    fun `notNullOrEmpty works for Validator`() = TestUtil.test { app, http ->
        app.unsafe.routes.get("/") { it.queryParamAsClass<String>("my-qp").get() }
        assertThat(http.get("/").body).isEqualTo("""{"my-qp":[{"message":"NULLCHECK_FAILED","args":{},"value":null}]}""")
        assertThat(http.get("/").httpCode()).isEqualTo(BAD_REQUEST)
    }

    @Test
    fun `notNullOrEmpty works for NullableValidator`() = TestUtil.test { app, http ->
        app.unsafe.routes.get("/") { it.queryParamAsClass<String>("my-qp").allowNullable().get() }
        assertThat(http.get("/").body).isEqualTo("")
        assertThat(http.get("/").httpCode()).isEqualTo(OK)
    }

    @Test
    fun `getAs clazz works`() = TestUtil.test { app, http ->
        app.unsafe.routes.get("/int") { ctx ->
            val myInt = ctx.queryParamAsClass<Int>("my-qp").get()
            ctx.result((myInt * 2).toString())
        }
        assertThat(http.get("/int").body).isEqualTo("""{"my-qp":[{"message":"NULLCHECK_FAILED","args":{},"value":null}]}""")
        assertThat(http.get("/int?my-qp=abc").body).isEqualTo("""{"my-qp":[{"message":"TYPE_CONVERSION_FAILED","args":{},"value":"abc"}]}""")
        assertThat(http.get("/int?my-qp=123").body).isEqualTo("246")
    }

    @Test
    fun `check works`() = TestUtil.test { app, http ->
        app.unsafe.routes.get("/") { ctx ->
            ctx.queryParamAsClass<String>("my-qp").check({ it.length > 5 }, "Length must be more than five").get()
        }
        assertThat(http.get("/?my-qp=1").body).isEqualTo("""{"my-qp":[{"message":"Length must be more than five","args":{},"value":"1"}]}""")
    }

    @Test
    fun `default query param values work`() = TestUtil.test { app, http ->
        app.unsafe.routes.get("/") { ctx ->
            val myInt = ctx.queryParamAsClass<Int>("my-qp").getOrDefault(788)
            ctx.result(myInt.toString())
        }
        assertThat(http.get("/?my-qp=a").body).isEqualTo("""{"my-qp":[{"message":"TYPE_CONVERSION_FAILED","args":{},"value":"a"}]}""")
        assertThat(http.get("/?my-qp=1").body).isEqualTo("1")
        assertThat(http.get("/").body).isEqualTo("788")
    }

    class CustomException(message: String) : RuntimeException(message)

    @Test
    fun `getOrThrow works`() = TestUtil.test { app, http ->
        app.unsafe.routes.get("/") { ctx ->
            val myInt = ctx.queryParamAsClass<Int>("my-qp").getOrThrow { CustomException("'${it.keys.first()}' is not a number") }
            ctx.result(myInt.toString())
        }.exception(CustomException::class.java) { e, ctx -> ctx.result(e.message ?: "") }
        assertThat(http.get("/").body).isEqualTo("'my-qp' is not a number")
    }

    @Test
    fun `hasValue does not throw exception if value is missing`() = TestUtil.test { app, http ->
        app.unsafe.routes.get("/") { ctx ->
            val hasValue = ctx.queryParamAsClass<Int>("my-qp").hasValue()
            ctx.result("$hasValue")
        }
        assertThat(http.get("/").body).isEqualTo("false")
        assertThat(http.get("/?my-qp=").body).isEqualTo("false")
    }

    @Test
    fun `unregistered converter fails`() = TestUtil.test { app, http ->
        app.unsafe.routes.get("/duration") { it.queryParamAsClass<Duration>("from").get() }
        assertThat(http.get("/duration?from=abc").status).isEqualTo(500)
    }

    val timeModuleMapper by lazy { JavalinJackson(ObjectMapper().apply { registerModule(JavaTimeModule()) }) }

    @Test
    fun `custom converter works`() = TestUtil.test(Javalin.create {
        it.jsonMapper(timeModuleMapper)
        it.validation.register(Instant::class.java) { Instant.ofEpochMilli(it.toLong()) }
    }) { app, http ->
        app.unsafe.routes.get("/instant") { ctx ->
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
    fun `can convert enum`() = TestUtil.test(Javalin.create {
        it.validation.register(MyEnum::class.java) { MyEnum.valueOf(it) }
    }) { app, http ->
        app.unsafe.routes.get("/enum") { ctx ->
            val myEnum = ctx.queryParamAsClass<MyEnum>("my-enum").get()
            ctx.result(myEnum.name)
        }
        assertThat(http.get("/enum?my-enum=CAT").body).isEqualTo("CAT")
        assertThat(http.get("/enum?my-enum=DOG").body).isEqualTo("DOG")
        assertThat(http.get("/enum?my-enum=HORSE").body).isEqualTo("""{"my-enum":[{"message":"TYPE_CONVERSION_FAILED","args":{},"value":"HORSE"}]}""")
    }

    @Test
    fun `custom converter works for null when nullable`() = TestUtil.test(Javalin.create {
        it.validation.register(Instant::class.java) { Instant.ofEpochMilli(it.toLong()) }
    }) { app, http ->
        app.unsafe.routes.get("/instant") { ctx ->
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
    fun `custom converter returns null`() = TestUtil.test(Javalin.create {
        it.validation.register(Instant::class.java) { null }
    }) { app, http ->
        app.unsafe.routes.get("/instant") { it.queryParamAsClass<Instant>("from").get() }
        assertThat(http.get("/instant?from=1262347200000").httpCode()).isEqualTo(BAD_REQUEST)
    }

    @Test
    fun `default converters work`() {
        val validation = Validation(ValidationConfig())
        assertThat(validation.validator("?", Boolean::class.java, "true").get()).hasSameClassAs("true".toBoolean())
        assertThat(validation.validator("?", Double::class.java, "1.2").get()).hasSameClassAs("1.2".toDouble())
        assertThat(validation.validator("?", Float::class.java, "1.2").get()).hasSameClassAs("1.2".toFloat())
        assertThat(validation.validator("?", Int::class.java, "123").get()).hasSameClassAs("123".toInt())
        assertThat(validation.validator("?", Long::class.java, "123").get()).hasSameClassAs("123".toLong())
    }

    @Test
    fun `bodyValidator works`() = TestUtil.test { app, http ->
        app.unsafe.routes.post("/json") { ctx ->
            val obj = ctx.bodyValidator<SerializableObject>()
                .check({ it.value1 == "Bananas" }, "value1 must be 'Bananas'")
                .get()
            ctx.result(obj.value1)
        }
        val invalidJson = fasterJacksonMapper.toJsonString(SerializableObject())
        val validJson = fasterJacksonMapper.toJsonString(SerializableObject().apply {
            value1 = "Bananas"
        })

        """{"REQUEST_BODY":[{"message":"DESERIALIZATION_FAILED","args":{},"value":"not-json"}]}""".let { expected ->
            assertThat(http.post("/json").body("not-json").asString().body).isEqualTo(expected)
        }
        """{"REQUEST_BODY":[{"message":"value1 must be 'Bananas'","args":{},"value":{"value1":"FirstValue","value2":"SecondValue"}}]}""".let { expected ->
            assertThat(http.post("/json").body(invalidJson).asString().body).isEqualTo(expected)
        }

        assertThat(http.post("/json").body(validJson).asString().body).isEqualTo("Bananas")
    }

    @Test
    fun `can use bodyValidator check with ValidationError`() = TestUtil.test { app, http ->
        app.unsafe.routes.post("/json") { ctx ->
            val obj = ctx.bodyValidator<SerializableObject>()
                .check({ it.value1 == "Bananas" }, ValidationError("value1 must be 'Bananas'"))
                .get()
        }
        val invalidJson = fasterJacksonMapper.toJsonString(SerializableObject())
        """{"REQUEST_BODY":[{"message":"value1 must be 'Bananas'","args":{},"value":{"value1":"FirstValue","value2":"SecondValue"}}]}""".let { expected ->
            assertThat(http.post("/json").body(invalidJson).asString().body).isEqualTo(expected)
        }
    }

    @Test
    fun `multiple checks and named fields work when validating class`() = TestUtil.test { app, http ->
        app.unsafe.routes.post("/json") { ctx ->
            val obj = ctx.bodyValidator<SerializableObject>()
                .check({ false }, "UnnamedFieldCheck1")
                .check({ false }, "UnnamedFieldCheck2")
                .check("named_field", { false }, "NamedFieldCheck3")
                .get()
        }
        val expected = """{"REQUEST_BODY":[
            {"message":"UnnamedFieldCheck1","args":{},"value":{"value1":"FirstValue","value2":"SecondValue"}},
            {"message":"UnnamedFieldCheck2","args":{},"value":{"value1":"First Value","value2":"SecondValue"}}],
            "named_field":[{"message":"NamedFieldCheck3","args":{},"value":{"value1":"FirstValue","value2":"SecondValue"}}]}""".replace("\\s".toRegex(), "")
        val response = http.post("/json").body(fasterJacksonMapper.toJsonString(SerializableObject())).asString().body
        assertThat(response).isEqualTo(expected)
    }

    @Test
    fun `custom treatment for ValidationException exception response works`() = TestUtil.test { app, http ->
        app.unsafe.routes.get("/") { ctx ->
            val myString = ctx.queryParamAsClass<String>("my-qp").get()
        }
        app.unsafe.routes.exception(ValidationException::class.java) { e, ctx ->
            ctx.status(EXPECTATION_FAILED)
            ctx.result("Error Expected!")
        }
        assertThat(http.get("/").body).isEqualTo("Error Expected!")
        assertThat(http.get("/").httpCode()).isEqualTo(EXPECTATION_FAILED)
    }

    @Test
    fun `allowNullable throws if called after check`() = TestUtil.test { app, http ->
        app.unsafe.routes.get("/") { it.queryParamAsClass<Int>("my-qp").check({ false }, "Irrelevant").allowNullable() }
        assertThat(http.get("/").httpCode()).isEqualTo(INTERNAL_SERVER_ERROR)
    }

    @Test
    fun `optional query param value works`() = TestUtil.test { app, http ->
        app.unsafe.routes.get("/") { ctx ->
            val myInt: Int? = ctx.queryParamAsClass<Int>("my-qp").allowNullable().get()
            assertThat(myInt).isEqualTo(null)
        }
        assertThat(http.get("/").httpCode()).isEqualTo(OK)
    }

    @Test
    fun `optional query param value with check works`() = TestUtil.test { app, http ->
        app.unsafe.routes.get("/") { ctx ->
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
            assertThat(httpCode()).isEqualTo(OK)
        }

        // Test invalid param
        http.get("/?id=4").apply {
            assertThat(body).isEqualTo("""{"id":[{"message":"id was not greater than 10","args":{},"value":4}]}""")
            assertThat(httpCode()).isEqualTo(BAD_REQUEST)
        }

        // test valid missing param
        http.get("/").apply {
            assertThat(body).isEqualTo("")
            assertThat(httpCode()).isEqualTo(OK)
        }
    }

    @Test
    fun `All errors can be collected from multiple validators`() = TestUtil.test { app, http ->

        app.unsafe.routes.get("/") { ctx ->
            val numberValidator = ctx.queryParamAsClass<Int>("number")
                .check({ it > 12 }, "must be greater than 12.")
                .check({ it.rem(2) == 0 }, "must be even.")

            val stringValidator = ctx.queryParamAsClass<String>("first_name")
                .check({ !it.contains("-") }, "cannot contain hyphens.")
                .check({ it.length < 10 }, "cannot be longer than 10 characters.")

            val nullableValidator = ctx.queryParamAsClass<String>("username")
                .allowNullable()
                .check({ it.isNullOrEmpty() || it != "admin" }, "cannot be admin user.")

            ctx.json(listOf(numberValidator, stringValidator, nullableValidator).collectErrors())
        }

        app.unsafe.routes.post("/") { ctx ->
            val bodyValidator = ctx.bodyValidator<Map<String, String>>()
                .check("first_name", { it.containsKey("first_name") }, "This field is mandatory")

            ctx.json(listOf(bodyValidator).collectErrors())
        }

        http.get("/?number=7&first_name=my-overly-long-first-name&username=admin").apply {
            assertThat(httpCode()).isEqualTo(OK)
            assertThat(body).contains("number", "first_name", "username")
            assertThat(body).contains("must be greater than 12.", "must be even.", "cannot be admin user.")
            assertThat(body).contains("cannot contain hyphens.", "cannot be longer than 10 characters.")
        }

        http.post("/").body("{\"number\":7}").asString().apply {
            assertThat(httpCode()).isEqualTo(OK)
            assertThat(body).isEqualTo("""{"first_name":[{"message":"This field is mandatory","args":{},"value":{"number":7}}]}""")
        }
    }

    @Test
    fun `body validator with check and retrieve errors`() = TestUtil.test { app, http ->
        app.unsafe.routes.post("/") { ctx ->
            val errors = ctx.bodyValidator<Map<String, String>>()
                .check("first_name", { it.containsKey("first_name") }, "This field is mandatory")
                .check("first_name", { (it["first_name"]?.length ?: 0) < 6 }, "Too long")
                .errors()

            ctx.json(errors)
        }

        // Test valid param
        http.post("/").body("{\"first_name\":\"John\"}").asString().apply {
            assertThat(httpCode()).isEqualTo(OK)
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
        app.unsafe.routes.get("/args") { ctx ->
            ctx.queryParamAsClass<Int>("my-qp")
                .check({ it <= 5 }, ValidationError("OVER_LIMIT", args = mapOf("limit" to 5)))
                .get()
        }
        assertThat(http.get("/args?my-qp=10").body).isEqualTo("""{"my-qp":[{"message":"OVER_LIMIT","args":{"limit":5},"value":10}]}""")
    }

    @Test
    fun `localization is easy`() = TestUtil.test { app, http ->
        app.unsafe.routes.get("/") { ctx ->
            ctx.queryParamAsClass<Int>("number")
                .check({ it in 6..9 }, ValidationError("NUMBER_NOT_IN_RANGE", args = mapOf("min" to 6, "max" to 9)))
                .get()
        }
        app.unsafe.routes.exception(ValidationException::class.java) { e, ctx ->
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

    data class KeyValuePair(val key: String, val value: String)

    @Test
    fun `typed value non-nullable validator works for positive case`() {
        val validator = Validation().validator("kvp", KeyValuePair("key", "value"))
        validator.check({ it.key == "key" }, "unexpected key")
        validator.check({ it.value == "value" }, "unexpected value")
        val errors = validator.errors()
        assertThat(errors).isEmpty()
    }

    @Test
    fun `typed value non-nullable validator works for negative case #1`() {
        val validator = Validation().validator("kvp", KeyValuePair("key", "value"))
        validator.check({ it.key != "key" }, "unexpected key")
        validator.check({ it.value == "value" }, "unexpected value")
        val errors = validator.errors()
        assertThat(errors).hasSize(1)
        assertThat(errors["kvp"]).hasSize(1)
    }

    @Test
    fun `typed value non-nullable validator works for negative case #2`() {
        val validator = Validation().validator("kvp", KeyValuePair("key", "value"))
        validator.check({ it.key != "key" }, "unexpected key")
        validator.check({ it.value != "value" }, "unexpected value")
        val errors = validator.errors()
        assertThat(errors).hasSize(1)
        assertThat(errors["kvp"]).hasSize(2)
    }

    @Test
    fun `typed value nullable validator works for positive case`() {
        val validator = Validation().validator("kvp", KeyValuePair("key", "value")).allowNullable()
        validator.check({ it!!.key == "key" }, "unexpected key")
        validator.check({ it!!.value == "value" }, "unexpected value")
        val errors = validator.errors()
        assertThat(errors).isEmpty()
    }

    @Test
    fun `typed value nullable validator works for negative case #1`() {
        val validator = Validation().validator("kvp", KeyValuePair("key", "value")).allowNullable()
        validator.check({ it!!.key != "key" }, "unexpected key")
        validator.check({ it!!.value == "value" }, "unexpected value")
        val errors = validator.errors()
        assertThat(errors).hasSize(1)
        assertThat(errors["kvp"]).hasSize(1)
    }

    @Test
    fun `typed value nullable validator works for negative case #2`() {
        val validator = Validation().validator("kvp", KeyValuePair("key", "value")).allowNullable()
        validator.check({ it!!.key != "key" }, "unexpected key")
        validator.check({ it!!.value != "value" }, "unexpected value")
        val errors = validator.errors()
        assertThat(errors).hasSize(1)
        assertThat(errors["kvp"]).hasSize(2)
    }

    @Test
    fun `typed value nullable validator works for null value`() {
        val validator = NullableValidator(Params("kvp", KeyValuePair::class.java))
        validator.check({ it!!.key == "key" }, "unexpected key")
        validator.check({ it!!.value == "value" }, "unexpected value")
    }

    @Test
    fun `typed value nullable validator constructed from a non-nullable one works for null value`() {
        val validator = Validation().validator("kvp", KeyValuePair("key", "value")).allowNullable()
        validator.check({ it!!.key == "key" }, "unexpected key")
        validator.check({ it!!.value == "value" }, "unexpected value")
    }

    @Test
    fun `can use JavalinValidation#collectErrors to collect errors from multiple Validators`() = TestUtil.test { app, http ->
        val validation = Validation(ValidationConfig())
        app.unsafe.routes.get("/collect-errors") { ctx ->
            val errors = Validation.collectErrors(
                validation.validator("first_name", String::class.java, ctx.queryParam("first_name"))
                    .check({ it.length > 2 }, "too short")
                    .check({ it.length < 10 }, "too long"),
                validation.validator("last_name", String::class.java, ctx.queryParam("last_name"))
                    .check({ it.length > 2 }, "too short")
                    .check({ it.length < 10 }, "too long")
            )
            ctx.json(errors)
        }
        assertThat(http.get("/collect-errors?first_name=1&last_name=2").body).isEqualTo("""{"first_name":[{"message":"too short","args":{},"value":"1"}],"last_name":[{"message":"too short","args":{},"value":"2"}]}""")
    }

    @Test
    fun `throws MissingConverterException if converter is missing`() = TestUtil.test { app, http ->
        app.unsafe.routes.get("/converter") { it.queryParamAsClass<Date>("date") }
        app.unsafe.routes.exception(MissingConverterException::class.java) { e, ctx ->
            ctx.result(e.javaClass.name + ":" + e.className)
        }
        assertThat(http.get("/converter?date=20").body).contains("io.javalin.validation.MissingConverterException:java.util.Date")
    }

    @Test
    fun `can access underlying exception through ValidationError in exception handler`() = TestUtil.test { app, http ->
        app.unsafe.routes.get("/exception") { it.queryParamAsClass<Int>("number").get() }
        app.unsafe.routes.exception(ValidationException::class.java) { e, ctx ->
            ctx.result(e.errors["number"]!!.first().exception()!!.javaClass.name)
        }
        assertThat(http.get("/exception?number=abc").body).isEqualTo("java.lang.NumberFormatException")
    }
}
