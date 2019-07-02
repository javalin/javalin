/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin

import com.mashape.unirest.http.Unirest
import com.sun.xml.internal.fastinfoset.util.StringArray
import io.javalin.core.util.Header
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test


class TestCompression {

    private data class SillyObject(var fieldOne: String, var fieldTwo: String, var fieldThree: String)

    private fun getSomeObjects(numberOfObjects: Int) = (1..numberOfObjects).map { i -> SillyObject("f$i", "f$i", "f$i") }.toList()

    val app = Javalin.create()
            .get("/huge") { ctx -> ctx.result(getSomeObjects(1000).toString()) }
            .get("/tiny") { ctx -> ctx.result(getSomeObjects(10).toString()) }

    val gzipDisabledApp = Javalin.create { it.dynamicGzip = false }
            .get("/huge") { ctx -> ctx.result(getSomeObjects(1000).toString()) }
            .get("/tiny") { ctx -> ctx.result(getSomeObjects(10).toString()) }

    val brotliDisabledApp = Javalin.create { it.dynamicBrotli = false }
            .get("/huge") { ctx -> ctx.result(getSomeObjects(1000).toString()) }
            .get("/tiny") { ctx -> ctx.result(getSomeObjects(10).toString()) }

    private val tinyLength = getSomeObjects(10).toString().length
    private val hugeLength = getSomeObjects(1000).toString().length

    @Test
    fun `doesn't gzip when Accepts is not set`() = TestUtil.test(app) { _, http ->
        assertThat(Unirest.get(http.origin + "/huge").header(Header.ACCEPT_ENCODING, "null").asString().body.length).isEqualTo(hugeLength)
        assertThat(getResponse(http.origin, "/huge", "null").headers().get(Header.CONTENT_ENCODING)).isNull()
    }

    @Test
    fun `doesn't brotli when Accepts is not set`() = TestUtil.test(app) { _, http ->
        assertThat(Unirest.get(http.origin + "/huge").header(Header.ACCEPT_ENCODING, "null").asString().body.length).isEqualTo(hugeLength)
        assertThat(getResponse(http.origin, "/huge", "null").headers().get(Header.CONTENT_ENCODING)).isNull()
    }

    @Test
    fun `doesn't gzip when response is too small`() = TestUtil.test(app) { _, http ->
        assertThat(Unirest.get(http.origin + "/tiny").asString().body.length).isEqualTo(tinyLength)
        assertThat(getResponse(http.origin, "/tiny", "gzip").headers().get(Header.CONTENT_ENCODING)).isNull()
    }

    @Test
    fun `doesn't brotli when response is too small`() = TestUtil.test(app) { _, http ->
        assertThat(Unirest.get(http.origin + "/tiny").asString().body.length).isEqualTo(tinyLength)
        assertThat(getResponse(http.origin, "/tiny", "br").headers().get(Header.CONTENT_ENCODING)).isNull()
    }

    @Test
    fun `does gzip when size is big and Accept header is set`() = TestUtil.test(app) { _, http ->
        assertThat(Unirest.get(http.origin + "/huge").asString().body.length).isEqualTo(hugeLength)
        assertThat(getResponse(http.origin, "/huge", "gzip").headers().get(Header.CONTENT_ENCODING)).isEqualTo("gzip")
        assertThat(getResponse(http.origin, "/huge", "gzip").body()!!.contentLength()).isEqualTo(7740L) // hardcoded because lazy
    }

    @Test
    fun `does brotli when size is big and Accept header is set`() = TestUtil.test(app) { _, http ->
        assertThat(Unirest.get(http.origin + "/huge").asString().body.length).isEqualTo(hugeLength)
        assertThat(getResponse(http.origin, "/huge", "br").headers().get(Header.CONTENT_ENCODING)).isEqualTo("br")
        assertThat(getResponse(http.origin, "/huge", "br").body()!!.contentLength()).isEqualTo(2232L) // hardcoded because lazy
    }

    @Test
    fun `doesn't gzip when gzip is disabled`() = TestUtil.test(gzipDisabledApp) { _, http ->
        assertThat(getResponse(http.origin, "/huge", "gzip").headers().get(Header.CONTENT_ENCODING)).isNull()
    }

    @Test
    fun `doesn't brotli when brotli is disabled`() = TestUtil.test(brotliDisabledApp) { _, http ->
        assertThat(getResponse(http.origin, "/huge", "br").headers().get(Header.CONTENT_ENCODING)).isNull()
    }

    @Test
    fun `does brotli when both brotli and gzip enabled and supported`() = TestUtil.test(app) { _, http ->
        assertThat(getResponse(http.origin, "/huge", "br, gzip").headers().get(Header.CONTENT_ENCODING)).isEqualTo("br")
    }

    @Test
    fun `does gzip when brotli disabled and both supported`() = TestUtil.test(brotliDisabledApp) { _, http ->
        assertThat(getResponse(http.origin, "/huge", "br, gzip").headers().get(Header.CONTENT_ENCODING)).isEqualTo("gzip")
    }

    // we need to use okhttp, because unirest omits the content-encoding header
    private fun getResponse(origin: String, url: String, encoding: String) = OkHttpClient()
            .newCall(Request.Builder()
                    .url(origin + url)
                    .header(Header.ACCEPT_ENCODING, encoding)
                    .build())
            .execute()
}
