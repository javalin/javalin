package io.javalin.staticfiles

import io.javalin.compression.CompressionStrategy
import io.javalin.compression.Gzip
import io.javalin.http.ContentDisposition
import io.javalin.http.Header
import io.javalin.http.staticfiles.Location
import io.javalin.testing.TestDependency
import okhttp3.OkHttpClient
import okhttp3.Request
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class TestStaticFilesContentDisposition {

    private val attachmentByName = { _: Any, path: String -> ContentDisposition.attachmentRfc5987(path.substringAfterLast('/')) }

    @Test
    fun `no header by default`() = testStaticFiles({ it.staticFiles.add("/public", Location.CLASSPATH) }) { _, http ->
        assertThat(http.get("/html.html").headers.containsKey(Header.CONTENT_DISPOSITION)).isFalse()
    }

    @Test
    fun `works for files on the filesystem`() = testStaticFiles({ cfg ->
        cfg.staticFiles.add {
            it.directory = "src/test/external/"
            it.location = Location.EXTERNAL
            it.contentDispositionFunction = attachmentByName
        }
    }) { _, http ->
        val response = http.get("/txt.txt")
        assertThat(response.status).isEqualTo(200)
        assertThat(response.headers.getFirst(Header.CONTENT_DISPOSITION)).isEqualTo("attachment; filename=\"txt.txt\"; filename*=UTF-8''txt.txt")
    }

    @Test
    fun `works for files on the classpath, and null means no header`() = testStaticFiles({ cfg ->
        cfg.staticFiles.add {
            it.directory = "/public"
            it.contentDispositionFunction = { _, path -> if (path.endsWith(".js")) ContentDisposition.attachment("script.js") else null }
        }
    }) { _, http ->
        assertThat(http.get("/script.js").headers.getFirst(Header.CONTENT_DISPOSITION)).isEqualTo("attachment; filename=\"script.js\"")
        assertThat(http.get("/html.html").headers.containsKey(Header.CONTENT_DISPOSITION)).isFalse()
    }

    @Test
    fun `works for files inside a webjar`() = testStaticFiles({ cfg ->
        cfg.staticFiles.add {
            it.hostedPath = "/webjars"
            it.directory = "META-INF/resources/webjars"
            it.contentDispositionFunction = attachmentByName
        }
    }) { _, http ->
        val response = http.get("/webjars/swagger-ui/${TestDependency.swaggerVersion}/swagger-ui.css")
        assertThat(response.status).isEqualTo(200)
        assertThat(response.headers.getFirst(Header.CONTENT_DISPOSITION)).startsWith("attachment; filename=\"swagger-ui.css\"")
    }

    @Test
    fun `works together with precompression`() = testStaticFiles({ cfg ->
        cfg.http.compressionStrategy = CompressionStrategy(null, Gzip())
        cfg.staticFiles.add {
            it.directory = "/public"
            it.precompressMaxSize = 1024 * 1024
            it.contentDispositionFunction = attachmentByName
        }
    }) { _, http ->
        val request = Request.Builder().url(http.origin + "/html.html").header(Header.ACCEPT_ENCODING, "gzip").build()
        OkHttpClient().newCall(request).execute().use { response ->
            assertThat(response.code).isEqualTo(200)
            assertThat(response.header(Header.CONTENT_ENCODING)).isEqualTo("gzip")
            assertThat(response.header(Header.CONTENT_DISPOSITION)).startsWith("attachment; filename=\"html.html\"")
        }
    }

    @Test
    fun `function receives the request`() = testStaticFiles({ cfg ->
        cfg.staticFiles.add {
            it.directory = "/public"
            it.contentDispositionFunction = { req, _ -> if (req.getParameter("download") != null) ContentDisposition.attachment() else ContentDisposition.inline() }
        }
    }) { _, http ->
        assertThat(http.get("/html.html").headers.getFirst(Header.CONTENT_DISPOSITION)).isEqualTo("inline")
        assertThat(http.get("/html.html?download").headers.getFirst(Header.CONTENT_DISPOSITION)).isEqualTo("attachment")
    }
}
