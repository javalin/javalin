/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin

import com.mashape.unirest.http.Unirest
import io.javalin.core.compression.Brotli
import io.javalin.core.compression.Gzip
import io.javalin.core.util.FileUtil
import io.javalin.core.util.Header
import io.javalin.core.util.OptionalDependency
import io.javalin.http.OutputStreamWrapper
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.jetty.http.GZIPContentDecoder
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.meteogroup.jbrotli.libloader.BrotliLibraryLoader
import java.nio.ByteBuffer

class TestCompression {

    private data class SillyObject(var fieldOne: String, var fieldTwo: String, var fieldThree: String)

    private fun getSomeObjects(numberOfObjects: Int) = (1..numberOfObjects).map { i -> SillyObject("f$i", "f$i", "f$i") }.toList()

    private val testDocument = FileUtil.readResource("/public/html.html")

    @Before
    fun reset() {
        OutputStreamWrapper.minSizeForCompression = testDocument.length
        OutputStreamWrapper.maxBufferSize = 1000000
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
        assumeTrue(tryLoadBrotli())
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
        assumeTrue(tryLoadBrotli())
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
    fun `dynamic responds with 304 when ETag is set`() = TestUtil.test(etagApp) { _, http ->
        val firstRes = getResponse(http.origin, "/huge", "br, gzip")
        val etag = firstRes.headers().get(Header.ETAG) ?: ""
        val secondRes = getResponseWithMultipleHeaders(http.origin, "/huge",
                Pair(Header.ACCEPT_ENCODING, "br, gzip"),
                Pair(Header.IF_NONE_MATCH, etag))
        assertThat(secondRes.code()).isEqualTo(304)
    }

    @Test
    fun `static responds with 304 when ETag is set`() = TestUtil.test(etagApp) { _, http ->
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
        val path = "/webjars/swagger-ui/${OptionalDependency.SWAGGERUI.version}/swagger-ui-bundle.js"
        val compressedWebjars = Javalin.create {
            it.compressionStrategy(Brotli(4), Gzip(6))
            it.enableWebjars()
        }
        TestUtil.test(compressedWebjars) { _, http ->
            val response = getResponse(http.origin, path, "br, gzip")
            // Jetty times out in SOME environments if body is not consumed, need to find out why
            val brotliBody = response.body()?.bytes()
            assertThat(response.header(Header.CONTENT_ENCODING)).isEqualTo("br")
            // currently, we cannot test if the brotli response matches the uncompressed,
            // so we only check the encoding header. This is because the jbrotli decompressor is broken
            // and doesn't decode the response correctly. However, performing this test in Chrome and
            // manually comparing the response diff shows that they do match
        }
    }

    @Test
    fun `brotli works for large dynamic responses (with failover)`() {
        val brotliEnabled = Javalin.create {
            it.compressionStrategy(Brotli(4), Gzip(6))
        }.get("/just-right") {
            val numRepeats = 1_000_000 / testDocument.length
            it.result(testDocument.repeat(numRepeats))
        }.get("/too-big") {
            val numRepeats = 1_000_000 / testDocument.length
            it.result(testDocument.repeat(numRepeats + 1))
        }
        TestUtil.test(brotliEnabled) { _, http ->
            val response = getResponse(http.origin, "/just-right", "br, gzip")
            assertThat(response.header(Header.CONTENT_ENCODING)).isEqualTo("br")
            val response2 = getResponse(http.origin, "/too-big", "br, gzip")
            assertThat(response2.header(Header.CONTENT_ENCODING)).isEqualTo("gzip")
        }
    }

    @Test
    fun `gzip works for dynamic responses of different sizes`() {
        val repeats = listOf(10, 100, 1000, 10000, 100000)
        val gzipApp = Javalin.create { it.compressionStrategy(null, Gzip(6)) }
        repeats.forEach { n -> gzipApp.get("/$n") { it.result(testDocument.repeat(n)) } }
        TestUtil.test(gzipApp) { _, http ->
            repeats.forEach { n -> assertValidGzipResponse(http.origin, "/$n") }
        }
    }

    @Test
    fun `compression fails over to gzip for large static files`() {
        assumeTrue(tryLoadBrotli())
        val path = "/webjars/swagger-ui/${OptionalDependency.SWAGGERUI.version}/swagger-ui-bundle.js"
        val compressedWebJars = Javalin.create {
            it.compressionStrategy(Brotli(4), Gzip(6)) // brotli must be enabled to test failover
            it.enableWebjars()
        }
        TestUtil.test(compressedWebJars) { _, http ->
            OutputStreamWrapper.maxBufferSize = 50000 // Set buffer size limit lower than the file we're getting, to test failover to gzip
            assertValidGzipResponse(http.origin, path)
        }
    }

    private fun assertValidGzipResponse(origin: String, url: String) {
        val response = getResponse(origin, url, "br, gzip")
        assertThat(response.header(Header.CONTENT_ENCODING)).isEqualTo("gzip")
        val gzipDecoder = GZIPContentDecoder(1024)
        val decoded = gzipDecoder.decode(ByteBuffer.wrap(response.body()?.bytes()))
        val uncompressedResponse = String(getResponse(origin, url, "null").body()?.bytes() ?: ByteArray(0))
        assertThat(String(decoded.array())).isEqualTo(uncompressedResponse)
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

    private fun tryLoadBrotli() = try {
        BrotliLibraryLoader.loadBrotli()
        true
    } catch (t: Throwable) {
        false
    }
}
