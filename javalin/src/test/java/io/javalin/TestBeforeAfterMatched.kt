package io.javalin

import io.javalin.http.ContentType
import io.javalin.http.HttpStatus
import io.javalin.http.servlet.DefaultTasks.AFTER
import io.javalin.http.servlet.DefaultTasks.AFTER_MATCHED
import io.javalin.http.servlet.DefaultTasks.BEFORE
import io.javalin.http.servlet.DefaultTasks.ERROR
import io.javalin.http.servlet.DefaultTasks.HTTP
import io.javalin.http.staticfiles.Location
import io.javalin.testing.TestUtil
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.jetty.server.handler.ContextHandler
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

class TestBeforeAfterMatched {

    @Test
    fun `beforeMatched and afterMatched work`() = TestUtil.test { app, http ->
        app.before { ctx ->
            ctx.result("foo")
        }
        app.beforeMatched { ctx ->
            ctx.result("before-matched")
        }
        app.get("/hello") { ctx ->
            ctx.result(ctx.result() + "-hello")
        }
        app.afterMatched { ctx ->
            ctx.result(ctx.result() + "-after-matched")
        }
        app.after { ctx ->
            ctx.result(ctx.result() + "!")
        }

        assertThat(http.getBody("/other-path")).isEqualToIgnoringCase("Not Found!")
        assertThat(http.getBody("/hello")).isEqualTo("before-matched-hello-after-matched!")
    }

    @ParameterizedTest
    @MethodSource("io.javalin.BeforeAfterTestParams#withPath")
    fun `beforeMatched with path only runs for specified paths`(
        path: String,
        statusCode: Int,
        body: String?,
        beforeStar: String?,
        beforeSubCurly: String?,
        beforeAngle: String?
    ) = TestUtil.test { app, http ->
        app.before {
            it.header("X-Always", "true")
        }

        app.beforeMatched {
            it.header("X-Before-Star", "true")
        }

        app.beforeMatched("/sub/{p}*") {
            it.header("X-Before-Sub-Curly", it.pathParam("p"))
        }

        app.beforeMatched("/angle/<a>") {
            it.header("X-Before-Angle", it.pathParam("a"))
        }

        app.get("/root") {
            it.result("root")
        }

        app.get("/sub/id/other/stuff") {
            it.result("id-other-stuff")
        }

        app.get("/angle/i/see/slashes") {
            it.result("i-see-slashes")
        }

        app.afterMatched {
            it.header("X-After-Star", "true")
        }

        val res = http.get(path)
        assertThat(res.status).describedAs("$path - status")
            .isEqualTo(statusCode)
        assertThat(res.body).describedAs("$path - body")
            .isEqualTo(body)
        assertThat(res.headers.getFirst("X-Always")).describedAs("$path - Before")
            .isEqualTo("true")
        assertThat(res.headers.getFirst("X-Before-Star")).describedAs("$path - Before-Star")
            .isEqualTo(beforeStar)
        assertThat(res.headers.getFirst("X-After-Star")).describedAs("$path - After-Star")
            .isEqualTo(beforeStar)
        assertThat(res.headers.getFirst("X-Before-Sub-Curly")).describedAs("$path - Before-Sub-Curly")
            .isEqualTo(beforeSubCurly)
        assertThat(res.headers.getFirst("X-Before-Angle")).describedAs("$path - Before-Angle")
            .isEqualTo(beforeAngle)
    }

    @Test
    fun `beforeMatched can skip remaining handlers`() = TestUtil.test { app, http ->
        app.beforeMatched {
            it.result("static-before")
            it.skipRemainingHandlers()
        }
        app.get("/hello") {
            it.result("hello")
        }
        assertThat(http.getBody("/other-path")).isEqualToIgnoringCase("Not Found")
        assertThat(http.getBody("/hello")).isEqualTo("static-before")
    }

    @Test
    fun `beforeMatched works with singlePageHandler`() = TestUtil.test(Javalin.create { config ->
        config.spaRoot.addHandler("/") { ctx ->
            ctx.result(ctx.attribute<String>("before") ?: "n/a")
        }
    }) { app, http ->
        app.beforeMatched {
            it.attribute("before", "matched")
        }

        app.afterMatched {
            it.result(it.result() + "!")
        }

        assertThat(http.getBody("/")).isEqualTo("matched!")
        assertThat(http.getBody("/other")).isEqualTo("matched!")
    }

    @Test
    fun `beforeMatched fires for head request on get handler`() = TestUtil.test { app, http ->
        app.beforeMatched {
            it.status(HttpStatus.IM_A_TEAPOT)
        }

        app.get("/hello") {
            it.result("hello")
        }

        app.afterMatched {
            it.result(it.result() + "!")
        }

        assertThat(http.call(kong.unirest.HttpMethod.HEAD, "/hello").status).isEqualTo(418)
        assertThat(http.getStatus("/hello")).isEqualTo(HttpStatus.IM_A_TEAPOT)
        assertThat(http.getBody("/hello")).isEqualTo("hello!")
        assertThat(http.getStatus("/other")).isEqualTo(HttpStatus.NOT_FOUND)
    }

    @Test
    fun `beforeMatched runs for ResourceHandler`() = TestUtil.test(Javalin.create { config ->
        config.staticFiles.add("public", Location.CLASSPATH)
    }) { app, http ->
        app.beforeMatched {
            it.header("X-Matched-Before", "true")
        }
        app.afterMatched {
            it.header("X-Matched-After", "true")
        }

        val res = http.get("/html.html")
        assertThat(res.status).describedAs("status").isEqualTo(HttpStatus.OK.code)
        assertThat(res.headers.getFirst("X-Matched-Before")).describedAs("before-header").isEqualTo("true")
        assertThat(res.headers.getFirst("X-Matched-After")).describedAs("after-header").isEqualTo("true")
        assertThat(res.headers.getFirst("Content-Type")).describedAs("content-type").isEqualTo(ContentType.HTML)
        assertThat(res.body).describedAs("body").contains("<h1>HTML works</h1>")
    }

