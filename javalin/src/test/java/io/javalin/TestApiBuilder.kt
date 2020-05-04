/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 *
 */

package io.javalin

import com.mashape.unirest.http.HttpMethod
import com.mashape.unirest.http.Unirest
import io.javalin.apibuilder.ApiBuilder
import io.javalin.apibuilder.ApiBuilder.after
import io.javalin.apibuilder.ApiBuilder.before
import io.javalin.apibuilder.ApiBuilder.crud
import io.javalin.apibuilder.ApiBuilder.delete
import io.javalin.apibuilder.ApiBuilder.get
import io.javalin.apibuilder.ApiBuilder.patch
import io.javalin.apibuilder.ApiBuilder.path
import io.javalin.apibuilder.ApiBuilder.post
import io.javalin.apibuilder.ApiBuilder.put
import io.javalin.apibuilder.CrudHandler
import io.javalin.http.Context
import io.javalin.http.Handler
import io.javalin.testing.TestUtil
import io.javalin.testing.TestUtil.okHandler
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.Test

class TestApiBuilder {

    @Test
    fun `ApiBuilder prefixes paths with slash`() = TestUtil.test { app, http ->
        app.routes {
            path("level-1") {
                get("hello", simpleAnswer("Hello from level 1"))
            }
        }
        assertThat(http.getBody("/level-1/hello")).isEqualTo("Hello from level 1")
    }

