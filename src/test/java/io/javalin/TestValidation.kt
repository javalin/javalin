/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin

import io.javalin.util.TestUtil
import io.javalin.validation.Validator
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import java.time.Instant

class TestValidation {

    @Test
    fun `test notNullOrBlank()`() = TestUtil.test { app, http ->
        app.get("/") { ctx ->
            val myString = ctx.validatedQueryParam("my-qp")
                    .notNullOrBlank()
                    .get()
        }
        assertThat(http.get("/").body, `is`("Query parameter 'my-qp' with value 'null' cannot be null or blank"))
        assertThat(http.get("/").status, `is`(400))
    }

    @Test
    fun `test getAs(clazz)`() = TestUtil.test { app, http ->
        app.get("/int") { ctx ->
            val myInt = ctx.validatedQueryParam("my-qp").getAs<Int>()
            ctx.result((myInt * 2).toString())
        }
        assertThat(http.get("/int").body, `is`("Query parameter 'my-qp' with value 'null' cannot be null or blank"))
        assertThat(http.get("/int?my-qp=abc").body, `is`("Query parameter 'my-qp' with value 'abc' is not a valid Integer"))
        assertThat(http.get("/int?my-qp=123").body, `is`("246"))
    }

    @Test
    fun `test check()`() = TestUtil.test { app, http ->
        app.get("/") { ctx ->
            val myString = ctx.validatedQueryParam("my-qp")
                    .check({ it.length > 5 }, "Length must be more than five")
                    .get()
        }
        assertThat(http.get("/?my-qp=1").body, `is`("Query parameter 'my-qp' with value '1' invalid - Length must be more than five"))
    }

    @Test
    fun `test matches()`() = TestUtil.test { app, http ->
        app.get("/") { ctx ->
            val myLong = ctx.validatedQueryParam("my-qp")
                    .matches("[0-9]")
                    .getAs(Long::class.java)
            ctx.result(myLong.toString())
        }
        assertThat(http.get("/?my-qp=a").body, `is`("Query parameter 'my-qp' with value 'a' does not match '[0-9]'"))
        assertThat(http.get("/?my-qp=1").body, `is`("1"))
    }

    @Test
    fun `test self-instantiated validator()`() = TestUtil.test { app, http ->
        try {
            val myValue = Validator(null).notNullOrBlank().get()
        } catch (e: BadRequestResponse) {
            assertThat(e.msg, `is`("Value cannot be null or blank"))
        }
        try {
            val jsonProp = ""
            val myValue = Validator(jsonProp, "jsonProp").notNullOrBlank().get()
        } catch (e: BadRequestResponse) {
            assertThat(e.msg, `is`("jsonProp cannot be null or blank"))
        }
    }

    @Test
    fun `test custom converter()`() = TestUtil.test { app, http ->
        app.get("/instant") { ctx ->
            val myInstant = ctx.validatedQueryParam("my-qp").getAs<Instant>{ Instant.ofEpochMilli(it.toLong()) }
            ctx.json(myInstant)
        }
        assertThat(http.get("/instant?my-qp=1262347200000").body, `is`("""{"nano":0,"epochSecond":1262347200}"""))
    }

}
