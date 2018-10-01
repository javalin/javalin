/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin

import com.google.gson.GsonBuilder
import io.javalin.misc.SerializeableObject
import io.javalin.util.TestUtil
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test

class TestJavalinOnContext {

    class MyJson {
        fun render(obj: Any) = GsonBuilder().create().toJson(obj)
    }

    private val extendedJavalin = Javalin.create().apply {
        register(MyJson::class.java, MyJson())
    }

    @Test
    fun `javalin instance is available and extensions work`() = TestUtil.test(extendedJavalin) { app, http ->
        val gson = GsonBuilder().create()
        app.get("/") { ctx ->
            val rendered = ctx.appAttribute(MyJson::class.java).render(SerializeableObject())
            ctx.result(rendered)
        }
        assertThat(http.getBody("/"), `is`(gson.toJson(SerializeableObject())))
    }

}
