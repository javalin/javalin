# Javalin Web Framework

Javalin is a lightweight web framework for Java and Kotlin built on top of Jetty. This is a multi-module Maven project with Java 17+ support.

Always reference these instructions first and fallback to search or bash commands only when you encounter unexpected information that does not match the info here.

## Working Effectively

### Bootstrap and Build
- **REQUIREMENT**: Always run `./mvnw package` before `./mvnw test` to avoid OSGI dependency errors
- Build without tests: `./mvnw package -DskipTests --batch-mode` -- takes 60 seconds. NEVER CANCEL. Set timeout to 120+ seconds.
- Build with tests: `./mvnw package --batch-mode` -- takes 3+ minutes. NEVER CANCEL. Set timeout to 300+ seconds.
- **CRITICAL**: Browser tests fail in CI environment due to missing WebDriver - this is expected and normal
- Clean build: `./mvnw clean package -DskipTests --batch-mode` -- takes 90 seconds. NEVER CANCEL. Set timeout to 180+ seconds.
- Full CI build: `./mvnw -DRunningOnCi=true clean verify --batch-mode` -- takes 4+ minutes. NEVER CANCEL. Set timeout to 360+ seconds.

### Testing
- Run all tests: `./mvnw test --batch-mode` -- takes 2+ minutes. NEVER CANCEL. Set timeout to 240+ seconds.
- Run specific module tests: `./mvnw test -pl javalin --batch-mode`
- **EXPECTED FAILURES**: ~20 browser tests fail in CI environment (TestJavalinVueBrowser) due to WebDriver unavailability
- Non-browser tests pass reliably and provide good validation coverage

### Project Structure
- **Root**: Multi-module Maven project with 11 modules
- **Main module**: `javalin/` - Core framework implementation
- **Key modules**:
  - `javalin-testtools/` - Testing utilities
  - `javalin-rendering/` - Template engine plugins
  - `javalin-ssl/` - SSL/TLS helpers
  - `javalin-bundle/` - All-in-one bundle

## Validation Scenarios

### Always validate changes with these scenarios:

#### 1. Basic Server Functionality
```java
// Test script: Validate core Javalin functionality
var app = Javalin.create(config -> {
    // Basic config test
}).get("/", ctx -> ctx.result("Hello Javalin!"))
  .get("/health", ctx -> ctx.json(Map.of("status", "ok")))
  .start(7070);

// Test HTTP requests work
// Verify responses are correct
// Stop gracefully: app.stop()
```

#### 2. Build Validation Steps
1. `./mvnw clean` - Clean all build artifacts
2. `./mvnw package -DskipTests --batch-mode` - Verify compilation (60s)
3. Test basic server functionality with above scenario
4. `./mvnw test -pl javalin --batch-mode` - Run core tests (120s)

#### 3. Module Dependencies
- Always test the main `javalin` module can be used standalone
- Verify proper module separation (no circular dependencies)
- Check that examples in README.md work correctly

## Common Tasks

### Repository Navigation
```
javalin/
├── javalin/                 # Core framework (main module)
│   ├── src/main/java/io/javalin/
│   │   ├── Javalin.java    # Main entry point
│   │   ├── http/           # HTTP handling
│   │   ├── router/         # Routing implementation  
│   │   ├── config/         # Configuration
│   │   └── websocket/      # WebSocket support
│   └── src/test/java/      # Core tests
├── javalin-testtools/      # Testing utilities
├── javalin-rendering/      # Template engines
├── javalin-ssl/           # SSL helpers
└── .github/workflows/     # CI configuration
```

### Development Commands
- **Java version**: JDK 17+ required (project targets Java 17)
- **Maven wrapper**: `./mvnw` (no system Maven installation required)
- **Editor config**: `.editorconfig` - 4 spaces, UTF-8, LF line endings
- **Code style**: IntelliJ defaults with import optimization

### Commit Message Convention
Follow the repository's strict commit message format:
```
[component/area]: Description of change
```

**Examples:**
- `[core] Add feature for HTTP request handling`
- `[workflow] Update GitHub Actions dependencies`
- `[deps] Bump Jackson version to fix security issue`
- `[static-files] Fix path decoding for special characters`
- `[context] Add method to disable response compression`
- `[tests] Add unit tests for new validation logic`
- `[github] Update documentation and README files`

