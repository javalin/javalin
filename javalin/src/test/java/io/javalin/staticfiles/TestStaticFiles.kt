/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 *
 */

package io.javalin.staticfiles

import io.javalin.Javalin
import io.javalin.core.util.Header
import io.javalin.core.util.OptionalDependency
import io.javalin.http.UnauthorizedResponse
import io.javalin.http.staticfiles.Location
import io.javalin.testing.TestLoggingUtil
import io.javalin.testing.TestUtil
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.jetty.server.ServletResponseHttpWrapper
import org.eclipse.jetty.server.handler.ContextHandler
import org.eclipse.jetty.servlet.FilterHolder
import org.junit.Test
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*
import javax.servlet.DispatcherType
import javax.servlet.Filter
import javax.servlet.FilterChain
import javax.servlet.FilterConfig
import javax.servlet.ServletRequest
import javax.servlet.ServletResponse

class TestStaticFiles {

    private val defaultStaticResourceApp: Javalin by lazy { Javalin.create { it.addStaticFiles("/public", Location.CLASSPATH) } }
    private val externalStaticResourceApp: Javalin by lazy { Javalin.create { it.addStaticFiles("src/test/external/", Location.EXTERNAL) } }
    private val multiLocationStaticResourceApp: Javalin by lazy {
        Javalin.create {
            it.addStaticFiles("src/test/external/", Location.EXTERNAL)
            it.addStaticFiles("/public/immutable", Location.CLASSPATH)
            it.addStaticFiles("/public/protected", Location.CLASSPATH)
            it.addStaticFiles("/public/subdir", Location.CLASSPATH)
        }
    }
    private val customHeaderApp: Javalin by lazy { Javalin.create { it.addStaticFiles { it.headers = mapOf(Header.CACHE_CONTROL to "max-age=31622400") } } }
    private val devLoggingApp: Javalin by lazy {
        Javalin.create {
            it.enableDevLogging()
            it.addStaticFiles("/public", Location.CLASSPATH)
        }
    }

    private val customFilterStaticResourceApp: Javalin by lazy {
        Javalin.create {
            val filter = object : Filter {
                override fun init(config: FilterConfig?) {
                }

                override fun doFilter(request: ServletRequest?, response: ServletResponse?, chain: FilterChain?) {
                    chain?.doFilter(request, ServletResponseHttpWrapper(response))
                }

                override fun destroy() {
                }
            }
            it.addStaticFiles("/public", Location.CLASSPATH)
            it.configureServletContextHandler { handler ->
                handler.addFilter(FilterHolder(filter), "/*", EnumSet.allOf(DispatcherType::class.java))
            }
        }
    }

    private fun createSymLink(targetPath: String, linkPath: String): File? {
        val target = Paths.get(targetPath).toAbsolutePath()
        val link = Paths.get(linkPath).toAbsolutePath()
        // delete before creating new link
        link.toFile().delete()
        try {
            Files.createSymbolicLink(link, target)
        } catch (e: IOException) {
            e.printStackTrace()
            return null
        }
        return link.toFile()
    }

    @Test
    fun `alias checks for static files should work`() {
        val staticWithAliasResourceApp = Javalin.create {
            // block aliases for txt files
            val aliasCheck = ContextHandler.AliasCheck { path, resource -> !path.endsWith(".txt") }
            it.addStaticFiles {
                it.aliasCheck = aliasCheck
                it.directory = "src/test/external/"
                it.location = Location.EXTERNAL
            }
            it.addStaticFiles {
                it.hostedPath = "/url-prefix"
                it.aliasCheck = aliasCheck
            }
        }

        val createdHtml = createSymLink("src/test/external/html.html", "src/test/external/linked_html.html")
        if (createdHtml != null) {
            val createdTxt = createSymLink("src/test/external/txt.txt", "src/test/external/linked_txt.txt")
            if (createdTxt != null) {
                TestUtil.test(staticWithAliasResourceApp) { _, http ->
                    assertThat(http.get("/linked_html.html").status).isEqualTo(200)
                    assertThat(http.get("/linked_txt.txt").status).isEqualTo(404)
                    assertThat(http.get("/url-prefix/styles.css").status).isEqualTo(200)
                }
                createdTxt.delete()
            }
            createdHtml.delete()
        }
    }

