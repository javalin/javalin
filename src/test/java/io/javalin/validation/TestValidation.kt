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
import org.hamcrest.CoreMatchers.*
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import java.time.Instant

class TestValidation {

    @Test
    fun `test notNullOrBlank()`() = TestUtil.test { app, http ->
        app.get("/") { ctx ->
            val myString = ctx.validatedQueryParam("my-qp")
                    .notNullOrEmpty()
                    .getOrThrow()
        }
        assertThat(http.get("/").body, `is`("Query parameter 'my-qp' with value 'null' invalid - cannot be null or empty"))
        assertThat(http.get("/").status, `is`(400))
    }

    @Test
    fun `test implicit notNullOrBlank()`() = TestUtil.test { app, http ->
        app.get("/") { ctx ->
            val myString = ctx.validatedQueryParam("my-qp")
                    .getOrThrow()
        }
        assertThat(http.get("/").body, `is`("Query parameter 'my-qp' with value 'null' invalid - cannot be null or empty"))
        assertThat(http.get("/").status, `is`(400))
    }

    @Test
    fun `test empty without empty check`() = TestUtil.test { app, http ->
        app.get("/") { ctx ->
            val myString = ctx.validatedQueryParam("my-qp")
                    .getOrThrow()
        }
        assertThat(http.get("/?my-qp=").body, `is`(""))
        assertThat(http.get("/?my-qp=").status, `is`(200))
    }

    @Test
    fun `test getAs(clazz)`() = TestUtil.test { app, http ->
        app.get("/int") { ctx ->
            val myInt = ctx.validatedQueryParam("my-qp").asInt().getOrThrow()
            ctx.result((myInt * 2).toString())
        }
        assertThat(http.get("/int").body, `is`("Query parameter 'my-qp' with value 'null' invalid - cannot be null or empty"))
        assertThat(http.get("/int?my-qp=abc").body, `is`("Query parameter 'my-qp' with value 'abc' invalid - not a valid int"))
        assertThat(http.get("/int?my-qp=123").body, `is`("246"))
    }

    @Test
    fun `test check()`() = TestUtil.test { app, http ->
        app.get("/") { ctx ->
            val myString = ctx.validatedQueryParam("my-qp")
                    .notNullOrEmpty()
                    .check({ it!!.length > 5 }, "Length must be more than five")
                    .getOrThrow()
        }
        assertThat(http.get("/?my-qp=1").body, `is`("Query parameter 'my-qp' with value '1' invalid - Length must be more than five"))
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
        assertThat(http.get("/?my-qp=a").body, `is`("Query parameter 'my-qp' with value 'a' invalid - does not match '[0-9]'"))
        assertThat(http.get("/?my-qp=1").body, `is`("1"))
    }

    @Test
    fun `test default()`() = TestUtil.test { app, http ->
        app.get("/") { ctx ->
            val myInt = ctx.validatedQueryParam("my-qp", "788")
                    .asInt()
                    .getOrThrow()
            ctx.result(myInt.toString())
        }
        assertThat(http.get("/?my-qp=a").body, `is`("Query parameter 'my-qp' with value 'a' invalid - not a valid int"))
        assertThat(http.get("/?my-qp=1").body, `is`("1"))
        assertThat(http.get("/").body, `is`("788"))
    }

    @Test
    fun `test Int getOrDefault()`() = TestUtil.test { app, http ->
        app.get("/") { ctx ->
            val myInt = ctx.validatedQueryParam("my-qp")
                    .asInt()
                    .getOrDefault(788)
            ctx.result(myInt.toString())
        }
        assertThat(http.get("/?my-qp=a").body, `is`("Query parameter 'my-qp' with value 'a' invalid - not a valid int"))
        assertThat(http.get("/?my-qp=1").body, `is`("1"))
        assertThat(http.get("/").body, `is`("788"))
    }

    @Test
    fun `test String getOrDefault()`() = TestUtil.test { app, http ->
        app.get("/") { ctx ->
            val myStr = ctx.validatedQueryParam("my-qp")
                    .getOrDefault("default")
            ctx.result(myStr)
        }
        assertThat(http.get("/?my-qp=1").body, `is`("1"))
        assertThat(http.get("/").body, `is`("default"))
    }

    @Test
    fun `test self-instantiated validator`() = TestUtil.test { app, http ->
        try {
            val myValue = validate(null).notNullOrEmpty().getOrThrow()
        } catch (e: BadRequestResponse) {
            assertThat(e.msg, `is`("Value invalid - cannot be null or empty"))
        }
        try {
            val jsonProp = ""
            val myValue = validate(jsonProp, "jsonProp").notNullOrEmpty().getOrThrow()
        } catch (e: BadRequestResponse) {
            assertThat(e.msg, `is`("jsonProp invalid - cannot be null or empty"))
        }
    }

    @Test
    fun `test custom converter`() = TestUtil.test { app, http ->
        JavalinValidation.register(Instant::class.java) { Instant.ofEpochMilli(it.toLong()) }
        app.get("/instant") { ctx ->
            val fromDate = ctx.validatedQueryParam("from")
                    .asClass<Instant>()
                    .getOrThrow()
            val toDate = ctx.validatedQueryParam("to")
                    .notNullOrEmpty()
                    .asClass<Instant>()
                    .check({ it!!.isAfter(fromDate) }, "'to' has to be after 'from'")
                    .getOrThrow()
            ctx.json(toDate.isAfter(fromDate))
        }
        assertThat(http.get("/instant?from=1262347200000&to=1262347300000").body, `is`("true"))
        assertThat(http.get("/instant?from=1262347200000&to=1262347100000").body, `is`("Query parameter 'to' with value '1262347100000' invalid - 'to' has to be after 'from'"))
    }

    @Test
    fun `test default converters`() = TestUtil.test { app, http ->
        assertThat(validate("true").asBoolean().getOrThrow(), `is`(instanceOf(Boolean::class.java)))
        assertThat(validate("1.2").asDouble().getOrThrow(), `is`(instanceOf(Double::class.java)))
        assertThat(validate("1.2").asFloat().getOrThrow(), `is`(instanceOf(Float::class.java)))
        assertThat(validate("123").asInt().getOrThrow(), `is`(instanceOf(Int::class.java)))
        assertThat(validate("123").asLong().getOrThrow(), `is`(instanceOf(Long::class.java)))
    }

    @Test
    fun `test validatedBody()`() = TestUtil.test { app, http ->
        app.post("/json") { ctx ->
        val obj = ctx.validatedBody<SerializeableObject>()
                .check({ it!!.value1 == "Bananas" }, "value1 must be 'Bananas'")
                .getOrThrow()
            ctx.result(obj.value1)
        }
        val invalidJson = JavalinJson.toJson(SerializeableObject())
        val validJson = JavalinJson.toJson(SerializeableObject().apply {
            value1 = "Bananas"
        })
        assertThat(http.post("/json").body("not-json").asString().body, `is`("Couldn't deserialize body to SerializeableObject"))
        assertThat(http.post("/json").body(invalidJson).asString().body, `is`("Request body as SerializeableObject invalid - value1 must be 'Bananas'"))
        assertThat(http.post("/json").body(validJson).asString().body, `is`("Bananas"))
    }

}
