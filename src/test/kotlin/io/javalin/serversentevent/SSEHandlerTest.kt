package io.javalin.serversentevent

import io.javalin.Context
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import org.junit.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.*
import java.nio.charset.Charset

import java.util.function.Consumer
import javax.servlet.AsyncContext
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class SSEHandlerTest {
    private val UTF_8 = Charset.forName("UTF-8")

    @Test
    fun happyPath() {
        val consumerSSE: Consumer<*>? = mock(Consumer::class.java)
        val sseHandler = SSEHandler.start(consumerSSE as Consumer<EventSource>)
        val context = mockk<Context>()
        val asyncContext = mockk<AsyncContext>()

        val request = spyk<HttpServletRequest>()
        val response = spyk<HttpServletResponse>()

        every { context.request() } returns request
        every { context.response() } returns response
        every { request.getHeader("Accept")} returns "text/event-stream"
        every {  context.pathParamMap } returns HashMap<String, String>()
        every { request.asyncContext } returns asyncContext
        every { asyncContext.response } returns response

        sseHandler.handle(context)

        io.mockk.verify {
            response.status = HttpServletResponse.SC_OK
            response.characterEncoding = UTF_8.name()
            response.contentType = "text/event-stream"
            response.addHeader("Connection", "close")
            response.flushBuffer()
            request.startAsync(request, response)
        }


        verify(consumerSSE).accept(ArgumentMatchers.isA(EventSource::class.java))
    }

}