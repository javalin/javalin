# Investigation: Jetty 12.1.1+ Multipart Bug

## Executive Summary

**Issue**: The Javalin test `custom multipart properties applied correctly` (in `TestMultipartForms.kt`) fails when upgrading from Jetty 12.1.0 to any version 12.1.1 or later.

**Root Cause**: Jetty PR #13481 - "Automatic MultiPart cleanup for Jetty 12" introduces premature connection closure when a `BadMessageException` is thrown during multipart parsing.

**Recommendation**: Stay on Jetty 12.1.0 until this issue is resolved in Jetty, or report the regression to the Jetty project.

---

## Problem Description

### Symptoms
- **Test**: `custom multipart properties applied correctly` in `javalin/src/test/java/io/javalin/TestMultipartForms.kt`
- **Working Version**: Jetty 12.1.0
- **Failing Versions**: Jetty 12.1.1, 12.1.2, 12.1.3
- **Error Message**: `"NoHttpResponseException: localhost:XXXXX failed to respond"`
- **Test has been stable**: Working since 2022, this is a recent regression

### Test Behavior
The failing test:
1. Sets a custom `MultipartConfigElement` with a 10-byte maximum file size limit
2. Attempts to upload a file larger than 10 bytes (image.png = 6690 bytes)
3. **Expected**: Jetty throws `BadMessageException`, which is caught by Javalin's exception handler and returned as a response containing "org.eclipse.jetty.http.BadMessageException: 400: bad multipart"
4. **Actual (12.1.1+)**: Connection closes before response can be sent, resulting in "failed to respond"

---

## Root Cause Analysis

### Timeline

| Event | Date | Details |
|-------|------|---------|
| Jetty 12.1.0 Tagged | August 15, 2025 | Last working version |
| Jetty 12.1.0 Released | August 18, 2025 | Official release |
| PR #13481 Merged | August 27, 2025 | Multipart cleanup feature added |
| Jetty 12.1.1 Released | September 8, 2025 | First version with the bug |

### The Breaking Change

