/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 *
 */

package io.javalin.staticfiles

import io.javalin.Javalin
import io.javalin.http.ContentType
import io.javalin.http.Header
import io.javalin.http.HttpStatus.NOT_FOUND
import io.javalin.http.HttpStatus.OK
import io.javalin.http.HttpStatus.UNAUTHORIZED
import io.javalin.http.UnauthorizedResponse
import io.javalin.http.staticfiles.Location
import io.javalin.testing.TestDependency
import io.javalin.testing.TestUtil
import io.javalin.testing.httpCode
import jakarta.servlet.DispatcherType
import jakarta.servlet.Filter
import jakarta.servlet.FilterChain
import jakarta.servlet.FilterConfig
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.jetty.server.ServletResponseHttpWrapper
import org.eclipse.jetty.server.handler.ContextHandler
import org.eclipse.jetty.servlet.FilterHolder
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*

class TestStaticFiles {

    @TempDir
    lateinit var workingDirectory: File

    private val defaultStaticResourceApp: Javalin by lazy { Javalin.create { it.staticFiles.add("/public", Location.CLASSPATH) } }
    private val externalStaticResourceApp: Javalin by lazy { Javalin.create { it.staticFiles.add("src/test/external/", Location.EXTERNAL) } }
    private val multiLocationStaticResourceApp: Javalin by lazy {
        Javalin.create {
            it.staticFiles.add("src/test/external/", Location.EXTERNAL)
            it.staticFiles.add("/public/immutable", Location.CLASSPATH)
            it.staticFiles.add("/public/protected", Location.CLASSPATH)
            it.staticFiles.add("/public/subdir", Location.CLASSPATH)
        }
    }
    private val customHeaderApp: Javalin by lazy { Javalin.create { it.staticFiles.add { it.headers = mapOf(Header.CACHE_CONTROL to "max-age=31622400") } } }
    private val devLoggingApp: Javalin by lazy {
        Javalin.create {
            it.plugins.enableDevLogging()
            it.staticFiles.add("/public", Location.CLASSPATH)
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
            it.staticFiles.add("/public", Location.CLASSPATH)
            it.jetty.contextHandlerConfig { handler ->
                handler.addFilter(FilterHolder(filter), "/*", EnumSet.allOf(DispatcherType::class.java))
            }
        }
    }

    private fun createSymLink(resourcePath: String, linkName: String): File? {
        val resource = Paths.get(resourcePath).toAbsolutePath()
        val link = workingDirectory.toPath().resolve(linkName).toAbsolutePath()
        Files.createSymbolicLink(link, resource)
        return link.toFile()
    }

    @Test
    fun `alias checks for static files should work`() {
        val staticWithAliasResourceApp = Javalin.create { config ->
            // block aliases for txt files
            val aliasCheck = ContextHandler.AliasCheck { path, resource -> !path.endsWith(".txt") }
            config.staticFiles.add {
                it.aliasCheck = aliasCheck
                it.directory = workingDirectory.absolutePath
                it.location = Location.EXTERNAL
            }
            config.staticFiles.add {
                it.hostedPath = "/url-prefix"
                it.aliasCheck = aliasCheck
            }
        }

        try {
            createSymLink("src/test/external/html.html", "linked_html.html")
            createSymLink("src/test/external/txt.txt", "linked_txt.txt")

            TestUtil.test(staticWithAliasResourceApp) { _, http ->
                assertThat(http.get("/linked_html.html").status).isEqualTo(200)
                assertThat(http.get("/linked_txt.txt").status).isEqualTo(404)
                assertThat(http.get("/url-prefix/styles.css").status).isEqualTo(200)
            }
        } catch (ioException: IOException) {
            ioException.printStackTrace()
        }
    }

    @Test
    fun `if aliases are not specified returns 404 for linked static file`() {
        val staticNoAliasCheckResourceApp = Javalin.create {
            it.staticFiles.add(workingDirectory.absolutePath, Location.EXTERNAL)
        }

        try {
            createSymLink("src/test/external/html.html", "linked_html.html")

            TestUtil.test(staticNoAliasCheckResourceApp) { _, http ->
                assertThat(http.get("/linked_html.html").status).isEqualTo(404)
            }
        } catch (ioException: IOException) {
            ioException.printStackTrace()
        }
    }

    @Test
    fun `serving HTML from classpath works`() = TestUtil.test(defaultStaticResourceApp) { _, http ->
        assertThat(http.get("/html.html").httpCode()).isEqualTo(OK)
        assertThat(http.get("/html.html").headers.getFirst(Header.CONTENT_TYPE)).contains(ContentType.HTML)
        assertThat(http.getBody("/html.html")).contains("HTML works")
    }

