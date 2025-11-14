package io.javalin.examples;

import io.javalin.Javalin;

import static io.javalin.apibuilder.ApiBuilder.*;

/**
 * Manual verification test to demonstrate that wsBefore and wsAfter 
 * now work properly in ApiBuilder after adding the static keyword.
 * 
 * This compiles successfully, proving the fix works.
 */
public class WebSocketApiBuilderDemo {
    
    public static void main(String[] args) {
        Javalin app = Javalin.create(config -> {
            config.routes.apiBuilder(() -> {
                
                // WebSocket before handler - now works with static keyword
                wsBefore("/ws/*", ws -> {
                    ws.onConnect(ctx -> System.out.println("WsBefore: WebSocket connecting to " + ctx.session.getUpgradeRequest().getRequestURI()));
                });
                
                // WebSocket route
                path("/ws", () -> {
                    ws("/chat", ws -> {
                        ws.onConnect(ctx -> {
                            System.out.println("Connected to /ws/chat");
                            ctx.send("Welcome to the chat!");
                        });
                        ws.onMessage(ctx -> {
                            System.out.println("Message received: " + ctx.message());
                            ctx.send("Echo: " + ctx.message());
                        });
                        ws.onClose(ctx -> {
                            System.out.println("Disconnected from /ws/chat");
                        });
                    });
                });
                
                // WebSocket after handler - now works with static keyword
                wsAfter("/ws/*", ws -> {
                    ws.onClose(ctx -> System.out.println("WsAfter: WebSocket closed"));
                });
                
                // Also test the versions without explicit paths
                wsBefore(ws -> {
                    ws.onConnect(ctx -> System.out.println("Global wsBefore handler"));
                });
                
                wsAfter(ws -> {
                    ws.onClose(ctx -> System.out.println("Global wsAfter handler"));
                });
            });
        });
        
        System.out.println("✓ Success! The application compiled and configured successfully.");
        System.out.println("✓ wsBefore and wsAfter are now usable in ApiBuilder with the static keyword.");
        System.out.println("\nTo test the WebSocket functionality:");
        System.out.println("1. Uncomment app.start(7070)");
        System.out.println("2. Run this class");
        System.out.println("3. Connect to ws://localhost:7070/ws/chat with a WebSocket client");
        
        // Uncomment to actually start the server:
        // app.start(7070);
    }
}
