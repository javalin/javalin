/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 *
 */
package io.javalin.staticfiles

import com.aayushatharva.brotli4j.Brotli4jLoader
import com.aayushatharva.brotli4j.decoder.BrotliInputStream
import io.javalin.Javalin
import io.javalin.http.Header
import io.javalin.jetty.JettyPrecompressingResourceHandler
import io.javalin.testing.HttpUtil
import io.javalin.testing.TestDependency
import io.javalin.testing.TestUtil
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import java.util.zip.GZIPInputStream

class TestStaticFilesPrecompressor {

    private val swaggerBasePath = "/webjars/swagger-ui/${TestDependency.swaggerVersion}"

    private val configPrecompressionStaticResourceApp: Javalin by lazy {
        Javalin.create { javalin ->
            javalin.compression.brotliAndGzip()
            javalin.staticFiles.add { staticFiles ->
                staticFiles.directory = "META-INF/resources/webjars"
                staticFiles.precompress = true
            }
            javalin.staticFiles.add { staticFiles ->
                staticFiles.directory = "/public/immutable"
                staticFiles.precompress = true
            }
            javalin.staticFiles.add { staticFiles ->
                staticFiles.directory = "/public/protected"
                staticFiles.precompress = true
            }
        }
    }

    @Test
    fun `content-length unavailable for large files if precompression not enabled`() = TestUtil.test(Javalin.create { config -> config.staticFiles.enableWebjars() }) { _, http ->
        assertThat(http.getFile("$swaggerBasePath/swagger-ui-bundle.js", "gzip").contentLength()).isNull()
        assertThat(http.getFile("$swaggerBasePath/swagger-ui.js.gz", "gzip").contentLength()).isNull()
    }

    @Test
    fun `content-length available for large files if precompression enabled`() = TestUtil.test(configPrecompressionStaticResourceApp) { _, http ->
        assertThat(http.getFile("/secret.html", "br, gzip").contentLength()).isNotBlank()
        assertThat(http.getFile("/library-1.0.0.min.js", "br").contentLength()).isNotBlank()
        assertThat(http.getFile("$swaggerBasePath/swagger-ui-bundle.js", "gzip").contentLength()).isNotBlank()
    }

    @Test
    fun `compression works if precompression enabled`() = TestUtil.test(configPrecompressionStaticResourceApp) { _, http ->
        assertThat(http.getFile("/secret.html", "gzip").contentEncoding()).isEqualTo("gzip")
        assertThat(http.getFile("/library-1.0.0.min.js", "br").contentEncoding()).isEqualTo("br")
        assertThat(http.getFile("$swaggerBasePath/swagger-ui-bundle.js", "gzip").contentEncoding()).isEqualTo("gzip")
        assertThat(http.getFile("$swaggerBasePath/swagger-ui.js.gz", "gzip").contentEncoding()).isNull()
    }

    @Test
    fun `only creates one compressed version even if query params are present`() = TestUtil.test(configPrecompressionStaticResourceApp) { _, http ->
        val oldSize = JettyPrecompressingResourceHandler.compressedFiles.size
        assertThat(http.getFile("/secret.html", "gzip").contentEncoding()).isEqualTo("gzip")
        assertThat(http.getFile("/secret.html?qp=1", "gzip").contentEncoding()).isEqualTo("gzip")
        assertThat(http.getFile("/secret.html?qp=2", "gzip").contentEncoding()).isEqualTo("gzip")
        assertThat(JettyPrecompressingResourceHandler.compressedFiles.size <= oldSize + 1)
    }

    @Test
    fun `first available encoding is used`() = TestUtil.test(configPrecompressionStaticResourceApp) { _, http ->
        assertThat(http.getFile("/secret.html", "br").contentEncoding()).isEqualTo("br")
        assertThat(http.getFile("/secret.html", "gzip").contentEncoding()).isEqualTo("gzip")
        assertThat(http.getFile("/secret.html", "gzip, br").contentEncoding()).isEqualTo("gzip")
        assertThat(http.getFile("/secret.html", "br, gzip").contentEncoding()).isEqualTo("br")
        assertThat(http.getFile("/secret.html", "deflate, gzip, br").contentEncoding()).isEqualTo("gzip")
    }

    @Test
    fun `precompressed files are served with correct content-type`() = TestUtil.test(configPrecompressionStaticResourceApp) { _, http ->
        assertThat(http.getFile("/secret.html", "br").header("Content-Type")).isEqualTo("text/html")
        assertThat(http.getFile("/library-1.0.0.min.js", "br").header("Content-Type")).isEqualTo("text/javascript")
        assertThat(http.getFile("/library-1.0.0.min.js", "gzip, br").header("Content-Type")).isEqualTo("text/javascript")
        assertThat(http.getFile("/library-1.0.0.min.js", "deflate").header("Content-Type")).isEqualTo("text/javascript")
    }

    @Test
    fun `response encoding matches header`() = TestUtil.test(configPrecompressionStaticResourceApp) { _, http ->
        // Gzip
        val gzipResponse = http.getFile("/secret.html", "gzip")
        assertThat(gzipResponse.header("Content-Encoding")).isEqualTo("gzip")
        val gzipInputStream = GZIPInputStream(gzipResponse.body?.byteStream())
        assertThat(gzipInputStream.readBytes().toString(Charsets.UTF_8)).contains("<h1>Secret file</h1>")
        gzipInputStream.close()

        // Brotli
        assumeTrue(Brotli4jLoader.isAvailable())
        val brotliResponse = http.getFile("/secret.html", "br")
        assertThat(brotliResponse.header("Content-Encoding")).isEqualTo("br")
        val brotliInputStream = BrotliInputStream(brotliResponse.body?.byteStream())
        assertThat(brotliInputStream.readBytes().toString(Charsets.UTF_8)).contains("<h1>Secret file</h1>")
        brotliInputStream.close()
    }

    @Test
    fun `compression strategy is used`() = TestUtil.test(Javalin.create { config ->
        config.compression.brotliOnly()
        config.staticFiles.add { staticFiles ->
            staticFiles.hostedPath = "/"
            staticFiles.directory = "/public"
            staticFiles.precompress = true
        }
    }) { _, http ->
        assumeTrue(Brotli4jLoader.isAvailable())
        assertThat(http.getFile("/html.html", "gzip").contentEncoding()).isNull()
        assertThat(http.getFile("/html.html", "gzip, br").contentEncoding()).isEqualTo("br")
        assertThat(http.getFile("/html.html", "br, gzip").contentEncoding()).isEqualTo("br")
        assertThat(http.getFile("/html.html", "deflate, gzip, br").contentEncoding()).isEqualTo("br")
    }


    private fun Response.contentLength() = this.headers.get(Header.CONTENT_LENGTH)
    private fun Response.contentEncoding() = this.headers.get(Header.CONTENT_ENCODING)

    private fun HttpUtil.getFile(url: String, encoding: String) =
        OkHttpClient().newCall(
            Request.Builder()
                .url(this.origin + url)
                .header(Header.ACCEPT_ENCODING, encoding)
                .build()
        )
            .execute()
}
