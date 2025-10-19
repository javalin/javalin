/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin

import io.javalin.testing.TestUtil
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

// Test event types
data class SchedulePrompt(val requestId: UUID, val prompt: String)
data class UserCreated(val userId: String, val email: String)
data class OrderPlaced(val orderId: Long, val amount: Double)

class TestUserDefinedEvents {

    @Test
    fun `emit event from context works`() {
        val events = mutableListOf<SchedulePrompt>()
        
        TestUtil.test(Javalin.create { config ->
            config.events.on(SchedulePrompt::class.java) { event ->
                events.add(event)
            }
            config.routes.get("/test") { ctx ->
                val requestId = UUID.randomUUID()
                ctx.emit(SchedulePrompt(requestId, "test prompt"))
                ctx.result(requestId.toString())
            }
        }) { app, client ->
            val response = client.get("/test")
            assertThat(response.status).isEqualTo(200)
            
            // Give events time to process
            Thread.sleep(100)
            
            assertThat(events).hasSize(1)
            assertThat(events[0].prompt).isEqualTo("test prompt")
        }
    }

    @Test
    fun `emit event from Javalin instance works`() {
        val events = mutableListOf<UserCreated>()
        
        val app = Javalin.create { config ->
            config.events.on(UserCreated::class.java) { event ->
                events.add(event)
            }
        }
        
        app.start(0)
        
        try {
            val future = app.emit(UserCreated("user123", "user@example.com"))
            future.get(1, TimeUnit.SECONDS)
            
            assertThat(events).hasSize(1)
            assertThat(events[0].userId).isEqualTo("user123")
            assertThat(events[0].email).isEqualTo("user@example.com")
        } finally {
            app.stop()
        }
    }

    @Test
    fun `multiple handlers for same event work`() {
        val handler1Events = mutableListOf<OrderPlaced>()
        val handler2Events = mutableListOf<OrderPlaced>()
        
        TestUtil.test(Javalin.create { config ->
            config.events.on(OrderPlaced::class.java) { event ->
                handler1Events.add(event)
            }
            config.events.on(OrderPlaced::class.java) { event ->
                handler2Events.add(event)
            }
            config.routes.post("/orders") { ctx ->
                val orderId = 12345L
                ctx.emit(OrderPlaced(orderId, 99.99))
                ctx.status(201).result("Order placed")
            }
        }) { app, client ->
            val response = client.post("/orders").asString()
            assertThat(response.status).isEqualTo(201)
            
            // Give events time to process
            Thread.sleep(100)
            
            assertThat(handler1Events).hasSize(1)
            assertThat(handler2Events).hasSize(1)
            assertThat(handler1Events[0].orderId).isEqualTo(12345L)
            assertThat(handler2Events[0].orderId).isEqualTo(12345L)
        }
    }

    @Test
    fun `events are processed asynchronously`() {
        val processedOrder = ConcurrentHashMap<String, String>()
        
        TestUtil.test(Javalin.create { config ->
            config.events.on(SchedulePrompt::class.java) { event ->
                // Simulate some processing time
                Thread.sleep(100)
                processedOrder[event.requestId.toString()] = event.prompt
            }
            config.routes.get("/async-test") { ctx ->
                val requestId = UUID.randomUUID()
                ctx.emit(SchedulePrompt(requestId, "async prompt"))
                // Response should be sent immediately, before event handler completes
                ctx.result(requestId.toString())
            }
        }) { app, client ->
            val start = System.currentTimeMillis()
            val response = client.get("/async-test")
            val responseTime = System.currentTimeMillis() - start
            
            assertThat(response.status).isEqualTo(200)
            // Response should come back quickly (less than 100ms), before event handler completes
            // Note: We use a lenient timeout to account for CI environment variability
            assertThat(responseTime).isLessThan(100)
            
            // Wait for event to be processed
            Thread.sleep(200)
            
            val requestId = response.body
            assertThat(processedOrder).containsKey(requestId)
            assertThat(processedOrder[requestId]).isEqualTo("async prompt")
        }
    }

    @Test
    fun `emit returns CompletableFuture that completes when handlers finish`() {
        val processedEvents = mutableListOf<String>()
        
        val app = Javalin.create { config ->
            config.events.on(UserCreated::class.java) { event ->
                Thread.sleep(100) // Simulate processing
                processedEvents.add(event.userId)
            }
        }
        
        app.start(0)
        
        try {
            val future = app.emit(UserCreated("user456", "test@example.com"))
            
            // Event should not be processed yet
            assertThat(processedEvents).isEmpty()
            
            // Wait for future to complete
            future.get(1, TimeUnit.SECONDS)
            
            // Now event should be processed
            assertThat(processedEvents).hasSize(1)
            assertThat(processedEvents[0]).isEqualTo("user456")
        } finally {
            app.stop()
        }
    }

    @Test
    fun `multiple events can be emitted and handled independently`() {
        val userEvents = mutableListOf<UserCreated>()
        val orderEvents = mutableListOf<OrderPlaced>()
        
        TestUtil.test(Javalin.create { config ->
            config.events.on(UserCreated::class.java) { event ->
                userEvents.add(event)
            }
            config.events.on(OrderPlaced::class.java) { event ->
                orderEvents.add(event)
            }
            config.routes.get("/test") { ctx ->
                ctx.emit(UserCreated("user1", "user1@example.com"))
                ctx.emit(OrderPlaced(100L, 50.0))
                ctx.result("Events emitted")
            }
        }) { app, client ->
            val response = client.get("/test")
            assertThat(response.status).isEqualTo(200)
            
            // Give events time to process
            Thread.sleep(100)
            
            assertThat(userEvents).hasSize(1)
            assertThat(orderEvents).hasSize(1)
            assertThat(userEvents[0].userId).isEqualTo("user1")
            assertThat(orderEvents[0].orderId).isEqualTo(100L)
        }
    }

    @Test
    fun `emitting event with no handlers completes successfully`() {
        val app = Javalin.create()
        app.start(0)
        
        try {
            // Emit an event with no registered handlers
            val future = app.emit(UserCreated("user999", "nohandler@example.com"))
            future.get(1, TimeUnit.SECONDS)
            
            // Should complete without error
            assertThat(future.isDone).isTrue()
            assertThat(future.isCompletedExceptionally).isFalse()
        } finally {
            app.stop()
        }
    }

    @Test
    fun `chaining futures from emit works`() {
        val results = mutableListOf<String>()
        
        val app = Javalin.create { config ->
            config.events.on(SchedulePrompt::class.java) { event ->
                Thread.sleep(50)
                results.add("Event processed: ${event.requestId}")
            }
        }
        
        app.start(0)
        
        try {
            val requestId = UUID.randomUUID()
            app.emit(SchedulePrompt(requestId, "test"))
                .thenRun {
                    results.add("Future completed")
                }
                .get(1, TimeUnit.SECONDS)
            
            assertThat(results).hasSize(2)
            assertThat(results[0]).startsWith("Event processed:")
            assertThat(results[1]).isEqualTo("Future completed")
        } finally {
            app.stop()
        }
    }
}