    @Test
    fun `if aliases are not specified returns 404 for linked static file`() {
        val staticNoAliasCheckResourceApp = Javalin.create {
            it.addStaticFiles("src/test/external/", Location.EXTERNAL)
        }
        val created = createSymLink("src/test/external/html.html", "src/test/external/linked_html.html")
        if (created != null) {
            TestUtil.test(staticNoAliasCheckResourceApp) { _, http ->
                assertThat(http.get("/linked_html.html").status).isEqualTo(404)
            }
            created.delete()
        }
    }

    @Test
    fun `serving HTML from classpath works`() = TestUtil.test(defaultStaticResourceApp) { _, http ->
        assertThat(http.get("/html.html").status).isEqualTo(200)
        assertThat(http.get("/html.html").headers.getFirst(Header.CONTENT_TYPE)).contains("text/html")
        assertThat(http.getBody("/html.html")).contains("HTML works")
    }

    @Test
    fun `serving JS from classpath works`() = TestUtil.test(defaultStaticResourceApp) { _, http ->
        assertThat(http.get("/script.js").status).isEqualTo(200)
        assertThat(http.get("/script.js").headers.getFirst(Header.CONTENT_TYPE)).contains("application/javascript")
        assertThat(http.getBody("/script.js")).contains("JavaScript works")
    }

    @Test
    fun `serving CSS from classpath works`() = TestUtil.test(defaultStaticResourceApp) { _, http ->
        assertThat(http.get("/styles.css").status).isEqualTo(200)
        assertThat(http.get("/styles.css").headers.getFirst(Header.CONTENT_TYPE)).contains("text/css")
        assertThat(http.getBody("/styles.css")).contains("CSS works")
    }

    @Test
    fun `before-handler runs before static resources`() = TestUtil.test(defaultStaticResourceApp) { app, http ->
        app.before("/protected/*") { throw UnauthorizedResponse("Protected") }
        assertThat(http.get("/protected/secret.html").status).isEqualTo(401)
        assertThat(http.getBody("/protected/secret.html")).isEqualTo("Protected")
    }

    @Test
    fun `directory root returns simple 404 if there is no welcome file`() = TestUtil.test(defaultStaticResourceApp) { _, http ->
        assertThat(http.get("/").status).isEqualTo(404)
        assertThat(http.getBody("/")).isEqualTo("Not found")
    }

    @Test
    fun `directory root return welcome file if there is a welcome file`() = TestUtil.test(defaultStaticResourceApp) { _, http ->
        assertThat(http.get("/subdir/").status).isEqualTo(200)
        assertThat(http.getBody("/subdir/")).isEqualTo("<h1>Welcome file</h1>")
        assertThat(http.get("/subdir").status).isEqualTo(200)
        assertThat(http.getBody("/subdir")).isEqualTo("<h1>Welcome file</h1>")
    }

    @Test
    fun `expires is set to max-age=0 by default`() = TestUtil.test(defaultStaticResourceApp) { _, http ->
        assertThat(http.get("/script.js").headers.getFirst(Header.CACHE_CONTROL)).isEqualTo("max-age=0")
    }

    @Test
    fun `can set custom headers`() = TestUtil.test(customHeaderApp) { _, http ->
        assertThat(http.get("/immutable/library-1.0.0.min.js").headers.getFirst(Header.CACHE_CONTROL)).isEqualTo("max-age=31622400")
    }

    @Test
    fun `files in external locations are found`() = TestUtil.test(externalStaticResourceApp) { _, http ->
        assertThat(http.get("/html.html").status).isEqualTo(200)
        assertThat(http.getBody("/html.html")).contains("HTML works")
    }

