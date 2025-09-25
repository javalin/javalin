package io.javalin

import io.javalin.http.ContentType
import io.javalin.http.HttpStatus
import io.javalin.http.servlet.DefaultTasks.AFTER
import io.javalin.http.servlet.DefaultTasks.AFTER_MATCHED
import io.javalin.http.servlet.DefaultTasks.BEFORE
import io.javalin.http.servlet.DefaultTasks.ERROR
import io.javalin.http.servlet.DefaultTasks.HTTP
import io.javalin.http.staticfiles.Location
import io.javalin.security.RouteRole
import io.javalin.testing.TestDependency
import io.javalin.testing.TestUtil
import io.javalin.testing.JavalinHttpResponse
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.jetty.server.AliasCheck
import org.junit.jupiter.api.Test

class TestBeforeAfterMatched {

    @Test
    fun `beforeMatched and afterMatched work`() = TestUtil.test { app, http ->
        app.before { it.result("foo") }
        app.beforeMatched { it.result("before-matched") }
        app.get("/hello") { it.result(it.result() + "-hello") }
        app.afterMatched { it.result(it.result() + "-after-matched") }
        app.after { it.result(it.result() + "!") }

        assertThat(http.getStatus("/other-path")).isEqualTo(HttpStatus.NOT_FOUND)
        assertThat(http.getBody("/other-path")).isEqualToIgnoringCase("Endpoint GET /other-path not found!")
        assertThat(http.getStatus("/hello")).isEqualTo(HttpStatus.OK)
        assertThat(http.getBody("/hello")).isEqualTo("before-matched-hello-after-matched!")
    }

    @Test
    fun `path - totally different path leads to 404 without global headers`() = TestUtil.test { app, http ->
        setAppRoutes(app)
        setHeaders(app)

        val path = "/other"
        val response = http.get(path)
        assertResponse(
            response,
            path,
            404,
            "Endpoint GET $path not found",
            "",
            "",
            ""
        )
    }

    @Test
    fun `path - matching sub curly in beforeMatched but no matching http handler leads to 404`() = TestUtil.test { app, http ->
        setAppRoutes(app)
        setHeaders(app)

        val path = "/sub/id/fake"
        val response = http.get(path)
        assertResponse(
            response,
            path,
            404,
            "Endpoint GET $path not found",
            "",
            "",
            ""
        )
    }

    @Test
    fun `path - matching angle brackets in beforeMatched but no matching http handler leads to 404`() = TestUtil.test { app, http ->
        setAppRoutes(app)
        setHeaders(app)

        val path = "/angle/i/see/nothing"
        val response = http.get(path)
        assertResponse(
            response,
            path,
            404,
            "Endpoint GET $path not found",
            "",
            "",
            ""
        )
    }

    @Test
    fun `path - perfect match`() = TestUtil.test { app, http ->
        setAppRoutes(app)
        setHeaders(app)

        val path = "/root"
        val response = http.get(path)
        assertResponse(
            response,
            path,
            200,
            "root",
            "true",
            "",
            ""
        )
    }

    @Test
    fun `path - sub curly in before matched happy path`() = TestUtil.test { app, http ->
        setAppRoutes(app)
        setHeaders(app)

        val path = "/sub/id/other/stuff"
        val response = http.get(path)
        assertResponse(
            response,
            path,
            200,
            "id-other-stuff",
            "true",
            "id",
            ""
        )
    }

    @Test
    fun `path - angle brackets in beforeMatched happy path`() = TestUtil.test { app, http ->
        setAppRoutes(app)
        setHeaders(app)

        val path = "/angle/i/see/slashes"
        val response = http.get(path)
        assertResponse(
            response,
            path,
            200,
            "i-see-slashes",
            "true",
            "",
            "i/see/slashes"
        )
    }


    private fun setAppRoutes(app: Javalin) {
        app.get("/root") { it.result("root") }
        app.get("/sub/id/other/stuff") { it.result("id-other-stuff") }
        app.get("/angle/i/see/slashes") { it.result("i-see-slashes") }
    }

    private fun setHeaders(app: Javalin) {
        app.before { it.header("X-Always", "true") }
        app.beforeMatched { it.header("X-Before-Star", "true") }
        app.beforeMatched("/sub/{p}*") { it.header("X-Before-Sub-Curly", it.pathParam("p")) }
        app.beforeMatched("/angle/<a>") { it.header("X-Before-Angle", it.pathParam("a")) }
        app.afterMatched { it.header("X-After-Star", "true") }
    }

