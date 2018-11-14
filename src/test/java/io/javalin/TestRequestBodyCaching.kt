package io.javalin

import io.javalin.util.TestUtil.test
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.*
import org.junit.Test

class TestRequestBodyCaching {

    @Test
    fun `consume_body_several_times_if_request_body_cache_size_is_enought_for_body_size` () {
        val javalin = Javalin.create().maxBodySizeForRequestCache(100)
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
            assertThat(http.post("/long-post-body").body(longPostBody).asString().body, `is`(longPostBody))
        }
    }

    @Test
    fun `throws_exception_when_request_body_cache_is_too_small_and_body_consumed_several_times` () {
        val javalin = Javalin.create().maxBodySizeForRequestCache(10)
        test(javalin) { app, http ->
            app.post("/long-post-body") {ctx ->
                val bodyStringFirstRead = ctx.body()
                val bodyString = ctx.body()
                ctx.result(bodyString)
            }

            val longPostBody = "{\"some_long_string\":\"loooooongteext\"}";
            assertThat(http.post("/long-post-body").body(longPostBody).asString().status, `is`(413))
        }
    }

}
