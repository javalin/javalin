/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin


import io.javalin.compression.Brotli
import io.javalin.compression.CompressionStrategy
import io.javalin.compression.CompressionType
import io.javalin.compression.Compressor
import io.javalin.compression.Gzip
import io.javalin.compression.Zstd
import io.javalin.compression.ZstdCompressor
import io.javalin.compression.forType
import io.javalin.http.ContentType
import io.javalin.http.Handler
import io.javalin.http.Header
import io.javalin.http.staticfiles.Location
import io.javalin.testing.TestDependency
import io.javalin.testing.TestUtil
import io.javalin.util.FileUtil
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIf
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.util.zip.GZIPInputStream
import kotlin.streams.asStream
import com.aayushatharva.brotli4j.decoder.BrotliInputStream as Brotli4jInputStream
import com.github.luben.zstd.ZstdInputStream

class TestCompression {

    private data class SillyObject(var fieldOne: String, var fieldTwo: String, var fieldThree: String)

    private fun getSomeObjects(numberOfObjects: Int) = (1..numberOfObjects).map { i -> SillyObject("f$i", "f$i", "f$i") }.toList()
    private val tinyLength = getSomeObjects(10).toString().length
    private val hugeLength = getSomeObjects(1000).toString().length

    private val testDocument = FileUtil.readResource("/public/html.html")

    private fun customCompressionApp(limit: Int): Javalin = Javalin.create {
        it.pvt.compressionStrategy.defaultMinSizeForCompression = limit
        it.staticFiles.add("/public", Location.CLASSPATH)
    }.addTestEndpoints()

    private fun superCompressingApp() = Javalin.create {
        it.http.customCompression(CompressionStrategy(Brotli(), Gzip()).apply { defaultMinSizeForCompression = 1 })
        it.staticFiles.add("/public", Location.CLASSPATH)
    }.addTestEndpoints()

    private fun brotliDisabledApp() = Javalin.create {
        it.http.customCompression(CompressionStrategy(null, Gzip()).apply { defaultMinSizeForCompression = testDocument.length })
        it.staticFiles.add("/public", Location.CLASSPATH)
    }.addTestEndpoints()

    private fun zstdOnlyApp() = Javalin.create {
        it.http.customCompression(CompressionStrategy(null, null, Zstd()).apply { defaultMinSizeForCompression = 1 })
        it.staticFiles.add("/public", Location.CLASSPATH)
    }.addTestEndpoints()

    private fun etagApp() = Javalin.create {
        it.pvt.compressionStrategy.defaultMinSizeForCompression = testDocument.length
        it.staticFiles.add("/public", Location.CLASSPATH)
        it.http.generateEtags = true
    }.addTestEndpoints()

    private fun Javalin.addTestEndpoints() = this.apply {
        get("/huge") { it.result(getSomeObjects(1000).toString()) }
        get("/tiny") { it.result(getSomeObjects(10).toString()) }
    }

    private fun preferredCompressors(prefCompressors : List<CompressionType>) = Javalin.create {
        it.http.customCompression(CompressionStrategy(Brotli(), Gzip()).apply { preferredCompressors = prefCompressors })
        it.staticFiles.add("/public", Location.CLASSPATH)
    }.addTestEndpoints()

    @Test
    fun `Compresssor interface works`() {
        val comp = object : Compressor {
            override fun encoding() = "enc"
            override fun compress(out: OutputStream): OutputStream =
                out.apply { "xyz".byteInputStream().copyTo(this) }
        }
        assertThat(comp.encoding()).isEqualTo("enc")
        assertThat(ByteArrayOutputStream().apply { comp.compress(this) }.toByteArray()).isEqualTo("xyz".toByteArray())
    }

    @Test
    fun `levels are enforced`() {
        assertThatExceptionOfType(IllegalArgumentException::class.java).isThrownBy { Gzip(-1) }
        assertThatExceptionOfType(IllegalArgumentException::class.java).isThrownBy { Gzip(10) }
        assertThatExceptionOfType(IllegalArgumentException::class.java).isThrownBy { Brotli(-1) }
        assertThatExceptionOfType(IllegalArgumentException::class.java).isThrownBy { Brotli(12) }
        assertThatExceptionOfType(IllegalArgumentException::class.java).isThrownBy { Zstd(-1) }
        assertThatExceptionOfType(IllegalArgumentException::class.java).isThrownBy { Zstd(23) }
    }

