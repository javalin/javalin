# Javalin Benchmark Module

This module contains JMH (Java Microbenchmark Harness) benchmarks written in **Kotlin** for measuring Javalin's performance in critical runtime paths.

## Overview

The benchmarks focus on performance-critical operations that are executed on every request:

- **Routing**: Path matching, parameter extraction, route finding
- **Static Files**: Resource resolution, etag handling, file serving
- **Context Methods**: JSON serialization/deserialization, parameter access
- **End-to-End**: Full request lifecycle with realistic scenarios

### Why Kotlin?

The benchmarks are written in Kotlin for:
- **Cleaner syntax**: More concise and readable code
- **Better type safety**: Null safety and type inference
- **Easier maintenance**: Less boilerplate, data classes, extension functions
- **Version compatibility**: Simpler to handle API differences across Javalin versions

## Architecture

The benchmark module uses **version-specific source directories** to handle API differences between Javalin major versions:

- `src/main/kotlin-javalin5/` - Benchmarks for Javalin 5.x (uses `app.get()`, `app.post()`, etc.)
- `src/main/kotlin-javalin6/` - Benchmarks for Javalin 6.x (uses `app.get()`, `app.post()`, etc.)
- `src/main/kotlin-javalin7/` - Benchmarks for Javalin 7.x (uses `app.unsafe.routes.get()`, etc.)

Maven profiles automatically select the correct source directory based on the Javalin version being benchmarked. This allows the same benchmark logic to run across all major versions with their respective APIs.

## Running Benchmarks

### Build the Benchmark JAR

```bash
cd javalin-benchmark
mvn clean package
```

This creates an executable JAR: `target/benchmarks.jar`

### Run All Benchmarks

```bash
java -jar target/benchmarks.jar
```

### Run Specific Benchmark Class

```bash
# Run only routing benchmarks
java -jar target/benchmarks.jar RoutingBenchmark

# Run only context methods benchmarks
java -jar target/benchmarks.jar ContextMethodsBenchmark

# Run only end-to-end benchmarks
java -jar target/benchmarks.jar EndToEndBenchmark
```

### Run Specific Benchmark Method

```bash
# Run only the path parameter extraction benchmark
java -jar target/benchmarks.jar RoutingBenchmark.extractSinglePathParam

# Run only JSON serialization benchmark
java -jar target/benchmarks.jar ContextMethodsBenchmark.jsonSerialization
```

### Customize Benchmark Parameters

```bash
# Run with custom iterations and forks
java -jar target/benchmarks.jar -wi 5 -i 10 -f 3

# Run with specific output format
java -jar target/benchmarks.jar -rf json -rff results.json

# Run with profilers (requires -XX:+UnlockDiagnosticVMOptions -XX:+DebugNonSafepoints)
java -jar target/benchmarks.jar -prof gc
java -jar target/benchmarks.jar -prof stack
```

### Common JMH Options

- `-wi <count>`: Number of warmup iterations (default: varies by benchmark)
- `-i <count>`: Number of measurement iterations (default: varies by benchmark)
- `-f <count>`: Number of forks (default: varies by benchmark)
- `-t <count>`: Number of threads
- `-rf <format>`: Result format (text, csv, json, latex)
- `-rff <file>`: Result file name
- `-prof <profiler>`: Enable profiler (gc, stack, perf, etc.)
- `-h`: Show help with all options

## Benchmark Structure

### RoutingBenchmark

Tests the performance of Javalin's routing system:

- **matchStaticRoute**: Matching exact static paths
- **matchSinglePathParam**: Matching routes with one path parameter
- **matchDoublePathParam**: Matching routes with two path parameters
- **matchTriplePathParam**: Matching complex nested routes
- **extractPathParams**: Extracting parameter values from URLs
- **hasEntries**: Checking if routes exist

**Why it matters**: Routing happens on every request. Faster routing = higher throughput.

### StaticFilesBenchmark

Tests static file serving performance:

- **canHandleStaticFile**: Overhead of checking if a file can be served
- **resourceLookupOverhead**: Resource resolution performance
- **etagGeneration**: ETag computation for caching
- **precompressedFileServing**: Serving precompressed files from cache

**Why it matters**: Static files are common in web apps. Efficient serving reduces server load.

### ContextMethodsBenchmark

Tests common Context operations:

