package io.javalin.performance;

import io.javalin.config.JavalinConfig;
import io.javalin.config.RouterConfig;
import static io.javalin.http.HandlerType.GET;

import io.javalin.http.HandlerType;
import io.javalin.router.Endpoint;
import io.javalin.router.ParsedEndpoint;
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
        var routingConfig = new RouterConfig(new JavalinConfig());
        for (int i = 0; i < 50; i++) {
            this.pathMatcher.add(new ParsedEndpoint(new Endpoint(GET, "/hello" + i, (ctx) -> {}), routingConfig));
            this.oldPathMatcher.add(new ParsedEndpoint(new Endpoint(GET, "/hello" + i, (ctx) -> {}), routingConfig));
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
    private final EnumMap<HandlerType, ArrayList<ParsedEndpoint>> handlerEntries = new EnumMap<HandlerType, ArrayList<ParsedEndpoint>>(
        HandlerType.getEntries().stream().collect(Collectors.toMap((handler) -> handler, (handler) -> new ArrayList<>()))
    );

    public void add(ParsedEndpoint entry) {
        handlerEntries.get(entry.getEndpoint().getMethod()).add(entry);
    }

    public List<ParsedEndpoint> findEntries(HandlerType handlerType, String requestUri) {
        var results = new ArrayList<ParsedEndpoint>();
        handlerEntries.get(handlerType).forEach(entry -> {
            if (match(entry, requestUri)) {
                results.add(entry);
            }
        });
        return results;
    }

    private boolean match(ParsedEndpoint entry, String requestPath) {
        if (entry.getEndpoint().getPath().equals("*")) return true;
        if (entry.getEndpoint().getPath().equals(requestPath)) return true;
        return entry.matches(requestPath);
    }
}
