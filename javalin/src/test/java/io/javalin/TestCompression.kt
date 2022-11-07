/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin

import com.nixxcode.jvmbrotli.common.BrotliLoader
import com.nixxcode.jvmbrotli.dec.BrotliInputStream
import io.javalin.compression.Brotli
import io.javalin.compression.CompressionStrategy
import io.javalin.compression.Gzip
import io.javalin.util.FileUtil
import io.javalin.http.Header
import io.javalin.http.staticfiles.Location
import io.javalin.testing.TestDependency
import io.javalin.testing.TestUtil
import kong.unirest.Unirest
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import java.util.zip.GZIPInputStream

class TestCompression {

    private data class SillyObject(var fieldOne: String, var fieldTwo: String, var fieldThree: String)

    private fun getSomeObjects(numberOfObjects: Int) = (1..numberOfObjects).map { i -> SillyObject("f$i", "f$i", "f$i") }.toList()
    private val tinyLength = getSomeObjects(10).toString().length
    private val hugeLength = getSomeObjects(1000).toString().length

    private val testDocument = FileUtil.readResource("/public/html.html")

    private fun customCompressionApp(limit: Int): Javalin = Javalin.create {
        it.pvt.compressionStrategy.minSizeForCompression = limit
        it.staticFiles.add("/public", Location.CLASSPATH)
    }.addTestEndpoints()

    private fun superCompressingApp() = Javalin.create {
        it.compression.custom(CompressionStrategy(Brotli(), Gzip()).apply { minSizeForCompression = 1 })
        it.staticFiles.add("/public", Location.CLASSPATH)
    }.addTestEndpoints()

    private fun brotliDisabledApp() = Javalin.create {
        it.compression.custom(CompressionStrategy(null, Gzip()).apply { minSizeForCompression = testDocument.length })
        it.staticFiles.add("/public", Location.CLASSPATH)
    }.addTestEndpoints()

    private fun etagApp() = Javalin.create {
        it.pvt.compressionStrategy.minSizeForCompression = testDocument.length
        it.staticFiles.add("/public", Location.CLASSPATH)
        it.http.generateEtags = true
    }.addTestEndpoints()

    private fun Javalin.addTestEndpoints() = this.apply {
        get("/huge") { it.result(getSomeObjects(1000).toString()) }
        get("/tiny") { it.result(getSomeObjects(10).toString()) }
    }

    @Test
    fun `doesn't compress when Accept-Encoding is not set`() = TestUtil.test(superCompressingApp()) { _, http ->
        Unirest.get(http.origin + "/huge").header(Header.ACCEPT_ENCODING, "null").asString().let { response -> // dynamic
            assertThat(response.body.length).isEqualTo(hugeLength)
            assertThat(response.headers[Header.CONTENT_ENCODING]).isEmpty()
        }
        Unirest.get(http.origin + "/html.html").header(Header.ACCEPT_ENCODING, "null").asString().let { response -> // static
            assertThat(response.body.length).isEqualTo(testDocument.length)
            assertThat(response.headers[Header.CONTENT_ENCODING]).isEmpty()
        }
    }

    @Test
    fun `doesn't compress when response is too small`() = TestUtil.test(customCompressionApp(tinyLength + 1)) { _, http ->
        Unirest.get(http.origin + "/tiny").header(Header.ACCEPT_ENCODING, "br, gzip").asString().let { response -> // dynamic
            assertThat(response.body.length).isEqualTo(tinyLength)
            assertThat(response.headers[Header.CONTENT_ENCODING]).isEmpty()
        }
        Unirest.get(http.origin + "/html.html").header(Header.ACCEPT_ENCODING, "br, gzip").asString().let { response -> // static
            assertThat(response.body.length).isEqualTo(testDocument.length)
            assertThat(response.headers[Header.CONTENT_ENCODING]).isEmpty()
        }
    }