    private fun assertResponse(
        res: JavalinHttpResponse, path: String,
        statusCode: Int, body: String?,
        beforeStar: String?, beforeSubCurly: String?,
        beforeAngle: String?
    ) {
        assertThat(res.status).describedAs("$path - status").isEqualTo(statusCode)
        assertThat(res.body).describedAs("$path - body").isEqualTo(body)
        assertThat(res.headers.getFirst("X-Always")).describedAs("$path - X-Always").isEqualTo("true")
        assertThat(res.headers.getFirst("X-Before-Star")).describedAs("$path - X-Before-Star")
            .isEqualTo(beforeStar)
        assertThat(res.headers.getFirst("X-After-Star")).describedAs("$path - X-After-Star")
            .isEqualTo(beforeStar)
        assertThat(res.headers.getFirst("X-Before-Sub-Curly")).describedAs("$path - X-Before-Sub-Curly")
            .isEqualTo(beforeSubCurly)
        assertThat(res.headers.getFirst("X-Before-Angle")).describedAs("$path - X-Before-Angle")
            .isEqualTo(beforeAngle)
    }

    @Test
    fun `beforeMatched can skip remaining handlers`() = TestUtil.test { app, http ->
        app.beforeMatched {
            it.result("static-before")
            it.skipRemainingHandlers()
        }
        app.get("/hello") { it.result("hello") }
        assertThat(http.getStatus("/other-path")).isEqualTo(HttpStatus.NOT_FOUND)
        assertThat(http.getBody("/other-path")).isEqualToIgnoringCase("Endpoint GET /other-path not found")
        assertThat(http.getStatus("/hello")).isEqualTo(HttpStatus.OK)
        assertThat(http.getBody("/hello")).isEqualTo("static-before")
    }

    @Test
    fun `beforeMatched works with singlePageHandler`() = TestUtil.test(Javalin.create { config ->
        config.spaRoot.addHandler("/") { ctx ->
            ctx.result(ctx.attribute<String>("before") ?: "n/a")
        }
    }) { app, http ->
        app.beforeMatched { it.attribute("before", "matched") }
        app.afterMatched { it.result(it.result() + "!") }

        assertThat(http.getStatus("/")).isEqualTo(HttpStatus.OK)
        assertThat(http.getBody("/")).isEqualTo("matched!")
        assertThat(http.getBody("/other")).isEqualTo("matched!")
    }

    @Test
    fun `beforeMatched fires for head request on get handler`() = TestUtil.test { app, http ->
        app.beforeMatched { it.status(HttpStatus.IM_A_TEAPOT) }
        app.get("/hello") { it.result("hello") }
        app.afterMatched { it.result(it.result() + "!") }

        assertThat(http.call(kong.unirest.HttpMethod.HEAD, "/hello").status).isEqualTo(418)
        assertThat(http.getStatus("/hello")).isEqualTo(HttpStatus.IM_A_TEAPOT)
        assertThat(http.getBody("/hello")).isEqualTo("hello!")
        assertThat(http.getStatus("/other")).isEqualTo(HttpStatus.NOT_FOUND)
    }

    @Test
    fun `beforeMatched runs for ResourceHandler`() = TestUtil.test(Javalin.create { config ->
        config.staticFiles.add("public", Location.CLASSPATH)
    }) { app, http ->
        app.beforeMatched { it.header("X-Matched-Before", "true") }
        app.afterMatched { it.header("X-Matched-After", "true") }

        val res = http.get("/html.html")
        assertThat(res.status).describedAs("status").isEqualTo(HttpStatus.OK.code)
        assertThat(res.headers.getFirst("X-Matched-Before")).describedAs("before-header").isEqualTo("true")
        assertThat(res.headers.getFirst("X-Matched-After")).describedAs("after-header").isEqualTo("true")
        assertThat(res.headers.getFirst("Content-Type")).describedAs("content-type").isEqualTo(ContentType.HTML)
        assertThat(res.body).describedAs("body").contains("<h1>HTML works</h1>")
    }

    @Test
    fun `beforeMatched runs for webjars`() = TestUtil.test(Javalin.create { config ->
        config.staticFiles.enableWebjars()
    }) { app, http ->
        app.beforeMatched { it.header("X-Matched-Before", "123") }
        app.afterMatched { it.header("X-Matched-After", "456") }
        val res = http.get("/webjars/swagger-ui/${TestDependency.swaggerVersion}/swagger-ui.css")
        assertThat(res.headers.getFirst("X-Matched-Before")).describedAs("before-header").isEqualTo("123")
        assertThat(res.headers.getFirst("X-Matched-After")).describedAs("after-header").isEqualTo("456")
    }