    @Test
    fun `serving JS from classpath works`() = TestUtil.test(defaultStaticResourceApp) { _, http ->
        assertThat(http.get("/script.js").httpCode()).isEqualTo(OK)
        assertThat(http.get("/script.js").headers.getFirst(Header.CONTENT_TYPE)).contains(ContentType.JAVASCRIPT_MODERN)
        assertThat(http.getBody("/script.js")).contains("JavaScript works")
    }

    @Test
    fun `serving mjs from classpath works`() = TestUtil.test(defaultStaticResourceApp) { _, http ->
        assertThat(http.get("/module.mjs").httpCode()).isEqualTo(OK)
        assertThat(http.get("/module.mjs").headers.getFirst(Header.CONTENT_TYPE)).contains(ContentType.JAVASCRIPT_MODERN)
        assertThat(http.getBody("/module.mjs")).contains("export function test()").contains("mjs works")
    }

    @Test
    fun `serving CSS from classpath works`() = TestUtil.test(defaultStaticResourceApp) { _, http ->
        assertThat(http.get("/styles.css").httpCode()).isEqualTo(OK)
        assertThat(http.get("/styles.css").headers.getFirst(Header.CONTENT_TYPE)).contains(ContentType.CSS)
        assertThat(http.getBody("/styles.css")).contains("CSS works")
    }

    @Test
    fun `before-handler runs before static resources`() = TestUtil.test(defaultStaticResourceApp) { app, http ->
        app.before("/protected/*") { throw UnauthorizedResponse("Protected") }
        assertThat(http.get("/protected/secret.html").httpCode()).isEqualTo(UNAUTHORIZED)
        assertThat(http.getBody("/protected/secret.html")).isEqualTo("Protected")
    }

    @Test
    fun `directory root returns simple 404 if there is no welcome file`() = TestUtil.test(defaultStaticResourceApp) { _, http ->
        assertThat(http.get("/").httpCode()).isEqualTo(NOT_FOUND)
        assertThat(http.getBody("/")).isEqualTo(NOT_FOUND.message)
    }

    @Test
    fun `directory root return welcome file if there is a welcome file`() = TestUtil.test(defaultStaticResourceApp) { _, http ->
        assertThat(http.get("/subdir/").httpCode()).isEqualTo(OK)
        assertThat(http.getBody("/subdir/")).isEqualTo("<h1>Welcome file</h1>")
        assertThat(http.get("/subdir").httpCode()).isEqualTo(OK)
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
        assertThat(http.get("/html.html").headers.getFirst(Header.CONTENT_TYPE)).contains(ContentType.HTML)
        assertThat(http.getBody("/html.html")).contains("HTML works")
        assertThat(http.get("/script.js").headers.getFirst(Header.CONTENT_TYPE)).contains(ContentType.JAVASCRIPT_MODERN)
        assertThat(http.get("/styles.css").headers.getFirst(Header.CONTENT_TYPE)).contains(ContentType.CSS)
    }

    @Test
    fun `WebJars available if enabled`() = TestUtil.test(Javalin.create { it.staticFiles.enableWebjars() }) { _, http ->
        assertThat(http.get("/webjars/swagger-ui/${TestDependency.swaggerVersion}/swagger-ui.css").status).isEqualTo(200)
        assertThat(http.get("/webjars/swagger-ui/${TestDependency.swaggerVersion}/swagger-ui.css").headers.getFirst(
            Header.CONTENT_TYPE)).contains(ContentType.CSS)
        assertThat(http.get("/webjars/swagger-ui/${TestDependency.swaggerVersion}/swagger-ui.css").headers.getFirst(
            Header.CACHE_CONTROL)).isEqualTo("max-age=31622400")
    }

    @Test
    fun `WebJars not available if not enabled`() = TestUtil.test { _, http ->
        assertThat(http.get("/webjars/swagger-ui/${TestDependency.swaggerVersion}/swagger-ui.css").status).isEqualTo(404)
    }

    @Test
    fun `Correct content type is returned when a custom filter with a response wrapper is added`() = TestUtil.test(customFilterStaticResourceApp) { _, http ->
        assertThat(http.get("/html.html").status).isEqualTo(200)
        assertThat(http.get("/html.html").headers.getFirst(Header.CONTENT_TYPE)).contains(ContentType.HTML)
    }

