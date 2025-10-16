/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 *
 */

package io.javalin

import io.javalin.apibuilder.ApiBuilder.get
import io.javalin.apibuilder.ApiBuilder.path
import io.javalin.testing.TestUtil
import okhttp3.OkHttpClient
import okhttp3.Request
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.net.URLEncoder

internal class TestTrailingSlashes {

    private val okHttp = OkHttpClient().newBuilder().build()
    private fun OkHttpClient.getBody(path: String) = this.newCall(Request.Builder().url(path).get().build()).execute().body!!.string()
    private val javalin = Javalin.create { it.router.ignoreTrailingSlashes = false; }

    @Test
    fun `trailing slashes are ignored by default`() = TestUtil.test { app, http ->
        app.unsafe.routes.get("/hello") { it.result("Hello, slash!") }
        assertThat(http.getBody("/hello")).isEqualTo("Hello, slash!")
        assertThat(http.getBody("/hello/")).isEqualTo("Hello, slash!")
    }

    @Test
    fun `trailing slashes are ignored by default - ApiBuilder`() = TestUtil.test(
        Javalin.create {
            it.routes.apiBuilder {
                path("a") {
                    get { it.result("a") }
                    get("/") { it.result("a-slash") }
                }
            }
        }
    ) { app, http ->
        assertThat(http.getBody("/a")).isEqualTo("a")
        assertThat(http.getBody("/a/")).isEqualTo("a")
    }

    @Test
    fun `trailing slashes are treat as different url, if configuration is set - ApiBuilder`() {
        TestUtil.test(javalin) { app, http ->
            app.unsafe.routes.get("/a") { it.result("a") }
            app.unsafe.routes.get("/a/") { it.result("a-slash") }
            assertThat(http.getBody("/a")).isEqualTo("a")
            assertThat(http.getBody("/a/")).isEqualTo("a-slash")
        }
    }

    @Test
    fun `trailing slashes don't change url params behaviour`() {
        TestUtil.test(javalin) { app, http ->
            app.unsafe.routes.get("/a") { it.result("a") }
            app.unsafe.routes.get("/a/") { it.result("a-slash") }
            assertThat(http.getBody("/a")).isEqualTo("a")
            assertThat(http.getBody("/a/")).isEqualTo("a-slash")
        }
    }

    @Test
    fun `wildcard first works`() = TestUtil.test(javalin) { app, http ->
        app.unsafe.routes.get("/*/test") { it.result("!") }
        app.unsafe.routes.get("/*/test/") { it.result("!/") }
        assertThat(http.getBody("/en/test")).isEqualTo("!")
        assertThat(http.getBody("/en/test/")).isEqualTo("!/")
    }

    @Test
    fun `wildcard middle works`() = TestUtil.test(javalin) { app, http ->
        app.unsafe.routes.get("/test/*/test") { it.result("!") }
        app.unsafe.routes.get("/test/*/test/") { it.result("!/") }
        assertThat(http.getBody("/test/en/test")).isEqualTo("!")
        assertThat(http.getBody("/test/en/test/")).isEqualTo("!/")
    }

    @Test
    fun `wildcard end works`() = TestUtil.test(javalin) { app, http ->
        app.unsafe.routes.get("/test/*") { it.result("!") }
        app.unsafe.routes.get("/test/*/") { it.result("!/") }
        assertThat(http.getBody("/test/en")).isEqualTo("!")
        assertThat(http.getBody("/test/en/")).isEqualTo("!") //wildcard at end match all urls, so trailingSlashes are captured by *
    }

    @Test
    fun `case sensitive urls work`() = TestUtil.test(javalin) { app, http ->
        app.unsafe.routes.get("/My-Url") { it.result("OK") }
        assertThat(http.getBody("/MY-URL")).isEqualTo("Endpoint GET /MY-URL not found")
        assertThat(http.getBody("/My-Url")).isEqualTo("OK")
        app.unsafe.routes.get("/My-Url/") { it.result("OK/") }
        assertThat(http.getBody("/MY-URL/")).isEqualTo("Endpoint GET /MY-URL/ not found")
        assertThat(http.getBody("/My-Url/")).isEqualTo("OK/")
    }

