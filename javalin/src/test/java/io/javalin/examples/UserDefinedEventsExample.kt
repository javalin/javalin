package io.javalin.examples

import io.javalin.Javalin
import java.util.UUID

/**
 * Example demonstrating the user-defined events API in Javalin (Kotlin version).
 * 
 * This example shows how to:
 * - Register event handlers for custom event types
 * - Emit events from route handlers (Context)
 * - Emit events from the Javalin application instance
 * - Work with CompletableFuture results from event emission
 */

// Define custom event types - these can be any data class
data class SchedulePrompt(val requestId: UUID, val prompt: String)
data class UserCreated(val userId: String, val email: String)

fun main() {
    // Create Javalin app with event handlers
    val app = Javalin.create { config ->
        
        // Register handler for SchedulePrompt events
        config.events.on(SchedulePrompt::class.java) { event ->
            // This runs asynchronously when a SchedulePrompt event is emitted
            println("Processing prompt: ${event.prompt}")
            
            // Simulate some async processing (e.g., calling an AI API)
            Thread.sleep(100)
            println("Finished processing prompt for request: ${event.requestId}")
        }

        // Register handler for UserCreated events
        config.events.on(UserCreated::class.java) { event ->
            // This runs asynchronously when a UserCreated event is emitted
            println("User created: ${event.userId} with email: ${event.email}")
            
            // Could send welcome email, update analytics, etc.
        }

        // Multiple handlers can be registered for the same event type
        config.events.on(UserCreated::class.java) { event ->
            println("Logging user creation to audit log: ${event.userId}")
        }

        // Define routes that emit events
        config.routes.get("/request") { ctx ->
            val requestId = UUID.randomUUID()
            
            // Emit event from context - fires asynchronously
            ctx.emit(SchedulePrompt(requestId, "Hello, this is a scheduled prompt."))
            
            // Response is sent immediately, event handler runs in background
            ctx.json(requestId)
        }

        config.routes.post("/users/{userId}") { ctx ->
            val userId = ctx.pathParam("userId")
            val email = ctx.body()
            
            // Emit event - fires asynchronously
            ctx.emit(UserCreated(userId, email))
            
            // Response is sent immediately
            ctx.status(201).result("User created")
        }
    }.start(7070)

    // Events can also be emitted from the Javalin instance itself
    // This is useful for triggering events outside of HTTP request context
    
    val externalRequestId = UUID.randomUUID()
    val future = app.emit(
        SchedulePrompt(externalRequestId, "External prompt from application")
    )

    // The future completes when all event handlers finish processing
    future.thenRun {
        println("All handlers completed for external request: $externalRequestId")
    }

    // Emit another event type
    app.emit(UserCreated("external-user-123", "external@example.com"))
        .thenRun {
            println("External user creation event processed")
        }

    println("Javalin started on http://localhost:7070")
    println("Try:")
    println("  GET  http://localhost:7070/request")
    println("  POST http://localhost:7070/users/john -d 'john@example.com'")
}
