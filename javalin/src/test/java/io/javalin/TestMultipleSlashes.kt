package io.javalin

import io.javalin.http.HttpStatus.NOT_FOUND
import io.javalin.http.HttpStatus.OK
import io.javalin.testing.TestUtil
import io.javalin.testing.httpCode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class TestMultipleSlashes {
    private val multipleSlashesApp = Javalin.create {
        it.router.treatMultipleSlashesAsSingleSlash = true
    }

    private val multipleSlashesWithSignificantTrailingSlashesApp = Javalin.create {
        it.router.treatMultipleSlashesAsSingleSlash = true
        it.router.ignoreTrailingSlashes = false
        it.routes.get("/a") { ctx -> ctx.result("a") }
        it.routes.get("/a/") { ctx -> ctx.result("a/") }
        it.routes.get("/a/b") { ctx -> ctx.result("b") }
        it.routes.get("/a/b/") { ctx -> ctx.result("b/") }
    }

    @Test
    fun `multiple slashes at the start are okay when multipleSlashes is enabled`() =
        TestUtil.test(multipleSlashesApp) { app, http ->
            app.unsafe.routes.get("/hello") { it.result("ok") }
            val res = http.get("//hello")
            assertThat(res.httpCode()).isEqualTo(OK)
            assertThat(res.body).isEqualTo("ok")
        }

    @Test
    fun `multiple slashes in the middle are okay when multipleSlashes is enabled`() =
        TestUtil.test(multipleSlashesApp) { app, http ->
            app.unsafe.routes.get("/hello/world") { it.result("ok") }
            val res = http.get("/hello//world")
            assertThat(res.httpCode()).isEqualTo(OK)
            assertThat(res.body).isEqualTo("ok")
        }

    @Test
    fun `multiple slashes at the end are okay when multipleSlashes is enabled`() =
        TestUtil.test(multipleSlashesApp) { app, http ->
            app.unsafe.routes.get("/hello") { it.result("ok") }
            val res = http.get("/hello//")
            assertThat(res.httpCode()).isEqualTo(OK)
            assertThat(res.body).isEqualTo("ok")
        }

    @Test
    fun `multiple slashes together with match sub path`() =
        TestUtil.test(multipleSlashesApp) { app, http ->
            app.unsafe.routes.get("/{name}*") { it.result(it.pathParam("name")) }
            assertThat(http.getBody("/text")).isEqualTo("text")
            assertThat(http.getBody("/text//two")).isEqualTo("text")
        }

    @Test
    fun `multiple slashes at the start work with significant trailing slashes`() =
        TestUtil.test(multipleSlashesWithSignificantTrailingSlashesApp) { _, http ->
            assertThat(http.getBody("//a")).isEqualTo("a")
            assertThat(http.getBody("//a/")).isEqualTo("a/")
        }

    @Test
    fun `multiple slashes in the middle work with significant trailing slashes`() =
        TestUtil.test(multipleSlashesWithSignificantTrailingSlashesApp) { _, http ->
            assertThat(http.getBody("/a//b")).isEqualTo("b")
            assertThat(http.getBody("/a//b/")).isEqualTo("b/")
        }

    @Test
    fun `multiple slashes at the end work with significant trailing slashes`() =
        TestUtil.test(multipleSlashesWithSignificantTrailingSlashesApp) { _, http ->
            assertThat(http.getBody("/a")).isEqualTo("a")
            assertThat(http.getBody("/a//")).isEqualTo("a/")
        }

    @Test
    fun `multiple slashes are not accepted when not enabled`() = TestUtil.test { app, http ->
        app.unsafe.routes.get("/hello/world") { it.result("ok") }
        listOf(
            "//hello/world",
            "/hello//world",
            "/hello/world//"
        ).forEach {
            assertThat(http.getStatus(it)).isNotNull.isEqualTo(NOT_FOUND)
        }
    }
}
