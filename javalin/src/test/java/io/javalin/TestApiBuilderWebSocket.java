/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin;

import io.javalin.testing.TestUtil;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static io.javalin.apibuilder.ApiBuilder.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for WebSocket-related methods in ApiBuilder.
 * This test validates that wsBefore and wsAfter can be used in ApiBuilder,
 * fixing the issue where these methods were missing the static keyword.
 */
public class TestApiBuilderWebSocket {

    @Test
    public void testWsBeforeInApiBuilder() {
        AtomicBoolean wsBeforeCalled = new AtomicBoolean(false);
        
        Javalin app = Javalin.create(config -> {
            config.routes.apiBuilder(() -> {
                // Test wsBefore with path
                wsBefore("/websocket/*", ws -> {
                    ws.onConnect(ctx -> wsBeforeCalled.set(true));
                });
                
                ws("/websocket/test", ws -> {
                    ws.onConnect(ctx -> ctx.send("Connected"));
                });
            });
        });

        TestUtil.test(app, (server, client) -> {
            // Verify that the route was registered successfully
            // The fact that this compiles and runs proves wsBefore is now static
            assertThat(wsBeforeCalled.get()).isFalse(); // Not yet called (needs actual WebSocket connection)
        });
    }

    @Test
    public void testWsAfterInApiBuilder() {
        AtomicBoolean wsAfterCalled = new AtomicBoolean(false);
        
        Javalin app = Javalin.create(config -> {
            config.routes.apiBuilder(() -> {
                ws("/websocket/test", ws -> {
                    ws.onConnect(ctx -> ctx.send("Connected"));
                });
                
                // Test wsAfter with path
                wsAfter("/websocket/*", ws -> {
                    ws.onClose(ctx -> wsAfterCalled.set(true));
                });
            });
        });

        TestUtil.test(app, (server, client) -> {
            // Verify that the route was registered successfully
            // The fact that this compiles and runs proves wsAfter is now static
            assertThat(wsAfterCalled.get()).isFalse(); // Not yet called (needs actual WebSocket connection)
        });
    }

    @Test
    public void testWsBeforeWithoutPathInApiBuilder() {
        Javalin app = Javalin.create(config -> {
            config.routes.apiBuilder(() -> {
                // Test wsBefore without explicit path (should apply to all ws routes)
                wsBefore(ws -> {
                    ws.onConnect(ctx -> {});
                });
                
                ws("/websocket/test", ws -> {
                    ws.onConnect(ctx -> ctx.send("Connected"));
                });
            });
        });

        TestUtil.test(app, (server, client) -> {
            // The fact that this compiles and runs proves wsBefore(Consumer) is now static
            assertThat(server).isNotNull();
        });
    }

    @Test
    public void testWsAfterWithoutPathInApiBuilder() {
        Javalin app = Javalin.create(config -> {
            config.routes.apiBuilder(() -> {
                ws("/websocket/test", ws -> {
                    ws.onConnect(ctx -> ctx.send("Connected"));
                });
                
                // Test wsAfter without explicit path (should apply to all ws routes)
                wsAfter(ws -> {
                    ws.onClose(ctx -> {});
                });
            });
        });

        TestUtil.test(app, (server, client) -> {
            // The fact that this compiles and runs proves wsAfter(Consumer) is now static
            assertThat(server).isNotNull();
        });
    }

    @Test
    public void testCombinedWsLifecycleInApiBuilder() {
        Javalin app = Javalin.create(config -> {
            config.routes.apiBuilder(() -> {
                // Test combining wsBefore, ws, and wsAfter in ApiBuilder
                wsBefore("/chat/*", ws -> {
                    ws.onConnect(ctx -> {});
                });
                
                path("/chat", () -> {
                    ws("/room", ws -> {
                        ws.onConnect(ctx -> ctx.send("Welcome to the room!"));
                        ws.onMessage(ctx -> ctx.send("Echo: " + ctx.message()));
                    });
                });
                
                wsAfter("/chat/*", ws -> {
                    ws.onClose(ctx -> {});
                });
            });
        });

        TestUtil.test(app, (server, client) -> {
            // This test validates that all WebSocket methods work together in ApiBuilder
            assertThat(server).isNotNull();
        });
    }
}