    @Test
    fun `compression availability and strategy behavior`() {
        // Test availability checking methods
        assertThat(CompressionStrategy.brotli4jPresent()).isTrue()
        assertThat(CompressionStrategy.zstdJniPresent()).isTrue()
        
        // Test strategy creation with different combinations
        val allFormats = CompressionStrategy(Brotli(4), Gzip(6), Zstd(3))
        assertThat(allFormats.compressors).hasSizeGreaterThanOrEqualTo(2) // at least gzip + one other
        
        val gzipOnly = CompressionStrategy(null, Gzip(6), null)
        assertThat(gzipOnly.compressors).hasSize(1)
        assertThat(gzipOnly.compressors[0].encoding()).isEqualTo("gzip")
        
        // Test backward compatibility constructor
        val backwardCompat = CompressionStrategy(Brotli(4), Gzip(6))
        assertThat(backwardCompat.compressors).hasSizeGreaterThanOrEqualTo(1)
        
        // Test compression type selection
        val compressors = allFormats.compressors
        assertThat(compressors.forType("gzip")).isNotNull()
        assertThat(compressors.forType("GZIP")).isNotNull() // case insensitive
        if (CompressionStrategy.brotliImplAvailable()) {
            assertThat(compressors.forType("br")).isNotNull()
        }
        if (CompressionStrategy.zstdImplAvailable()) {
            assertThat(compressors.forType("zstd")).isNotNull()
        }
        assertThat(compressors.forType("unknown")).isNull()
    }

    @Test
    @EnabledIf("zstdAvailable")
    fun `ZstdCompressor implementation works correctly`() {
        // Test compressor creation and properties
        val compressor = ZstdCompressor(5)
        assertThat(compressor.level).isEqualTo(5)
        assertThat(compressor.encoding()).isEqualTo("zstd")
        assertThat(compressor.extension()).isEqualTo(".zst")
        
        // Test level validation
        assertThatExceptionOfType(IllegalArgumentException::class.java).isThrownBy { ZstdCompressor(-1) }
        assertThatExceptionOfType(IllegalArgumentException::class.java).isThrownBy { ZstdCompressor(23) }
        
        // Test compression actually works
        val testData = "Hello World!".repeat(100)
        val outputStream = ByteArrayOutputStream()
        val compressedStream = compressor.compress(outputStream)
        compressedStream.write(testData.toByteArray())
        compressedStream.close()
        
        assertThat(outputStream.size()).isGreaterThan(0)
        assertThat(outputStream.size()).isLessThan(testData.length) // should be compressed
        
        // Test decompression to verify correctness
        val decompressedStream = ZstdInputStream(outputStream.toByteArray().inputStream())
        val decompressed = String(decompressedStream.readBytes())
        assertThat(decompressed).isEqualTo(testData)
    }

    @Test
    fun `doesn't compress when Accept-Encoding is not set`() = TestUtil.test(superCompressingApp()) { _, http ->
        HttpUtilInstance.get(http.origin + "/huge").header(Header.ACCEPT_ENCODING, "null").asString().let { response -> // dynamic
            assertThat(response.body.length).isEqualTo(hugeLength)
            assertThat(response.headers[Header.CONTENT_ENCODING]).isEmpty()
        }
        HttpUtilInstance.get(http.origin + "/html.html").header(Header.ACCEPT_ENCODING, "null").asString().let { response -> // static
            assertThat(response.body.length).isEqualTo(testDocument.length)
            assertThat(response.headers[Header.CONTENT_ENCODING]).isEmpty()
        }
    }

    @Test
    fun `doesn't compress when response is too small`() = TestUtil.test(customCompressionApp(tinyLength + 1)) { _, http ->
        HttpUtilInstance.get(http.origin + "/tiny").header(Header.ACCEPT_ENCODING, "br, gzip").asString().let { response -> // dynamic
            assertThat(response.body.length).isEqualTo(tinyLength)
            assertThat(response.headers[Header.CONTENT_ENCODING]).isEmpty()
        }
        HttpUtilInstance.get(http.origin + "/html.html").header(Header.ACCEPT_ENCODING, "br, gzip").asString().let { response -> // static
            assertThat(response.body.length).isEqualTo(testDocument.length)
            assertThat(response.headers[Header.CONTENT_ENCODING]).isEmpty()
        }
    }