    @Test
    fun `utf-8 encoded path-params work`() = TestUtil.test(javalin) { app, http ->
        app.unsafe.routes.get("/{path-param}") { it.result(it.pathParam("path-param")) }
        app.unsafe.routes.get("/{path-param}/") { it.result(it.pathParam("path-param") + "/") }
        assertThat(okHttp.getBody(http.origin + "/" + URLEncoder.encode("TE/ST", "UTF-8"))).isEqualTo("TE/ST")
        assertThat(okHttp.getBody(http.origin + "/" + URLEncoder.encode("TE/ST/", "UTF-8"))).isEqualTo("TE/ST/")
    }

    @Test
    fun `path-params work case-sensitive`() = TestUtil.test(javalin) { app, http ->
        app.unsafe.routes.get("/{userId}") { it.result(it.pathParam("userId")) }
        assertThat(http.getBody("/path-param")).isEqualTo("path-param")
        app.unsafe.routes.get("/{a}/{A}") { it.result("${it.pathParam("a")}-${it.pathParam("A")}") }
        assertThat(http.getBody("/a/B")).isEqualTo("a-B")

        app.unsafe.routes.get("/{userId}/") { it.result(it.pathParam("userId") + "/") }
        assertThat(http.getBody("/path-param/")).isEqualTo("path-param/")
        app.unsafe.routes.get("/{a}/{A}/") { it.result("${it.pathParam("a")}-${it.pathParam("A")}/") }
        assertThat(http.getBody("/a/B/")).isEqualTo("a-B/")
    }

    @Test
    fun `path-param values retain their casing`() = TestUtil.test(javalin) { app, http ->
        app.unsafe.routes.get("/{path-param}") { it.result(it.pathParam("path-param")) }
        app.unsafe.routes.get("/{path-param}/") { it.result(it.pathParam("path-param") + "/") }
        assertThat(http.getBody("/SomeCamelCasedValue/")).isEqualTo("SomeCamelCasedValue/")
        assertThat(http.getBody("/SomeCamelCasedValue/")).isEqualTo("SomeCamelCasedValue/")
    }

    // looking for a solution to enable this on a per-path basis
    @Disabled
    @Test
    fun `path regex works`() = TestUtil.test(javalin) { app, http ->
        app.unsafe.routes.get("/{path-param}/[0-9]+") { it.result(it.pathParam("path-param")) }
        app.unsafe.routes.get("/{path-param}/[0-9]+/") { it.result(it.pathParam("path-param") + "/") }
        assertThat(http.getBody("/test/pathParam")).isEqualTo("Not found")
        assertThat(http.getBody("/test/21")).isEqualTo("test")
        assertThat(http.getBody("/test/pathParam/")).isEqualTo("Not found")
        assertThat(http.getBody("/test/21/")).isEqualTo("test/")
    }

    @Test
    fun `automatic slash prefixing works`() = TestUtil.test(
        Javalin.create {
            it.router.ignoreTrailingSlashes = false
            it.routes.apiBuilder {
                path("test") {
                    path("{id}") { get { it.result(it.pathParam("id")) } }
                    path("{id}/") { get { it.result(it.pathParam("id") + "/") } }
                    get { it.result("test") }
                }
            }
        }
    ) { app, http ->
        assertThat(http.getBody("/test/path-param")).isEqualTo("path-param")
        assertThat(http.getBody("/test/path-param/")).isEqualTo("path-param/")
        assertThat(http.getBody("/test/")).isEqualTo("Endpoint GET /test/ not found")
        assertThat(http.getBody("/test")).isEqualTo("test")
    }

}
