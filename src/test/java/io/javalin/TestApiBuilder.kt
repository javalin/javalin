/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 *
 */

package io.javalin

import com.mashape.unirest.http.HttpMethod
import io.javalin.ApiBuilder.*
import io.javalin.util.BaseTest
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.junit.Test

class TestApiBuilder : BaseTest() {

    @Test
    fun autoPrefix_path_works() {
        app.routes { path("level-1") { get("/hello", simpleAnswer("Hello from level 1")) } }
        assertThat(http.getBody("/level-1/hello"), `is`("Hello from level 1"))
    }

    @Test
    fun routesWithoutPathArg_works() {
        app.routes {
            path("api") {
                get(okHandler)
                post(okHandler)
                put(okHandler)
                delete(okHandler)
                patch(okHandler)
                path("user") {
                    get(okHandler)
                    post(okHandler)
                    put(okHandler)
                    delete(okHandler)
                    patch(okHandler)
                }
            }
        }
        val httpMethods = arrayOf(HttpMethod.GET, HttpMethod.POST, HttpMethod.PUT, HttpMethod.DELETE, HttpMethod.PATCH)
        for (httpMethod in httpMethods) {
            assertThat(http.call(httpMethod, "/api").status, `is`(200))
            assertThat(http.call(httpMethod, "/api/user").status, `is`(200))
        }
    }

    @Test
    fun test_pathWorks_forGet() {
        app.routes {
            get("/hello", simpleAnswer("Hello from level 0"))
            path("/level-1") {
                get("/hello", simpleAnswer("Hello from level 1"))
                get("/hello-2", simpleAnswer("Hello again from level 1"))
                post("/create-1", simpleAnswer("Created something at level 1"))
                path("/level-2") {
                    get("/hello", simpleAnswer("Hello from level 2"))
                    path("/level-3") { get("/hello", simpleAnswer("Hello from level 3")) }
                }
            }
        }
        assertThat(http.getBody("/hello"), `is`("Hello from level 0"))
        assertThat(http.getBody("/level-1/hello"), `is`("Hello from level 1"))
        assertThat(http.getBody("/level-1/level-2/hello"), `is`("Hello from level 2"))
        assertThat(http.getBody("/level-1/level-2/level-3/hello"), `is`("Hello from level 3"))
    }

    @Test
    fun test_pathWorks_forFilters() {
        app.routes {
            path("level-1") {
                before { ctx -> ctx.result("1") }
                path("level-2") {
                    path("level-3") { get("/hello", updateAnswer("Hello")) }
                    after(updateAnswer("2"))
                }
            }
        }
        assertThat(http.getBody("/level-1/level-2/level-3/hello"), `is`("1Hello2"))
    }

    @Test
    fun test_pathWorks_forNonSlashVerb() {
        app.routes {
            path("level-1") {
                get { ctx -> ctx.result("level-1") }
                get("hello") { ctx -> ctx.result("Hello") }
            }
        }
        assertThat(http.getBody("/level-1"), `is`("level-1"))
        assertThat(http.getBody("/level-1/hello"), `is`("Hello"))
    }

    private fun simpleAnswer(body: String) = Handler { ctx -> ctx.result(body) }
    private fun updateAnswer(body: String) = Handler { ctx -> ctx.result(ctx.resultString()!! + body) }

    @Test(expected = IllegalStateException::class)
    fun test_throwsException_ifUsedOutsideRoutes() {
        ApiBuilder.get("/") { ctx -> ctx.result("") }
    }

}