**Component categories commonly used:**
- `[core]` - Core framework functionality
- `[context]` - Request/response context changes
- `[router]` - Routing and handler logic
- `[static-files]` - Static file serving
- `[websocket]` - WebSocket functionality
- `[jetty]` - Jetty server configuration
- `[deps]` - Dependency updates
- `[tests]` - Test-related changes
- `[workflow]` - GitHub Actions and CI
- `[github]` - Documentation and repository files
- `[maven-release-plugin]` - Release process
- `[ssl]` - SSL/TLS functionality

### Testing Guidelines
- **Unit tests**: Fast, no external dependencies
- **Integration tests**: Use TestUtil.test() helper for server lifecycle
- **Browser tests**: Use WebDriverUtil (fails in CI, works locally with Chrome)
- **Test utilities**: io.javalin.testing package provides helpers

### Key Files to Monitor
- `pom.xml` - Main project configuration and dependencies
- `javalin/pom.xml` - Core module configuration  
- `javalin/src/main/java/io/javalin/Javalin.java` - Main API entry point
- `.github/workflows/main.yml` - CI configuration and build validation
- `README.md` - Developer instructions (different from public README)
- `.github/README.md` - Public documentation and examples

### Timing Expectations
- **Clean build**: 60-90 seconds
- **Full test suite**: 2-4 minutes  
- **Single module tests**: 30-120 seconds
- **Application startup**: 1-2 seconds
- **NEVER CANCEL** long-running builds - they will complete successfully

### Known Issues and Workarounds
- **OSGI Error**: Always run `package` before `test` 
- **Browser Test Failures**: Expected in CI - missing WebDriver dependencies
- **Profile Warning**: CI uses `-P dev` but profile doesn't exist (safely ignored)
- **Multiple SLF4J Bindings**: Warning is normal (both logback and slf4j-simple present)

## Quick Start Validation

After making changes, always run this validation sequence:

1. **Clean build**: `./mvnw clean package -DskipTests --batch-mode`
2. **Test core functionality**: Create and test basic Javalin server as shown above
3. **Run tests**: `./mvnw test -pl javalin --batch-mode` 
4. **Verify specific changes**: Test your specific functionality thoroughly

This ensures your changes integrate properly with the framework and don't break core functionality.

## Javalin Philosophy and Design Patterns

Javalin is designed as a lightweight library (not a framework) with these core principles:

### Core Principles
- **No Annotations**: All configuration is done through code, not annotations
- **No Reflection**: Everything is explicit and statically typed
- **No Magic**: Just plain code that's easy to understand and debug
- **No Extension Required**: You don't need to extend any base classes
- **Functional First**: Heavy use of lambdas and functional interfaces
- **Consumer-based Configuration**: All configuration uses `Consumer<Config>` pattern

### Configuration Patterns

#### Always Use Consumers for Configuration
Javalin uses the consumer pattern extensively for configuration. This allows for flexible, type-safe configuration:

```java
// CORRECT: Using consumer pattern
var app = Javalin.create(config -> {
    config.http.asyncTimeout = 10_000L;
    config.staticFiles.add("/public");
    config.useVirtualThreads = true;
});

// AVOID: Don't try to configure after creation
var app = Javalin.create();
app.unsafe.http.asyncTimeout = 10_000L; // Avoid using 'unsafe' unless necessary
```

#### Nested Configuration
Configuration objects are organized hierarchically and use consumers:

```java
Javalin.create(config -> {
    // HTTP layer configuration
    config.http.asyncTimeout = 10_000L;
    config.http.maxRequestSize = 1_000_000L;
    
    // Router configuration
    config.router.contextPath = "/api";
    config.router.treatMultipleSlashesAsSingleSlash = true;
    
    // Static files
    config.staticFiles.add("/public");
    config.staticFiles.enableWebjars();
    
    // Events using consumer
    config.events(events -> {
        events.serverStarting(() -> System.out.println("Starting..."));
        events.serverStarted(() -> System.out.println("Started!"));
    });
});
```

### Handler Patterns

#### Functional Interface Handlers
All handlers are functional interfaces - use lambdas or method references.

