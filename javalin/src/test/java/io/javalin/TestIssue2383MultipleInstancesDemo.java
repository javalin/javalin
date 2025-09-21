/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin;

import io.javalin.testing.HttpUtil;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Timeout;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Demonstration test that replicates the exact scenario described in issue #2383.
 * This shows that multiple Javalin instances using createAndStart() work correctly
 * and can handle concurrent load without interfering with each other.
 */
public class TestIssue2383MultipleInstancesDemo {

    private List<Javalin> instances;
    private ExecutorService executor;

    @BeforeEach
    void setUp() {
        instances = new ArrayList<>();
        executor = Executors.newCachedThreadPool();
    }

    @AfterEach
    void tearDown() {
        instances.forEach(Javalin::stop);
        instances.clear();
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * This test replicates the exact usage pattern described in issue #2383:
     * Multiple Javalin instances created using createAndStart() with custom configuration.
     */
    @Test
    @Timeout(60)
    void demonstrateMultipleInstancesFromIssue2383() throws Exception {
        final int numInstances = 3;
        final int requestsPerInstance = 100;
        final int concurrentThreads = 10;

        // Create multiple instances exactly as described in the issue
        for (int i = 0; i < numInstances; i++) {
            final int instanceId = i;
            
            Javalin javalin = Javalin.createAndStart((config) -> {
                // Simulate custom configuration as mentioned in the issue
                config.jetty.defaultPort = 0; // Use random port like in real scenario
                config.router.contextPath = "/app" + instanceId;
                
                // Add some basic configuration to make instances distinguishable
                config.bundledPlugins.enableCors(cors -> {
                    cors.addRule(it -> {
                        it.anyHost();
                    });
                });
            });
            
            // Add routes after creation (common pattern)
            javalin.get("/health", ctx -> ctx.json(
                java.util.Map.of(
                    "status", "healthy",
                    "instance", instanceId,
                    "timestamp", System.currentTimeMillis()
                )
            ));
            
            javalin.get("/load", ctx -> {
                // Simulate some processing work
                Thread.sleep(5); // Small delay to simulate real work
                ctx.json(java.util.Map.of(
                    "instance", instanceId,
                    "thread", Thread.currentThread().getName(),
                    "processed", System.currentTimeMillis()
                ));
            });
            
            instances.add(javalin);
        }

        // Verify all instances are running on different ports
        List<Integer> ports = instances.stream().mapToInt(Javalin::port).boxed().toList();
        assertThat(ports).hasSize(numInstances);
        assertThat(ports.stream().distinct().count()).isEqualTo(numInstances); // All different ports

        // Test concurrent load across all instances (simulating the problematic scenario)
        List<CompletableFuture<InstanceTestResult>> futures = new ArrayList<>();
        
        for (int t = 0; t < concurrentThreads; t++) {
            final int threadId = t;
            
            CompletableFuture<InstanceTestResult> future = CompletableFuture.supplyAsync(() -> {
                InstanceTestResult result = new InstanceTestResult();
                
                for (int r = 0; r < requestsPerInstance; r++) {
                    for (int i = 0; i < numInstances; i++) {
                        Javalin instance = instances.get(i);
                        String url = "http://localhost:" + instance.port() + "/app" + i + "/load";
                        
                        try {
                            HttpResponse<String> response = Unirest.get(url).asString();
                            if (response.getStatus() == 200) {
                                result.successCount++;
                                // Verify the response contains the correct instance ID
                                if (response.getBody().contains("\"instance\":" + i)) {
                                    result.correctInstanceResponses++;
                                }
                            } else {
                                result.failureCount++;
                            }
                        } catch (Exception e) {
                            result.exceptionCount++;
                        }
                    }
                }
                
                result.threadId = threadId;
                return result;
            }, executor);
            
            futures.add(future);
        }

        // Wait for all concurrent requests to complete
        List<InstanceTestResult> results = futures.stream()
            .map(CompletableFuture::join)
            .toList();

        // Analyze results
        int totalSuccess = results.stream().mapToInt(r -> r.successCount).sum();
        int totalFailures = results.stream().mapToInt(r -> r.failureCount).sum();
        int totalExceptions = results.stream().mapToInt(r -> r.exceptionCount).sum();
        int correctResponses = results.stream().mapToInt(r -> r.correctInstanceResponses).sum();
        
        int expectedTotal = concurrentThreads * requestsPerInstance * numInstances;
        
        // Verify that all instances handled requests correctly without interference
        System.out.println("=== Multiple Instances Test Results ===");
        System.out.println("Total requests: " + expectedTotal);
        System.out.println("Successful responses: " + totalSuccess);
        System.out.println("Failed responses: " + totalFailures);
        System.out.println("Exceptions: " + totalExceptions);
        System.out.println("Correct instance responses: " + correctResponses);
        System.out.println("Success rate: " + (100.0 * totalSuccess / expectedTotal) + "%");
        
        // The instances should handle all requests successfully
        assertThat(totalSuccess).isEqualTo(expectedTotal);
        assertThat(totalFailures).isEqualTo(0);
        assertThat(totalExceptions).isEqualTo(0);
        assertThat(correctResponses).isEqualTo(expectedTotal);
        
        // Verify health endpoints also work correctly
        for (int i = 0; i < numInstances; i++) {
            Javalin instance = instances.get(i);
            String healthUrl = "http://localhost:" + instance.port() + "/app" + i + "/health";
            HttpResponse<String> healthResponse = Unirest.get(healthUrl).asString();
            
            assertThat(healthResponse.getStatus()).isEqualTo(200);
            assertThat(healthResponse.getBody()).contains("\"status\":\"healthy\"");
            assertThat(healthResponse.getBody()).contains("\"instance\":" + i);
        }
    }

    /**
     * Test that validates the specific createAndStart pattern mentioned in the issue
     */
    @Test
    void validateCreateAndStartPattern() {
        // This exactly matches the pattern shown in the issue:
        Javalin javalin1 = Javalin.createAndStart((config) -> {
            config.jetty.defaultPort = 0;
            // Configure as needed
        });
        javalin1.get("/test", ctx -> ctx.result("Instance 1"));
        
        Javalin javalin2 = Javalin.createAndStart((config) -> {
            config.jetty.defaultPort = 0;
            // Configure as needed  
        });
        javalin2.get("/test", ctx -> ctx.result("Instance 2"));
        
        instances.add(javalin1);
        instances.add(javalin2);
        
        // Verify both instances work independently
        HttpResponse<String> response1 = Unirest.get("http://localhost:" + javalin1.port() + "/test").asString();
        HttpResponse<String> response2 = Unirest.get("http://localhost:" + javalin2.port() + "/test").asString();
        
        assertThat(response1.getStatus()).isEqualTo(200);
        assertThat(response1.getBody()).isEqualTo("Instance 1");
        
        assertThat(response2.getStatus()).isEqualTo(200);
        assertThat(response2.getBody()).isEqualTo("Instance 2");
        
        // Verify they are on different ports
        assertThat(javalin1.port()).isNotEqualTo(javalin2.port());
    }

    /**
     * Test that shows static API (ApiBuilder) is thread-safe across instances
     */
    @Test
    void staticApiIsThreadSafeAcrossInstances() throws Exception {
        final int numInstances = 5;
        
        // Create instances concurrently using static API
        List<CompletableFuture<Javalin>> creationFutures = new ArrayList<>();
        
        for (int i = 0; i < numInstances; i++) {
            final int instanceId = i;
            
            CompletableFuture<Javalin> future = CompletableFuture.supplyAsync(() -> {
                return Javalin.create(config -> {
                    config.router.apiBuilder(() -> {
                        io.javalin.apibuilder.ApiBuilder.path("/api/v" + instanceId, () -> {
                            io.javalin.apibuilder.ApiBuilder.get("/test", ctx -> 
                                ctx.result("API Version " + instanceId));
                        });
                    });
                }).start(0);
            }, executor);
            
            creationFutures.add(future);
        }
        
        // Wait for all instances to be created
        List<Javalin> createdInstances = creationFutures.stream()
            .map(CompletableFuture::join)
            .toList();
        
        instances.addAll(createdInstances);
        
        // Test all instances work correctly
        for (int i = 0; i < numInstances; i++) {
            Javalin instance = createdInstances.get(i);
            String url = "http://localhost:" + instance.port() + "/api/v" + i + "/test";
            HttpResponse<String> response = Unirest.get(url).asString();
            
            assertThat(response.getStatus()).isEqualTo(200);
            assertThat(response.getBody()).isEqualTo("API Version " + i);
        }
    }

    private static class InstanceTestResult {
        int threadId;
        int successCount = 0;
        int failureCount = 0;
        int exceptionCount = 0;
        int correctInstanceResponses = 0;
    }
}