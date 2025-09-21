package io.javalin.testtools;

import io.javalin.Javalin;

public class CookieValidationApp {
    public static void main(String[] args) {
        System.out.println("Testing manual cookie functionality...");
        
        JavalinTest.test((server, client) -> {
            server.get("/set-session", ctx -> {
                ctx.sessionAttribute("user", "john_doe");
                ctx.result("Session set");
            });
            
            server.get("/get-session", ctx -> {
                String user = ctx.sessionAttribute("user");
                ctx.result(user != null ? "User: " + user : "No session");
            });
            
            server.get("/set-custom-cookie", ctx -> {
                ctx.cookie("custom", "test123");
                ctx.result("Cookie set");
            });
            
            server.get("/get-custom-cookie", ctx -> {
                String cookie = ctx.cookie("custom");
                ctx.result(cookie != null ? "Cookie: " + cookie : "No cookie");
            });
            
            // Test session functionality
            var sessionSetResponse = client.get("/set-session");
            System.out.println("Set session response: " + sessionSetResponse.body().string());
            System.out.println("Session cookie: " + sessionSetResponse.headers().get("Set-Cookie"));
            
            var sessionGetResponse = client.get("/get-session");
            System.out.println("Get session response: " + sessionGetResponse.body().string());
            
            // Test custom cookie functionality
            var cookieSetResponse = client.get("/set-custom-cookie");
            System.out.println("Set cookie response: " + cookieSetResponse.body().string());
            System.out.println("Custom cookie: " + cookieSetResponse.headers().get("Set-Cookie"));
            
            var cookieGetResponse = client.get("/get-custom-cookie");
            System.out.println("Get cookie response: " + cookieGetResponse.body().string());
        });
        
        System.out.println("Manual validation completed successfully!");
    }
}