**NOTE**: In Javalin 7, routes are defined inside `Javalin.create()` using `config.routes`:

```java
// Lambda handler - PREFERRED for simple cases
Javalin.create(config -> {
    config.routes.get("/hello", ctx -> ctx.result("Hello"));
    
    // Multi-line lambda
    config.routes.post("/users", ctx -> {
        User user = ctx.bodyAsClass(User.class);
        UserService.create(user);
        ctx.status(201).json(user);
    });
    
    // Method reference - PREFERRED for complex logic
    config.routes.get("/users", UserController::getAll);
    config.routes.post("/users", UserController::create);
});

// AVOID: Don't create anonymous classes
config.routes.get("/bad", new Handler() {
    @Override
    public void handle(Context ctx) { // Too verbose
        ctx.result("Bad");
    }
});
```

#### Handler Return Type
Handlers have void return type - always use `ctx.result()` or `ctx.json()` to return data:

```java
// CORRECT: Use ctx methods to set response
Javalin.create(config -> {
    config.routes.get("/data", ctx -> {
        ctx.json(Map.of("key", "value"));
    });
});

// AVOID: Don't return values
config.routes.get("/wrong", ctx -> {
    return "Hello"; // WRONG - this doesn't work
});
```

### Routing Patterns

#### Two Routing Styles
Javalin supports two routing styles - choose based on your needs:

```java
// Style 1: Direct routing within config.routes - simple and clear
Javalin.create(config -> {
    config.routes.get("/users", UserController::getAll);
    config.routes.post("/users", UserController::create);
    config.routes.get("/users/{id}", UserController::getOne);
    config.routes.delete("/users/{id}", UserController::delete);
});

// Style 2: ApiBuilder - better for nested structures
import io.javalin.apibuilder.ApiBuilder.*;

Javalin.create(config -> {
    config.routes.apiBuilder(() -> {
        path("/users", () -> {
            get(UserController::getAll);
            post(UserController::create);
            path("/{id}", () -> {
                get(UserController::getOne);
                patch(UserController::update);
                delete(UserController::delete);
            });
            ws("/events", UserController::webSocketEvents);
        });
    });
});
```

#### Path Parameters and Query Parameters

```java
// Path parameters with {paramName}
Javalin.create(config -> {
    config.routes.get("/users/{userId}/posts/{postId}", ctx -> {
        String userId = ctx.pathParam("userId");
        String postId = ctx.pathParam("postId");
        ctx.json(PostService.getPost(userId, postId));
    });
    
    // Query parameters
    config.routes.get("/search", ctx -> {
        String query = ctx.queryParam("q"); // nullable
        int page = ctx.queryParamAsClass("page", Integer.class).getOrDefault(1);
        ctx.json(SearchService.search(query, page));
    });
});
```

### Lifecycle Management

#### Before and After Handlers
Use lifecycle handlers for cross-cutting concerns:

```java
Javalin.create(config -> {
    // Before all requests
    config.routes.before(ctx -> {
        ctx.header("X-Request-Id", UUID.randomUUID().toString());
    });
    
    // Before specific paths
    config.routes.before("/api/*", ctx -> {
        // Auth check, logging, etc.
        if (!isAuthenticated(ctx)) {
            throw new UnauthorizedResponse();
        }
    });
    
    // beforeMatched runs after routing is resolved
    config.routes.beforeMatched(ctx -> {
        // Access route-specific data like roles
        if (!hasRequiredRole(ctx)) {
            throw new ForbiddenResponse();
        }
    });
    
    // After handlers
    config.routes.after(ctx -> {
        // Logging, cleanup, etc.
        log.info("Request completed: {}", ctx.status());
    });
});
```

#### Exception Handling
Use exception handlers instead of try-catch in every route:

```java
Javalin.create(config -> {
    // Specific exception handling
    config.routes.exception(ValidationException.class, (e, ctx) -> {
        ctx.status(400).json(Map.of("error", e.getMessage()));
    });
    
    config.routes.exception(NotFoundException.class, (e, ctx) -> {
        ctx.status(404).json(Map.of("error", "Not found"));
    });
    
    // Generic exception handling
    config.routes.exception(Exception.class, (e, ctx) -> {
        log.error("Unhandled exception", e);
        ctx.status(500).json(Map.of("error", "Internal server error"));
    });
});

// In handlers, just throw - don't catch
app.get("/users/{id}", ctx -> {
    User user = UserService.findById(ctx.pathParam("id"))
        .orElseThrow(() -> new NotFoundException("User not found"));
    ctx.json(user);
});
```

