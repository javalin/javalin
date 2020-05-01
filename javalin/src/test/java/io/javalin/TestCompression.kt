/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin

import com.mashape.unirest.http.Unirest
import com.nixxcode.jvmbrotli.common.BrotliLoader
import com.nixxcode.jvmbrotli.dec.BrotliInputStream
import io.javalin.core.compression.Brotli
import io.javalin.core.compression.Gzip
import io.javalin.core.util.FileUtil
import io.javalin.core.util.Header
import io.javalin.core.util.OptionalDependency
import io.javalin.http.OutputStreamWrapper
import io.javalin.testing.TestUtil
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.assertj.core.api.Assertions.assertThat
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import java.util.zip.GZIPInputStream

class TestCompression {

    private data class SillyObject(var fieldOne: String, var fieldTwo: String, var fieldThree: String)

    private fun getSomeObjects(numberOfObjects: Int) = (1..numberOfObjects).map { i -> SillyObject("f$i", "f$i", "f$i") }.toList()

    private val testDocument = FileUtil.readResource("/public/html.html")

    @Before
    fun reset() {
        OutputStreamWrapper.minSizeForCompression = testDocument.length
    }

    val fullCompressionApp by lazy {
        Javalin.create {
            it.compressionStrategy(Brotli(), Gzip())
            it.addStaticFiles("/public")
        }.addTestEndpoints()
    }

    val brotliDisabledApp by lazy {
        Javalin.create {
            it.compressionStrategy(null, Gzip())
            it.addStaticFiles("/public")
        }.addTestEndpoints()
    }

    val etagApp by lazy {
        Javalin.create {
            it.addStaticFiles("/public")
            it.autogenerateEtags = true
        }.addTestEndpoints()
    }

    fun Javalin.addTestEndpoints() = this.apply {
        get("/huge") { ctx -> ctx.result(getSomeObjects(1000).toString()) }
        get("/tiny") { ctx -> ctx.result(getSomeObjects(10).toString()) }
    }

    private val tinyLength = getSomeObjects(10).toString().length
    private val hugeLength = getSomeObjects(1000).toString().length

    @Test
    fun `doesn't compress when Accepts is not set`() = TestUtil.test(fullCompressionApp) { _, http ->
        assertThat(Unirest.get(http.origin + "/huge").header(Header.ACCEPT_ENCODING, "null").asString().body.length).isEqualTo(hugeLength)
        assertThat(getResponse(http.origin, "/huge", "null").headers().get(Header.CONTENT_ENCODING)).isNull()

        assertThat(Unirest.get(http.origin + "/html.html").header(Header.ACCEPT_ENCODING, "null").asString().body.length).isEqualTo(testDocument.length)
        assertThat(getResponse(http.origin, "/html.html", "null").headers().get(Header.CONTENT_ENCODING)).isNull()
    }

    @Test
    fun `doesn't compress when response is too small`() = TestUtil.test(fullCompressionApp) { _, http ->
        OutputStreamWrapper.minSizeForCompression = tinyLength + 1 // Ensure tiny response length is too short for compression
        assertThat(Unirest.get(http.origin + "/tiny").asString().body.length).isEqualTo(tinyLength)
        assertThat(getResponse(http.origin, "/tiny", "br, gzip").headers().get(Header.CONTENT_ENCODING)).isNull()

        OutputStreamWrapper.minSizeForCompression = testDocument.length + 1 // Ensure static file length is too short for compression
        assertThat(Unirest.get(http.origin + "/html.html").header(Header.ACCEPT_ENCODING, "br, gzip").asString().body.length).isEqualTo(testDocument.length)
        assertThat(getResponse(http.origin, "/html.html", "br, gzip").headers().get(Header.CONTENT_ENCODING)).isNull()
    }

    @Test
    fun `does gzip when size is big and Accept header is set`() = TestUtil.test(fullCompressionApp) { _, http ->
        assertThat(Unirest.get(http.origin + "/huge").asString().body.length).isEqualTo(hugeLength)
        assertThat(getResponse(http.origin, "/huge", "gzip").headers().get(Header.CONTENT_ENCODING)).isEqualTo("gzip")
        assertThat(getResponse(http.origin, "/huge", "gzip").body()!!.contentLength()).isEqualTo(7740L) // hardcoded because lazy

        assertThat(Unirest.get(http.origin + "/html.html").header(Header.ACCEPT_ENCODING, "null").asString().body.length).isEqualTo(testDocument.length)
        assertThat(getResponse(http.origin, "/html.html", "gzip").headers().get(Header.CONTENT_ENCODING)).isEqualTo("gzip")
    }

