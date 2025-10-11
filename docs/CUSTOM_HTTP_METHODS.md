# Custom HTTP Methods in Javalin

This document explains how to handle non-standard HTTP methods (such as WebDAV methods like PROPFIND, MKCOL, etc.) in Javalin.

## Solution

Javalin provides the `CustomHttpMethodHandler` utility class and a convenient `Context` extension function to handle custom HTTP methods.

## Usage

### Using the Extension Function (Recommended)

The simplest way to handle custom HTTP methods is using the `handleCustomMethod` extension function:

```kotlin
app.before("/dav/*") { ctx ->
    ctx.handleCustomMethod(
        "PROPFIND" to Handler { it.result("PROPFIND response") },
        "MKCOL" to Handler { it.result("Collection created").status(201) }
    )
}
```

### Using the Utility Class Directly

You can also use the `CustomHttpMethodHandler` class directly:

```kotlin
app.before("/dav/*") { ctx ->
    CustomHttpMethodHandler.handle(ctx, mapOf(
        "PROPFIND" to Handler { it.result("PROPFIND response") },
        "MKCOL" to Handler { it.result("Collection created").status(201) }
    ))
}
```

## How It Works

The custom method handler works by:

1. Checking the incoming HTTP request method using `ctx.req().method`
2. Looking up the corresponding handler from the provided map
3. Executing the handler if found
4. Calling `ctx.skipRemainingHandlers()` to prevent the 404 error from being thrown
5. If no handler matches, the request continues normally and Javalin returns a 404

## Features

- **Case-insensitive**: Method names are automatically uppercased for comparison
- **Path parameters**: Custom method handlers have full access to path parameters and all Context methods
- **Multiple methods**: You can register multiple custom methods for the same path
- **Standard methods**: While not recommended, you can use this utility for standard HTTP methods too

## Examples

### WebDAV Server

```kotlin
app.before("/dav/*") { ctx ->
    ctx.handleCustomMethod(
        "PROPFIND" to Handler { ctx ->
            // Handle PROPFIND request
            val properties = getFileProperties(ctx.pathParam("*"))
            ctx.result(properties).contentType("application/xml")
        },
        "PROPPATCH" to Handler { ctx ->
            // Handle PROPPATCH request
            updateFileProperties(ctx.pathParam("*"), ctx.body())
            ctx.status(207) // Multi-Status
        },
        "MKCOL" to Handler { ctx ->
            // Handle MKCOL request
            createCollection(ctx.pathParam("*"))
            ctx.status(201) // Created
        },
        "COPY" to Handler { ctx ->
            // Handle COPY request
            val destination = ctx.header("Destination")
            copyResource(ctx.pathParam("*"), destination)
            ctx.status(201)
        },
        "MOVE" to Handler { ctx ->
            // Handle MOVE request
            val destination = ctx.header("Destination")
            moveResource(ctx.pathParam("*"), destination)
            ctx.status(201)
        },
        "LOCK" to Handler { ctx ->
            // Handle LOCK request
            val lockToken = lockResource(ctx.pathParam("*"))
            ctx.header("Lock-Token", lockToken).status(200)
        },
        "UNLOCK" to Handler { ctx ->
            // Handle UNLOCK request
            unlockResource(ctx.pathParam("*"), ctx.header("Lock-Token"))
            ctx.status(204)
        }
    )
}
```

### Custom REST-like Methods

```kotlin
app.before("/api/*") { ctx ->
    ctx.handleCustomMethod(
        "PURGE" to Handler { ctx ->
            // Handle cache purge
            purgeCache(ctx.path())
            ctx.result("Cache purged").status(200)
        }
    )
}
```

## Java Usage

The utility works in Java as well:

```java
app.before("/dav/*", ctx -> {
    Map<String, Handler> handlers = Map.of(
        "PROPFIND", (Handler) it -> it.result("PROPFIND response"),
        "MKCOL", (Handler) it -> it.result("Collection created").status(201)
    );
    CustomHttpMethodHandler.handle(ctx, handlers);
});
```

## Implementation Details

The solution leverages Javalin's `before` handlers to intercept requests before they reach the routing system. When a custom method is detected:

1. The appropriate handler is executed
2. `skipRemainingHandlers()` is called to prevent further processing
3. The response is sent directly to the client

This approach provides a clean API while minimizing changes to the core routing system.

## Testing

The implementation includes comprehensive tests covering:
- Multiple custom methods
- Path parameter access
- Case-insensitive method names
- 404 handling for unhandled methods
- Standard HTTP methods (for compatibility testing)

See `TestCustomHttpMethodHandler.kt` for examples.
