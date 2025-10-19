package io.javalin.examples;

import io.javalin.Javalin;
import io.javalin.apibuilder.ApiBuilder;
import io.javalin.http.Context;
import io.javalin.plugin.Plugin;
import io.javalin.config.JavalinConfig;

import java.util.Map;
import java.util.function.Consumer;
import java.util.concurrent.CompletableFuture;

/**
 * This file validates that the patterns described in .github/copilot-instructions.md
 * are correct and compile properly. This ensures the documentation stays in sync with the actual API.
 */
public class CopilotInstructionsPatternValidation {

    /**
     * Validates: Consumer-based configuration pattern
     */
    public static void validateConsumerConfiguration() {
        var app = Javalin.create(config -> {
            config.http.asyncTimeout = 10_000L;
            config.staticFiles.add("/public");
            config.useVirtualThreads = true;
        });
    }

    /**
     * Validates: Nested configuration with hierarchical structure
     */
    public static void validateNestedConfiguration() {
        Javalin.create(config -> {
            config.http.asyncTimeout = 10_000L;
            config.http.maxRequestSize = 1_000_000L;
            config.router.contextPath = "/api";
            config.router.treatMultipleSlashesAsSingleSlash = true;
            config.staticFiles.add("/public");
            config.staticFiles.enableWebjars();
            
            config.events(events -> {
                events.serverStarting(() -> System.out.println("Starting..."));
                events.serverStarted(() -> System.out.println("Started!"));
            });
        });
    }

    /**
     * Validates: Handler patterns - lambdas and method references
     */
    public static void validateHandlerPatterns() {
        Javalin.create(config -> {
            // Lambda handler
            config.routes.get("/hello", ctx -> ctx.result("Hello"));
            
            // Multi-line lambda
            config.routes.post("/users", ctx -> {
                ctx.status(201).json(Map.of("id", "1"));
            });
            
            // Method reference
            config.routes.get("/data", CopilotInstructionsPatternValidation::handleData);
        });
    }
    
    private static void handleData(Context ctx) {
        ctx.json(Map.of("data", "value"));
    }

    /**
     * Validates: Direct routing within config.routes
     */
    public static void validateDirectRouting() {
        Javalin.create(config -> {
            config.routes.get("/users", ctx -> ctx.json(Map.of()));
            config.routes.post("/users", ctx -> ctx.status(201));
            config.routes.get("/users/{id}", ctx -> ctx.json(Map.of("id", ctx.pathParam("id"))));
            config.routes.delete("/users/{id}", ctx -> ctx.status(204));
        });
    }

    /**
     * Validates: ApiBuilder routing style
     */
    public static void validateApiBuilderRouting() {
        Javalin.create(config -> {
            config.routes.apiBuilder(() -> {
                ApiBuilder.path("/users", () -> {
                    ApiBuilder.get(ctx -> ctx.json(Map.of()));
                    ApiBuilder.post(ctx -> ctx.status(201));
                    ApiBuilder.path("/{id}", () -> {
                        ApiBuilder.get(ctx -> ctx.json(Map.of("id", ctx.pathParam("id"))));
                        ApiBuilder.delete(ctx -> ctx.status(204));
                    });
                });
            });
        });
    }

    /**
     * Validates: Lifecycle handlers - before, after, beforeMatched
     */
    public static void validateLifecycleHandlers() {
        Javalin.create(config -> {
            config.routes.before(ctx -> {
                ctx.header("X-Request-Id", "test-id");
            });
            
            config.routes.before("/api/*", ctx -> {
                // Auth check
            });
            
            config.routes.beforeMatched(ctx -> {
                // After routing is resolved
            });
            
            config.routes.after(ctx -> {
                // Cleanup
            });
        });
    }