- **jsonSerialization/Deserialization**: Converting objects to/from JSON
- **jsonStreamSerialization/Deserialization**: Streaming JSON for large payloads
- **mapSerialization**: Serializing dynamic map responses
- **listSerialization**: Serializing collections
- **smallJsonPayload**: Typical simple responses
- **mediumJsonPayload**: Typical entity responses
- **largeJsonPayload**: Collection responses with many items

**Why it matters**: JSON operations are the most common in REST APIs. Faster serialization = faster responses.

### EndToEndBenchmark

Tests complete request/response cycles:

- **simpleGetRequest**: Basic GET endpoint
- **getWithPathParam**: GET with path parameters
- **getWithQueryParams**: GET with query parameters
- **postWithJsonBody**: POST with JSON payload
- **putWithPathParamAndBody**: PUT with parameters and body
- **deleteRequest**: DELETE operations
- **complexNestedRoute**: Multi-level nested routes
- **searchWithMultipleParams**: Complex query parameter handling

**Why it matters**: These represent real-world usage patterns and measure total system performance.

## Interpreting Results

JMH outputs throughput in operations per time unit. Higher is better.

Example output:
```
Benchmark                                    Mode  Cnt    Score    Error   Units
RoutingBenchmark.matchSinglePathParam       thrpt   10  1234.567 ± 12.345  ops/us
```

- **Mode**: `thrpt` = throughput (operations per time unit)
- **Cnt**: Number of measurements taken
- **Score**: Average throughput
- **Error**: Margin of error (95% confidence interval)
- **Units**: `ops/us` = operations per microsecond

### What to Look For

1. **Regressions**: Compare results before/after changes. Significant drops indicate performance issues.
2. **Hotspots**: Benchmarks with low scores may need optimization.
3. **Variance**: High error margins suggest inconsistent performance.

## Comparing Versions

The benchmark module can compare performance between different Javalin versions **without switching git branches**. It uses Maven profiles to build benchmarks against different Javalin versions from Maven Central.

### How It Works

The benchmarks use `app.unsafe.routes` to define routes, which works consistently across **all Javalin versions (5, 6, and 7)**:

```kotlin
app = Javalin.create()

with(app.unsafe.routes) {
    get("/path") { ctx -> ctx.result("response") }
    post("/path") { ctx -> ctx.result("response") }
}
```

This approach means the same benchmark code compiles and runs against all versions without modification or reflection.

### Quick Comparison

**Windows (PowerShell):**
```powershell
.\javalin-benchmark\compare-versions.bat -v1 6.3.0 -v2 7.0.0-SNAPSHOT
```

**Linux/Mac:**
```bash
./javalin-benchmark/compare-versions.sh -v1 6.3.0 -v2 7.0.0-SNAPSHOT
```

This will:
1. Build benchmark JARs for both versions
2. Run all benchmarks for each version
3. Display results directly in the terminal with percentage changes

### Comparison Options

```bash
-v1 <version>    First Javalin version (required)
-v2 <version>    Second Javalin version (required)
-b <pattern>     Benchmark pattern regex (default: .*)
-w <iterations>  Warmup iterations (default: 3)
-m <iterations>  Measurement iterations (default: 5)
-f <forks>       Number of forks (default: 1)
-o <dir>         Output directory (default: benchmark-results)
```

### Examples

**Quick comparison (minimal iterations):**
```bash
.\javalin-benchmark\compare-versions.bat -v1 6.3.0 -v2 7.0.0-SNAPSHOT -w 1 -m 1
```

**Compare only routing benchmarks:**
```bash
.\javalin-benchmark\compare-versions.bat -v1 6.3.0 -v2 7.0.0-SNAPSHOT -b ".*Routing.*"
```

**Production-quality comparison:**
```bash
.\javalin-benchmark\compare-versions.bat -v1 6.3.0 -v2 7.0.0-SNAPSHOT -w 5 -m 10 -f 3
```

### Sample Output

