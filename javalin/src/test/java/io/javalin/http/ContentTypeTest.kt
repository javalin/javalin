package io.javalin.http

import io.javalin.Javalin
import io.javalin.core.util.Header
import io.javalin.testing.TestUtil
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

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
    fun `file should be served even if content type isn't set`() {
        val precompressingApp = Javalin.create { config ->
            config.addStaticFiles {
                it.precompress = true
                it.directory = "/markdown"
            }
        }
        TestUtil.test(precompressingApp) { _, http ->
            val response = http.get("/test.md")
            assertThat(response.status).isEqualTo(200)
            assertThat(response.body).contains("# Hello Markdown!")
            assertThat(response.headers.getFirst(Header.CONTENT_TYPE)).isNull()
        }
    }

}