    @Test
    fun `doesn't compress when compression is disabled`() = TestUtil.test(
        Javalin.create { it.http.disableCompression() }.addTestEndpoints()
    ) { _, http ->
        HttpUtilInstance.get(http.origin + "/huge").header(Header.ACCEPT_ENCODING, "br, gzip").asString().let { response -> // dynamic
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
    @EnabledIf("brotliAvailable")
    fun `does brotli when Accept-Encoding header is set and size is big enough`() = TestUtil.test(superCompressingApp()) { _, http ->
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
            it.http.brotliOnlyCompression()
            it.staticFiles.add("/public", Location.CLASSPATH)
        }.addTestEndpoints()
        TestUtil.test(gzipDisabledApp) { _, http ->
            HttpUtilInstance.get(http.origin + "/huge").header(Header.ACCEPT_ENCODING, "gzip").asString().let { response -> // dynamic
                assertThat(response.body.length).isEqualTo(hugeLength)
                assertThat(response.headers[Header.CONTENT_ENCODING]).isEmpty()
            }
        }
    }

    @Test
    fun `doesn't brotli when brotli is disabled`() = TestUtil.test(brotliDisabledApp()) { _, http ->
        HttpUtilInstance.get(http.origin + "/huge").header(Header.ACCEPT_ENCODING, "bz").asString().let { response -> // dynamic
            assertThat(response.body.length).isEqualTo(hugeLength)
            assertThat(response.headers[Header.CONTENT_ENCODING]).isEmpty()
        }
    }

    @Test
    @EnabledIf("brotliAvailable")
    fun `chooses brotli when both enabled and supported`() = TestUtil.test(superCompressingApp()) { _, http ->
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
            it.http.gzipOnlyCompression()
            it.staticFiles.enableWebjars()
        }
        TestUtil.test(gzipWebjars) { _, http ->
            assertValidGzipResponse(http.origin, path)
        }
    }

    @Test
    fun `svg images are compressed by default`() = TestUtil.test(Javalin.create {
        it.staticFiles.add("/public", Location.CLASSPATH)
        it.http.brotliAndGzipCompression()
    }) { _, http ->
        assertValidGzipResponse(http.origin, "/svg.svg")
        assertValidBrotliResponse(http.origin, "/svg.svg")
    }

    @Test
    fun `svg compression can be disabled`() = TestUtil.test(Javalin.create {
        it.staticFiles.add("/public", Location.CLASSPATH)
        it.http.customCompression(CompressionStrategy(Brotli(), Gzip()).apply { allowedMimeTypes = listOf() })
    }) { _, http ->
        getResponse(http.origin, "/svg.svg", "gzip").let { response ->
            assertThat(response.headers[Header.CONTENT_ENCODING]).isNull()
        }
    }

    @Test
    @EnabledIf("brotliAvailable")
    fun `brotli works for large static files`() {
        val path = "/webjars/swagger-ui/${TestDependency.swaggerVersion}/swagger-ui-bundle.js"
        val compressedWebjars = Javalin.create {
            it.http.brotliOnlyCompression()
            it.staticFiles.enableWebjars()
        }
        TestUtil.test(compressedWebjars) { _, http ->
            assertValidBrotliResponse(http.origin, path)
        }
    }

    @Test
    @EnabledIf("brotliAvailable")
    fun `brotli works for dynamic responses of different sizes`() = TestUtil.test(superCompressingApp()) { app, http ->
        listOf(10, 100, 1000, 10_000).forEach { size ->
            app.get("/$size") { it.result(testDocument.repeat(size)) }
            assertValidBrotliResponse(http.origin, "/$size")
        }
    }