    @Test
    fun `doesn't compress when compression is disabled`() = TestUtil.test(
        Javalin.create { it.compression.none() }.addTestEndpoints()
    ) { _, http ->
        Unirest.get(http.origin + "/huge").header(Header.ACCEPT_ENCODING, "br, gzip").asString().let { response -> // dynamic
            assertThat(response.body.length).isEqualTo(hugeLength)
            assertThat(response.headers[Header.CONTENT_ENCODING]).isEmpty()
        }
    }

    @Test
    fun `does gzip when Accept-Encoding header is set and size is big enough`() = TestUtil.test(superCompressingApp()) { _, http ->
        getResponse(http.origin, "/huge", "gzip").let { response -> // dynamic
            assertThat(response.headers[Header.CONTENT_ENCODING]).isEqualTo("gzip")
            assertThat(response.body!!.contentLength()).isEqualTo(7740L) // hardcoded because lazy
        }
        getResponse(http.origin, "/html.html", "gzip").let { response -> // static
            assertThat(response.headers[Header.CONTENT_ENCODING]).isEqualTo("gzip")
            assertThat(response.body!!.contentLength()).isBetween(170L, 180L) // hardcoded because lazy
        }
    }

    @Test
    fun `does brotli when Accept-Encoding header is set and size is big enough`() = TestUtil.test(superCompressingApp()) { _, http ->
        assumeTrue(BrotliLoader.isBrotliAvailable())
        getResponse(http.origin, "/huge", "br").let { response -> // dynamic
            assertThat(response.headers[Header.CONTENT_ENCODING]).isEqualTo("br")
            assertThat(response.body!!.contentLength()).isEqualTo(2235L) // hardcoded because lazy
        }
        getResponse(http.origin, "/html.html", "br").let { response -> // static
            assertThat(response.headers[Header.CONTENT_ENCODING]).isEqualTo("br")
            assertThat(response.body!!.contentLength()).isBetween(130L, 150L) // hardcoded because lazy
        }
    }

    @Test
    fun `doesn't gzip when gzip is disabled`() {
        val gzipDisabledApp = Javalin.create {
            it.compression.brotliOnly()
            it.staticFiles.add("/public", Location.CLASSPATH)
        }.addTestEndpoints()
        TestUtil.test(gzipDisabledApp) { _, http ->
            Unirest.get(http.origin + "/huge").header(Header.ACCEPT_ENCODING, "gzip").asString().let { response -> // dynamic
                assertThat(response.body.length).isEqualTo(hugeLength)
                assertThat(response.headers[Header.CONTENT_ENCODING]).isEmpty()
            }
        }
    }

    @Test
    fun `doesn't brotli when brotli is disabled`() = TestUtil.test(brotliDisabledApp()) { _, http ->
        Unirest.get(http.origin + "/huge").header(Header.ACCEPT_ENCODING, "bz").asString().let { response -> // dynamic
            assertThat(response.body.length).isEqualTo(hugeLength)
            assertThat(response.headers[Header.CONTENT_ENCODING]).isEmpty()
        }
    }

    @Test
    fun `chooses brotli when both enabled and supported`() = TestUtil.test(superCompressingApp()) { _, http ->
        assumeTrue(BrotliLoader.isBrotliAvailable())
        getResponse(http.origin, "/huge", "br, gzip").let { response -> // dynamic
            assertThat(response.headers[Header.CONTENT_ENCODING]).isEqualTo("br")
            assertThat(response.body!!.contentLength()).isEqualTo(2235L) // hardcoded because lazy
        }
    }

    @Test
    fun `does gzip when brotli disabled, but both requested`() = TestUtil.test(brotliDisabledApp()) { _, http ->
        getResponse(http.origin, "/huge", "br, gzip").let { response -> // dynamic
            assertThat(response.headers[Header.CONTENT_ENCODING]).isEqualTo("gzip")
            assertThat(response.body!!.contentLength()).isEqualTo(7740L) // hardcoded because lazy
        }
    }

