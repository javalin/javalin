package io.javalin.performance;

import io.javalin.Javalin;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.profile.GCProfiler;
import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

/**
 * Performance benchmark suite for Javalin's core responsibilities.
 * Spins up a real Javalin server and measures end-to-end throughput + allocations.
 * Run via IDE: execute the main() method.
 * Run via Maven:
 *   mvn test-compile exec:java -pl javalin \
 *     -Dexec.mainClass="io.javalin.performance.PerformanceBenchmarkSuite" \
 *     -Dexec.classpathScope=test
 * Total runtime: ~50 seconds.
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Fork(1)
@Warmup(iterations = 3, time = 2)
@Measurement(iterations = 5, time = 3)
public class PerformanceBenchmarkSuite {

    private Javalin app;
    private HttpClient httpClient;

    // Pre-built requests (immutable after setup)
    private HttpRequest reqPlainGet;
    private HttpRequest reqSingleParam;
    private HttpRequest reqMultiParam;
    private HttpRequest reqStaticFile;
    private HttpRequest reqStaticFileMissing;
    private HttpRequest reqFirstRoute;
    private HttpRequest reqLastRoute;
    private HttpRequest reqPostBody;

    private static final HttpResponse.BodyHandler<byte[]> BYTE_HANDLER = HttpResponse.BodyHandlers.ofByteArray();

    @Setup(Level.Trial)
    public void setup() {
        app = Javalin.create(config -> {
            config.startup.showJavalinBanner = false;

            // Static file serving from classpath
            config.staticFiles.add("/public");

            // Plain routes
            config.routes.get("/hello", ctx -> ctx.result("OK"));

            // Path param routes
            config.routes.get("/users/{id}", ctx -> ctx.result("user-" + ctx.pathParam("id")));
            config.routes.get("/users/{userId}/posts/{postId}", ctx -> ctx.result(ctx.pathParam("userId") + "-" + ctx.pathParam("postId")));

            // POST
            config.routes.post("/echo", ctx -> ctx.result(ctx.body()));

            // Many routes (for first-match vs last-match comparison)
            for (int i = 0; i < 50; i++) {
                final int idx = i;
                config.routes.get("/r" + i, ctx -> ctx.result("r" + idx));
            }
        }).start(0);

        String base = "http://localhost:" + app.port();
        httpClient = HttpClient.newHttpClient();

        reqPlainGet        = get(base + "/hello");
        reqSingleParam     = get(base + "/users/42");
        reqMultiParam      = get(base + "/users/7/posts/123");
        reqStaticFile      = get(base + "/html.html");
        reqStaticFileMissing = get(base + "/does-not-exist.xyz");
        reqFirstRoute      = get(base + "/r0");
        reqLastRoute       = get(base + "/r49");
        reqPostBody        = HttpRequest.newBuilder(URI.create(base + "/echo"))
            .POST(HttpRequest.BodyPublishers.ofString("{\"key\":\"value\"}"))
            .header("Content-Type", "application/json")
            .build();
    }

    @TearDown(Level.Trial)
    public void tearDown() {
        if (app != null) app.stop();
    }

    // -- Benchmarks ----------------------------------------------------------

    @Benchmark
    public void plainGet(Blackhole bh) throws Exception {
        bh.consume(httpClient.send(reqPlainGet, BYTE_HANDLER));
    }

    @Benchmark
    public void singlePathParam(Blackhole bh) throws Exception {
        bh.consume(httpClient.send(reqSingleParam, BYTE_HANDLER));
    }

    @Benchmark
    public void multiPathParam(Blackhole bh) throws Exception {
        bh.consume(httpClient.send(reqMultiParam, BYTE_HANDLER));
    }

    @Benchmark
    public void staticFile(Blackhole bh) throws Exception {
        bh.consume(httpClient.send(reqStaticFile, BYTE_HANDLER));
    }

    @Benchmark
    public void staticFile_miss(Blackhole bh) throws Exception {
        bh.consume(httpClient.send(reqStaticFileMissing, BYTE_HANDLER));
    }

    @Benchmark
    public void manyRoutes_first(Blackhole bh) throws Exception {
        bh.consume(httpClient.send(reqFirstRoute, BYTE_HANDLER));
    }

    @Benchmark
    public void manyRoutes_last(Blackhole bh) throws Exception {
        bh.consume(httpClient.send(reqLastRoute, BYTE_HANDLER));
    }

    @Benchmark
    public void postBody(Blackhole bh) throws Exception {
        bh.consume(httpClient.send(reqPostBody, BYTE_HANDLER));
    }

    // -- Helpers -------------------------------------------------------------

    private static HttpRequest get(String url) {
        return HttpRequest.newBuilder(URI.create(url)).GET().build();
    }

    // -- Runner + Table ------------------------------------------------------

    public static void main(String[] args) throws Exception {
        var opt = new OptionsBuilder()
            .include(PerformanceBenchmarkSuite.class.getName())
            .addProfiler(GCProfiler.class)
            .build();

        Collection<RunResult> results = new Runner(opt).run();
        printTable(results);
    }

    private static void printTable(Collection<RunResult> results) {
        record Row(String name, String rps, String alloc) {}
        var rows = new ArrayList<Row>();

        for (var rr : results) {
            var p = rr.getPrimaryResult();
            String name = p.getLabel()
                .replace("io.javalin.performance.PerformanceBenchmarkSuite.", "");
            String rps = fmt(p.getScore()) + " +/- " + fmt(p.getStatistics().getMeanErrorAt(0.99));

            String alloc = "-";
            for (var e : rr.getSecondaryResults().entrySet()) {
                if (e.getKey().contains("gc.alloc.rate.norm")) {
                    alloc = String.format("%.0f B/op", e.getValue().getScore());
                    break;
                }
            }
            rows.add(new Row(name, rps, alloc));
        }

        int nW = Math.max(9,  rows.stream().mapToInt(r -> r.name.length()).max().orElse(0));
        int rW = Math.max(5,  rows.stream().mapToInt(r -> r.rps.length()).max().orElse(0));
        int aW = Math.max(12, rows.stream().mapToInt(r -> r.alloc.length()).max().orElse(0));
        String bar = "=".repeat(nW + rW + aW + 10);
        String sep = "-".repeat(nW + 2) + "+" + "-".repeat(rW + 2) + "+" + "-".repeat(aW + 2);
        String f   = " %-" + nW + "s | %" + rW + "s | %" + aW + "s%n";

        System.out.println();
        System.out.println(bar);
        System.out.println(" JAVALIN PERFORMANCE SUITE  (real HTTP, single-threaded client)");
        System.out.println(bar);
        System.out.printf(f, "Benchmark", "rps", "alloc (B/op)");
        System.out.println(sep);
        rows.forEach(r -> System.out.printf(f, r.name, r.rps, r.alloc));
        System.out.println(bar);
        System.out.println();
    }

    private static String fmt(double v) {
        if (Double.isNaN(v) || Double.isInfinite(v)) return "N/A";
        if (v >= 1_000_000) return String.format("%.2fM", v / 1_000_000);
        if (v >= 1_000)     return String.format("%.2fK", v / 1_000);
        return String.format("%.2f", v);
    }
}
