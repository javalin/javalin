package io.javalin

import io.javalin.http.Context
import io.javalin.http.HttpStatus
import io.javalin.http.HttpStatus.IM_A_TEAPOT
import io.javalin.http.HttpStatus.OK
import io.javalin.http.bodyAsClass
import io.javalin.json.JavalinJackson
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.lang.reflect.Type
import java.util.concurrent.atomic.AtomicReference

internal class TestMocking {

    @Nested
    inner class Mockito {

        @Test
        fun `should create mock of context from interface`() {
            val context = assertDoesNotThrow { mock(Context::class.java) }
            assertThat(context).isNotNull()
        }

        @Test
        fun `should mock request and response based methods`() {
            val context = mock(Context::class.java)
            val status = AtomicReference(OK)

            `when`(context.status(any(HttpStatus::class.java))).then {
                status.set(it.getArgument(0, HttpStatus::class.java))
                context
            }
            `when`(context.status()).then { status.get() }

            assertThat(context.status()).isEqualTo(OK)
            context.status(IM_A_TEAPOT)
            assertThat(context.status()).isEqualTo(IM_A_TEAPOT)
        }

        // Pass "any()" value mock to non-nullable argument (workaround for Kotlin)
        private fun <T : Any?> any(type: Class<T>): T = org.mockito.Mockito.any(type)

    }

    @Nested
    inner class Mockk {

        @Test
        fun `should create context mockk`() {
            val context = mockk<Context>()
            assertThat(context).isNotNull()
        }

        @Test
        fun `gh-1953 should mock body`() {
            data class Test(val message: String)

            val context = mockk<Context>()
            every { context.body() } returns """{"message":"Hello"}"""
            every { context.bodyAsClass<Test>(Test::class.java as Type) } answers { JavalinJackson.defaultMapper().readValue(context.body(), Test::class.java) }

            val body = context.bodyAsClass<Test>()
            assertThat(body.message).isEqualTo("Hello")
        }

    }

}
