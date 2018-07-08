/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 *
 */

package io.javalin

import io.javalin.ApiBuilder.get
import io.javalin.ApiBuilder.path
import io.javalin.util.TestUtil
import okhttp3.OkHttpClient
import okhttp3.Request
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.not
import org.junit.Test
import java.net.URLEncoder

class TestRouting {

    private val okHttp = OkHttpClient().newBuilder().build()
    fun OkHttpClient.getBody(path: String) = this.newCall(Request.Builder().url(path).get().build()).execute().body()!!.string()

    @Test
    fun `general integration test`() = TestUtil.test { app, http ->
        app.get("/") { ctx -> ctx.result("/") }
        app.get("/path") { ctx -> ctx.result("/path") }
        app.get("/path/:path-param") { ctx -> ctx.result("/path/" + ctx.pathParam("path-param")) }
        app.get("/path/:path-param/*") { ctx -> ctx.result("/path/" + ctx.pathParam("path-param") + "/" + ctx.splat(0)) }
        app.get("/*/*") { ctx -> ctx.result("/" + ctx.splat(0) + "/" + ctx.splat(1)) }
        app.get("/*/unreachable") { ctx -> ctx.result("reached") }
        app.get("/*/*/:path-param") { ctx -> ctx.result("/" + ctx.splat(0) + "/" + ctx.splat(1) + "/" + ctx.pathParam("path-param")) }
        app.get("/*/*/:path-param/*") { ctx -> ctx.result("/" + ctx.splat(0) + "/" + ctx.splat(1) + "/" + ctx.pathParam("path-param") + "/" + ctx.splat(2)) }
        assertThat(http.getBody("/"), `is`("/"))
        assertThat(http.getBody("/path"), `is`("/path"))
        assertThat(http.getBody("/path/p"), `is`("/path/p"))
        assertThat(http.getBody("/path/p/s"), `is`("/path/p/s"))
        assertThat(http.getBody("/s1/s2"), `is`("/s1/s2"))
        assertThat(http.getBody("/s/unreachable"), not("reached"))
        assertThat(http.getBody("/s1/s2/p"), `is`("/s1/s2/p"))
        assertThat(http.getBody("/s1/s2/p/s3"), `is`("/s1/s2/p/s3"))
        assertThat(http.getBody("/s/s/s/s"), `is`("/s/s/s/s"))
    }

    @Test
    fun `extracting path-param and splat works`() = TestUtil.test { app, http ->
        app.get("/:path-param/path/*") { ctx -> ctx.result(ctx.pathParam("path-param") + ctx.splat(0)!!) }
        assertThat(http.getBody("/path-param/path/splat"), `is`("path-paramsplat"))
    }

    @Test
    fun `utf-8 encoded path-params work`() = TestUtil.test { app, http ->
        app.get("/:path-param") { ctx -> ctx.result(ctx.pathParam("path-param")) }
        val paramValue = "te/st"
        assertThat(okHttp.getBody(http.origin + "/" + URLEncoder.encode(paramValue, "UTF-8")), `is`(paramValue))
    }

    @Test
    fun `utf-8 encoded splat works`() = TestUtil.test { app, http ->
        app.get("/:path-param/path/*") { ctx -> ctx.result(ctx.pathParam("path-param") + ctx.splat(0)!!) }
        val responseBody = okHttp.getBody(http.origin + "/"
                + URLEncoder.encode("java/kotlin", "UTF-8")
                + "/path/"
                + URLEncoder.encode("/java/kotlin", "UTF-8")
        )
        assertThat(responseBody, `is`("java/kotlin/java/kotlin"))
    }

    @Test
    fun `paths are not case sensitive`() = TestUtil.test { app, http ->
        app.get("/HELLO") { ctx -> ctx.result("Hello") }
        assertThat(http.getBody("/hello"), `is`("Hello"))
    }

    @Test
    fun `path-param keys are transformed to lowercase`() = TestUtil.test { app, http ->
        app.get("/:ParaM") { ctx -> ctx.result(ctx.pathParam("pArAm")) }
        assertThat(http.getBody("/path-param"), `is`("path-param"))
    }

    @Test
    fun `path-param values are transformed to lowercase`() = TestUtil.test { app, http ->
        app.get("/:path-param") { ctx -> ctx.result(ctx.pathParam("path-param")) }
        assertThat(http.getBody("/SomeCamelCasedValue"), `is`("somecamelcasedvalue"))
    }

    @Test
    fun `path regex works`() = TestUtil.test { app, http ->
        app.get("/:path-param/[0-9]+/") { ctx -> ctx.result(ctx.pathParam("path-param")) }
        assertThat(http.getBody("/test/pathParam"), `is`("Not found"))
        assertThat(http.getBody("/test/21"), `is`("test"))
    }

    @Test
    fun `automatic slash prefixing works`() = TestUtil.test { app, http ->
        app.routes {
            path("test") {
                path(":id") { get { ctx -> ctx.result(ctx.pathParam("id")) } }
                get { ctx -> ctx.result("test") }
            }
        }
        assertThat(http.getBody("/test/path-param/"), `is`("path-param"))
        assertThat(http.getBody("/test/"), `is`("test"))
    }

}
