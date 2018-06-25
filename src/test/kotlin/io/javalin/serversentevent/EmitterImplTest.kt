package io.javalin.serversentevent

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.*
import java.io.IOException
import javax.servlet.AsyncContext
import javax.servlet.ServletOutputStream
import javax.servlet.ServletResponse

class EmitterImplTest {
    lateinit var asyncContext: AsyncContext
    lateinit var response: ServletResponse
    lateinit var outputStream: ServletOutputStream
    val CRLF = "\r\n"
    val event = "test"
    val data = "data"

    @Before
    fun setup() {
        asyncContext = mock(javax.servlet.AsyncContext::class.java)
        response = mock(ServletResponse::class.java)
        outputStream = mock(ServletOutputStream::class.java)
        `when`(asyncContext.response).thenReturn(response)
        `when`(response.outputStream).thenReturn(outputStream)
    }

    @Test
    fun createEmitter() {
        var emitter: Emitter = EmitterImpl(asyncContext)

        assertFalse(emitter.isClose())
    }

    @Test
    fun failToCreateEmitter() {
        `when`(response.outputStream).thenThrow(IOException::class.java)

        var emitter: Emitter = EmitterImpl(asyncContext)

        assertTrue(emitter.isClose())
    }

    @Test
    fun sendEvent() {
        var emitter: Emitter = EmitterImpl(asyncContext)

        emitter.event(event, data)
        verify(outputStream).println("event: $event$CRLF")
        verify(outputStream).println("data: $data$CRLF")
        verify(response).flushBuffer()
        assertFalse(emitter.isClose())
    }

    @Test
    fun failSendEvent() {
        `when`(outputStream.println(ArgumentMatchers.anyString())).thenThrow(IOException::class.java)

        var emitter: Emitter = EmitterImpl(asyncContext)

        emitter.event(event, data)
        assertTrue(emitter.isClose())
    }
}