#### Error Handlers
Error handlers run when a specific HTTP status is set:

```java
Javalin.create(config -> {
    config.routes.error(404, ctx -> {
        ctx.json(Map.of("error", "Page not found"));
    });
    
    config.routes.error(500, ctx -> {
        ctx.json(Map.of("error", "Server error"));
    });
});
```

### Context Usage Patterns

#### Request Data Access
The Context object provides all request information:

```java
Javalin.create(config -> {
    config.routes.post("/submit", ctx -> {
        // Body parsing
        User user = ctx.bodyAsClass(User.class);
        String rawBody = ctx.body();
        
        // Form parameters
        String name = ctx.formParam("name");
        
        // Uploaded files
        ctx.uploadedFiles("files").forEach(file -> {
            saveFile(file.content(), file.filename());
        });
        
        // Headers
        String authHeader = ctx.header("Authorization");
        Map<String, String> headers = ctx.headerMap();
        
        // Cookies
        String session = ctx.cookie("session");
        
        // Request info
        String method = ctx.method().toString();
        String path = ctx.path();
        String ip = ctx.ip();
    });
});
```

#### Response Building
Build responses using Context methods:

```java
Javalin.create(config -> {
    config.routes.get("/response", ctx -> {
        // JSON response (most common)
        ctx.json(myObject);
        
        // Plain text
        ctx.result("Hello World");
        
        // HTML
        ctx.html("<h1>Hello</h1>");
        
        // Status codes
        ctx.status(201);
        ctx.status(HttpStatus.CREATED);
        
        // Headers
        ctx.header("X-Custom", "value");
        ctx.contentType("application/json");
        
        // Cookies
        ctx.cookie("session", "abc123");
        ctx.cookie(new Cookie("name", "value", "/path", 3600));
        
        // Redirects
        ctx.redirect("/new-location");
        ctx.redirect("/new-location", HttpStatus.PERMANENT_REDIRECT);
    });
});
```

### Plugin Patterns

#### Using Plugins
Plugins are registered during configuration:

```java
import io.javalin.plugin.bundled.*;

Javalin.create(config -> {
    // Simple plugins without config
    config.registerPlugin(new DevLoggingPlugin());
    
    // Plugins with configuration using consumer pattern
    config.registerPlugin(new CorsPlugin(cors -> {
        cors.addRule(rule -> {
            rule.anyHost();
        });
    }));
    
    // SSL Plugin
    config.registerPlugin(new SSLPlugin(ssl -> {
        ssl.pemFromPath("/path/to/cert.pem", "/path/to/key.pem");
    }));
});
```

#### Creating Custom Plugins
Plugins extend the Plugin base class and use consumers for configuration:

```java
// Plugin with configuration
class MyPlugin extends Plugin<MyPlugin.Config> {
    
    MyPlugin(Consumer<Config> userConfig) {
        super(userConfig, new Config());
    }
    
    @Override
    public void onStart(JavalinConfig config) {
        // Register handlers, configure Javalin, etc.
        config.routes.before(ctx -> {
            // Plugin logic using pluginConfig
            if (pluginConfig.enabled) {
                doSomething();
            }
        });
    }
    
    public static class Config {
        public boolean enabled = true;
        public int timeout = 5000;
    }
}

// Usage
Javalin.create(config -> {
    config.registerPlugin(new MyPlugin(cfg -> {
        cfg.enabled = true;
        cfg.timeout = 10000;
    }));
});
```

#### Context-extending Plugins
Use ContextPlugin to add extension methods to Context:

