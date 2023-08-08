package io.javalin.performance;

import io.javalin.config.JavalinConfig;
import io.javalin.config.PrivateConfig;
import io.javalin.config.RoutingConfig;
import static io.javalin.http.HandlerType.GET;

import io.javalin.http.HandlerType;
import io.javalin.router.HandlerEntry;
import io.javalin.router.matcher.PathMatcher;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.stream.Collectors;

@State(Scope.Benchmark)
@Fork(value = 1)
@Warmup(iterations = 2)
@Measurement(iterations = 2)
@SuppressWarnings({ "OptionalGetWithoutIsPresent" })
public class PathMatcherBenchmark {

    public static void main(String[] args) throws Exception {
        var opt = new OptionsBuilder()
            .include(PathMatcherBenchmark.class.getName())
            .build();
        new Runner(opt).run();
    }

    private OldPathMatcher oldPathMatcher;
    private PathMatcher pathMatcher;

    @Setup
    public void setup() {
        this.oldPathMatcher = new OldPathMatcher();
        this.pathMatcher = new PathMatcher();
        var routingConfig = new RoutingConfig(new JavalinConfig());
        for (int i = 0; i < 50; i++) {
            this.pathMatcher.add(new HandlerEntry(GET, "/hello" + i, routingConfig, Collections.emptySet(), (ctx) -> {}));
            this.oldPathMatcher.add(new HandlerEntry(GET, "/hello" + i, routingConfig, Collections.emptySet(), (ctx) -> {}));
        }
    }

    @Benchmark
    public void matchFirstStream(Blackhole blackhole) {
        blackhole.consume(pathMatcher.findEntries(GET, "/hello0").findFirst().get());
    }

    @Benchmark
    public void matchLastStream(Blackhole blackhole) {
        blackhole.consume(pathMatcher.findEntries(GET, "/hello49").findFirst().get());
    }

    @Benchmark
    public void matchFirstList(Blackhole blackhole) {
        blackhole.consume(oldPathMatcher.findEntries(GET, "/hello0").iterator().next());
    }

    @Benchmark
    public void matchLastList(Blackhole blackhole) {
        blackhole.consume(oldPathMatcher.findEntries(GET, "/hello49").iterator().next());
    }

}

final class OldPathMatcher {
    @SuppressWarnings("Convert2Diamond")
    private final EnumMap<HandlerType, ArrayList<HandlerEntry>> handlerEntries = new EnumMap<HandlerType, ArrayList<HandlerEntry>>(
        Arrays.stream(HandlerType.values()).collect(Collectors.toMap((handler) -> handler, (handler) -> new ArrayList<>()))
    );

    public void add(HandlerEntry entry) {
        handlerEntries.get(entry.getType()).add(entry);
    }

    public List<HandlerEntry> findEntries(HandlerType handlerType, String requestUri) {
        var results = new ArrayList<HandlerEntry>();
        handlerEntries.get(handlerType).forEach(entry -> {
            if (match(entry, requestUri)) {
                results.add(entry);
            }
        });
        return results;
    }

    private boolean match(HandlerEntry entry, String requestPath) {
        if (entry.getPath().equals("*")) return true;
        if (entry.getPath().equals(requestPath)) return true;
        return entry.matches(requestPath);
    }
}

