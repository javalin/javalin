/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 *
 */

package io.javalin

import io.javalin.core.util.Header
import io.javalin.staticfiles.Location
import io.javalin.util.TestUtil
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.containsString
import org.junit.Test

class TestStaticFiles {

    private val defaultStaticResourceApp = Javalin.create().enableStaticFiles("/public") // classpath
    private val externalStaticResourceApp = Javalin.create().enableStaticFiles("src/test/external/", Location.EXTERNAL)
    private val multiLocationStaticResourceApp = Javalin.create()
            .enableStaticFiles("src/test/external/", Location.EXTERNAL)
            .enableStaticFiles("/public/immutable")
            .enableStaticFiles("/public/protected")
            .enableStaticFiles("/public/subdir")

    @Test
    fun test_Html() = TestUtil(defaultStaticResourceApp).test { app, http ->
        assertThat(http.get("/html.html").code(), `is`(200))
        assertThat(http.get("/html.html").header(Header.CONTENT_TYPE), containsString("text/html"))
        assertThat(http.getBody("/html.html"), containsString("HTML works"))
    }

    @Test
    fun test_getJs() = TestUtil(defaultStaticResourceApp).test { app, http ->
        assertThat(http.get("/script.js").code(), `is`(200))
        assertThat(http.get("/script.js").header(Header.CONTENT_TYPE), containsString("application/javascript"))
        assertThat(http.getBody("/script.js"), containsString("JavaScript works"))
    }

    @Test
    fun test_getCss() = TestUtil(defaultStaticResourceApp).test { app, http ->
        assertThat(http.get("/styles.css").code(), `is`(200))
        assertThat(http.get("/styles.css").header(Header.CONTENT_TYPE), containsString("text/css"))
        assertThat(http.getBody("/styles.css"), containsString("CSS works"))
    }

    @Test
    fun test_beforeFilter() = TestUtil(defaultStaticResourceApp).test { app, http ->
        app.before("/protected/*") { ctx -> throw HaltException(401, "Protected") }
        assertThat(http.get("/protected/secret.html").code(), `is`(401))
        assertThat(http.getBody("/protected/secret.html"), `is`("Protected"))
    }

    @Test
    fun test_rootReturns404_ifNoWelcomeFile() = TestUtil(defaultStaticResourceApp).test { app, http ->
        assertThat(http.get("/").code(), `is`(404))
        assertThat(http.getBody("/"), `is`("Not found"))
    }

    @Test
    fun test_rootReturnsWelcomeFile_ifWelcomeFileExists() = TestUtil(defaultStaticResourceApp).test { app, http ->
        assertThat(http.get("/subdir/").code(), `is`(200))
        assertThat(http.getBody("/subdir/"), `is`("<h1>Welcome file</h1>"))
    }

    @Test
    fun test_expiresWorksAsExpected() = TestUtil(defaultStaticResourceApp).test { app, http ->
        assertThat(http.get("/script.js").header(Header.CACHE_CONTROL), `is`("max-age=0"))
        assertThat(http.get("/immutable/library-1.0.0.min.js").header(Header.CACHE_CONTROL), `is`("max-age=31622400"))
    }

    @Test
    fun test_externalFolder() = TestUtil(externalStaticResourceApp).test { app, http ->
        assertThat(http.get("/html.html").code(), `is`(200))
        assertThat(http.getBody("/html.html"), containsString("HTML works"))
    }

    @Test
    fun test_multipleCallsToEnableStaticFiles() = TestUtil(multiLocationStaticResourceApp).test { app, http ->
        assertThat(http.get("/html.html").code(), `is`(200)) // src/test/external/html.html
        assertThat(http.getBody("/html.html"), containsString("HTML works"))
        assertThat(http.get("/").code(), `is`(200))
        assertThat(http.getBody("/"), `is`("<h1>Welcome file</h1>"))
        assertThat(http.get("/secret.html").code(), `is`(200))
        assertThat(http.getBody("/secret.html"), `is`("<h1>Secret file</h1>"))
        assertThat(http.get("/styles.css").code(), `is`(404))
    }

}
