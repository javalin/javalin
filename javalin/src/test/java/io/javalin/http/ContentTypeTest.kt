package io.javalin.http

import io.javalin.Javalin
import io.javalin.http.HttpStatus.OK
import io.javalin.testing.TestUtil
import io.javalin.testing.httpCode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ContentTypeTest {

    @Test
    fun `fetching content type by mime type should get the right type`() {
        ContentType.entries.forEach {
            assertThat(ContentType.getContentType(it.mimeType)).isEqualTo(it)
        }
    }

    @Test
    fun `fetching content type by its extension should get the right type`() {
        ContentType.entries.forEach { type ->
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
            config.staticFiles.add {
                it.precompress = true
                it.directory = "/markdown"
            }
        }
        TestUtil.test(precompressingApp) { _, http ->
            val response = http.get("/test.md")
            assertThat(response.httpCode()).isEqualTo(OK)
            assertThat(response.body).contains("# Hello Markdown!")
            assertThat(response.headers.getFirst(Header.CONTENT_TYPE)).isEqualTo("")
        }
    }

    fun contentTypes(): Stream<Arguments?> {
        return Stream.of(
            Arguments.of("application/json;charset=utf-8"),
            Arguments.of("application/json;charset=UTF-8"),
            Arguments.of("application/json;Charset=\"utf-8\" "),
            Arguments.of("application/json; charset=\"utf-8\""),
        )
    }

    @ParameterizedTest
    @MethodSource("contentTypes")
    fun `quoted charset parameter should be handled successfully`(contentType: String) {
        TestUtil.test { app, http ->
            app.post("/foo") { ctx ->
                ctx.result(ctx.body())
            }
            val result = http.post("/foo")
                .header("Content-type", contentType)
                .body("{}")
                .asString()
            assertThat(result.httpCode()).isEqualTo(OK)
        }
    }

}
