package io.javalin.http

import io.javalin.Javalin
import io.javalin.core.util.Header
import io.javalin.http.staticfiles.Location
import io.javalin.testing.TestUtil
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.jetty.servlet.ServletContextHandler
import org.junit.Test
import io.javalin.jetty.JettyPrecompressingResourceHandler

class ContentTypeTest {

    @Test
    fun `fetching content type by mime type should get the right type`() {
        ContentType.values().forEach {
            assertThat(ContentType.getContentType(it.mimeType)).isEqualTo(it)
        }
    }

    @Test
    fun `fetching content type by its extension should get the right type`() {
        ContentType.values().forEach { type ->
            type.extensions.forEach { extension ->
                assertThat(ContentType.getContentTypeByExtension(extension)).isEqualTo(type)
                assertThat(ContentType.getMimeTypeByExtension(extension)).isEqualTo(type.mimeType)
            }
        }
    }

    @Test
    fun `string representation should return mime type`() {
        assertThat(ContentType.TEXT_PLAIN.toString()).isEqualTo(ContentType.TEXT_PLAIN.mimeType)
    }

    @Test
    fun `custom mime type should work when precompression is turned on`() {
        TestUtil.test(Javalin.create { config ->
            config.addStaticFiles { staticFiles ->
                staticFiles.hostedPath = "/"
                staticFiles.directory = "/markdown"
                staticFiles.location = Location.CLASSPATH
                staticFiles.precompress = true
            }

            config.configureServletContextHandler {
                context: ServletContextHandler -> context.mimeTypes.addMimeMapping("md", ContentType.PLAIN)
                JettyPrecompressingResourceHandler.servletContextHandler = context
            }

            config.enableDevLogging()
        }) { _, http ->
            assertThat(http.get("/test.md").status).isEqualTo(200)
            assertThat(http.get("/test.md").headers.getFirst(Header.CONTENT_TYPE)).contains(ContentType.PLAIN)
        }
    }

}