    @Test
    fun `pathless routes are handled properly`() = TestUtil.test { app, http ->
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
            assertThat(http.call(httpMethod, "/api").status).isEqualTo(200)
            assertThat(http.call(httpMethod, "/api/user").status).isEqualTo(200)
        }
    }

    @Test
    fun `multiple nested path-method calls works`() = TestUtil.test { app, http ->
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
        assertThat(http.getBody("/hello")).isEqualTo("Hello from level 0")
        assertThat(http.getBody("/level-1/hello")).isEqualTo("Hello from level 1")
        assertThat(http.getBody("/level-1/level-2/hello")).isEqualTo("Hello from level 2")
        assertThat(http.getBody("/level-1/level-2/level-3/hello")).isEqualTo("Hello from level 3")
    }

    @Test
    fun `filters work as expected`() = TestUtil.test { app, http ->
        app.routes {
            path("level-1") {
                before { ctx -> ctx.result("1") }
                path("level-2") {
                    path("level-3") { get("/hello", updateAnswer("Hello")) }
                    after(updateAnswer("2"))
                }
            }
        }
        assertThat(http.getBody("/level-1/level-2/level-3/hello")).isEqualTo("1Hello2")
    }

    @Test
    fun `slashes can be omitted for both path-method and verbs`() = TestUtil.test { app, http ->
        app.routes {
            path("level-1") {
                get { ctx -> ctx.result("level-1") }
                get("hello") { ctx -> ctx.result("Hello") }
            }
        }
        assertThat(http.getBody("/level-1")).isEqualTo("level-1")
        assertThat(http.getBody("/level-1/hello")).isEqualTo("Hello")
    }

    private fun simpleAnswer(body: String) = Handler { ctx -> ctx.result(body) }
    private fun updateAnswer(body: String) = Handler { ctx -> ctx.result(ctx.resultString()!! + body) }

    @Test
    fun `ApiBuilder throws if used outside of routes{} call`() = TestUtil.test { _, _ ->
        assertThatExceptionOfType(IllegalStateException::class.java)
                .isThrownBy { ApiBuilder.get("/") { ctx -> ctx.result("") } }
                .withMessageStartingWith("The static API can only be used within a routes() call.")
    }

    @Test
    fun `ApiBuilder works with two services at once`() {
        val app1 = Javalin.create().start(0)
        val app2 = Javalin.create().start(0)
        app1.routes { get("/hello-1") { ctx -> ctx.result("Hello-1") } }
        app1.routes { get("/hello-2") { ctx -> ctx.result("Hello-2") } }
        app2.routes { get("/hello-1") { ctx -> ctx.result("Hello-1") } }
        app2.routes { get("/hello-2") { ctx -> ctx.result("Hello-2") } }
        assertThat(Unirest.get("http://localhost:" + app1.port() + "/hello-1").asString().body).isEqualTo("Hello-1")
        assertThat(Unirest.get("http://localhost:" + app1.port() + "/hello-2").asString().body).isEqualTo("Hello-2")
        assertThat(Unirest.get("http://localhost:" + app2.port() + "/hello-1").asString().body).isEqualTo("Hello-1")
        assertThat(Unirest.get("http://localhost:" + app2.port() + "/hello-2").asString().body).isEqualTo("Hello-2")
        app1.stop()
        app2.stop()
    }

    @Test
    fun `CrudHandler works`() = TestUtil.test { app, http ->
        app.routes {
            crud("users/:user-id", UserController())
            path("/s") {
                crud("/users/:user-id", UserController())
            }
        }
        assertThat(Unirest.get(http.origin + "/users").asString().body).isEqualTo("All my users")
        assertThat(Unirest.post(http.origin + "/users").asString().status).isEqualTo(201)
        assertThat(Unirest.get(http.origin + "/users/myUser").asString().body).isEqualTo("My single user: myUser")
        assertThat(Unirest.patch(http.origin + "/users/myUser").asString().status).isEqualTo(204)
        assertThat(Unirest.delete(http.origin + "/users/myUser").asString().status).isEqualTo(204)

        assertThat(Unirest.get(http.origin + "/s/users").asString().body).isEqualTo("All my users")
        assertThat(Unirest.post(http.origin + "/s/users").asString().status).isEqualTo(201)
        assertThat(Unirest.get(http.origin + "/s/users/myUser").asString().body).isEqualTo("My single user: myUser")
        assertThat(Unirest.patch(http.origin + "/s/users/myUser").asString().status).isEqualTo(204)
        assertThat(Unirest.delete(http.origin + "/s/users/myUser").asString().status).isEqualTo(204)
    }

    @Test
    fun `CrudHandler works with long nested resources`() = TestUtil.test { app, http ->
        app.routes {
            crud("/foo/bar/users/:user-id", UserController())
            path("/foo/baz") {
                crud("/users/:user-id", UserController())
            }
        }
        assertThat(Unirest.get(http.origin + "/foo/bar/users").asString().body).isEqualTo("All my users")
        assertThat(Unirest.post(http.origin + "/foo/bar/users").asString().status).isEqualTo(201)
        assertThat(Unirest.get(http.origin + "/foo/bar/users/myUser").asString().body).isEqualTo("My single user: myUser")
        assertThat(Unirest.patch(http.origin + "/foo/bar/users/myUser").asString().status).isEqualTo(204)
        assertThat(Unirest.delete(http.origin + "/foo/bar/users/myUser").asString().status).isEqualTo(204)

        assertThat(Unirest.get(http.origin + "/foo/baz/users").asString().body).isEqualTo("All my users")
        assertThat(Unirest.post(http.origin + "/foo/baz/users").asString().status).isEqualTo(201)
        assertThat(Unirest.get(http.origin + "/foo/baz/users/myUser").asString().body).isEqualTo("My single user: myUser")
        assertThat(Unirest.patch(http.origin + "/foo/baz/users/myUser").asString().status).isEqualTo(204)
        assertThat(Unirest.delete(http.origin + "/foo/baz/users/myUser").asString().status).isEqualTo(204)
    }

    @Test
    fun `CrudHandler rejects resource in the middle`() = TestUtil.test { app, http ->
        assertThatExceptionOfType(IllegalArgumentException::class.java).isThrownBy {
            app.routes {
                crud("/foo/bar/:user-id/users", UserController())
            }
        }.withMessageStartingWith("CrudHandler requires a path-parameter at the end of the provided path e.g. '/users/:user-id'")
    }

    @Test
    fun `CrudHandler rejects missing resource`() = TestUtil.test { app, http ->
        assertThatExceptionOfType(IllegalArgumentException::class.java).isThrownBy {
            app.routes {
                crud("/foo/bar/users", UserController())
            }
        }.withMessageStartingWith("CrudHandler requires a path-parameter at the end of the provided path e.g. '/users/:user-id'")
    }

    @Test
    fun `CrudHandler rejects missing resource base`() = TestUtil.test { app, http ->
        assertThatExceptionOfType(IllegalArgumentException::class.java).isThrownBy {
            app.routes {
                crud("/:user-id", UserController())
            }
        }.withMessageStartingWith("CrudHandler requires a resource base at the beginning of the provided path e.g. '/users/:user-id'")
    }

    @Test
    fun `CrudHandler works with wildcards`() = TestUtil.test { app, http ->
        app.routes {
            path("/s") {
                crud("/*/:user-id", UserController())
            }
            crud("*/:user-id", UserController())
        }
        assertThat(Unirest.get(http.origin + "/users").asString().body).isEqualTo("All my users")
        assertThat(Unirest.post(http.origin + "/users").asString().status).isEqualTo(201)
        assertThat(Unirest.get(http.origin + "/users/myUser").asString().body).isEqualTo("My single user: myUser")
        assertThat(Unirest.patch(http.origin + "/users/myUser").asString().status).isEqualTo(204)
        assertThat(Unirest.delete(http.origin + "/users/myUser").asString().status).isEqualTo(204)

        assertThat(Unirest.get(http.origin + "/s/users").asString().body).isEqualTo("All my users")
        assertThat(Unirest.post(http.origin + "/s/users").asString().status).isEqualTo(201)
        assertThat(Unirest.get(http.origin + "/s/users/myUser").asString().body).isEqualTo("My single user: myUser")
        assertThat(Unirest.patch(http.origin + "/s/users/myUser").asString().status).isEqualTo(204)
        assertThat(Unirest.delete(http.origin + "/s/users/myUser").asString().status).isEqualTo(204)
    }

    class UserController : CrudHandler {

        override fun getAll(ctx: Context) {
            ctx.result("All my users")
        }

        override fun getOne(ctx: Context, resourceId: String) {
            ctx.result("My single user: $resourceId")
        }

        override fun create(ctx: Context) {
            ctx.status(201)
        }

        override fun update(ctx: Context, resourceId: String) {
            ctx.status(204)
        }

        override fun delete(ctx: Context, resourceId: String) {
            ctx.status(204)
        }
    }

}