```java
class RateLimitPlugin extends ContextPlugin<RateLimitPlugin.Config, RateLimitPlugin.Extension> {
    
    RateLimitPlugin(Consumer<Config> userConfig) {
        super(userConfig, new Config());
    }
    
    @Override
    public Extension createExtension(Context context) {
        return new Extension(context);
    }
    
    class Extension {
        Context ctx;
        Extension(Context ctx) { this.ctx = ctx; }
        
        public void checkLimit() {
            // Rate limiting logic
        }
    }
    
    static class Config {
        int limit = 100;
    }
}

// Usage in handlers
Javalin.create(config -> {
    config.routes.get("/", ctx -> {
        ctx.with(RateLimitPlugin.class).checkLimit();
        ctx.result("OK");
    });
});
```

### WebSocket Patterns

#### WebSocket Handlers
WebSocket handlers use a consumer pattern for configuration:

```java
Javalin.create(config -> {
    config.routes.ws("/chat", ws -> {
        ws.onConnect(ctx -> {
            System.out.println("Connected: " + ctx.sessionId());
            ctx.send("Welcome!");
        });
        
        ws.onMessage(ctx -> {
            String message = ctx.message();
            // Broadcast to all connected clients
            ctx.sessionManager().sessions().forEach(session -> {
                session.send(message);
            });
        });
        
        ws.onClose(ctx -> {
            System.out.println("Closed: " + ctx.sessionId());
        });
        
        ws.onError(ctx -> {
            System.err.println("Error: " + ctx.error());
        });
    });
});

// With ApiBuilder
import io.javalin.apibuilder.ApiBuilder.*;

Javalin.create(config -> {
    config.routes.apiBuilder(() -> {
        ws("/websocket", ws -> {
            ws.onConnect(ctx -> ctx.send("Hello"));
            ws.onMessage(ctx -> ctx.send("Echo: " + ctx.message()));
        });
    });
});
```

#### JSON over WebSocket

```java
Javalin.create(config -> {
    config.routes.ws("/events", ws -> {
        ws.onMessage(ctx -> {
            // Parse JSON message
            Event event = ctx.messageAsClass(Event.class);
            
            // Send JSON response
            ctx.send(new EventResponse(event.getId(), "processed"));
        });
    });
});
```

### Async and Future Patterns

#### CompletableFuture Support
Javalin has built-in support for async operations:

```java
Javalin.create(config -> {
    config.routes.get("/async", ctx -> {
        ctx.future(() -> {
            return CompletableFuture.supplyAsync(() -> {
                // Long-running operation
                return expensiveOperation();
            }).thenApply(result -> {
                ctx.json(result);
                return null;
            });
        });
    });
    
    // With executor
    ExecutorService executor = Executors.newFixedThreadPool(10);
    
    config.routes.get("/async-executor", ctx -> {
        ctx.future(() -> {
            return CompletableFuture.supplyAsync(() -> {
                return database.query();
            }, executor).thenApply(data -> {
                ctx.json(data);
                return null;
            });
        });
    });
});
```

#### Virtual Threads (Java 21+)
Enable virtual threads for better async performance:

```java
Javalin.create(config -> {
    config.useVirtualThreads = true;
}).start(7070);
```

### JSON Serialization Patterns

#### Using Built-in JSON Support
Javalin uses Jackson by default:

```java
Javalin.create(config -> {
    // Serialize to JSON
    config.routes.get("/users", ctx -> {
        List<User> users = UserService.getAll();
        ctx.json(users); // Automatic serialization
    });
    
    // Deserialize from JSON
    config.routes.post("/users", ctx -> {
        User user = ctx.bodyAsClass(User.class);
        UserService.save(user);
        ctx.status(201).json(user);
    });
    
    // Type-safe deserialization with generics
    config.routes.post("/items", ctx -> {
        List<Item> items = ctx.bodyAsClass(new TypeReference<List<Item>>() {});
        ctx.json(items);
    });
});
```

#### Custom JSON Mapper
Configure a custom JSON mapper if needed:

```java
Javalin.create(config -> {
    config.jsonMapper(new JavalinJackson()); // or custom implementation
});
```

### Common Anti-Patterns to Avoid

#### ❌ Don't Use Annotations
```java
// WRONG - Javalin doesn't use annotations
@Path("/users")
@GET
public void getUsers() { } // This won't work
```

#### ❌ Don't Extend Classes
```java
// WRONG - No need to extend anything
public class MyApp extends Javalin { } // Don't do this
```

#### ❌ Don't Use Reflection
```java
// WRONG - Javalin is reflection-free
config.routes.get("/users", "UserController.getAll"); // String-based routing doesn't exist
```