    @Test
    fun `beforeMatched runs for every ResourceHandler`() = TestUtil.test(Javalin.create { config ->
        config.staticFiles.add("/public/subdir", Location.CLASSPATH)
        config.staticFiles.add("/public/assets", Location.CLASSPATH)
        config.staticFiles.add("src/test/external/", Location.EXTERNAL)
    }) { app, http ->
        app.beforeMatched { it.header("X-Matched-Before", "abc") }
        app.afterMatched { it.header("X-Matched-After", "xyz") }
        fun assertHeaders(path: String) {
            val res = http.get(path)
            assertThat(res.headers.getFirst("X-Matched-Before")).describedAs("before-header").isEqualTo("abc")
            assertThat(res.headers.getFirst("X-Matched-After")).describedAs("after-header").isEqualTo("xyz")
        }
        assertHeaders("/index.html") // from /public/subdir
        assertHeaders("/filtered-styles.css") // from /public/assets
        assertHeaders("/txt.txt") // from external
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
        app.beforeMatched { it.header("X-Matched-Before", "true") }

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
        assertThat(res.headers.getFirst("X-After")).describedAs("after-header").isEqualTo("true")
        assertThat(res.headers.getFirst("X-Matched-After")).describedAs("after-matched-header").isEqualTo("true")
        assertThat(res.headers.getFirst("Content-Type")).describedAs("content-type").isEqualTo(ContentType.HTML)
        assertThat(res.body).describedAs("body").contains("<h1>HTML works</h1>")
    }

    @Test
    fun `alias problem does not occur`() = TestUtil.test(Javalin.create { config ->
        config.staticFiles.add {
            it.directory = "public"
            it.location = Location.CLASSPATH
        }
    }) { app, http ->
        app.afterMatched { it.header("X-After", "true") }

        val slash = http.get("/file/")
        assertThat(slash.status).isEqualTo(HttpStatus.NOT_FOUND.code)

        val noSlash = http.get("/file")
        assertThat(noSlash.body).isEqualTo("TESTFILE")
        assertThat(noSlash.status).isEqualTo(HttpStatus.OK.code)
        assertThat(noSlash.headers.getFirst("X-After")).isEqualTo("true")
    }

    @Test
    fun `alias problem does not occur with alias check`() = TestUtil.test(Javalin.create { config ->
        config.staticFiles.add {
            it.directory = "public"
            it.location = Location.CLASSPATH
            it.aliasCheck = AliasCheck { _, _ -> true }
        }
    }) { app, http ->
        app.afterMatched { it.header("X-After", "true") }

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
    fun `alias problem does not occur when the BEFORE_MATCHED stage is skipped`() =
        TestUtil.test(Javalin.create { config ->
            config.staticFiles.add {
                it.directory = "public"
                it.location = Location.CLASSPATH
                it.aliasCheck = AliasCheck { _, _ -> true }
            }
            config.pvt.servletRequestLifecycle = mutableListOf(BEFORE, HTTP, AFTER_MATCHED, ERROR, AFTER)
        }) { app, http ->
            app.afterMatched { it.header("X-After", "true") }

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
    fun `pathParams are extracted from endpoint if beforeMatched has no path-params`() =
        TestUtil.test(Javalin.create { config ->
            config.router.mount {
                it.beforeMatched { ctx -> ctx.result(ctx.pathParamMap().toString()) }
                it.get("/{endpoint}") { ctx -> ctx.result(ctx.result() + "/" + ctx.pathParamMap()) }
            }
        }) { _, http ->
            assertThat(http.getBody("/p")).isEqualTo("{endpoint=p}/{endpoint=p}")
        }

    @Test
    fun `pathParams are extracted from beforeMatched if beforeMatched has path-params`() =
        TestUtil.test(Javalin.create { config ->
            config.router.mount {
                it.beforeMatched("/{before}") { ctx -> ctx.result(ctx.pathParamMap().toString()) }
                it.get("/{endpoint}") { ctx -> ctx.result(ctx.result() + "/" + ctx.pathParamMap()) }
            }
        }) { _, http ->
            assertThat(http.getBody("/p")).isEqualTo("{before=p}/{endpoint=p}")
        }

    private enum class Role : RouteRole { A }

    @Test
    fun `routeRoles are available in beforeMatched`() = TestUtil.test { app, http ->
        app.beforeMatched { it.result(it.routeRoles().toString()) }
        app.get("/test", {}, Role.A)
        assertThat(http.getBody("/test")).isEqualTo("[A]")
    }

    @Test
    fun `routeRoles are available in afterMatched`() = TestUtil.test { app, http ->
        app.get("/test", {}, Role.A)
        app.afterMatched { it.result(it.routeRoles().toString()) }
        assertThat(http.getBody("/test")).isEqualTo("[A]")
    }

}
