# User-Defined Events API Implementation

## Summary

This implementation extends Javalin's event system to support user-defined events that can be emitted via `Context` and `Javalin` instances, with asynchronous execution using the existing thread pool.

## Changes Made

### 1. Extended EventManager (`io.javalin.event.EventManager.kt`)
- Added `userEventHandlers` map to store handlers by event type
- Added `on<T>()` method to register handlers for custom event types
- Added `emit<T>()` method to emit events to all registered handlers asynchronously
- Events are processed using `CompletableFuture.runAsync()` with the configured executor

### 2. Updated EventConfig (`io.javalin.config.EventConfig.kt`)
- Added `on<T>()` method to expose event handler registration in the config API
- Delegates to `EventManager.on()` method

### 3. Extended Context Interface (`io.javalin.http.Context.kt`)
- Added `emit<T>()` method to emit events from request handlers
- Returns `CompletableFuture<Void>` for handling async completion

### 4. Implemented emit() in JavalinServletContext (`io.javalin.http.servlet.JavalinServletContext.kt`)
- Implemented `emit<T>()` method in the Context implementation
- Added `eventManager` and `asyncExecutor` to `JavalinServletContextConfig`
- Uses the configured async executor for event processing

### 5. Extended Javalin Class (`io.javalin.Javalin.java`)
- Added `emit<T>()` method to emit events from the application instance
- Useful for triggering events outside of HTTP request context

### 6. Enhanced AsyncExecutor (`io.javalin.http.util.AsyncUtil.kt`)
- Added `getExecutor()` method to access the underlying ExecutorService
- Required for event emission

### 7. Comprehensive Tests (`io.javalin.TestUserDefinedEvents.kt`)
- Test emitting events from Context
- Test emitting events from Javalin instance
- Test multiple handlers for the same event type
- Test asynchronous event processing
- Test CompletableFuture completion
- Test multiple different event types
- Test emitting events with no handlers
- Test chaining futures from emit

### 8. Example Code
- Java example: `UserDefinedEventsExample.java`
- Kotlin example: `UserDefinedEventsExample.kt`

## API Usage

### Registering Event Handlers

```kotlin
Javalin.create { config ->
    config.events.on(SchedulePrompt::class.java) { event ->
        // Handle the event asynchronously
        aiClient.chat.request(event.prompt)
        db.save(event.requestId, result)
    }
}
```

### Emitting Events from Context

```kotlin
config.routes.get("/request") { ctx ->
    val requestId = UUID.randomUUID()
    ctx.emit(SchedulePrompt(requestId, "Hello, this is a scheduled prompt."))
    ctx.result(requestId)
}
```

### Emitting Events from Javalin Instance

```kotlin
val requestId = UUID.randomUUID()
application
    .emit(SchedulePrompt(requestId, "Test prompt"))
    .thenApply { println(db.get(requestId)) }
```

## Key Features

1. **Type-Safe**: Event types are checked at compile time using Java generics
2. **Asynchronous**: All event handlers run asynchronously using the existing thread pool
3. **CompletableFuture Support**: The `emit()` method returns a `CompletableFuture<Void>` that completes when all handlers finish
4. **Multiple Handlers**: Multiple handlers can be registered for the same event type
5. **No Breaking Changes**: The implementation extends the existing event system without modifying lifecycle events
6. **Thread-Safe**: Uses `ConcurrentHashMap` for thread-safe event handler storage

## Testing

All tests pass successfully:
- 8 new tests for user-defined events
- 5 existing lifecycle event tests still pass
- 851 total tests pass in the javalin module (20 browser tests expected to fail in CI)

## Technical Notes

- Uses the existing `AsyncExecutor` and thread pool configured in `PrivateConfig`
- Event handlers are stored in a `ConcurrentHashMap<Class<*>, MutableSet<Consumer<*>>>`
- The `emit()` method creates a `CompletableFuture` for each handler and combines them with `CompletableFuture.allOf()`
- If no handlers are registered for an event type, `emit()` returns a completed future
- Events are processed in parallel, not sequentially
