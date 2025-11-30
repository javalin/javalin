package io.javalin

import io.javalin.event.EventManager
import io.javalin.event.HandlerMetaInfo
import io.javalin.event.JavalinLifecycleEvent
import io.javalin.event.WsHandlerMetaInfo
import io.javalin.http.HandlerType
import io.javalin.security.RouteRole
import io.javalin.websocket.WsHandlerType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.function.Consumer

class TestEventManager {
    @Test
    fun `fire events with no listeners doesn't throw`() {
        val manager = EventManager()
        JavalinLifecycleEvent.entries.forEach { manager.fireEvent(it) }
        manager.fireHandlerAddedEvent(HandlerMetaInfo(HandlerType.GET, "/", {}, emptySet()))
        manager.fireWsHandlerAddedEvent(WsHandlerMetaInfo(WsHandlerType.WEBSOCKET, "/", Consumer {}, emptySet()))
    }

    @Test
    fun `fireHandlerAddedEvent calls all handlers`() {
        val manager = EventManager()
        val calls = mutableListOf<HandlerMetaInfo>()
        manager.handlerAddedHandlers.add(Consumer { calls.add(it) })
        manager.handlerAddedHandlers.add(Consumer { calls.add(it) })

        val meta = HandlerMetaInfo(HandlerType.POST, "/api", {}, setOf(TestRole()))
        manager.fireHandlerAddedEvent(meta)

        assertThat(calls).hasSize(2).allMatch { it == meta }
    }

    @Test
    fun `fireWsHandlerAddedEvent calls all handlers`() {
        val manager = EventManager()
        val calls = mutableListOf<WsHandlerMetaInfo>()
        manager.wsHandlerAddedHandlers.add(Consumer { calls.add(it) })
        manager.wsHandlerAddedHandlers.add(Consumer { calls.add(it) })

        val meta = WsHandlerMetaInfo(WsHandlerType.WEBSOCKET, "/ws", Consumer {}, setOf(TestRole()))
        manager.fireWsHandlerAddedEvent(meta)

        assertThat(calls).hasSize(2).allMatch { it == meta }
    }

    @Test
    fun `HandlerMetaInfo works`() {
        val h = HandlerMetaInfo(HandlerType.GET, "/path", {}, setOf(TestRole()))
        assertThat(h.httpMethod).isEqualTo(HandlerType.GET)
        assertThat(h.path).isEqualTo("/path")
        assertThat(h.roles).hasSize(1)
    }

    @Test
    fun `WsHandlerMetaInfo works`() {
        val h = WsHandlerMetaInfo(WsHandlerType.WEBSOCKET_AFTER, "/ws", Consumer {}, setOf(TestRole()))
        assertThat(h.handlerType).isEqualTo(WsHandlerType.WEBSOCKET_AFTER)
        assertThat(h.path).isEqualTo("/ws")
        assertThat(h.roles).hasSize(1)
    }

    @Test
    fun `EventManager initializes handlers for all events`() {
        val manager = EventManager()
        JavalinLifecycleEvent.entries.forEach {
            assertThat(manager.lifecycleHandlers[it]).isNotNull().isEmpty()
        }
    }

    private class TestRole : RouteRole
}