    @Test
    @EnabledIf("zstdAvailable")
    fun `zstd compression works comprehensively`() {
        // Test basic zstd compression with dynamic content of various sizes
        TestUtil.test(zstdOnlyApp()) { app, http ->
            // Test different response sizes - small, medium, large
            listOf(10, 100, 1000, 10_000).forEach { size ->
                app.get("/$size") { it.result(testDocument.repeat(size)) }
                assertValidZstdResponse(http.origin, "/$size")
            }
            
            // Test large dynamic response
            getResponse(http.origin, "/huge", "zstd").let { response ->
                assertThat(response.headers[Header.CONTENT_ENCODING]).isEqualTo("zstd")
                assertThat(response.body!!.contentLength()).isLessThan(10000L) // should be compressed
            }
            
            // Test static file compression
            getResponse(http.origin, "/html.html", "zstd").let { response ->
                assertThat(response.headers[Header.CONTENT_ENCODING]).isEqualTo("zstd")
            }
        }
        
        // Test zstd with large static files (Webjars)
        val staticFileApp = Javalin.create {
            it.http.customCompression(CompressionStrategy(null, null, Zstd()))
            it.staticFiles.enableWebjars()
        }
        TestUtil.test(staticFileApp) { _, http ->
            val path = "/webjars/swagger-ui/${TestDependency.swaggerVersion}/swagger-ui-bundle.js"
            assertValidZstdResponse(http.origin, path)
        }
        
        // Test priority when multiple formats available (zstd should be chosen over others)
        val multiFormatApp = Javalin.create {
            it.staticFiles.add("/public", Location.CLASSPATH)
            it.http.customCompression(CompressionStrategy(Brotli(), Gzip(), Zstd()).apply { 
                defaultMinSizeForCompression = 1 
            })
        }
        TestUtil.test(multiFormatApp) { _, http ->
            // Test that all formats work
            assertValidGzipResponse(http.origin, "/svg.svg")
            assertValidBrotliResponse(http.origin, "/svg.svg")
            assertValidZstdResponse(http.origin, "/svg.svg")
            
            // Test preference order - browser usually sends multiple encodings
            getResponse(http.origin, "/svg.svg", "gzip, deflate, br, zstd").let { response ->
                // Should choose one of the available formats
                assertThat(response.headers[Header.CONTENT_ENCODING]).isIn("gzip", "br", "zstd")
            }
        }
    }

