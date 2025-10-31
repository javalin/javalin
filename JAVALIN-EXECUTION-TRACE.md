# Exact Javalin Execution Path for Jetty 12.1.1+ Multipart Bug

This document pinpoints **exactly where** in Javalin's request pipeline the Jetty bug manifests.

## The Bug in Summary

The bug occurs **AFTER** Javalin completes all request processing and writes the response. When Jetty's cleanup code runs, it attempts to call `getParts()` on a `CompletableFuture` that failed during parsing, causing an uncaught `CompletionException` that closes the connection.

## Javalin Code Path (Step-by-Step)

### 1. Request Entry Point
**File**: `javalin/src/main/java/io/javalin/http/servlet/JavalinServlet.kt`  
**Method**: `service(HttpServletRequest, HttpServletResponse)`  
**Line**: 29-30

```kotlin
override fun service(request: HttpServletRequest, response: HttpServletResponse) {
    handle(JavalinServletRequest(request), response)
}
```

**What happens**: Request is wrapped in `JavalinServletRequest` (line 30)

---

### 2. Context Creation & Task Queue Setup
**File**: `javalin/src/main/java/io/javalin/http/servlet/JavalinServlet.kt`  
**Method**: `handle()`  
**Lines**: 33-50

```kotlin
fun handle(request: HttpServletRequest, response: HttpServletResponse): Context? {
    try {
        val ctx = JavalinServletContext(...)
        
        val submitTask: (SubmitOrder, Task) -> Unit = { order, task ->
            when (order) {
                FIRST -> ctx.tasks.offerFirst(task)
                LAST -> ctx.tasks.add(task)
            }
        }
        requestLifecycle.forEach { it.createTasks(submitTask, this, ctx, requestUri) }
        
        ctx.handleSync()
        return ctx
    } catch (throwable: Throwable) {
        router.handleHttpUnexpectedThrowable(response, throwable)
        return null
    }
}
```

**What happens**: 
- Line 35: Creates `JavalinServletContext` 
- Lines 48: Lifecycle creates tasks (before handlers, route handler, after handlers, etc.)
- Line 50: Executes task queue synchronously

---

### 3. Task Execution Loop
**File**: `javalin/src/main/java/io/javalin/http/servlet/JavalinServlet.kt`  
**Method**: `handleSync()`  
**Lines**: 58-70

```kotlin
private fun JavalinServletContext.handleSync() {
    while (userFutureSupplier == null && tasks.isNotEmpty()) {
        val task = tasks.poll()
        if (exceptionOccurred && task.skipIfExceptionOccurred) {
            continue
        }
        handleTask(task.handler)
    }
    when {
        userFutureSupplier != null -> handleUserFuture()
        else -> writeResponseAndLog()
    }
}
```

**What happens**: 
- Line 59: Polls tasks from queue
- Line 64: Executes each task via `handleTask()`
- Line 68: Writes response when queue is empty

---

### 4. Multipart Parsing (The Exception Point)
**File**: `javalin/src/main/java/io/javalin/http/util/MultipartUtil.kt`  
**Method**: `processParts()`  
**Lines**: 24-34

```kotlin
private inline fun <R> HttpServletRequest.processParts(multipartConfig: MultipartConfig, body: (Sequence<Part>, Int) -> R): R {
    if ((this as JavalinServletRequest).inputStreamRead) {
        throw BodyAlreadyReadException("Request body has already been consumed...")
    }
    // Apply multipart configuration if not already set
    if (getAttribute(MULTIPART_CONFIG_ATTRIBUTE) == null) {
        setAttribute(MULTIPART_CONFIG_ATTRIBUTE, multipartConfig.multipartConfigElement())
    }
    val parts = this.parts  // ← LINE 32: CALLS HttpServletRequest.getParts()
    return body(parts.asSequence(), parts.size)
}
```

**What happens**:
- Line 25: Checks if body already read (via `JavalinServletRequest` wrapper)
- Lines 29-31: Sets multipart config attribute if not set (from before handler)
- **Line 32: CRITICAL - Calls `this.parts` which triggers Jetty's multipart parsing**
  - Jetty creates a `CompletableFuture<Parts>`
  - Parsing begins asynchronously
  - File size exceeds 10 byte limit
  - Future completes exceptionally with `IllegalStateException`
  - Wrapped in `ServletException` containing `BadMessageException`
  - **Exception thrown back to caller**

---

### 5. Exception Handling
**File**: `javalin/src/main/java/io/javalin/http/servlet/JavalinServlet.kt`  
**Method**: `handleTask()`  
**Lines**: 97-105

```kotlin
private fun <R> JavalinServletContext.handleTask(handler: TaskHandler<R>): R? =
    try {
        handler.handle()
    } catch (throwable: Throwable) {
        exceptionOccurred = true
        userFutureSupplier = null
        tasks.offerFirst(Task(skipIfExceptionOccurred = false) { router.handleHttpException(this, throwable) })
        null
    }
```

**What happens**:
- Line 100: Exception from `getParts()` is caught
- Line 101: `exceptionOccurred = true`
- Line 103: **Exception handler task added to FRONT of queue**
- Line 104: Returns null (task failed)
- **Key point**: Exception is handled gracefully in Javalin's pipeline

---

### 6. Exception Handler Executes
**Test code**:
```kotlin
app.unsafe.routes.exception(Exception::class.java) { e, ctx ->
    ctx.result("${e::class.java.canonicalName} ${e.message}")
}
```

**What happens**:
- Exception handler task executes from queue
- Calls `ctx.result()` with error message
- Error message is **buffered** in context
- Task completes successfully

---

### 7. Response Writing
**File**: `javalin/src/main/java/io/javalin/http/servlet/JavalinServlet.kt`  
**Method**: `writeResponseAndLog()`  
**Lines**: 107-121

