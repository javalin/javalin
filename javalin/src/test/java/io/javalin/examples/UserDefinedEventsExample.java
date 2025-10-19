package io.javalin.examples;

import io.javalin.Javalin;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Example demonstrating the user-defined events API in Javalin.
 * 
 * This example shows how to:
 * - Register event handlers for custom event types
 * - Emit events from route handlers (Context)
 * - Emit events from the Javalin application instance
 * - Work with CompletableFuture results from event emission
 */
public class UserDefinedEventsExample {

    // Define custom event types - these can be any POJO
    static class SchedulePrompt {
        final UUID requestId;
        final String prompt;

        SchedulePrompt(UUID requestId, String prompt) {
            this.requestId = requestId;
            this.prompt = prompt;
        }
    }

    static class UserCreated {
        final String userId;
        final String email;

        UserCreated(String userId, String email) {
            this.userId = userId;
            this.email = email;
        }
    }

    public static void main(String[] args) {
        // Create Javalin app with event handlers
        Javalin app = Javalin.create(config -> {
            
            // Register handler for SchedulePrompt events
            config.events.on(SchedulePrompt.class, event -> {
                // This runs asynchronously when a SchedulePrompt event is emitted
                System.out.println("Processing prompt: " + event.prompt);
                
                // Simulate some async processing (e.g., calling an AI API)
                try {
                    Thread.sleep(100);
                    System.out.println("Finished processing prompt for request: " + event.requestId);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });

            // Register handler for UserCreated events
            config.events.on(UserCreated.class, event -> {
                // This runs asynchronously when a UserCreated event is emitted
                System.out.println("User created: " + event.userId + " with email: " + event.email);
                
                // Could send welcome email, update analytics, etc.
            });

            // Multiple handlers can be registered for the same event type
            config.events.on(UserCreated.class, event -> {
                System.out.println("Logging user creation to audit log: " + event.userId);
            });

            // Define routes that emit events
            config.routes.get("/request", ctx -> {
                UUID requestId = UUID.randomUUID();
                
                // Emit event from context - fires asynchronously
                ctx.emit(new SchedulePrompt(requestId, "Hello, this is a scheduled prompt."));
                
                // Response is sent immediately, event handler runs in background
                ctx.json(requestId);
            });

            config.routes.post("/users/{userId}", ctx -> {
                String userId = ctx.pathParam("userId");
                String email = ctx.body();
                
                // Emit event - fires asynchronously
                ctx.emit(new UserCreated(userId, email));
                
                // Response is sent immediately
                ctx.status(201).result("User created");
            });
        }).start(7070);

        // Events can also be emitted from the Javalin instance itself
        // This is useful for triggering events outside of HTTP request context
        
        UUID externalRequestId = UUID.randomUUID();
        CompletableFuture<Void> future = app.emit(
            new SchedulePrompt(externalRequestId, "External prompt from application")
        );

        // The future completes when all event handlers finish processing
        future.thenRun(() -> {
            System.out.println("All handlers completed for external request: " + externalRequestId);
        });

        // Emit another event type
        app.emit(new UserCreated("external-user-123", "external@example.com"))
            .thenRun(() -> {
                System.out.println("External user creation event processed");
            });

        System.out.println("Javalin started on http://localhost:7070");
        System.out.println("Try:");
        System.out.println("  GET  http://localhost:7070/request");
        System.out.println("  POST http://localhost:7070/users/john -d 'john@example.com'");
    }
}
