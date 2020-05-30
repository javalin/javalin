/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 *
 */
package io.javalin

import io.javalin.core.compression.Brotli
import io.javalin.core.compression.Gzip
import io.javalin.core.util.Header
import io.javalin.core.util.OptionalDependency
import io.javalin.http.staticfiles.PrecompressingResourceHandler
import io.javalin.testing.HttpUtil
import io.javalin.testing.TestUtil
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class TestStaticFilesPrecompressor {

    private val swaggerBasePath = "/webjars/swagger-ui/${OptionalDependency.SWAGGERUI.version}"

    private val configPrecompressionStaticResourceApp: Javalin by lazy {
        Javalin.create { config ->
            config.precompressStaticFiles = true
            config.compressionStrategy(Brotli(), Gzip())
            config.enableWebjars()
            config.addStaticFiles("/public/immutable")
            config.addStaticFiles("/public/protected")
        }
    }

    @Test
    fun `content-length unavailable for large files if precompression not enabled`() = TestUtil.test(Javalin.create { config -> config.enableWebjars() }) { _, http ->
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
        val oldSize = PrecompressingResourceHandler.compressedFiles.size
        assertThat(http.getFile("/secret.html", "gzip").contentEncoding()).isEqualTo("gzip")
        assertThat(http.getFile("/secret.html?qp=1", "gzip").contentEncoding()).isEqualTo("gzip")
        assertThat(http.getFile("/secret.html?qp=2", "gzip").contentEncoding()).isEqualTo("gzip")
        assertThat(PrecompressingResourceHandler.compressedFiles.size <= oldSize + 1)
    }

    private fun Response.contentLength() = this.headers().get(Header.CONTENT_LENGTH)
    private fun Response.contentEncoding() = this.headers().get(Header.CONTENT_ENCODING)

    private fun HttpUtil.getFile(url: String, encoding: String) =
            OkHttpClient().newCall(Request.Builder()
                    .url(this.origin + url)
                    .header(Header.ACCEPT_ENCODING, encoding)
                    .build())
                    .execute()
}