**PR #13481**: "Issue #13464 - fix and test the multipart auto cleanup in HttpChannelState"
- **Commit**: [`c10adfe26f8f6f0e2b1989613efd0b98b0798e1d`](https://github.com/jetty/jetty.project/commit/c10adfe26f8f6f0e2b1989613efd0b98b0798e1d)
- **Merged**: August 27, 2025
- **Jetty Issue**: [#13464](https://github.com/jetty/jetty.project/issues/13464)
- **Jetty PR**: [#13481](https://github.com/jetty/jetty.project/pull/13481)
- **Author**: lachlan-roberts

### Code Change

The PR added automatic cleanup in `HttpChannelState.completeStream()`:

```java
// Clean up any multipart tmp files and release any associated resources.
MultiPartFormData.Parts parts = MultiPartFormData.getParts(_request);
if (parts != null)
    parts.close();
```

**Location**: `jetty-core/jetty-server/src/main/java/org/eclipse/jetty/server/internal/HttpChannelState.java`  
**Exact problematic line**: Line 769  
**Method**: `completeStream(HttpStream stream, Throwable failure)`

### Why This Breaks

The cleanup logic doesn't account for error conditions during multipart parsing:

1. **Normal Flow** (Working):
   - Multipart request is parsed successfully
   - Handler processes request
   - Response is sent
   - `completeStream()` runs cleanup → `parts.close()` safely cleans up temp files

2. **Error Flow** (Broken in 12.1.1+):
   - Multipart parsing exceeds size limit → `IllegalStateException` thrown
   - `CompletableFuture<Parts>` stored in request attributes completes exceptionally
   - Exception is caught by Javalin's exception handler
   - Handler prepares error response
   - **Problem**: `completeStream()` is called during request completion
   - **Line 769**: `MultiPartFormData.getParts(_request)` is invoked
   - **Inside getParts() at line 133**: `futureParts.join()` is called
   - **`join()` throws `CompletionException`** wrapping the original `IllegalStateException`
   - **Exception is NOT caught** in the try block
   - Exception propagates up, causing connection to close immediately
   - Client receives: "Connection reset" / "failed to respond"

### The Exact Bug

**File**: `jetty-core/jetty-http/src/main/java/org/eclipse/jetty/http/MultiPartFormData.java`  
**Problematic method**: `getParts(Attributes attributes)`  
**Line 133**: `return (Parts)futureParts.join();`

When the `CompletableFuture<Parts>` has completed exceptionally (due to size limit violation), calling `join()` throws a `CompletionException`. This exception is not caught in `HttpChannelState.completeStream()`, causing the connection to close prematurely.

```java
public static Parts getParts(Attributes attributes)
{
    Object attribute = attributes.getAttribute(MultiPartFormData.class.getName());
    if (attribute instanceof Parts parts)
        return parts;
    if (attribute instanceof CompletableFuture<?> futureParts && futureParts.isDone())
        return (Parts)futureParts.join();  // ← THROWS CompletionException on failed future!
    return null;
}
```

### Technical Details

When a `BadMessageException` is thrown during multipart parsing:
- Line 720 in `MultiPartFormData.java`: Size check fails
  ```java
  if (maxPartSize >= 0 && size > maxPartSize)
  {
      onFailure(new IllegalStateException("max file size exceeded: %d".formatted(maxPartSize)));
      return;
  }
  ```
- The `CompletableFuture<Parts>` object stored in request attributes completes exceptionally
- The future is stored under key `MultiPartFormData.class.getName()`
- When `completeStream()` calls `getParts()` during cleanup
- `getParts()` calls `futureParts.join()` on line 133
- `join()` throws `CompletionException` wrapping the `IllegalStateException`
- This exception is **not caught** in `HttpChannelState.completeStream()`
- The uncaught exception causes immediate connection closure
- No error response can be sent

**The core issue**: Calling `join()` on a failed `CompletableFuture` throws an exception, and the cleanup code doesn't handle this case.

---

## Evidence

### Reproduction
1. Checkout Javalin repository
2. Set Jetty version to 12.1.0 in `pom.xml`
3. Run: `./mvnw test -pl javalin -Dtest=TestMultipartForms#"custom multipart properties applied correctly"`
   - **Result**: ✅ Test passes
4. Change Jetty version to 12.1.3
5. Run same test
   - **Result**: ❌ Test fails with "NoHttpResponseException: failed to respond"

### Jetty Commit History
Verified by examining Jetty's commit history between tags:
- `jetty-12.1.0` (August 15, 2025)
- `jetty-12.1.1` (September 8, 2025)

The multipart cleanup commit (`c10adfe26f`) is present in 12.1.1 but not in 12.1.0.

---

## Impact Assessment

### Severity
- **High**: This is a regression that breaks existing, stable functionality
- The test has been working reliably since 2022
- Affects any application that:
  - Uses multipart file uploads with size limits
  - Relies on exception handlers to send error responses
  - Expects proper HTTP error responses instead of connection closure

### Scope
- Affects Jetty versions: 12.1.1, 12.1.2, 12.1.3 (and likely all future 12.1.x until fixed)
- Does **not** affect: Jetty 12.1.0, Jetty 12.0.x

---

## Recommended Actions

### For Javalin Maintainers

1. **Short Term**: Stay on Jetty 12.1.0
   - This is the last known working version
   - All tests pass
   - Stable and reliable

2. **Medium Term**: Report to Jetty Project
   - Create an issue in [Jetty's GitHub repository](https://github.com/jetty/jetty.project/issues)
   - Reference PR #13481 and issue #13464
   - Provide minimal reproduction case
   - Link to this investigation
   - **Suggest the fix**: Wrap `getParts()` call in try-catch, or make `getParts()` handle failed futures gracefully

3. **Long Term**: Monitor Jetty Releases
   - Watch for fixes to the multipart cleanup logic
   - Test future Jetty releases before upgrading
   - Consider adding specific regression tests for this scenario

### Proposed Fix for Jetty

**Option 1**: Catch exception in cleanup code (`HttpChannelState.java` line 768-771)
```java
// Clean up any multipart tmp files and release any associated resources.
try
{
    MultiPartFormData.Parts parts = MultiPartFormData.getParts(_request);
    if (parts != null)
        parts.close();
}
catch (Exception e)
{
    // Failed to get or close parts, but this is cleanup phase - don't propagate
    if (LOG.isDebugEnabled())
        LOG.debug("Failed to cleanup multipart parts", e);
}
```

**Option 2**: Make `getParts()` safe for failed futures (`MultiPartFormData.java` line 127-135)
```java
public static Parts getParts(Attributes attributes)
{
    Object attribute = attributes.getAttribute(MultiPartFormData.class.getName());
    if (attribute instanceof Parts parts)
        return parts;
    if (attribute instanceof CompletableFuture<?> futureParts && futureParts.isDone())
    {
        try
        {
            return (Parts)futureParts.join();
        }
        catch (CompletionException e)
        {
            // Future completed exceptionally, no parts to return
            return null;
        }
    }
    return null;
}
```

### Bug Report Template for Jetty

```markdown
Title: Regression in 12.1.1: CompletionException in multipart cleanup causes premature connection closure

## Description
Starting in Jetty 12.1.1, when multipart parsing fails (e.g., due to exceeding size limits), the connection closes without sending an error response. This worked correctly in 12.1.0.

## Root Cause
PR #13481 added multipart cleanup in `HttpChannelState.completeStream()` that calls `MultiPartFormData.getParts()`. When parsing has failed, the stored `CompletableFuture<Parts>` has completed exceptionally. The `getParts()` method calls `futureParts.join()` on line 133, which throws `CompletionException` for failed futures. This exception is not caught, causing the connection to close before error handlers can send a response.

## Exact Problem Location
**File**: `jetty-core/jetty-http/src/main/java/org/eclipse/jetty/http/MultiPartFormData.java`  
**Line 133**: `return (Parts)futureParts.join();` ← Throws `CompletionException` on failed future  

**File**: `jetty-core/jetty-server/src/main/java/org/eclipse/jetty/server/internal/HttpChannelState.java`  
**Line 769**: `MultiPartFormData.Parts parts = MultiPartFormData.getParts(_request);` ← Doesn't catch exception  

## Reproduction
1. Set a custom `MultipartConfigElement` with small size limit (e.g., 10 bytes)
2. Upload a file larger than the limit
3. Configure an exception handler to catch parsing errors and send error response
4. **Expected**: HTTP 400 response with error message
5. **Actual in 12.1.1+**: Connection closes, client sees "failed to respond"
6. **Actual in 12.1.0**: Works correctly, client receives error response

## Proposed Fix
Make `getParts()` safe for failed futures:
```java
if (attribute instanceof CompletableFuture<?> futureParts && futureParts.isDone())
{
    try
    {
        return (Parts)futureParts.join();
    }
    catch (CompletionException e)
    {
        return null; // Future failed, no parts available
    }
}
```

Or wrap the cleanup call in try-catch in `HttpChannelState.completeStream()`.

## Impact
Applications relying on exception handlers to send error responses for multipart upload failures will experience broken behavior.

## Related
- PR: #13481
- Issue: #13464
- Commit: c10adfe26f8f6f0e2b1989613efd0b98b0798e1d
```

---

## Alternative Workarounds

### Option 1: Stay on Jetty 12.1.0 (Recommended)
- ✅ Simple and reliable
- ✅ All tests pass
- ❌ Miss out on other 12.1.1+ improvements/fixes

### Option 2: Modify the Test
```kotlin
// Not recommended - hides the real problem
@Test
fun `custom multipart properties applied correctly`() = TestUtil.test { app, http ->
    app.unsafe.routes.before("/test-upload") { ctx ->
        ctx.attribute("org.eclipse.jetty.multipartConfig", MultipartConfigElement("/tmp", 10, 10, 5))
    }
    
    app.unsafe.routes.post("/test-upload") { ctx ->
        ctx.result(ctx.uploadedFiles().joinToString(", ") { it.filename() })
    }
    
    try {
        val response = http.post("/test-upload")
            .field("upload", File("src/test/resources/upload-test/image.png"))
            .asString()
        // Either we get the exception in the response...
        assertThat(response.body).contains("BadMessageException")
    } catch (e: UnirestException) {
        // ...or the connection closes (which is the bug, but we'd accept it)
        assertThat(e.message).contains("failed to respond")
    }
}
```

### Option 3: Downgrade Jetty Temporarily
- Use 12.0.x series instead of 12.1.x
- ⚠️ Different feature set, requires more validation

---

## Additional Investigation

### Is This Intentional Behavior?

**No**. This appears to be an unintended side effect of the cleanup feature:
- The PR #13481 aimed to prevent temporary file accumulation
- The tests added in that PR only cover successful multipart parsing scenarios
- No test coverage for error conditions during parsing
- The Jetty team likely didn't anticipate this interaction with exception handling

### Could This Be a Javalin Issue?

**No**. The test is correctly using Jetty's public API:
- Sets valid `MultipartConfigElement` via standard request attribute
- Uses standard exception handling
- Works perfectly in Jetty 12.1.0
- Only breaks after Jetty internal changes

---

## References

### Jetty Issues & PRs
- Issue #13464: https://github.com/jetty/jetty.project/issues/13464
- PR #13481: https://github.com/jetty/jetty.project/pull/13481
- Commit: https://github.com/jetty/jetty.project/commit/c10adfe26f8f6f0e2b1989613efd0b98b0798e1d

### Jetty Releases
- 12.1.0: https://github.com/jetty/jetty.project/releases/tag/jetty-12.1.0
- 12.1.1: https://github.com/jetty/jetty.project/releases/tag/jetty-12.1.1
- 12.1.2: https://github.com/jetty/jetty.project/releases/tag/jetty-12.1.2
- 12.1.3: https://github.com/jetty/jetty.project/releases/tag/jetty-12.1.3

### Javalin Test
- File: `javalin/src/test/java/io/javalin/TestMultipartForms.kt`
- Test: `custom multipart properties applied correctly` (line 140)

---

## Conclusion

The Jetty 12.1.1+ regression is caused by PR #13481's automatic multipart cleanup feature, which doesn't properly handle error conditions during parsing. The cleanup code runs too early, closing connections before error responses can be sent.

**Recommended Action**: Stay on Jetty 12.1.0 and report this regression to the Jetty project.

---

*Investigation conducted: October 31, 2025*
*Investigator: GitHub Copilot*
*Javalin Repository: https://github.com/javalin/javalin*