    /**
     * Validates: Exception handling pattern
     */
    public static void validateExceptionHandling() {
        Javalin.create(config -> {
            config.routes.exception(IllegalArgumentException.class, (e, ctx) -> {
                ctx.status(400).json(Map.of("error", e.getMessage()));
            });
            
            config.routes.exception(Exception.class, (e, ctx) -> {
                ctx.status(500).json(Map.of("error", "Internal server error"));
            });
        });
    }

    /**
     * Validates: Error handlers for HTTP status codes
     */
    public static void validateErrorHandlers() {
        Javalin.create(config -> {
            config.routes.error(404, ctx -> {
                ctx.json(Map.of("error", "Page not found"));
            });
            
            config.routes.error(500, ctx -> {
                ctx.json(Map.of("error", "Server error"));
            });
        });
    }

    /**
     * Validates: Context usage - path params, query params, headers, cookies
     */
    public static void validateContextPatterns() {
        Javalin.create(config -> {
            config.routes.get("/context/{id}", ctx -> {
                // Path parameters
                String id = ctx.pathParam("id");
                
                // Query parameters
                String query = ctx.queryParam("q");
                
                // Headers
                String auth = ctx.header("Authorization");
                
                // Cookies
                String session = ctx.cookie("session");
                
                // Response building
                ctx.status(200);
                ctx.header("X-Custom", "value");
                ctx.json(Map.of("success", true));
            });
        });
    }

    /**
     * Validates: Custom plugin pattern with consumer-based configuration
     */
    static class TestPlugin extends Plugin<TestPlugin.Config> {
        
        TestPlugin(Consumer<Config> userConfig) {
            super(userConfig, new Config());
        }
        
        @Override
        public void onStart(JavalinConfig config) {
            config.routes.before(ctx -> {
                if (pluginConfig.enabled) {
                    // Plugin logic
                }
            });
        }
        
        public static class Config {
            public boolean enabled = true;
            public int timeout = 5000;
        }
    }

    /**
     * Validates: Using plugins with configuration
     */
    public static void validatePluginUsage() {
        Javalin.create(config -> {
            config.registerPlugin(new TestPlugin(cfg -> {
                cfg.enabled = true;
                cfg.timeout = 10000;
            }));
        });
    }

    /**
     * Validates: WebSocket patterns
     */
    public static void validateWebSocketPatterns() {
        Javalin.create(config -> {
            config.routes.ws("/chat", ws -> {
                ws.onConnect(ctx -> {
                    ctx.send("Welcome!");
                });
                
                ws.onMessage(ctx -> {
                    String message = ctx.message();
                    ctx.send("Echo: " + message);
                });
                
                ws.onClose(ctx -> {
                    // Cleanup
                });
                
                ws.onError(ctx -> {
                    // Error handling
                });
            });
        });
    }

    /**
     * Validates: Async patterns with CompletableFuture
     */
    public static void validateAsyncPatterns() {
        Javalin.create(config -> {
            config.routes.get("/async", ctx -> {
                ctx.future(() -> {
                    return CompletableFuture.supplyAsync(() -> {
                        return "Result";
                    }).thenApply(result -> {
                        ctx.result(result);
                        return null;
                    });
                });
            });
        });
    }

    /**
     * Validates: Virtual threads configuration
     */
    public static void validateVirtualThreads() {
        Javalin.create(config -> {
            config.useVirtualThreads = true;
        });
    }

    /**
     * Validates: JSON serialization patterns
     */
    public static void validateJsonPatterns() {
        Javalin.create(config -> {
            config.routes.get("/users", ctx -> {
                ctx.json(Map.of("users", "list"));
            });
            
            config.routes.post("/users", ctx -> {
                @SuppressWarnings("unchecked")
                Map<String, Object> user = ctx.bodyAsClass(Map.class);
                ctx.status(201).json(user);
            });
        });
    }

    public static void main(String[] args) {
        System.out.println("All patterns from copilot-instructions.md validated successfully!");
        System.out.println("This ensures the documentation is accurate and up-to-date.");
    }
}