    @Test
    fun `does brotli when size is big and Accept header is set`() = TestUtil.test(fullCompressionApp) { _, http ->
        assumeTrue(BrotliLoader.isBrotliAvailable())
        assertThat(Unirest.get(http.origin + "/huge").asString().body.length).isEqualTo(hugeLength)
        assertThat(getResponse(http.origin, "/huge", "br").headers().get(Header.CONTENT_ENCODING)).isEqualTo("br")
        assertThat(getResponse(http.origin, "/huge", "br").body()!!.contentLength()).isEqualTo(2235L) // hardcoded because lazy

        assertThat(Unirest.get(http.origin + "/html.html").header(Header.ACCEPT_ENCODING, "null").asString().body.length).isEqualTo(testDocument.length)
        assertThat(getResponse(http.origin, "/html.html", "br").headers().get(Header.CONTENT_ENCODING)).isEqualTo("br")
    }

    @Test
    fun `doesn't gzip when gzip is disabled`() {
        val gzipDisabledApp = Javalin.create {
            it.compressionStrategy(Brotli(), null)
            it.addStaticFiles("/public")
        }.addTestEndpoints()
        TestUtil.test(gzipDisabledApp) { _, http ->
            assertThat(getResponse(http.origin, "/huge", "gzip").headers().get(Header.CONTENT_ENCODING)).isNull()

            assertThat(Unirest.get(http.origin + "/html.html").header(Header.ACCEPT_ENCODING, "gzip").asString().body.length).isEqualTo(testDocument.length)
            assertThat(getResponse(http.origin, "/html.html", "gzip").headers().get(Header.CONTENT_ENCODING)).isNull()
        }
    }

    @Test
    fun `doesn't brotli when brotli is disabled`() = TestUtil.test(brotliDisabledApp) { _, http ->
        assertThat(getResponse(http.origin, "/huge", "br").headers().get(Header.CONTENT_ENCODING)).isNull()

        assertThat(Unirest.get(http.origin + "/html.html").header(Header.ACCEPT_ENCODING, "br").asString().body.length).isEqualTo(testDocument.length)
        assertThat(getResponse(http.origin, "/html.html", "br").headers().get(Header.CONTENT_ENCODING)).isNull()
    }

    @Test
    fun `does brotli when both enabled and supported`() = TestUtil.test(fullCompressionApp) { _, http ->
        assumeTrue(BrotliLoader.isBrotliAvailable())
        val res = getResponse(http.origin, "/huge", "br, gzip")
        assertThat(res.headers().get(Header.CONTENT_ENCODING)).isEqualTo("br")

        assertThat(Unirest.get(http.origin + "/html.html").header(Header.ACCEPT_ENCODING, "null").asString().body.length).isEqualTo(testDocument.length)
        assertThat(getResponse(http.origin, "/html.html", "br, gzip").headers().get(Header.CONTENT_ENCODING)).isEqualTo("br")
    }

    @Test
    fun `does gzip when brotli disabled, but both supported`() = TestUtil.test(brotliDisabledApp) { _, http ->
        assertThat(getResponse(http.origin, "/huge", "br, gzip").headers().get(Header.CONTENT_ENCODING)).isEqualTo("gzip")

        assertThat(Unirest.get(http.origin + "/html.html").header(Header.ACCEPT_ENCODING, "null").asString().body.length).isEqualTo(testDocument.length)
        assertThat(getResponse(http.origin, "/html.html", "br, gzip").headers().get(Header.CONTENT_ENCODING)).isEqualTo("gzip")
    }

