# Virtual Threads (Project Loom) Investigation for Javalin HTTP Thread-Pool

## Summary

This document investigates the use of Java's virtual threads (Project Loom) as the HTTP thread-pool
in Javalin, as requested in issue #1778. The investigation covers memory usage, CPU performance,
locking concerns, and the current implementation's limitations.

## Current Implementation

When `useVirtualThreads = true`, Javalin creates a `LoomThreadPool` that wraps a
`newThreadPerTaskExecutor` with a `NamedVirtualThreadFactory`. This pool:

- Creates a new virtual thread for **every** incoming request
- Has **no upper bound** on concurrent virtual threads
- Has **no request queuing** mechanism
- Reports `getThreads() = 1`, `getIdleThreads() = 1`, `isLowOnThreads() = false` (hardcoded)
- Bypasses Jetty's `QueuedThreadPool` entirely, losing its built-in limits and monitoring

## Investigation Findings

### 1. Memory Concerns

Virtual threads are lightweight (~1KB initial stack) compared to platform threads (~1MB), but they
are not free. With no concurrency limit:

- **Unbounded request acceptance**: Under load, the server will accept every connection and spawn
  a virtual thread, potentially creating millions of virtual threads simultaneously
- **Heap exhaustion**: Each virtual thread still holds stack frames, request/response buffers,
  and application objects. Under extreme load, this can exhaust heap memory
- **No backpressure**: The current implementation provides no mechanism to reject or queue requests
  when the system is overwhelmed

**Recommendation**: The `LoomThreadPool` should support a configurable concurrency limit
(e.g., via a `Semaphore`) that bounds the number of simultaneously executing virtual threads,
while still allowing Jetty's acceptor to queue connections up to a configurable limit.

### 2. CPU-Bound Workloads

Virtual threads are designed for I/O-bound workloads where threads frequently block. For CPU-bound
workloads:

- Virtual threads still run on the ForkJoinPool's carrier threads (default = number of CPU cores)
- CPU-bound tasks will monopolize carrier threads, preventing other virtual threads from running
- A traditional `QueuedThreadPool` with a bounded thread count matching available cores is more
  appropriate for CPU-heavy handlers

**Recommendation**: Document that `useVirtualThreads` is best suited for I/O-heavy applications
(database queries, HTTP client calls, file I/O). CPU-bound applications should use the default
`QueuedThreadPool`.

### 3. Locking / Pinning Concerns

When a virtual thread enters a `synchronized` block, it "pins" its carrier thread, preventing
other virtual threads from using that carrier. This can drastically reduce the effective
parallelism of the ForkJoinPool.

**Javalin's own `synchronized` usage** (as of this investigation):
- `Emitter.kt` — SSE emitter uses `synchronized(this)` to serialize writes to the response
  output stream. This is per-connection and short-lived, so pinning impact is minimal.
- `WsAutomaticPing.kt` — WebSocket ping manager uses `synchronized(ctx)` for enable/disable.
  These are infrequent operations with negligible pinning impact.

**Javalin's mitigation for Loom pinning**:
- `ReentrantLazy` — Javalin already replaces `kotlin.lazy(SYNCHRONIZED)` with a
  `ReentrantLock`-based implementation when Loom is available, avoiding pinning during lazy
  initialization.

**Third-party library concerns**:
- Jackson, Gson, and other JSON libraries may use `synchronized` internally
- JDBC drivers commonly use `synchronized` in connection pool implementations
- Logging frameworks (Logback, Log4j2) may pin during appender writes
- Users should audit their dependencies for `synchronized` usage when enabling virtual threads

**Recommendation**: Replace the two remaining `synchronized` blocks in Javalin with
`ReentrantLock` to be fully Loom-friendly. Document that users should be aware of pinning
in third-party libraries.

### 4. Jetty Integration

The current `LoomThreadPool` implements Jetty's `ThreadPool` interface minimally:
- `getThreads()` and `getIdleThreads()` return hardcoded values (1), making Jetty's
  `LowResourceMonitor` and `StatisticsHandler` unable to accurately report thread usage
- `isLowOnThreads()` always returns `false`, disabling Jetty's backpressure mechanisms
- `join()` is a no-op, meaning graceful shutdown won't wait for in-flight requests

**Recommendation**: Improve `LoomThreadPool` to track active virtual threads (via `AtomicInteger`)
and implement meaningful `getThreads()`/`isLowOnThreads()` based on a configurable threshold.

### 5. Graceful Shutdown

The current `join()` implementation is a no-op. When the server stops, in-flight requests on
virtual threads may be interrupted abruptly.

**Recommendation**: Track active virtual threads and implement `join()` to wait for completion
with a configurable timeout.

## Conclusions

1. **Virtual threads should NOT be the default** — The current `useVirtualThreads = false` default
   is correct. Virtual threads are an opt-in feature for users who understand their implications.

2. **The implementation needs limits** — The unbounded `LoomThreadPool` is the primary concern.
   A semaphore-based concurrency limit would prevent resource exhaustion while preserving the
   benefits of virtual threads.

3. **Pinning risk in Javalin itself is low** — Only two `synchronized` blocks exist in hot paths,
   both scoped to individual connections. Replacing them with `ReentrantLock` would eliminate
   all internal pinning risk.

4. **User education is important** — Users enabling virtual threads should understand:
   - Best for I/O-bound workloads
   - `synchronized` in their code or libraries can cause pinning
   - No built-in concurrency limit (requests can overwhelm the system)

## Action Items

- [x] Default `useVirtualThreads` to `false` (already done)
- [x] Fix lazy evaluation of thread pool config (already done, PR #2074)
- [ ] Replace `synchronized` blocks with `ReentrantLock` in `Emitter.kt` and `WsAutomaticPing.kt`
- [ ] Add concurrency limit support to `LoomThreadPool` (configurable max virtual threads)
- [ ] Implement proper `getThreads()` / `isLowOnThreads()` tracking in `LoomThreadPool`
- [ ] Implement graceful `join()` in `LoomThreadPool`
- [ ] Add documentation about virtual threads trade-offs to javalin.io
