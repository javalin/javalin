package io.javalin.serversentevent

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.*

class EventSourceImplTest {
    val event = "event"
    val data = "data"
    val pathParam = HashMap<String, String>()

    @Test
    fun onOpen() {
        var called = false;
        val emitter = mock(Emitter::class.java)
        val onOpen = { eventsource: EventSource -> called = true}

        val eventSource = EventSourceImpl(emitter, pathParam)
        eventSource.onOpen(onOpen)

        assertTrue(called)
    }


    @Test
    fun sendEvent() {
        val emitter = mock(Emitter::class.java)
        `when`(emitter.isClose()).thenReturn(false)
        val eventSource = EventSourceImpl(emitter, pathParam)
        eventSource.sendEvent(event, data)

        verify(emitter).event(event, data)
    }

    @Test
    fun onClose() {
        var sseOnClose: EventSource = mock(EventSource::class.java)
        val emitter = mock(Emitter::class.java)
        val onClose = { eventsource: EventSource -> sseOnClose = eventsource}
       `when`(emitter.isClose()).thenReturn(true)
        val eventSource = EventSourceImpl(emitter, pathParam)
        eventSource.onClose(onClose)

        eventSource.sendEvent(event, data)
        assertEquals(eventSource, sseOnClose)
    }

    @Test
    fun pathParamMap() {
        val emitter = mock(Emitter::class.java)
        val eventSource = EventSourceImpl(emitter, pathParam)
        assertEquals(pathParam, eventSource.pathParamMap())
    }

}