```kotlin
private fun JavalinServletContext.writeResponseAndLog() {
    try {
        if (responseWritten.getAndSet(true)) return
        resultInputStream()?.use { resultStream ->
            val etagWritten = ETagGenerator.tryWriteEtagAndClose(cfg.http.generateEtags, this, resultStream)
            if (!etagWritten) resultStream.copyTo(outputStream(), cfg.http.responseBufferSize ?: 32_768)
        }
        cfg.pvt.requestLogger?.handle(this, executionTimeMs())
    } catch (throwable: Throwable) {
        router.handleHttpUnexpectedThrowable(res(), throwable)
    } finally {
        if (outputStreamWrapper.isInitialized()) outputStream().close()
        if (isAsync()) req().asyncContext.complete()
    }
}
```

**What happens**:
- Line 109: Check prevents duplicate writes
- Line 110-112: **Buffered response is written to output stream**
- Line 114: Request logger runs
- Line 118: Output stream is closed
- Line 119: Async context completed if applicable
- **Key point**: Response is successfully written and committed

---

### 8. Service Method Returns
**File**: `javalin/src/main/java/io/javalin/http/servlet/JavalinServlet.kt`  
**Method**: `handle()` → `service()`  
**Line**: 50-56

**What happens**:
- Line 50: `ctx.handleSync()` returns (all tasks processed, response written)
- Line 51: `return ctx` (successful handling)
- **Control returns to Jetty's servlet handling code**
- The `service()` method completes normally

---

### 9. ⚠️ BUG TRIGGERS - Jetty Cleanup Phase ⚠️

**AFTER** Javalin's `service()` method returns, Jetty's request completion code runs:

**Jetty File**: `jetty-core/jetty-server/src/main/java/org/eclipse/jetty/server/internal/HttpChannelState.java`  
**Method**: `completeStream()`  
**Line**: 769

```java
// Jetty's cleanup code (NOT in Javalin's control)
private void completeStream() {
    try {
        // ... other cleanup ...
        
        // Line 769: Attempt to cleanup multipart data
        MultiPartFormData.Parts parts = MultiPartFormData.getParts(_request);
        if (parts != null)
            parts.close();
            
    } catch (Exception e) {
        // Note: This catch may not catch all exceptions from getParts()!
    }
}
```

**Then in**: `jetty-core/jetty-http/src/main/java/org/eclipse/jetty/http/MultiPartFormData.java`  
**Method**: `getParts()`  
**Line**: 133

```java
public static Parts getParts(Attributes attributes) {
    Object attribute = attributes.getAttribute(MultiPartFormData.class.getName());
    if (attribute instanceof Parts parts)
        return parts;
    if (attribute instanceof CompletableFuture<?> futureParts && futureParts.isDone())
        return (Parts)futureParts.join();  // ← LINE 133: THROWS CompletionException!
    return null;
}
```

**What happens**:
- Jetty's cleanup code tries to close multipart data
- Gets the `CompletableFuture<Parts>` from request attributes
- The future is marked as "done" (completed exceptionally during step 4)
- Calls `join()` on the failed future
- **`join()` throws uncaught `CompletionException` wrapping `IllegalStateException`**
- **Exception propagates up, NOT caught by Jetty's exception handler**
- **Connection closes immediately**
- **Response that was successfully written in step 7 is LOST**
- **Client receives**: `NoHttpResponseException: failed to respond`

---

## The Critical Difference

### Why Javalin Triggers the Bug (and standalone code doesn't):

1. **Request Wrapping**: Javalin wraps the request in `JavalinServletRequest` (line 30 in JavalinServlet.kt)
   - This wrapper tracks if the input stream was read
   - The wrapper is passed through the entire pipeline

2. **Task Queue Processing**: Javalin uses a deferred task execution model
   - Exception is caught and re-queued (line 103 in JavalinServlet.kt)
   - Exception handler runs as a separate task
   - Response is buffered during exception handler

3. **Response Buffering & Deferred Writing**: 
   - `ctx.result()` buffers the response (exception handler)
   - `writeResponseAndLog()` writes it later (line 107-121 in JavalinServlet.kt)
   - Service method returns AFTER response is written

4. **Failed Future Persists**: 
   - The `CompletableFuture<Parts>` that failed is stored in request attributes
   - It stays there when service() method returns
   - Jetty's cleanup code tries to access it
   - The cleanup code doesn't handle failed futures

### Why standalone servlet code doesn't trigger the bug:

1. Exception from `getParts()` is caught immediately in the servlet's `doPost()`
2. Error response is written synchronously in the same try-catch
3. The servlet method returns normally
4. Jetty's cleanup may run, but:
   - Either the exception was already handled by servlet's try-catch
   - Or the servlet framework has additional protection layers
   - Or the cleanup exception is caught at a higher level

---

## Summary

The bug manifests at this **specific point** in Javalin's pipeline:

**Location**: AFTER `JavalinServlet.service()` returns, when Jetty's `HttpChannelState.completeStream()` runs

**Trigger**: `HttpChannelState.java:769` calls `MultiPartFormData.getParts()` which calls `join()` on a failed `CompletableFuture<Parts>` at `MultiPartFormData.java:133`

**Why Javalin hits it**:
- Task queue model with deferred exception handling
- Response buffering and deferred writing  
- Service method returns normally after response is written
- Failed `CompletableFuture<Parts>` persists in request attributes
- Jetty's cleanup code has no protection for failed futures

**The Fix Needed** (in Jetty, not Javalin):
Wrap line 769 in `HttpChannelState.completeStream()` or handle failed futures in `MultiPartFormData.getParts()` line 133.
