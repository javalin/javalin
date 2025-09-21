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
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests to validate that multiple Javalin instances can run concurrently in the same JVM
 * without interfering with each other.
 */
public class TestMultipleInstances {

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

    @Test
    void multipleInstancesCanRunOnDifferentPorts() {
        // Create and start multiple instances on different ports
        Javalin app1 = Javalin.create()
            .get("/", ctx -> ctx.result("App 1"))
            .start(0); // random port

        Javalin app2 = Javalin.create()
            .get("/", ctx -> ctx.result("App 2"))
            .start(0); // random port

        instances.add(app1);
        instances.add(app2);

        // Verify they are running on different ports
        assertThat(app1.port()).isNotEqualTo(app2.port());

        // Verify each responds correctly
        HttpResponse<String> response1 = Unirest.get("http://localhost:" + app1.port()).asString();
        HttpResponse<String> response2 = Unirest.get("http://localhost:" + app2.port()).asString();

        assertThat(response1.getStatus()).isEqualTo(200);
        assertThat(response1.getBody()).isEqualTo("App 1");
        
        assertThat(response2.getStatus()).isEqualTo(200);
        assertThat(response2.getBody()).isEqualTo("App 2");
    }

    @Test
    void multipleInstancesHaveIsolatedConfigurations() {
        // Create instances with different configurations
        Javalin app1 = Javalin.create(config -> {
            config.router.contextPath = "/api";
        }).get("/test", ctx -> ctx.result("App 1")).start(0);

        Javalin app2 = Javalin.create(config -> {
            config.router.contextPath = "/service";
        }).get("/test", ctx -> ctx.result("App 2")).start(0);

        instances.add(app1);
        instances.add(app2);

        // Verify different context paths work independently
        HttpResponse<String> response1 = Unirest.get("http://localhost:" + app1.port() + "/api/test").asString();
        HttpResponse<String> response2 = Unirest.get("http://localhost:" + app2.port() + "/service/test").asString();

        assertThat(response1.getStatus()).isEqualTo(200);
        assertThat(response1.getBody()).isEqualTo("App 1");
        
        assertThat(response2.getStatus()).isEqualTo(200);
        assertThat(response2.getBody()).isEqualTo("App 2");

        // Verify context paths are isolated
        HttpResponse<String> wrongPath1 = Unirest.get("http://localhost:" + app1.port() + "/service/test").asString();
        HttpResponse<String> wrongPath2 = Unirest.get("http://localhost:" + app2.port() + "/api/test").asString();

        assertThat(wrongPath1.getStatus()).isEqualTo(404);
        assertThat(wrongPath2.getStatus()).isEqualTo(404);
    }

    @Test
    @Timeout(30)
    void concurrentRequestsToMultipleInstancesWorkCorrectly() throws Exception {
        final int numInstances = 3;
        final int requestsPerInstance = 50;
        
        // Create multiple instances
        for (int i = 0; i < numInstances; i++) {
            final int instanceId = i;
            Javalin app = Javalin.create()
                .get("/", ctx -> ctx.result("Instance " + instanceId))
                .start(0);
            instances.add(app);
        }

        // Send concurrent requests to all instances
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        AtomicInteger totalSuccess = new AtomicInteger(0);
        AtomicInteger totalErrors = new AtomicInteger(0);

        for (int i = 0; i < numInstances; i++) {
            final Javalin app = instances.get(i);
            final int expectedInstanceId = i;
            
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                for (int j = 0; j < requestsPerInstance; j++) {
                    try {
                        HttpResponse<String> response = Unirest.get("http://localhost:" + app.port()).asString();
                        if (response.getStatus() == 200 && 
                            response.getBody().equals("Instance " + expectedInstanceId)) {
                            totalSuccess.incrementAndGet();
                        } else {
                            totalErrors.incrementAndGet();
                        }
                    } catch (Exception e) {
                        totalErrors.incrementAndGet();
                    }
                }
            }, executor);
            
