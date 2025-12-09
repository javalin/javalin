package io.javalin

import io.javalin.http.HttpStatus
import io.javalin.testing.TestUtil
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicInteger

/**
 * Tests that validate and document the behavior of Context#redirect in relation to
 * the request lifecycle (before, beforeMatched, after, afterMatched handlers).
 *
 * Key behaviors:
 * 1. Redirect from BEFORE or BEFORE_MATCHED handler skips remaining BEFORE, BEFORE_MATCHED, and HTTP handlers
 * 2. AFTER and AFTER_MATCHED handlers still execute after redirect from BEFORE or BEFORE_MATCHED
 * 3. Redirect from HTTP handler behaves normally (no skipping)
 * 4. This allows for early redirects (auth, validation) while maintaining cleanup handlers
 */
class TestRedirectLifecycle {

    @Test
    fun `redirect in before handler skips remaining before handlers`() = TestUtil.test { app, http ->
        val executionOrder = mutableListOf<String>()

        app.unsafe.routes.before("/test") { ctx ->
            executionOrder.add("before-1")
            ctx.redirect("/redirected")
        }
        app.unsafe.routes.before("/test") {
            executionOrder.add("before-2-should-not-run")
        }
        app.unsafe.routes.get("/test") {
            executionOrder.add("http-handler-should-not-run")
        }
        app.unsafe.routes.get("/redirected") { ctx ->
            ctx.result("Redirected response")
        }

        val response = http.get("/test")
        assertThat(response.status).isEqualTo(HttpStatus.OK.code)
        assertThat(response.body).isEqualTo("Redirected response")
        assertThat(executionOrder).containsExactly("before-1")
    }

    @Test
    fun `redirect in before handler skips beforeMatched handlers`() = TestUtil.test { app, http ->
        val executionOrder = mutableListOf<String>()

        app.unsafe.routes.before("/test") { ctx ->
            executionOrder.add("before")
            ctx.redirect("/redirected")
        }
        app.unsafe.routes.beforeMatched("/test") {
            executionOrder.add("beforeMatched-should-not-run")
        }
        app.unsafe.routes.get("/test") {
            executionOrder.add("http-handler-should-not-run")
        }
        app.unsafe.routes.get("/redirected") { ctx ->
            ctx.result("Redirected response")
        }

        val response = http.get("/test")
        assertThat(response.status).isEqualTo(HttpStatus.OK.code)
        assertThat(response.body).isEqualTo("Redirected response")
        assertThat(executionOrder).containsExactly("before")
    }

    @Test
    fun `redirect in before handler still runs after handlers`() = TestUtil.test { app, http ->
        val executionOrder = mutableListOf<String>()

        app.unsafe.routes.before("/test") { ctx ->
            executionOrder.add("before")
            ctx.redirect("/redirected")
        }
        app.unsafe.routes.get("/test") {
            executionOrder.add("http-handler-should-not-run")
        }
        app.unsafe.routes.after("/test") {
            executionOrder.add("after")
        }
        app.unsafe.routes.get("/redirected") { ctx ->
            executionOrder.add("redirected-handler")
            ctx.result("Redirected response")
        }

        val response = http.get("/test")
        assertThat(response.status).isEqualTo(HttpStatus.OK.code)
        assertThat(response.body).isEqualTo("Redirected response")
        // AFTER handler runs as part of the original request lifecycle before redirect processing
        assertThat(executionOrder).contains("before", "after", "redirected-handler")
    }

    @Test
    fun `redirect in before handler still runs afterMatched handlers`() = TestUtil.test { app, http ->
        val executionOrder = mutableListOf<String>()

        app.unsafe.routes.before("/test") { ctx ->
            executionOrder.add("before")
            ctx.redirect("/redirected")
        }
        app.unsafe.routes.get("/test") {
            executionOrder.add("http-handler-should-not-run")
        }
        app.unsafe.routes.afterMatched("/test") {
            executionOrder.add("afterMatched")
        }
        app.unsafe.routes.get("/redirected") { ctx ->
            executionOrder.add("redirected-handler")
            ctx.result("Redirected response")
        }

        val response = http.get("/test")
        assertThat(response.status).isEqualTo(HttpStatus.OK.code)
        assertThat(response.body).isEqualTo("Redirected response")
        // AFTER_MATCHED handler runs because it has skipIfExceptionOccurred=false,
        // so it's preserved even when redirect() removes other tasks
        assertThat(executionOrder).contains("before", "afterMatched", "redirected-handler")
    }

    @Test
    fun `redirect in http handler does not skip after handlers`() = TestUtil.test { app, http ->
        val executionOrder = mutableListOf<String>()

        http.disableUnirestRedirects()

        app.unsafe.routes.get("/test") { ctx ->
            executionOrder.add("http-handler")
            ctx.redirect("/redirected")
        }
        app.unsafe.routes.after("/test") {
            executionOrder.add("after")
        }
        app.unsafe.routes.get("/redirected") { ctx ->
            ctx.result("Redirected response")
        }

        val response = http.get("/test")
        assertThat(response.status).isEqualTo(HttpStatus.FOUND.code)
        assertThat(response.body).isEqualTo("Redirected")
        assertThat(executionOrder).containsExactly("http-handler", "after")

        http.enableUnirestRedirects()
    }

