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
import io.javalin.http.ContentType
import io.javalin.http.Header
import io.javalin.http.HttpStatus
import io.javalin.http.staticfiles.Location
import io.javalin.http.staticfiles.NativePrecompressingResourceHandler
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
            javalin.http.brotliAndGzipCompression()
            javalin.staticFiles.add { staticFiles ->
                staticFiles.hostedPath = "/webjars"
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
        assertThat(http.getFile("$swaggerBasePath/swagger-ui-bundle.js", "gzip"))
            .extracting({ it.code }, { it.contentLength() })
            .containsExactly(HttpStatus.OK.code, null)
        assertThat(http.getFile("$swaggerBasePath/swagger-ui.js.gz", "gzip"))
            .extracting({ it.code }, { it.contentLength() })
            .containsExactly(HttpStatus.OK.code, null)
    }

    @Test
    fun `content-length available for large files if precompression enabled`() = TestUtil.test(configPrecompressionStaticResourceApp) { _, http ->
        val secretResponse = http.getFile("/secret.html", "br, gzip")
        assertThat(secretResponse.code).isEqualTo(200)
        assertThat(secretResponse.contentLength()).isNotBlank()

        val libraryResponse = http.getFile("/library-1.0.0.min.js", "br")
        assertThat(libraryResponse.code).isEqualTo(200)
        assertThat(libraryResponse.contentLength()).isNotBlank()

        val swaggerResponse = http.getFile("$swaggerBasePath/swagger-ui-bundle.js", "gzip")
        assertThat(swaggerResponse.code).isEqualTo(200)
        assertThat(swaggerResponse.contentLength()).isNotBlank()
    }

    @Test
    fun `compression works if precompression enabled`() = TestUtil.test(configPrecompressionStaticResourceApp) { _, http ->
        assertThat(http.getFile("/secret.html", "gzip"))
            .extracting({ it.code }, { it.contentEncoding() })
            .containsExactly(HttpStatus.OK.code, "gzip")
        assertThat(http.getFile("/library-1.0.0.min.js", "br"))
            .extracting({ it.code }, { it.contentEncoding() })
            .containsExactly(HttpStatus.OK.code, "br")
        assertThat(http.getFile("$swaggerBasePath/swagger-ui-bundle.js", "gzip"))
            .extracting({ it.code }, { it.contentEncoding() })
            .containsExactly(HttpStatus.OK.code, "gzip")
        assertThat(http.getFile("$swaggerBasePath/swagger-ui.js.gz", "gzip"))
            .extracting({ it.code }, { it.contentEncoding() })
            .containsExactly(HttpStatus.OK.code, null)
    }

    @Test
    fun `only creates one compressed version even if query params are present`() = TestUtil.test(configPrecompressionStaticResourceApp) { _, http ->
        val oldSize = NativePrecompressingResourceHandler.compressedFiles.size
        assertThat(http.getFile("/secret.html", "gzip"))
            .extracting({ it.code }, { it.contentEncoding() })
            .containsExactly(HttpStatus.OK.code, "gzip")
        assertThat(http.getFile("/secret.html?qp=1", "gzip"))
            .extracting({ it.code }, { it.contentEncoding() })
            .containsExactly(HttpStatus.OK.code, "gzip")
        assertThat(http.getFile("/secret.html?qp=2", "gzip"))
            .extracting({ it.code }, { it.contentEncoding() })
            .containsExactly(HttpStatus.OK.code, "gzip")
        assertThat(NativePrecompressingResourceHandler.compressedFiles.size <= oldSize + 1)
    }

    @Test
    fun `first available encoding is used`() = TestUtil.test(configPrecompressionStaticResourceApp) { _, http ->
        assertThat(http.getFile("/secret.html", "br"))
            .extracting({ it.code }, { it.contentEncoding() })
            .containsExactly(HttpStatus.OK.code, "br")
        assertThat(http.getFile("/secret.html", "gzip"))
            .extracting({ it.code }, { it.contentEncoding() })
            .containsExactly(HttpStatus.OK.code, "gzip")
        assertThat(http.getFile("/secret.html", "gzip, br"))
            .extracting({ it.code }, { it.contentEncoding() })
            .containsExactly(HttpStatus.OK.code, "gzip")
        assertThat(http.getFile("/secret.html", "br, gzip"))
            .extracting({ it.code }, { it.contentEncoding() })
            .containsExactly(HttpStatus.OK.code, "br")
        assertThat(http.getFile("/secret.html", "deflate, gzip, br"))
            .extracting({ it.code }, { it.contentEncoding() })
            .containsExactly(HttpStatus.OK.code, "gzip")
    }

    @Test
    fun `precompressed files are served with correct content-type`() = TestUtil.test(configPrecompressionStaticResourceApp) { _, http ->
        assertThat(http.getFile("/secret.html", "br"))
            .extracting({ it.code }, { it.header("Content-Type") })
            .containsExactly(HttpStatus.OK.code, "text/html")
        assertThat(http.getFile("/library-1.0.0.min.js", "br"))
            .extracting({ it.code }, { it.header("Content-Type") })
            .containsExactly(HttpStatus.OK.code, "text/javascript")
        assertThat(http.getFile("/library-1.0.0.min.js", "gzip, br"))
            .extracting({ it.code }, { it.header("Content-Type") })
            .containsExactly(HttpStatus.OK.code, "text/javascript")
        assertThat(http.getFile("/library-1.0.0.min.js", "deflate"))
            .extracting({ it.code }, { it.header("Content-Type") })
            .containsExactly(HttpStatus.OK.code, "text/javascript")
    }

    @Test
    fun `response encoding matches header`() = TestUtil.test(configPrecompressionStaticResourceApp) { _, http ->
        // Gzip
        val gzipResponse = http.getFile("/secret.html", "gzip")
        assertThat(gzipResponse)
            .extracting({ it.code }, { it.header("Content-Encoding") })
            .containsExactly(HttpStatus.OK.code, "gzip")
        val gzipInputStream = GZIPInputStream(gzipResponse.body?.byteStream())
        assertThat(gzipInputStream.readBytes().toString(Charsets.UTF_8)).contains("<h1>Secret file</h1>")
        gzipInputStream.close()

        // Brotli
        assumeTrue(Brotli4jLoader.isAvailable())
        val brotliResponse = http.getFile("/secret.html", "br")
        assertThat(brotliResponse)
            .extracting({ it.code }, { it.header("Content-Encoding") })
            .containsExactly(HttpStatus.OK.code, "br")
        val brotliInputStream = BrotliInputStream(brotliResponse.body?.byteStream())
        assertThat(brotliInputStream.readBytes().toString(Charsets.UTF_8)).contains("<h1>Secret file</h1>")
        brotliInputStream.close()
    }

    @Test
    fun `compression strategy is used`() = TestUtil.test(Javalin.create { config ->
        config.http.brotliOnlyCompression()
        config.staticFiles.add { staticFiles ->
            staticFiles.hostedPath = "/"
            staticFiles.directory = "/public"
            staticFiles.precompress = true
        }
    }) { _, http ->
        assumeTrue(Brotli4jLoader.isAvailable())
        assertThat(http.getFile("/html.html", "gzip"))
            .extracting({ it.code }, { it.contentEncoding() })
            .containsExactly(HttpStatus.OK.code, null)
        assertThat(http.getFile("/html.html", "gzip, br"))
            .extracting({ it.code }, { it.contentEncoding() })
            .containsExactly(HttpStatus.OK.code, "br")
        assertThat(http.getFile("/html.html", "br, gzip"))
            .extracting({ it.code }, { it.contentEncoding() })
            .containsExactly(HttpStatus.OK.code, "br")
        assertThat(http.getFile("/html.html", "deflate, gzip, br"))
            .extracting({ it.code }, { it.contentEncoding() })
            .containsExactly(HttpStatus.OK.code, "br")
    }

    @Test
    fun `can set headers after precompressing handler is done`() = TestUtil.test(Javalin.create { config ->
        config.staticFiles.add {
            it.directory = "public"
            it.location = Location.CLASSPATH
            it.precompress = true
        }
    }) { app, http ->
        app.after { it.header("X-After", "true") }
        val res = http.get("/html.html")
        assertThat(res.status).describedAs("status").isEqualTo(HttpStatus.OK.code)
        assertThat(res.headers.getFirst("Content-Type")).describedAs("content-type").isEqualTo(ContentType.HTML)
        assertThat(res.body).describedAs("body").contains("<h1>HTML works</h1>")
        assertThat(res.headers.getFirst("X-After")).describedAs("after-header").isEqualTo("true")
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