    @Test
    fun `one app can handle multiple static file locations`() = TestUtil.test(multiLocationStaticResourceApp) { _, http ->
        assertThat(http.get("/html.html").status).isEqualTo(200) // src/test/external/html.html
        assertThat(http.getBody("/html.html")).contains("HTML works")
        assertThat(http.get("/").status).isEqualTo(200)
        assertThat(http.getBody("/")).isEqualTo("<h1>Welcome file</h1>")
        assertThat(http.get("/secret.html").status).isEqualTo(200)
        assertThat(http.getBody("/secret.html")).isEqualTo("<h1>Secret file</h1>")
        assertThat(http.get("/styles.css").status).isEqualTo(404)
    }

    @Test
    fun `content type works in debugmmode`() = TestUtil.test(devLoggingApp) { _, http ->
        assertThat(http.get("/html.html").status).isEqualTo(200)
        assertThat(http.get("/html.html").headers.getFirst(Header.CONTENT_TYPE)).contains("text/html")
        assertThat(http.getBody("/html.html")).contains("HTML works")
        assertThat(http.get("/script.js").headers.getFirst(Header.CONTENT_TYPE)).contains("application/javascript")
        assertThat(http.get("/styles.css").headers.getFirst(Header.CONTENT_TYPE)).contains("text/css")
    }

    @Test
    fun `WebJars available if enabled`() = TestUtil.test(Javalin.create { it.enableWebjars() }) { _, http ->
        assertThat(http.get("/webjars/swagger-ui/${OptionalDependency.SWAGGERUI.version}/swagger-ui.css").status).isEqualTo(200)
        assertThat(http.get("/webjars/swagger-ui/${OptionalDependency.SWAGGERUI.version}/swagger-ui.css").headers.getFirst(Header.CONTENT_TYPE)).contains("text/css")
        assertThat(http.get("/webjars/swagger-ui/${OptionalDependency.SWAGGERUI.version}/swagger-ui.css").headers.getFirst(Header.CACHE_CONTROL)).isEqualTo("max-age=31622400")
    }

    @Test
    fun `WebJars not available if not enabled`() = TestUtil.test { _, http ->
        assertThat(http.get("/webjars/swagger-ui/${OptionalDependency.SWAGGERUI.version}/swagger-ui.css").status).isEqualTo(404)
    }

    @Test
    fun `Correct content type is returned when a custom filter with a response wrapper is added`() = TestUtil.test(customFilterStaticResourceApp) { _, http ->
        assertThat(http.get("/html.html").status).isEqualTo(200)
        assertThat(http.get("/html.html").headers.getFirst(Header.CONTENT_TYPE)).contains("text/html")
    }

    @Test
    fun `serving from custom url path works`() {
        TestUtil.test(Javalin.create { javalin ->
            javalin.addStaticFiles("/public", Location.CLASSPATH)
            javalin.addStaticFiles { staticFiles ->
                staticFiles.hostedPath = "/url-prefix"
                staticFiles.directory = "/public"
                staticFiles.location = Location.CLASSPATH
            }
        }) { _, http ->
            assertThat(http.get("/styles.css").status).isEqualTo(200)
            assertThat(http.get("/url-prefix/styles.css").status).isEqualTo(200)
        }
    }

    @Test
    fun `urlPathPrefix filters requests to a specific subfolder`() {
        TestUtil.test(Javalin.create { servlet ->
            // effectively equivalent to servlet.addStaticFiles("/public", Location.CLASSPATH)
            // but with benefit of additional "filtering": only requests matching /assets/* will be matched against static resources handler
            servlet.addStaticFiles {
                it.hostedPath = "/assets"
                it.directory = "/public/assets"
                it.location = Location.CLASSPATH
            }
        }) { _, http ->
            assertThat(http.get("/assets/filtered-styles.css").status).isEqualTo(200) // access to urls matching /assets/* is allowed
            assertThat(http.get("/filtered-styles.css").status).isEqualTo(404) // direct access to a file in the subfolder is not allowed
            assertThat(http.get("/styles.css").status).isEqualTo(404) // access to other locations in /public is not allowed
        }
    }

    @Test
    fun `logs handlers added on startup`() {
        val logOutput = TestLoggingUtil.captureStdOut { TestUtil.test(multiLocationStaticResourceApp) { _, _ -> } }
        assertThat(logOutput.split("Static file handler added").size - 1).isEqualTo(4)
    }

}