```
========================================
Javalin Performance Comparison
========================================
Version 1: 6.3.0
Version 2: 7.0.0-SNAPSHOT

[6.3.0] Building benchmark JAR...
[6.3.0] Build complete
[6.3.0] Running benchmarks...

Benchmark                           Mode  Cnt   Score   Error   Units
RoutingBenchmark.matchStaticRoute  thrpt       19.814          ops/us

[7.0.0-SNAPSHOT] Building benchmark JAR...
[7.0.0-SNAPSHOT] Build complete
[7.0.0-SNAPSHOT] Running benchmarks...

Benchmark                           Mode  Cnt   Score   Error   Units
RoutingBenchmark.matchStaticRoute  thrpt       20.279          ops/us

Results saved to:
  benchmark-results\results-6.3.0.json
  benchmark-results\results-7.0.0-SNAPSHOT.json
```

In this example, version 7.0.0-SNAPSHOT shows a ~2.3% improvement in routing performance.

### Visualizing Results

**Option 1: JMH Visualizer (Recommended)**

[JMH Visualizer](https://jmh.morethan.io/) is a free web tool that provides:
- Side-by-side comparison charts
- Performance trend visualization
- Statistical analysis
- Interactive graphs

Simply upload both JSON files from `benchmark-results/` and it will generate visual comparisons.

**Option 2: Manual Analysis**

Use `jq` to extract specific data:
```bash
# Get all benchmark scores from version 6.3.0
jq '.[].primaryMetric.score' benchmark-results/results-6.3.0.json

# Get benchmark names and scores
jq '.[] | {benchmark: .benchmark, score: .primaryMetric.score}' benchmark-results/results-6.3.0.json
```

**Option 3: Spreadsheet**

Import the JSON files into Excel/Google Sheets for custom analysis and charting.

### Supported Versions

The comparison framework supports:
- **Javalin 7.x** (current development)
- **Javalin 6.x** (6.0.0, 6.1.0, 6.2.0, 6.3.0)
- **Javalin 5.x** (5.5.0, 5.6.0)

Predefined profiles exist for common versions. For other versions, the script will attempt to download them from Maven Central.

### How It Works

The comparison framework uses:
1. **Maven profiles** to switch Javalin dependency versions
2. **Compatibility layer** (`JavalinCompat`) to handle API differences between versions
3. **Reflection** to access version-specific internals when needed
4. **Separate JAR builds** for each version to ensure isolation

## Best Practices

1. **Close other applications** to reduce noise
2. **Run on consistent hardware** for meaningful comparisons
3. **Use multiple forks** to account for JVM variance
4. **Warm up properly** to reach steady-state performance
5. **Run on production-like hardware** for realistic results
6. **Disable CPU frequency scaling** for consistent results:
   ```bash
   # Linux
   sudo cpupower frequency-set --governor performance
   ```

## Adding New Benchmarks

1. Create a new class in `src/main/java/io/javalin/benchmark/`
2. Annotate with JMH annotations:
   ```java
   @BenchmarkMode(Mode.Throughput)
   @OutputTimeUnit(TimeUnit.MICROSECONDS)
   @State(Scope.Benchmark)
   @Fork(value = 2)
   @Warmup(iterations = 3, time = 2)
   @Measurement(iterations = 5, time = 2)
   public class MyBenchmark {
       
       @Setup
       public void setup() {
           // Initialize resources
       }
       
       @Benchmark
       public void myBenchmark(Blackhole blackhole) {
           // Benchmark code
           blackhole.consume(result);
       }
       
       @TearDown
       public void teardown() {
           // Clean up resources
       }
   }
   ```
3. Rebuild and run: `mvn clean package && java -jar target/benchmarks.jar MyBenchmark`

## Resources

- [JMH Documentation](https://github.com/openjdk/jmh)
- [JMH Samples](https://github.com/openjdk/jmh/tree/master/jmh-samples/src/main/java/org/openjdk/jmh/samples)
- [Avoiding Benchmarking Pitfalls](https://shipilev.net/blog/2014/nanotrusting-nanotime/)

## CI Integration

To run benchmarks in CI and detect regressions:

```bash
# Run quick benchmark (fewer iterations for faster feedback)
java -jar target/benchmarks.jar -wi 1 -i 2 -f 1 -rf json -rff ci-results.json

# Compare with baseline (requires custom script)
./compare-benchmarks.sh baseline.json ci-results.json
```

## Notes

- Benchmarks are **not** run as part of regular tests (`mvn test`)
- The benchmark module is **not** published to Maven Central
- Results vary by hardware - use relative comparisons, not absolute numbers
- End-to-end benchmarks include network overhead and are slower than micro-benchmarks