    /* Test for backwards compatibility. Ensures that the old dynamicGzip boolean is respected
       when a CompressionStrategy is not set */
    @Test
    fun `does gzip when CompressionStrategy not set`() {
        val defaultApp = Javalin.create {
            it.addStaticFiles("/public")
        }.addTestEndpoints()
        TestUtil.test(defaultApp) { _, http ->
            assertThat(getResponse(http.origin, "/huge", "br, gzip").headers().get(Header.CONTENT_ENCODING)).isEqualTo("gzip")
            assertThat(Unirest.get(http.origin + "/html.html").header(Header.ACCEPT_ENCODING, "null").asString().body.length).isEqualTo(testDocument.length)
            assertThat(getResponse(http.origin, "/html.html", "br, gzip").headers().get(Header.CONTENT_ENCODING)).isEqualTo("gzip")
        }
    }

    @Test
    fun `writes ETag when uncompressed`() = TestUtil.test(etagApp) { _, http ->
        val res = getResponse(http.origin, "/huge", "null")
        val staticRes = getResponse(http.origin, "/html.html", "null")

        assertThat(res.headers().get(Header.CONTENT_ENCODING)).isNull()
        assertThat(res.headers().get(Header.ETAG)).isNotNull()

        assertThat(staticRes.headers().get(Header.CONTENT_ENCODING)).isNull()
        assertThat(staticRes.headers().get(Header.ETAG)).isNotNull()
    }

    @Test
    fun `writes ETag when compressed`() = TestUtil.test(etagApp) { _, http ->
        val res = getResponse(http.origin, "/huge", "br, gzip")
        val staticRes = getResponse(http.origin, "/html.html", "br, gzip")

        assertThat(res.headers().get(Header.CONTENT_ENCODING)).isEqualTo("gzip")
        assertThat(res.headers().get(Header.ETAG)).isNotNull()

        assertThat(staticRes.headers().get(Header.CONTENT_ENCODING)).isEqualTo("gzip")
        assertThat(staticRes.headers().get(Header.ETAG)).isNotNull()
    }

    @Test
    fun `dynamic handler responds with 304 when ETag is set`() = TestUtil.test(etagApp) { _, http ->
        val firstRes = getResponse(http.origin, "/huge", "br, gzip")
        val etag = firstRes.headers().get(Header.ETAG) ?: ""
        val secondRes = getResponseWithMultipleHeaders(http.origin, "/huge",
                Pair(Header.ACCEPT_ENCODING, "br, gzip"),
                Pair(Header.IF_NONE_MATCH, etag))
        assertThat(secondRes.code()).isEqualTo(304)
    }

    @Test
    fun `static handler responds with 304 when ETag is set`() = TestUtil.test(etagApp) { _, http ->
        val firstRes = getResponse(http.origin, "/html.html", "br, gzip")
        val etag = firstRes.headers().get(Header.ETAG) ?: ""
        val secondRes = getResponseWithMultipleHeaders(http.origin, "/html.html",
                Pair(Header.ACCEPT_ENCODING, "br, gzip"),
                Pair(Header.IF_NONE_MATCH, etag))
        assertThat(secondRes.code()).isEqualTo(304)
    }

    @Test
    fun `gzip works for large static files`() {
        val path = "/webjars/swagger-ui/${OptionalDependency.SWAGGERUI.version}/swagger-ui-bundle.js"
        val gzipWebjars = Javalin.create {
            it.compressionStrategy(null, Gzip(6))
            it.enableWebjars()
        }
        TestUtil.test(gzipWebjars) { _, http ->
            assertValidGzipResponse(http.origin, path)
        }
    }

    @Test
    fun `brotli works for large static files`() {
        assumeTrue(BrotliLoader.isBrotliAvailable())
        val path = "/webjars/swagger-ui/${OptionalDependency.SWAGGERUI.version}/swagger-ui-bundle.js"
        val compressedWebjars = Javalin.create {
            it.compressionStrategy(Brotli(4), Gzip(6))
            it.enableWebjars()
        }
        TestUtil.test(compressedWebjars) { _, http ->
            assertValidBrotliResponse(http.origin, path)
        }
    }

