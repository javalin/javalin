package io.javalin

import io.javalin.util.TestUtil.test
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.*
import org.junit.Test

class TestRequestTooLong {

    @Test
    fun `throws exception when request cache is too small for request body` () {
        val javalin = Javalin.create().enableRequestTooLongException().maxBodySizeForRequestCache(10)
        test(javalin) { app, http ->
            app.post("/long-post-body") {ctx ->
                val bodyStringFirstRead = ctx.body()
                val bodyString = ctx.body()
                ctx.result(bodyString)
            }
            app.exception(Exception::class.java) { e, ctx ->
                ctx.result(e.toString())
            }

            val longPostBody = "{\"some_long_string\":\"loooooongteext\"}";
            assertThat(http.post("/long-post-body").body(longPostBody).asString().body, containsString("io.javalin.RequestTooLongResponse") )
        }
    }

    @Test
    fun `do_not_throw_exception_when_request_cache_is_too_small_if_throwing_exception_disabled_(default)` () {
        val javalin = Javalin.create().maxBodySizeForRequestCache(10)
        test(javalin) { app, http ->
            app.post("/long-post-body") {ctx ->
                val bodyStringFirstRead = ctx.body()
                val bodyString = ctx.body()
                ctx.result(bodyString)
            }

            val longPostBody = "{\"some_long_string\":\"loooooongteext\"}";
            assertThat(http.post("/long-post-body").body(longPostBody).asString().body, `is`(""))
        }
    }

}