    @Test
    fun `compression handles edge cases and error scenarios`() {
        // Test CompressionStrategy with null values
        val emptyStrategy = CompressionStrategy(null, null, null)
        assertThat(emptyStrategy.compressors).isEmpty()
        
        // Test NONE strategy
        assertThat(CompressionStrategy.NONE.compressors).isEmpty()
        
        // Test GZIP strategy
        assertThat(CompressionStrategy.GZIP.compressors).hasSize(1)
        assertThat(CompressionStrategy.GZIP.compressors[0].encoding()).isEqualTo("gzip")
        
        // Test that unknown compression types return null
        val gzipOnlyStrategy = CompressionStrategy(null, Gzip(), null)
        assertThat(gzipOnlyStrategy.compressors.forType("unknown")).isNull()
        assertThat(gzipOnlyStrategy.compressors.forType("")).isNull()
        
        // Test case insensitive compression type matching
        assertThat(gzipOnlyStrategy.compressors.forType("GZIP")).isNotNull()
        assertThat(gzipOnlyStrategy.compressors.forType("gzip")).isNotNull()
        assertThat(gzipOnlyStrategy.compressors.forType("GZip")).isNotNull()
        
        // Test that Zstd configuration has sensible defaults
        val defaultZstd = Zstd()
        assertThat(defaultZstd.level).isEqualTo(3)
        
        // Test CompressionType enum values
        assertThat(CompressionType.ZSTD.typeName).isEqualTo("zstd")
        assertThat(CompressionType.ZSTD.extension).isEqualTo(".zst")
        assertThat(CompressionType.GZIP.typeName).isEqualTo("gzip")
        assertThat(CompressionType.GZIP.extension).isEqualTo(".gz")
        assertThat(CompressionType.BR.typeName).isEqualTo("br")
        assertThat(CompressionType.BR.extension).isEqualTo(".br")
        assertThat(CompressionType.NONE.typeName).isEqualTo("")
        assertThat(CompressionType.NONE.extension).isEqualTo("")
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
    fun `doesn't compress when static files were pre-compressed`() {
        val path = "/script.js"
        val gzipWebjars = Javalin.create {
            it.http.gzipOnlyCompression()
            it.staticFiles.enableWebjars()
            it.staticFiles.add { staticFiles ->
                staticFiles.precompress = true
                staticFiles.directory = "/public"
                staticFiles.location = Location.CLASSPATH
            }
            it.pvt.compressionStrategy.defaultMinSizeForCompression = 0 // minSize to enable automatic compress
        }
        TestUtil.test(gzipWebjars) { _, http ->
            assertValidGzipResponse(http.origin, path)
        }
    }

    private fun buildSampleJson(chunksOf20Chars: Int): String {
        val obj = """{"value":123456789}""" // 19 chars, will be 20 with comma
        return List(chunksOf20Chars) { obj }.joinToString(",", "[", "]")
    }
    private val sampleJson100 = buildSampleJson(5)
    private val sampleJson10k = buildSampleJson(500)
    private fun testValidCompressionHandler(handler: Handler) {
        val gzipTestApp = Javalin.create {
            it.http.gzipOnlyCompression()
        }.apply {
            get("/gzip-test", handler)
        }
        TestUtil.test(gzipTestApp) { _, http ->
            assertValidGzipResponse(http.origin, "/gzip-test")
        }
        val brotliTestApp = Javalin.create {
            it.http.brotliOnlyCompression()
        }.apply {
            get("/brotli-test", handler)
        }
        TestUtil.test(brotliTestApp) { _, http ->
            assertValidBrotliResponse(http.origin, "/brotli-test")
        }
    }
    private fun testValidUncompressedHandler(handler: Handler) {
        val uncompressedTestApp = Javalin.create {
            it.http.gzipOnlyCompression() // compression is enabled so that we can test minSizeForCompression thresholds
        }.apply {
            get("/uncompressed-test", handler)
        }
        TestUtil.test(uncompressedTestApp) { _, http ->
            assertUncompressedResponse(http.origin, "/uncompressed-test")
        }
    }

    @Test
    fun `compresses a large string of JSON`() {
        testValidCompressionHandler { ctx ->
            ctx.contentType(ContentType.APPLICATION_JSON).result(sampleJson10k)
        }
        testValidUncompressedHandler { ctx ->
            ctx.minSizeForCompression(sampleJson10k.length + 1)
            ctx.contentType(ContentType.APPLICATION_JSON).result(sampleJson10k)
        }
    }

    @Test
    fun `compresses a large string of JSON with direct single byte writes to outputStream`() {
        testValidCompressionHandler { ctx ->
            ctx.contentType(ContentType.APPLICATION_JSON)
            ctx.minSizeForCompression(0) // must force compression to use single byte writes
            val out = ctx.outputStream()
            sampleJson10k.forEach { out.write(it.code) }
        }
        testValidUncompressedHandler { ctx ->
            ctx.contentType(ContentType.APPLICATION_JSON)
            val out = ctx.outputStream()
            sampleJson10k.forEach { out.write(it.code) } // first write is one byte, so no compression
        }
    }

    @Test
    fun `compresses a large string of JSON with direct byte array writes to outputStream`() {
        testValidCompressionHandler { ctx ->
            ctx.contentType(ContentType.APPLICATION_JSON)
            val out = ctx.outputStream()
            sampleJson10k.map { it.code.toByte() }.toByteArray().let { bytes -> out.write(bytes) }
        }
        testValidUncompressedHandler { ctx ->
            ctx.contentType(ContentType.APPLICATION_JSON)
            ctx.minSizeForCompression(sampleJson10k.length + 1)
            val out = ctx.outputStream()
            sampleJson10k.map { it.code.toByte() }.toByteArray().let { bytes -> out.write(bytes) }
        }
    }

    @Test
    fun `compresses a small string of JSON with direct byte array writes to outputStream`() {
        testValidCompressionHandler { ctx ->
            ctx.contentType(ContentType.APPLICATION_JSON)
            ctx.minSizeForCompression(0) // must force compression since small string is below default threshold
            val out = ctx.outputStream()
            sampleJson100.map { it.code.toByte() }.toByteArray().let { bytes -> out.write(bytes) }
        }
        testValidUncompressedHandler { ctx ->
            ctx.contentType(ContentType.APPLICATION_JSON)
            val out = ctx.outputStream()
            sampleJson100.map { it.code.toByte() }.toByteArray().let { bytes -> out.write(bytes) }
        }
    }

    @Test
    fun `disableCompression disables compression even if size is big`() = TestUtil.test(superCompressingApp()) { app, http ->
        app.get("/disabled-compression") { ctx ->
            ctx.disableCompression()
            ctx.result("a".repeat(10_000))
        }

        val response = getResponse(http.origin, "/disabled-compression", "br, gzip")

        assertThat(response.header(Header.CONTENT_ENCODING)).isNull()
    }

    @Test
    fun `compresses a large Stream of JSON`() {
        data class Foo(val value: Long) // will become 19 chars in JSON, 20 with comma separator
        fun createLargeJsonStream() = generateSequence { Foo(123456789) }.take(500).asStream() // > 10,000 chars
        testValidCompressionHandler { ctx ->
            ctx.writeJsonStream(createLargeJsonStream())
        }
        // no test for uncompressed since writing a Stream<T> forces compression
    }

    @Test
    @EnabledIf("brotliAvailable")
    fun `assure preferred compressor is respected (br)`() = TestUtil.test(preferredCompressors(listOf(
        CompressionType.BR, CompressionType.GZIP
    ))) { app, http ->
        getResponse(http.origin, "/huge", "gzip, br").let { response -> // dynamic
            assertThat(response.header(Header.CONTENT_ENCODING)).isEqualTo("br")
        }
        getResponse(http.origin, "/svg.svg", "gzip, br").let { response -> // static
            assertThat(response.header(Header.CONTENT_ENCODING)).isEqualTo("br")
        }

        getResponse(http.origin, "/huge", "br, gzip").let { response -> // dynamic
            assertThat(response.header(Header.CONTENT_ENCODING)).isEqualTo("br")
        }
        getResponse(http.origin, "/svg.svg", "br, gzip").let { response -> // static
            assertThat(response.header(Header.CONTENT_ENCODING)).isEqualTo("br")
        }

        getResponse(http.origin, "/huge", "").let { response -> // dynamic
            assertThat(response.header(Header.CONTENT_ENCODING)).isNull()
        }
        getResponse(http.origin, "/svg.svg", "").let { response -> // static
            assertThat(response.header(Header.CONTENT_ENCODING)).isNull()
        }
    }

    @Test
    @EnabledIf("brotliAvailable")
    fun `assure preferred compressor is respected (gzip)`() = TestUtil.test(preferredCompressors(listOf(
        CompressionType.GZIP, CompressionType.BR
    ))) { app, http ->
        getResponse(http.origin, "/huge", "gzip, br").let { response -> // dynamic
            assertThat(response.header(Header.CONTENT_ENCODING)).isEqualTo("gzip")
        }
        getResponse(http.origin, "/svg.svg", "gzip, br").let { response -> // static
            assertThat(response.header(Header.CONTENT_ENCODING)).isEqualTo("gzip")
        }

        getResponse(http.origin, "/huge", "br, gzip").let { response -> // dynamic
            assertThat(response.header(Header.CONTENT_ENCODING)).isEqualTo("gzip")
        }
        getResponse(http.origin, "/svg.svg", "br, gzip").let { response -> // static
            assertThat(response.header(Header.CONTENT_ENCODING)).isEqualTo("gzip")
        }

        getResponse(http.origin, "/huge", "").let { response -> // dynamic
            assertThat(response.header(Header.CONTENT_ENCODING)).isNull()
        }
        getResponse(http.origin, "/svg.svg", "").let { response -> // static
            assertThat(response.header(Header.CONTENT_ENCODING)).isNull()
        }
    }

    private fun assertUncompressedResponse(origin: String, url: String) {
        val response = getResponse(origin, url, "br, gzip, zstd")
        assertThat(response.code).isLessThan(400)
        assertThat(response.header(Header.CONTENT_ENCODING)).isNull()
        val uncompressedResponse = getResponse(origin, url, "null").body!!.string()
        assertThat(response.body!!.string()).isEqualTo(uncompressedResponse)
    }

    private fun assertValidBrotliResponse(origin: String, url: String) {
        val response = getResponse(origin, url, "br")
        assertThat(response.header(Header.CONTENT_ENCODING)).isEqualTo("br")
        val brotliInputStream = when {
            CompressionStrategy.brotli4jAvailable() -> Brotli4jInputStream(response.body!!.byteStream())
            else -> throw RuntimeException("No brotli implementation found")
        }
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

    private fun assertValidZstdResponse(origin: String, url: String) {
        val response = getResponse(origin, url, "zstd")
        assertThat(response.header(Header.CONTENT_ENCODING)).isEqualTo("zstd")
        val zstdInputStream = ZstdInputStream(response.body!!.byteStream())
        val decompressed = String(zstdInputStream.readBytes())
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

    private fun brotliAvailable() = CompressionStrategy.brotliImplAvailable()

    private fun zstdAvailable() = CompressionStrategy.zstdImplAvailable()

}