    @Test
    fun `beforeMatched runs for ResourceHandler - precompress`() = TestUtil.test(Javalin.create { config ->
        config.staticFiles.add {
            it.directory = "public"
            it.location = Location.CLASSPATH
            it.precompress = true
        }
    }) { app, http ->
        var afterMatchedRan = false
        var afterRan = false
        app.beforeMatched {
            it.header("X-Matched-Before", "true")
        }

        app.afterMatched {
            it.header("X-Matched-After", "true")
            afterMatchedRan = true
        }

        app.after {
            it.header("X-After", "true")
            afterRan = true
        }

        val res = http.get("/html.html")
        assertThat(res.status).describedAs("status").isEqualTo(HttpStatus.OK.code)
        assertThat(res.headers).isNotNull
        assertThat(res.headers.getFirst("X-Matched-Before")).describedAs("before-header").isEqualTo("true")
        assertThat(afterMatchedRan).describedAs("after-matched-ran").isEqualTo(true)
        assertThat(afterRan).describedAs("after-ran").isEqualTo(true)
        // TODO: see #1985, once fixed these two line should work
        //assertThat(res.headers.getFirst("X-After")).describedAs("after-header").isEqualTo("true")
        //assertThat(res.headers.getFirst("X-Matched-After")).describedAs("after-matched-header").isEqualTo("true")
        assertThat(res.headers.getFirst("Content-Type")).describedAs("content-type").isEqualTo(ContentType.HTML)
        assertThat(res.body).describedAs("body").contains("<h1>HTML works</h1>")
    }

    @Test
    fun `alias problem does occur - todo!`() = TestUtil.test(Javalin.create{ config ->
        config.staticFiles.add {
            it.directory = "public"
            it.location = Location.CLASSPATH
        }
    }) { app, http ->
        app.afterMatched {
            it.header("X-After", "true")
        }

        val slash = http.get("/file/")
        // TODO: has to be fixed, should be NOT_FOUND
        assertThat(slash.status).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.code)

        val noSlash = http.get("/file")
        assertThat(noSlash.body).isEqualTo("TESTFILE")
        assertThat(noSlash.status).isEqualTo(HttpStatus.OK.code)
        assertThat(noSlash.headers.getFirst("X-After")).isEqualTo("true")
    }

    @Test
    fun `alias problem does not occur with alias check`() = TestUtil.test(Javalin.create{ config ->
        config.staticFiles.add {
            it.directory = "public"
            it.location = Location.CLASSPATH
            it.aliasCheck = ContextHandler.AliasCheck { _, _ -> true }
        }
    }) { app, http ->
        app.afterMatched {
            it.header("X-After", "true")
        }

        val noSlash = http.get("/file")
        assertThat(noSlash.body).isEqualTo("TESTFILE")
        assertThat(noSlash.status).isEqualTo(HttpStatus.OK.code)
        assertThat(noSlash.headers.getFirst("X-After")).isEqualTo("true")

        val slash = http.get("/file/")
        assertThat(slash.body).isEqualTo("TESTFILE")
        assertThat(slash.status).isEqualTo(HttpStatus.OK.code)
        assertThat(slash.headers.getFirst("X-After")).isEqualTo("true")
    }

    @Test
    fun `alias problem does not occur when the BEFORE_MATCHED stage is skipped`() = TestUtil.test(Javalin.create{ config ->
        config.staticFiles.add {
            it.directory = "public"
            it.location = Location.CLASSPATH
            it.aliasCheck = ContextHandler.AliasCheck { _, _ -> true }
        }
        config.pvt.servletRequestLifecycle = listOf(BEFORE, HTTP, AFTER_MATCHED, ERROR, AFTER)
    }) { app, http ->
        app.afterMatched {
            it.header("X-After", "true")
        }

        val noSlash = http.get("/file")
        assertThat(noSlash.body).isEqualTo("TESTFILE")
        assertThat(noSlash.status).isEqualTo(HttpStatus.OK.code)
        assertThat(noSlash.headers.getFirst("X-After")).isEqualTo("true")

        val slash = http.get("/file/")
        assertThat(slash.body).isEqualTo("TESTFILE")
        assertThat(slash.status).isEqualTo(HttpStatus.OK.code)
        assertThat(slash.headers.getFirst("X-After")).isEqualTo("true")
    }
}

@Suppress("unused") // used a test method
internal object BeforeAfterTestParams {
    @JvmStatic
    fun withPath(): Stream<Arguments> = Stream.of(
        Arguments.of(
            "/other",
            404,
            "Not Found",
            "",
            "",
            "",
        ),
        Arguments.of(
            "/sub/id/fake",
            404,
            "Not Found",
            "",
            "",
            ""
        ),
        Arguments.of(
            "/angle/i/see/nothing",
            404,
            "Not Found",
            "",
            "",
            ""
        ),
        // happy paths
        Arguments.of(
            "/root",
            200,
            "root",
            "true",
            "",
            "",
        ),
        Arguments.of(
            "/sub/id/other/stuff",
            200,
            "id-other-stuff",
            "true",
            "id",
            "",
        ),
        Arguments.of(
            "/angle/i/see/slashes",
            200,
            "i-see-slashes",
            "true",
            "",
            "i/see/slashes",
        ),
    )
}
