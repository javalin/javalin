package io.javalin

import io.javalin.json.PipedStreamExecutor
import io.javalin.testing.TestUtil
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.io.IOException

class TestPipedStreamExecutor {
    @Test
    fun `failed response stream is closed after serving a real request`() {
        val input = PipedStreamExecutor(false).getInputStream { output ->
            output.write(42)
            throw IllegalStateException("serialization failed")
        }
        TestUtil.test { app, http ->
            app.unsafe.routes.get("/") { it.result(input) }
            http.get("/")
        }
        assertThatThrownBy { input.read() }
            .isInstanceOf(IOException::class.java)
            .hasMessage("Pipe closed")
    }

    @Test
    fun `close releases the input pipe when the writer fails`() {
        val failure = IllegalStateException("serialization failed")
        val input = PipedStreamExecutor(false).getInputStream { output ->
            output.write(42)
            throw failure
        }
        assertThat(input.read()).isEqualTo(42)
        assertThat(input.read()).isEqualTo(-1)
        assertThatThrownBy { input.close() }.isSameAs(failure)
        assertThatThrownBy { input.read() }
            .isInstanceOf(IOException::class.java)
            .hasMessage("Pipe closed")
    }

    @Test
    fun `close releases the input pipe after a successful writer`() {
        val input = PipedStreamExecutor(false).getInputStream { output -> output.write(42) }
        assertThat(input.read()).isEqualTo(42)
        assertThat(input.read()).isEqualTo(-1)
        input.close()
        assertThatThrownBy { input.read() }
            .isInstanceOf(IOException::class.java)
            .hasMessage("Pipe closed")
    }
}