    @Test
    fun `does gzip when CompressionStrategy not set`() = TestUtil.test(Javalin.create().addTestEndpoints()) { _, http ->
        getResponse(http.origin, "/huge", "br, gzip").let { response -> // dynamic
            assertThat(response.headers[Header.CONTENT_ENCODING]).isEqualTo("gzip")
            assertThat(response.body!!.contentLength()).isEqualTo(7740L) // hardcoded because lazy
        }
    }

    @Test
    fun `writes ETag when uncompressed`() = TestUtil.test(etagApp()) { _, http ->
        getResponse(http.origin, "/huge", "null").let { response -> // dynamic
            assertThat(response.headers[Header.CONTENT_ENCODING]).isNull()
            assertThat(response.headers[Header.ETAG]).isNotNull()
        }
        getResponse(http.origin, "/html.html", "null").let { response -> // static
            assertThat(response.headers[Header.CONTENT_ENCODING]).isNull()
            assertThat(response.headers[Header.ETAG]).isNotNull()
        }
    }

    @Test
    fun `writes ETag when compressed`() = TestUtil.test(etagApp()) { _, http ->
        getResponse(http.origin, "/huge", "gzip").let { response -> // dynamic
            assertThat(response.headers[Header.CONTENT_ENCODING]).isEqualTo("gzip")
            assertThat(response.headers[Header.ETAG]).isNotNull()
        }
        getResponse(http.origin, "/html.html", "gzip").let { response -> // static
            assertThat(response.headers[Header.CONTENT_ENCODING]).isEqualTo("gzip")
            assertThat(response.headers[Header.ETAG]).isNotNull()
        }
    }

    @Test
    fun `dynamic handler responds with 304 when ETag is set`() = TestUtil.test(etagApp()) { _, http ->
        val firstRes = getResponse(http.origin, "/huge", "br, gzip")
        val etag = firstRes.headers[Header.ETAG] ?: ""
        val secondRes = getResponseWithEtag(http.origin, "/huge", "gzip", etag)
        assertThat(secondRes.code).isEqualTo(304)
    }

    @Test
    fun `static handler responds with 304 when ETag is set`() = TestUtil.test(etagApp()) { _, http ->
        val firstRes = getResponse(http.origin, "/html.html", "br, gzip")
        val etag = firstRes.headers[Header.ETAG] ?: ""
        val secondRes = getResponseWithEtag(http.origin, "/html.html", "gzip", etag)
        assertThat(secondRes.code).isEqualTo(304)
    }

    @Test
    fun `gzip works for large static files`() {
        val path = "/webjars/swagger-ui/${TestDependency.swaggerVersion}/swagger-ui-bundle.js"
        val gzipWebjars = Javalin.create {
            it.compression.gzipOnly()
            it.staticFiles.enableWebjars()
        }
        TestUtil.test(gzipWebjars) { _, http ->
            assertValidGzipResponse(http.origin, path)
        }
    }

    @Test
    fun `brotli works for large static files`() {
        assumeTrue(BrotliLoader.isBrotliAvailable())
        val path = "/webjars/swagger-ui/${TestDependency.swaggerVersion}/swagger-ui-bundle.js"
        val compressedWebjars = Javalin.create {
            it.compression.brotliOnly()
            it.staticFiles.enableWebjars()
        }
        TestUtil.test(compressedWebjars) { _, http ->
            assertValidBrotliResponse(http.origin, path)
        }
    }

    @Test
    fun `brotli works for dynamic responses of different sizes`() = TestUtil.test(superCompressingApp()) { app, http ->
        assumeTrue(BrotliLoader.isBrotliAvailable())
        listOf(10, 100, 1000, 10_000).forEach { size ->
            app.get("/$size") { it.result(testDocument.repeat(size)) }
            assertValidBrotliResponse(http.origin, "/$size")
        }
    }