    @Test
    fun `brotli works for dynamic responses of different sizes`() {
        assumeTrue(BrotliLoader.isBrotliAvailable())
        val repeats = listOf(10, 100, 1000, 10_000)
        val brotliApp = Javalin.create { it.compressionStrategy(Brotli(4), Gzip(6)) }
        repeats.forEach { n -> brotliApp.get("/$n") { it.result(testDocument.repeat(n)) } }
        TestUtil.test(brotliApp) { _, http ->
            repeats.partition { it < 10_000 }.apply {
                first.forEach { n -> assertValidBrotliResponse(http.origin, "/$n") }
                second.forEach { n -> assertValidGzipResponse(http.origin, "/$n") } // larger than 1mb, fail over to gzip
            }
        }
    }

    @Test
    fun `gzip works for dynamic responses of different sizes`() {
        val repeats = listOf(10, 100, 1000, 10_000)
        val gzipApp = Javalin.create { it.compressionStrategy(null, Gzip(6)) }
        repeats.forEach { n -> gzipApp.get("/$n") { it.result(testDocument.repeat(n)) } }
        TestUtil.test(gzipApp) { _, http ->
            repeats.forEach { n -> assertValidGzipResponse(http.origin, "/$n") }
        }
    }

    @Test
    fun `doesn't compress media files`() {
        val mediaTestApp = Javalin.create {
            it.compressionStrategy(null, Gzip())
            it.addStaticFiles("/upload-test")
        }
        TestUtil.test(mediaTestApp) { _, http ->
            assertUncompressedResponse(http.origin, "/image.png")
            assertUncompressedResponse(http.origin, "/sound.mp3")
        }
    }

    @Test
    fun `doesn't compress pre-compressed files`() {
        val preCompressedTestApp = Javalin.create {
            it.compressionStrategy(null, Gzip())
            it.addStaticFiles("/public")
            it.enableWebjars()
        }
        TestUtil.test(preCompressedTestApp) { _, http ->
            assertUncompressedResponse(http.origin, "/webjars/swagger-ui/${OptionalDependency.SWAGGERUI.version}/swagger-ui.js.gz")
            assertUncompressedResponse(http.origin, "/readme.md.br")
        }
    }

    private fun assertUncompressedResponse(origin: String, url: String) {
        val response = getResponse(origin, url, "br, gzip")
        assertThat(response.code()).isLessThan(400)
        assertThat(response.header(Header.CONTENT_ENCODING)).isNull()
        val uncompressedResponse = getResponse(origin, url, "null").body()!!.string()
        assertThat(response.body()!!.string()).isEqualTo(uncompressedResponse)
    }

    private fun assertValidBrotliResponse(origin: String, url: String) {
        val response = getResponse(origin, url, "br")
        assertThat(response.header(Header.CONTENT_ENCODING)).isEqualTo("br")
        val brotliInputStream = BrotliInputStream(response.body()!!.byteStream())
        val decompressed = String(brotliInputStream.readBytes())
        val uncompressedResponse = getResponse(origin, url, "null").body()!!.string()
        assertThat(decompressed).isEqualTo(uncompressedResponse)
    }

    private fun assertValidGzipResponse(origin: String, url: String) {
        val response = getResponse(origin, url, "gzip")
        assertThat(response.header(Header.CONTENT_ENCODING)).isEqualTo("gzip")
        val gzipInputStream = GZIPInputStream(response.body()!!.byteStream())
        val decompressed = String(gzipInputStream.readBytes())
        val uncompressedResponse = getResponse(origin, url, "null").body()!!.string()
        assertThat(decompressed).isEqualTo(uncompressedResponse)
    }

    // we need to use okhttp, because unirest omits the content-encoding header
    private fun getResponse(origin: String, url: String, encoding: String) = OkHttpClient()
            .newCall(Request.Builder()
                    .url(origin + url)
                    .header(Header.ACCEPT_ENCODING, encoding)
                    .build())
            .execute()

    // allows passing of multiple headers via string pairs
    private fun getResponseWithMultipleHeaders(origin: String, url: String, vararg headers: Pair<String, String>): Response {
        val headBuilder = Headers.Builder()
        for (headerPair in headers) {
            headBuilder.add(headerPair.first, headerPair.second)
        }
        val finalHeaders = headBuilder.build()
        return OkHttpClient().newCall(Request.Builder()
                .url(origin + url)
                .headers(finalHeaders)
                .build())
                .execute()
    }

}
