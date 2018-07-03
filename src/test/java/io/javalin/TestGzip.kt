/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin

import com.mashape.unirest.http.Unirest
import io.javalin.core.util.Header
import io.javalin.util.TestUtil
import okhttp3.OkHttpClient
import okhttp3.Request
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.nullValue
import org.junit.Test


class TestGzip {

    private data class SillyObject(var fieldOne: String, var fieldTwo: String, var fieldThree: String)

    private fun getSomeObjects(numberOfObjects: Int) = (1..numberOfObjects).map { i -> SillyObject("f$i", "f$i", "f$i") }.toList()

    val app = Javalin.create()
            .get("/huge") { ctx -> ctx.result(getSomeObjects(1000).toString()) }
            .get("/tiny") { ctx -> ctx.result(getSomeObjects(10).toString()) }

    val gzipDisabledApp = Javalin.create().disableDynamicGzip()
            .get("/huge") { ctx -> ctx.result(getSomeObjects(1000).toString()) }
            .get("/tiny") { ctx -> ctx.result(getSomeObjects(10).toString()) }

    private val tinyLength = getSomeObjects(10).toString().length
    private val hugeLength = getSomeObjects(1000).toString().length

    @Test
    fun `doesn't gzip when Accepts is not set`() = TestUtil.test(app) { app, http ->
        assertThat(Unirest.get(http.origin + "/huge").header(Header.ACCEPT_ENCODING, "null").asString().body.length, `is`(hugeLength))
        assertThat(getResponse(http.origin, "/huge", "null").headers().get(Header.CONTENT_ENCODING), `is`(nullValue()))
    }

    @Test
    fun `doesn't gzip when response is too small`() = TestUtil.test(app) { app, http ->
        assertThat(Unirest.get(http.origin + "/tiny").asString().body.length, `is`(tinyLength))
        assertThat(getResponse(http.origin, "/tiny", "gzip").headers().get(Header.CONTENT_ENCODING), `is`(nullValue()))
    }

    @Test
    fun `does gzip when size is big and Accept header is set`() = TestUtil.test(app) { app, http ->
        assertThat(Unirest.get(http.origin + "/huge").asString().body.length, `is`(hugeLength))
        assertThat(getResponse(http.origin, "/huge", "gzip").headers().get(Header.CONTENT_ENCODING), `is`("gzip"))
        assertThat(getResponse(http.origin, "/huge", "gzip").body()!!.contentLength(), `is`(7740L)) // hardcoded because lazy
    }

    @Test
    fun `doesn't gzip when gzip is disabled`() = TestUtil.test(gzipDisabledApp) { app, http ->
        assertThat(getResponse(http.origin, "/huge", "gzip").headers().get(Header.CONTENT_ENCODING), `is`(nullValue()))
    }

    // we need to use okhttp, because unirest omits the content-encoding header
    private fun getResponse(origin: String, url: String, encoding: String) = OkHttpClient()
            .newCall(Request.Builder()
                    .url(origin + url)
                    .header(Header.ACCEPT_ENCODING, encoding)
                    .build()).execute()
}
