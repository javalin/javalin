package io.javalin

import io.javalin.http.Context
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class TestContextMocking {

    @Test
    fun `test context can be mocked`() {
        val context = mockk<Context>()

        every { context.body() } returns "body"
        every { context.header(any()) } returns "header"
        every { context.pathParam(any()) } returns "pathParam"
        every { context.queryParam(any()) } returns "queryParam"
        every { context.formParam(any()) } returns "formParam"

        assertThat(context.body()).isEqualTo("body")
        assertThat(context.header("whatever")).isEqualTo("header")
        assertThat(context.pathParam("whatever")).isEqualTo("pathParam")
        assertThat(context.queryParam("whatever")).isEqualTo("queryParam")
        assertThat(context.formParam("whatever")).isEqualTo("formParam")
    }
}