    @Test
    fun `serving from custom url path works`() {
        TestUtil.test(Javalin.create { javalin ->
            javalin.staticFiles.add("/public", Location.CLASSPATH)
            javalin.staticFiles.add { staticFiles ->
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
    fun `no exceptions in logs when getting hosted path`() = TestUtil.test(Javalin.create { javalin ->
        javalin.staticFiles.add { staticFiles -> staticFiles.hostedPath = "/url-prefix" }
    }) { _, http ->
        val log = TestUtil.captureStdOut {
            http.get("/url-prefix")
            http.get("/url-prefixy")
        }
        assertThat(http.get("/url-prefix").status).isEqualTo(404)
        assertThat(http.get("/url-prefixy").status).isEqualTo(404)
        assertThat(log).doesNotContain("Exception occurred while handling static resource")
    }

    @Test
    fun `urlPathPrefix filters requests to a specific subfolder`() {
        TestUtil.test(Javalin.create { servlet ->
            // effectively equivalent to servlet.staticFiles.add("/public", Location.CLASSPATH)
            // but with benefit of additional "filtering": only requests matching /assets/* will be matched against static resources handler
            servlet.staticFiles.add {
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
    fun `only handle resources matching the filter`() {
        TestUtil.test(Javalin.create { servlet ->
            // use custom filter - only urls ending with ".css" are handled
            servlet.staticFiles.add {
                it.hostedPath = "/"
                it.directory = "/public"
                it.location = Location.CLASSPATH
                it.skipFileFunction = { !it.requestURI.endsWith(".css") }
            }
        }) { _, http ->
            assertThat(http.get("/styles.css").status).isEqualTo(200)
            assertThat(http.get("/script.js").status).isEqualTo(404)
        }
    }

    @Test
    fun `logs handlers added on startup`() {
        TestUtil.test(multiLocationStaticResourceApp) { _, _ -> }
        assertThat(multiLocationStaticResourceApp.attribute<String>("testlogs").split("Static file handler added").size - 1).isEqualTo(4)
    }

    @Test
    fun `static files can be added after app creation`() = TestUtil.test(
        Javalin.create().updateConfig { it.staticFiles.add("/public", Location.CLASSPATH) }
    ) { _, http ->
        assertThat(http.get("/html.html").httpCode()).isEqualTo(OK)
        assertThat(http.get("/html.html").headers.getFirst(Header.CONTENT_TYPE)).contains(ContentType.HTML)
        assertThat(http.getBody("/html.html")).contains("HTML works")
    }

    @Test
    fun `static files can be added after app start with previous static files`() = TestUtil.test(
        Javalin.create().updateConfig { it.staticFiles.add("/public", Location.CLASSPATH) }
    ) { app, http ->
        app.updateConfig {
            it.staticFiles.add { staticFiles ->
                staticFiles.hostedPath = "/url-prefix"
                staticFiles.directory = "/public"
                staticFiles.location = Location.CLASSPATH
            }
        }
        assertThat(http.get("/html.html").httpCode()).isEqualTo(OK)
        assertThat(http.get("/html.html").headers.getFirst(Header.CONTENT_TYPE)).contains(ContentType.HTML)
        assertThat(http.getBody("/html.html")).contains("HTML works")

        assertThat(http.get("/url-prefix/html.html").httpCode()).isEqualTo(OK)
        assertThat(http.get("/url-prefix/html.html").headers.getFirst(Header.CONTENT_TYPE)).contains(ContentType.HTML)
        assertThat(http.getBody("/url-prefix/html.html")).contains("HTML works")
    }

    @Test
    fun `static files can be added after app start without previous static files`() = TestUtil.test(
        Javalin.create()
    ) { app, http ->
        app.updateConfig {
            it.staticFiles.add("/public", Location.CLASSPATH)
        }

        assertThat(http.get("/html.html").httpCode()).isEqualTo(OK)
        assertThat(http.get("/html.html").headers.getFirst(Header.CONTENT_TYPE)).contains(ContentType.HTML)
        assertThat(http.getBody("/html.html")).contains("HTML works")
    }

    @Test
    fun `can add custom mimetype mappings`() = TestUtil.test(Javalin.create { config ->
        config.staticFiles.add {
            it.directory = "/public"
            it.location = Location.CLASSPATH
            it.mimeTypes.add("application/x-javalin", "javalin")
        }
    }) { _, http ->
        assertThat(http.get("/file.javalin").httpCode()).isEqualTo(OK)
        assertThat(http.get("/file.javalin").headers.getFirst(Header.CONTENT_TYPE)).contains("application/x-javalin")
        assertThat(http.getBody("/file.javalin")).contains("TESTFILE.javalin")
    }
}
