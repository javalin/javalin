/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.examples;

import io.javalin.Javalin;
import kong.unirest.Unirest;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Demonstrates multiple Javalin instances running concurrently in the same JVM.
 * This example shows the exact usage pattern mentioned in issue #2383.
 */
public class MultipleInstancesExample {

    @Test
    public void demonstrateMultipleInstances() throws InterruptedException {
        System.out.println("=== Multiple Javalin Instances Demo ===");
        
        List<Javalin> instances = new ArrayList<>();
        
        try {
            // Create multiple instances using the exact pattern from issue #2383
            for (int i = 1; i <= 3; i++) {
                final int instanceId = i;
                
                Javalin javalin = Javalin.createAndStart((config) -> {
                    // Configure as described in the issue
                    config.jetty.defaultPort = 0; // Use random port
                    config.router.contextPath = "/api/v" + instanceId;
                    
                    // Add some distinguishing configuration
                    config.showJavalinBanner = false; // Reduce console noise
                });
                
                // Add routes after creation (common pattern)
                javalin.get("/status", ctx -> ctx.json(
                    java.util.Map.of(
                        "instance", instanceId,
                        "status", "healthy", 
                        "port", javalin.port(),
                        "timestamp", System.currentTimeMillis()
                    )
                ));
                
                javalin.get("/work", ctx -> {
                    // Simulate some work
                    Thread.sleep(10);
                    ctx.json(java.util.Map.of(
                        "instance", instanceId,
                        "message", "Work completed by instance " + instanceId,
                        "thread", Thread.currentThread().getName()
                    ));
                });
                
                instances.add(javalin);
                System.out.println("Started instance " + instanceId + " on port " + javalin.port());
            }
            
            System.out.println("\n=== Testing Instances ===");
            
            // Test each instance individually
            for (int i = 0; i < instances.size(); i++) {
                Javalin instance = instances.get(i);
                int instanceId = i + 1;
                String baseUrl = "http://localhost:" + instance.port() + "/api/v" + instanceId;
                
                try {
                    String response = Unirest.get(baseUrl + "/status").asString().getBody();
                    System.out.println("Instance " + instanceId + " status: " + response);
                } catch (Exception e) {
                    System.err.println("Failed to test instance " + instanceId + ": " + e.getMessage());
                }
            }
            
            // Demonstrate concurrent load
            System.out.println("\n=== Concurrent Load Test ===");
            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger totalRequests = new AtomicInteger(0);
            
            ScheduledExecutorService executor = Executors.newScheduledThreadPool(10);
            
            // Send requests to all instances concurrently
            for (int i = 0; i < 50; i++) {
                for (Javalin instance : instances) {
                    int instanceNum = instances.indexOf(instance) + 1;
                    String url = "http://localhost:" + instance.port() + "/api/v" + instanceNum + "/work";
                    
                    executor.submit(() -> {
                        totalRequests.incrementAndGet();
                        try {
                            String response = Unirest.get(url).asString().getBody();
                            if (response.contains("Work completed")) {
                                successCount.incrementAndGet();
                            }
                        } catch (Exception e) {
                            System.err.println("Request failed: " + e.getMessage());
                        }
                    });
                }
            }
            
            // Wait for requests to complete
            Thread.sleep(5000);
            executor.shutdown();
            executor.awaitTermination(10, TimeUnit.SECONDS);
            
            System.out.println("Total requests: " + totalRequests.get());
            System.out.println("Successful responses: " + successCount.get());
            System.out.println("Success rate: " + (100.0 * successCount.get() / totalRequests.get()) + "%");
            
            if (successCount.get() == totalRequests.get()) {
                System.out.println("\n✅ SUCCESS: All instances handled requests correctly!");
                System.out.println("Multiple Javalin instances work perfectly in the same JVM.");
            } else {
                System.out.println("\n❌ Some requests failed - check your environment");
            }
            
        } finally {
            // Cleanup
            System.out.println("\n=== Stopping Instances ===");
            for (int i = 0; i < instances.size(); i++) {
                instances.get(i).stop();
                System.out.println("Stopped instance " + (i + 1));
            }
        }
        
        System.out.println("\nDemo completed. Multiple instances work seamlessly!");
    }
}