    @Test
    fun `gzip works for dynamic responses of different sizes`() = TestUtil.test(superCompressingApp()) { app, http ->
        listOf(10, 100, 1000, 10_000).forEach { size ->
            app.get("/$size") { it.result(testDocument.repeat(size)) }
            assertValidGzipResponse(http.origin, "/$size")
        }
    }

    @Test
    fun `doesn't compress media files`() {
        val mediaTestApp = Javalin.create {
            it.staticFiles.add("/upload-test", Location.CLASSPATH)
        }
        TestUtil.test(mediaTestApp) { _, http ->
            assertUncompressedResponse(http.origin, "/image.png")
            assertUncompressedResponse(http.origin, "/sound.mp3")
        }
    }

    @Test
    fun `doesn't compress pre-compressed files`() {
        val preCompressedTestApp = Javalin.create {
            it.staticFiles.add("/public", Location.CLASSPATH)
            it.staticFiles.enableWebjars()
        }
        TestUtil.test(preCompressedTestApp) { _, http ->
            assertUncompressedResponse(http.origin, "/webjars/swagger-ui/${TestDependency.swaggerVersion}/swagger-ui.js.gz")
            assertUncompressedResponse(http.origin, "/readme.md.br")
        }
    }

    @Test
    fun `doesn't compress when static files were pre-compressed`(){
        val path = "/script.js"
        val gzipWebjars = Javalin.create {
            it.compression.gzipOnly()
            it.staticFiles.enableWebjars()
            it.staticFiles.add{ staticFiles->
                staticFiles.precompress = true
                staticFiles.directory = "/public"
                staticFiles.location = Location.CLASSPATH
            }
            it.pvt.compressionStrategy.minSizeForCompression = 0 // minSize to enable automatic compress
        }
        TestUtil.test(gzipWebjars) { _, http ->
            assertValidGzipResponse(http.origin, path)
        }
    }

    private fun assertUncompressedResponse(origin: String, url: String) {
        val response = getResponse(origin, url, "br, gzip")
        assertThat(response.code).isLessThan(400)
        assertThat(response.header(Header.CONTENT_ENCODING)).isNull()
        val uncompressedResponse = getResponse(origin, url, "null").body!!.string()
        assertThat(response.body!!.string()).isEqualTo(uncompressedResponse)
    }

    private fun assertValidBrotliResponse(origin: String, url: String) {
        val response = getResponse(origin, url, "br")
        assertThat(response.header(Header.CONTENT_ENCODING)).isEqualTo("br")
        val brotliInputStream = BrotliInputStream(response.body!!.byteStream())
        val decompressed = String(brotliInputStream.readBytes())
        val uncompressedResponse = getResponse(origin, url, "null").body!!.string()
        assertThat(decompressed).isEqualTo(uncompressedResponse)
    }

    private fun assertValidGzipResponse(origin: String, url: String) {
        val response = getResponse(origin, url, "gzip")
        assertThat(response.header(Header.CONTENT_ENCODING)).isEqualTo("gzip")
        val gzipInputStream = GZIPInputStream(response.body!!.byteStream())
        val decompressed = String(gzipInputStream.readBytes())
        val uncompressedResponse = getResponse(origin, url, "null").body!!.string()
        assertThat(decompressed).isEqualTo(uncompressedResponse)
    }

    // we need to use okhttp, because unirest omits the content-encoding header
    private fun getResponse(origin: String, url: String, encoding: String) = OkHttpClient()
        .newCall(
            Request.Builder()
                .url(origin + url)
                .header(Header.ACCEPT_ENCODING, encoding)
                .build()
        ).execute()

    // allows passing of multiple headers via string pairs
    private fun getResponseWithEtag(origin: String, url: String, encoding: String, etag: String): Response = OkHttpClient().newCall(
        Request.Builder()
            .url(origin + url)
            .header(Header.ACCEPT_ENCODING, encoding)
            .header(Header.IF_NONE_MATCH, etag)
            .build()
    ).execute()

}