#### ❌ Don't Configure After Creation (Usually)
```java
// AVOID - Configure during creation
var app = Javalin.create();
app.unsafe.http.asyncTimeout = 5000; // Avoid 'unsafe' access

// PREFERRED - Configure in create()
var app = Javalin.create(config -> {
    config.http.asyncTimeout = 5000;
});
```

#### ❌ Don't Use Old-style Direct Routing (Pre-v7)
```java
// WRONG - This was the old Javalin 6 API
var app = Javalin.create();
app.get("/hello", ctx -> ctx.result("Hello")); // Doesn't exist in v7+

// CORRECT - v7+ routes go in config
Javalin.create(config -> {
    config.routes.get("/hello", ctx -> ctx.result("Hello"));
});
```

#### ❌ Don't Block WebSocket Handlers
```java
// WRONG - Blocking in WebSocket handler
ws.onMessage(ctx -> {
    Thread.sleep(5000); // Don't block!
    ctx.send("Delayed");
});

// CORRECT - Use async if needed
ws.onMessage(ctx -> {
    CompletableFuture.runAsync(() -> {
        // Long operation
    }).thenRun(() -> ctx.send("Done"));
});
```

#### ❌ Don't Catch Everything
```java
// AVOID - Use exception handlers instead
Javalin.create(config -> {
    config.routes.get("/users", ctx -> {
        try {
            User user = UserService.find(id);
            ctx.json(user);
        } catch (Exception e) {
            ctx.status(500).result("Error");
        }
    });
});

// PREFERRED - Let exception handlers handle it
Javalin.create(config -> {
    config.routes.exception(NotFoundException.class, (e, ctx) -> {
        ctx.status(404).json(Map.of("error", e.getMessage()));
    });
    
    config.routes.get("/users", ctx -> {
        User user = UserService.find(id); // Just throw if not found
        ctx.json(user);
    });
});
```

### Testing Patterns

#### Use TestUtil for Integration Tests
The TestUtil helper manages server lifecycle:

```java
@Test
void testEndpoint() {
    TestUtil.test((app, http) -> {
        // Define routes in the configuration
        Javalin configured = Javalin.create(config -> {
            config.routes.get("/hello", ctx -> ctx.result("Hello"));
        });
        
        // Note: TestUtil.test() expects routes to be added via app parameter
        // For simple tests, use the app parameter directly:
    });
    
    // Or use it this way:
    TestUtil.test(Javalin.create(config -> {
        config.routes.get("/hello", ctx -> ctx.result("Hello"));
    }), (app, http) -> {
        HttpResponse<String> response = http.get("/hello");
        assertThat(response.body()).isEqualTo("Hello");
        assertThat(response.statusCode()).isEqualTo(200);
    });
}

// With custom Javalin config
@Test
void testWithConfig() {
    Javalin app = Javalin.create(config -> {
        config.http.asyncTimeout = 5000L;
        config.routes.get("/test", ctx -> ctx.result("Test"));
    });
    
    TestUtil.test(app, (javalin, http) -> {
        assertThat(http.get("/test").statusCode()).isEqualTo(200);
    });
}
```

#### Unit Testing Handlers
Test handlers independently using mocks:

```java
@Test
void testHandler() throws Exception {
    Context ctx = mock(Context.class);
    when(ctx.pathParam("id")).thenReturn("123");
    
    UserController.getUser(ctx);
    
    verify(ctx).json(any(User.class));
}
```

### Best Practices Summary

1. **Always use consumers for configuration** - This is the Javalin way
2. **Prefer lambdas and method references** - Keep handlers clean and functional
3. **Use exception handlers** - Don't catch exceptions in every handler
4. **Choose the right routing style** - Direct for simple APIs, ApiBuilder for complex structures
5. **Leverage lifecycle handlers** - Use before/after for cross-cutting concerns
6. **Keep handlers focused** - Extract business logic to service classes
7. **Use plugins for reusable features** - Don't repeat yourself across apps
8. **Test with TestUtil** - It handles server lifecycle properly
9. **Embrace the functional style** - Javalin is designed for it
10. **Read the docs** - https://javalin.io/documentation has comprehensive examples