    @Test
    fun `redirect in beforeMatched handler skips http handler`() = TestUtil.test { app, http ->
        val executionOrder = mutableListOf<String>()

        app.unsafe.routes.beforeMatched("/test") { ctx ->
            executionOrder.add("beforeMatched")
            ctx.redirect("/redirected")
        }
        app.unsafe.routes.get("/test") {
            executionOrder.add("http-handler-should-not-run")
        }
        app.unsafe.routes.get("/redirected") { ctx ->
            executionOrder.add("redirected-handler")
            ctx.result("Redirected response")
        }

        val response = http.get("/test")
        assertThat(response.status).isEqualTo(HttpStatus.OK.code)
        assertThat(response.body).isEqualTo("Redirected response")
        // HTTP handler is skipped because redirect from BEFORE_MATCHED now has the same behavior as BEFORE
        assertThat(executionOrder).containsExactly("beforeMatched", "redirected-handler")
    }

    @Test
    fun `redirect in beforeMatched handler skips remaining beforeMatched handlers`() = TestUtil.test { app, http ->
        val executionOrder = mutableListOf<String>()

        app.unsafe.routes.beforeMatched("/test") { ctx ->
            executionOrder.add("beforeMatched-1")
            ctx.redirect("/redirected")
        }
        app.unsafe.routes.beforeMatched("/test") {
            executionOrder.add("beforeMatched-2-should-not-run")
        }
        app.unsafe.routes.get("/test") {
            executionOrder.add("http-handler-should-not-run")
        }
        app.unsafe.routes.get("/redirected") { ctx ->
            executionOrder.add("redirected-handler")
            ctx.result("Redirected response")
        }

        val response = http.get("/test")
        assertThat(response.status).isEqualTo(HttpStatus.OK.code)
        assertThat(response.body).isEqualTo("Redirected response")
        assertThat(executionOrder).containsExactly("beforeMatched-1", "redirected-handler")
    }

    @Test
    fun `redirect in beforeMatched handler still runs afterMatched handlers`() = TestUtil.test { app, http ->
        val executionOrder = mutableListOf<String>()

        app.unsafe.routes.beforeMatched("/test") { ctx ->
            executionOrder.add("beforeMatched")
            ctx.redirect("/redirected")
        }
        app.unsafe.routes.get("/test") {
            executionOrder.add("http-handler-should-not-run")
        }
        app.unsafe.routes.afterMatched("/test") {
            executionOrder.add("afterMatched")
        }
        app.unsafe.routes.get("/redirected") { ctx ->
            executionOrder.add("redirected-handler")
            ctx.result("Redirected response")
        }

        val response = http.get("/test")
        assertThat(response.status).isEqualTo(HttpStatus.OK.code)
        assertThat(response.body).isEqualTo("Redirected response")
        // AFTER_MATCHED handler runs because it has skipIfExceptionOccurred=false
        assertThat(executionOrder).contains("beforeMatched", "afterMatched", "redirected-handler")
    }

    @Test
    fun `multiple before handlers - only those after redirect are skipped`() = TestUtil.test { app, http ->
        val callCount = AtomicInteger(0)

        app.unsafe.routes.before("/test") {
            callCount.incrementAndGet()
        }
        app.unsafe.routes.before("/test") { ctx ->
            callCount.incrementAndGet()
            ctx.redirect("/redirected")
        }
        app.unsafe.routes.before("/test") {
            callCount.incrementAndGet() // Should not run
        }
        app.unsafe.routes.get("/test") {
            callCount.incrementAndGet() // Should not run
        }
        app.unsafe.routes.get("/redirected") { ctx ->
            ctx.result("Redirected")
        }

        val response = http.get("/test")
        assertThat(response.status).isEqualTo(HttpStatus.OK.code)
        assertThat(callCount.get()).isEqualTo(2) // Only first two before handlers run
    }

    @Test
    fun `redirect preserves custom status code`() = TestUtil.test { app, http ->
        http.disableUnirestRedirects()

        app.unsafe.routes.before("/test") { ctx ->
            ctx.redirect("/redirected", HttpStatus.MOVED_PERMANENTLY)
        }
        app.unsafe.routes.get("/test") {
            // Should not run
        }
        app.unsafe.routes.get("/redirected") { ctx ->
            ctx.result("Redirected response")
        }

        val response = http.get("/test")
        assertThat(response.status).isEqualTo(HttpStatus.MOVED_PERMANENTLY.code)
        assertThat(response.body).isEqualTo("Redirected")

        http.enableUnirestRedirects()
    }

    @Test
    fun `redirect from before handler with path params works correctly`() = TestUtil.test { app, http ->
        val executionOrder = mutableListOf<String>()

        app.unsafe.routes.before("/user/{id}") { ctx ->
            executionOrder.add("before-" + ctx.pathParam("id"))
            ctx.redirect("/redirected/" + ctx.pathParam("id"))
        }
        app.unsafe.routes.get("/user/{id}") {
            executionOrder.add("http-should-not-run")
        }
        app.unsafe.routes.get("/redirected/{id}") { ctx ->
            executionOrder.add("redirected-" + ctx.pathParam("id"))
            ctx.result("Redirected to " + ctx.pathParam("id"))
        }

        val response = http.get("/user/123")
        assertThat(response.status).isEqualTo(HttpStatus.OK.code)
        assertThat(response.body).isEqualTo("Redirected to 123")
        assertThat(executionOrder).containsExactly("before-123", "redirected-123")
    }
}
