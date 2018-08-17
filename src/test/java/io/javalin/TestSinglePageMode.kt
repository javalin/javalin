/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin

import io.javalin.core.util.Header
import io.javalin.core.util.OptionalDependency
import io.javalin.util.TestUtil
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test

class TestSinglePageMode {

    private val rootSinglePageApp = Javalin.create().enableStaticFiles("/public").enableWebJars().enableSinglePageMode("/", "/public/html.html")
    private val dualSinglePageApp = Javalin.create().enableStaticFiles("/public")
            .enableSinglePageMode("/admin", "/public/protected/secret.html")
            .enableSinglePageMode("/public", "/public/html.html")

    @Test
    fun `SinglePageHandler works for HTML requests`() = TestUtil.test(rootSinglePageApp) { app, http ->
        assertThat(http.htmlGet("/not-a-path").body, containsString("HTML works"))
        assertThat(http.htmlGet("/not-a-file.html").body, containsString("HTML works"))
        assertThat(http.htmlGet("/not-a-file.html").status, `is`(200))
    }

    @Test
    fun `SinglePageHandler doesn't affect static files`() = TestUtil.test(rootSinglePageApp) { app, http ->
        assertThat(http.htmlGet("/script.js").headers.getFirst(Header.CONTENT_TYPE), containsString("application/javascript"))
        assertThat(http.htmlGet("/webjars/swagger-ui/${OptionalDependency.SWAGGERUI.version}/swagger-ui.css").headers.getFirst(Header.CONTENT_TYPE), containsString("text/css"))
        assertThat(http.htmlGet("/webjars/swagger-ui/${OptionalDependency.SWAGGERUI.version}/swagger-ui.css").status, `is`(200))
    }

    @Test
    fun `SinglePageHandler doesn't affect JSON requests`() = TestUtil.test(rootSinglePageApp) { app, http ->
        assertThat(http.jsonGet("/").body, containsString("Not found"))
        assertThat(http.jsonGet("/not-a-file.html").body, containsString("Not found"))
        assertThat(http.jsonGet("/not-a-file.html").status, `is`(404))
    }

    @Test
    fun `SinglePageHandler works for just subpaths`() = TestUtil.test(dualSinglePageApp) { app, http ->
        assertThat(http.htmlGet("/").body, containsString("Not found"))
        assertThat(http.htmlGet("/").status, `is`(404))
        assertThat(http.htmlGet("/admin").body, containsString("Secret file"))
        assertThat(http.htmlGet("/admin/not-a-path").body, containsString("Secret file"))
        assertThat(http.htmlGet("/public").body, containsString("HTML works"))
        assertThat(http.htmlGet("/public/not-a-file.html").body, containsString("HTML works"))
        assertThat(http.htmlGet("/public/not-a-file.html").status, `is`(200))
    }

}

