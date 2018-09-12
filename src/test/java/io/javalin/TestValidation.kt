/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin

import io.javalin.util.TestUtil
import io.javalin.validation.Param
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test

class TestValidation {

    @Test
    fun `test notNullOrBlank()`() = TestUtil.test { app, http ->
        app.get("/") { ctx ->
            val test = ctx.validate(Param.QUERY, "my-qp")
                    .notNullOrBlank()
                    .get()
        }
        assertThat(http.get("/").body, `is`("Query parameter 'my-qp' cannot be null or blank"))
        assertThat(http.get("/").status, `is`(400))
    }

    @Test
    fun `test getAs(clazz)`() = TestUtil.test { app, http ->
        app.get("/int") { ctx ->
            val myInt = ctx.validate(Param.QUERY, "my-qp").getAs<Int>()
            ctx.result((myInt * 2).toString())
        }
        assertThat(http.get("/int").body, `is`("Query parameter 'my-qp' cannot be null or blank"))
        assertThat(http.get("/int?my-qp=abc").body, `is`("Query parameter 'my-qp' is not a valid Integer"))
        assertThat(http.get("/int?my-qp=123").body, `is`("246"))
    }

    @Test
    fun `test check()`() = TestUtil.test { app, http ->
        app.get("/") { ctx ->
            val test = ctx.validate(Param.QUERY, "my-qp")
                    .check({ it.length > 5 }, "Length must be more than five")
                    .get()
        }
        assertThat(http.get("/?my-qp=1").body, `is`("Query parameter 'my-qp' invalid - Length must be more than five"))
    }

}