            futures.add(future);
        }

        // Wait for all requests to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get();

        // Verify all requests were successful
        assertThat(totalSuccess.get()).isEqualTo(numInstances * requestsPerInstance);
        assertThat(totalErrors.get()).isEqualTo(0);
    }

    @Test
    void instancesCanBeCreatedAndStartedUsingCreateAndStart() {
        Javalin app1 = Javalin.createAndStart(config -> {
            config.jetty.defaultPort = 0; // random port
        });
        app1.get("/", ctx -> ctx.result("CreateAndStart App"));
        
        instances.add(app1);

        HttpResponse<String> response = Unirest.get("http://localhost:" + app1.port()).asString();
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo("CreateAndStart App");
    }

    @Test
    void instancesHaveSeparateThreadPoolsWhenConfigured() {
        // Create instances with custom thread pool configurations
        Javalin app1 = Javalin.create(config -> {
            config.jetty.threadPool = io.javalin.util.ConcurrencyUtil.jettyThreadPool("App1Pool", 2, 10, false);
        }).get("/thread", ctx -> ctx.result("App1-" + Thread.currentThread().getName())).start(0);

        Javalin app2 = Javalin.create(config -> {
            config.jetty.threadPool = io.javalin.util.ConcurrencyUtil.jettyThreadPool("App2Pool", 2, 10, false);
        }).get("/thread", ctx -> ctx.result("App2-" + Thread.currentThread().getName())).start(0);

        instances.add(app1);
        instances.add(app2);

        // Make requests to verify different thread pools
        HttpResponse<String> response1 = Unirest.get("http://localhost:" + app1.port() + "/thread").asString();
        HttpResponse<String> response2 = Unirest.get("http://localhost:" + app2.port() + "/thread").asString();

        assertThat(response1.getStatus()).isEqualTo(200);
        assertThat(response2.getStatus()).isEqualTo(200);
        
        // Verify different thread pool names
        assertThat(response1.getBody()).contains("App1Pool");
        assertThat(response2.getBody()).contains("App2Pool");
        assertThat(response1.getBody()).doesNotContain("App2Pool");
        assertThat(response2.getBody()).doesNotContain("App1Pool");
    }

    @Test
    void apiBuilderWorksCorrectlyWithMultipleInstances() {
        Javalin app1 = Javalin.create(config -> {
            config.router.apiBuilder(() -> {
                io.javalin.apibuilder.ApiBuilder.path("/api", () -> {
                    io.javalin.apibuilder.ApiBuilder.get("/test", ctx -> ctx.result("App1 API"));
                });
            });
        }).start(0);

        Javalin app2 = Javalin.create(config -> {
            config.router.apiBuilder(() -> {
                io.javalin.apibuilder.ApiBuilder.path("/api", () -> {
                    io.javalin.apibuilder.ApiBuilder.get("/test", ctx -> ctx.result("App2 API"));
                });
            });
        }).start(0);

        instances.add(app1);
        instances.add(app2);

        // Verify ApiBuilder routes work independently
        HttpResponse<String> response1 = Unirest.get("http://localhost:" + app1.port() + "/api/test").asString();
        HttpResponse<String> response2 = Unirest.get("http://localhost:" + app2.port() + "/api/test").asString();

        assertThat(response1.getStatus()).isEqualTo(200);
        assertThat(response1.getBody()).isEqualTo("App1 API");
        
        assertThat(response2.getStatus()).isEqualTo(200);
        assertThat(response2.getBody()).isEqualTo("App2 API");
    }

    @Test
    void stressTestMultipleInstancesUnderLoad() throws Exception {
        final int numInstances = 5;
        final int numThreads = 20;
        final int requestsPerThread = 25;
        
        // Create multiple instances
        for (int i = 0; i < numInstances; i++) {
            final int instanceId = i;
            Javalin app = Javalin.create()
                .get("/load", ctx -> {
                    // Simulate some processing time
                    Thread.sleep(1);
                    ctx.result("Load test response from instance " + instanceId);
                })
                .start(0);
            instances.add(app);
        }

        // Create multiple threads making requests to all instances
        List<CompletableFuture<Integer>> futures = new ArrayList<>();
        
        for (int t = 0; t < numThreads; t++) {
            CompletableFuture<Integer> future = CompletableFuture.supplyAsync(() -> {
                int successCount = 0;
                for (int r = 0; r < requestsPerThread; r++) {
                    for (Javalin app : instances) {
                        try {
                            HttpResponse<String> response = Unirest.get("http://localhost:" + app.port() + "/load").asString();
                            if (response.getStatus() == 200 && response.getBody().contains("Load test response")) {
                                successCount++;
                            }
                        } catch (Exception e) {
                            // Count failures
                        }
                    }
                }
                return successCount;
            }, executor);
            
            futures.add(future);
        }

        // Wait for all requests and count total successes
        int totalSuccess = futures.stream()
            .map(CompletableFuture::join)
            .mapToInt(Integer::intValue)
            .sum();

        int expectedTotal = numThreads * requestsPerThread * numInstances;
        
        // We should have a very high success rate (allow for some network variability)
        assertThat(totalSuccess).isGreaterThanOrEqualTo((int)(expectedTotal * 0.95));
    }
}