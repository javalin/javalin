# Jetty 12.1.1 Multipart Bug - Minimal Standalone Demo

This module contains a single Java file that demonstrates multipart handling in Jetty 12.1.1.

## The Bug

When multipart parsing fails due to size limits in Jetty 12.1.1+, the connection can close without sending an error response in certain scenarios. This worked correctly in Jetty 12.1.0.

**Root Cause (from investigation):**
- `HttpChannelState.java:769` calls `MultiPartFormData.getParts()` during cleanup
- `MultiPartFormData.java:133` calls `futureParts.join()` on a failed `CompletableFuture`
- `join()` throws uncaught `CompletionException`
- Connection closes before error response can be sent

## This Demo

**Status**: This demo shows the **expected correct behavior** using the servlet API.

The servlet API path (`HttpServletRequest.getParts()`) appears to handle cleanup differently than Javalin's internal multipart handling, so this specific demo doesn't reproduce the exact bug that occurs in the Javalin test.

However, this demo is still valuable because it:
- ✅ Shows the expected correct behavior (error response is sent)
- ✅ Provides a minimal, standalone example using only Jetty
- ✅ Can be used to test future Jetty versions
- ✅ Demonstrates proper multipart size limit configuration  
- ✅ Uses only JDK HTTP client - no external dependencies

## Running the Demo

### Quick Start
```bash
# From the repository root
./mvnw clean compile exec:java -pl jetty-bug-demo
```

### Step by Step
```bash
# Build the module
./mvnw clean compile -pl jetty-bug-demo

# Run the demo
./mvnw exec:java -pl jetty-bug-demo
```

## Output

### Current Behavior (Servlet API)
```
Server started on port 35881
Caught exception: ServletException: org.eclipse.jetty.http.BadMessageException: 400: bad multipart

========================================
RESULT:
========================================
HTTP 400: Error: org.eclipse.jetty.http.BadMessageException: 400: bad multipart
========================================

✅ NO BUG - Error response was successfully sent!
Server handled the exception and sent an error response.
```

**Exit code**: 0 (success)

### The Actual Bug (in Javalin with Jetty 12.1.3)
The Javalin test `custom multipart properties applied correctly` fails with:
```
NoHttpResponseException: localhost:XXXXX failed to respond
```

This suggests the bug is triggered by Javalin's specific multipart handling code path, which differs from the basic servlet API demonstrated here.

## The Code

The demo consists of:
- **1 Java file** (`JettyMultipartBugDemo.java`) - 200 lines
- **1 pom.xml** - Maven configuration
- **Dependencies**: Only Jetty 12.1.1 core libraries

### What it does:
1. Creates a Jetty server with a servlet
2. Configures multipart with 10-byte size limit
3. Tries to upload a file > 10 bytes
4. Catches the exception and sends HTTP 400 response
5. Client reads the response using JDK HttpURLConnection
6. Verifies the error response was received

## Key Points

- **Zero external dependencies** beyond Jetty 12.1.1
- **Single Java file** - easy to copy and share
- **Self-contained** - runs and verifies automatically
- **Clear output** - shows exactly what happened
- **Exit codes**:
  - 0: Error response received (expected behavior)
  - 1: Connection reset (bug reproduced)
  - 2: Unexpected result

## Files

- `pom.xml` - Maven configuration with Jetty 12.1.1 dependency
- `src/main/java/io/javalin/jetty/bug/JettyMultipartBugDemo.java` - The demo code (200 lines)
- `README.md` - This file

## Related

- **Investigation Report**: See `JETTY-12.1.1-INVESTIGATION.md` in repository root for full analysis
- **Jetty Issue**: #13464
- **Jetty PR**: #13481 (introduced the bug)
- **Jetty Commit**: c10adfe26f8f6f0e2b1989613efd0b98b0798e1d
- **Javalin Issue**: #2492

## Notes

While this demo doesn't reproduce the exact bug (servlet API handles it gracefully), it provides:
1. A baseline for expected behavior
2. A minimal test case for Jetty developers
3. A way to verify fixes in future Jetty versions
4. Documentation of the proper multipart configuration

The actual bug occurs in a more complex code path involving async multipart parsing with `CompletableFuture`, which is used internally by Javalin but not exposed in the basic servlet API.
