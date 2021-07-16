/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 *
 */

package io.javalin

import com.mashape.unirest.http.HttpMethod
import com.mashape.unirest.http.Unirest
import io.javalin.http.Handler
import io.javalin.testing.TestUserController
import io.javalin.testing.TestUtil
import io.javalin.testing.TestUtil.okHandler
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.Test

class TestSubRouter {

    @Test
    fun `SubRouters prefixe paths with slash`() = TestUtil.test { app, http ->
        app.path("level-1") {
            it.get("hello", simpleAnswer("Hello from level 1"))
        }
        assertThat(http.getBody("/level-1/hello")).isEqualTo("Hello from level 1")
    }

    @Test
    fun `pathless routes are handled properly`() = TestUtil.test { app, http ->
        app.path("api") { router ->
            router.get(okHandler)
            router.post(okHandler)
            router.put(okHandler)
            router.delete(okHandler)
            router.patch(okHandler)
            router.path("user") {
                it.get(okHandler)
                it.post(okHandler)
                it.put(okHandler)
                it.delete(okHandler)
                it.patch(okHandler)
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
        app.get("/hello", simpleAnswer("Hello from level 0"))
                .path("/level-1") { router ->
                    router.get("/hello", simpleAnswer("Hello from level 1"))
                    router.get("/hello-2", simpleAnswer("Hello again from level 1"))
                    router.post("/create-1", simpleAnswer("Created something at level 1"))
                    router.path("/level-2") {
                        it.get("/hello", simpleAnswer("Hello from level 2"))
                        it.path("/level-3") { lowerRouter -> lowerRouter.get("/hello", simpleAnswer("Hello from level 3")) }
                    }
                }
        assertThat(http.getBody("/hello")).isEqualTo("Hello from level 0")
        assertThat(http.getBody("/level-1/hello")).isEqualTo("Hello from level 1")
        assertThat(http.getBody("/level-1/level-2/hello")).isEqualTo("Hello from level 2")
        assertThat(http.getBody("/level-1/level-2/level-3/hello")).isEqualTo("Hello from level 3")
    }

    @Test
    fun `filters work as expected`() = TestUtil.test { app, http ->
        app.path("level-1") { router ->
            router.before { ctx -> ctx.result("1") }
            router.path("level-2") {
                it.path("level-3") { lowestRouter -> lowestRouter.get("/hello", updateAnswer("Hello")) }
                it.after(updateAnswer("2"))
            }
        }
        assertThat(http.getBody("/level-1/level-2/level-3/hello")).isEqualTo("1Hello2")
    }

    @Test
    fun `slashes can be omitted for both path-method and verbs`() = TestUtil.test { app, http ->
        app.path("level-1") {
            it.get { ctx -> ctx.result("level-1") }
            it.get("hello") { ctx -> ctx.result("Hello") }
        }
        assertThat(http.getBody("/level-1")).isEqualTo("level-1")
        assertThat(http.getBody("/level-1/hello")).isEqualTo("Hello")
    }

    private fun simpleAnswer(body: String) = Handler { ctx -> ctx.result(body) }
    private fun updateAnswer(body: String) = Handler { ctx -> ctx.result(ctx.resultString()!! + body) }

    @Test
    fun `CrudHandler works`() = TestUtil.test { app, http ->
        app.crud("users/:user-id", TestUserController())
                .path("/s") {
                    it.crud("/users/:user-id", TestUserController())
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
        app.crud("/foo/bar/users/:user-id", TestUserController())
                .path("/foo/baz") {
                    it.crud("/users/:user-id", TestUserController())
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
            app.crud("/foo/bar/:user-id/users", TestUserController())
        }.withMessageStartingWith("CrudHandler requires a path-parameter at the end of the provided path, e.g. '/users/:user-id'")
    }

    @Test
    fun `CrudHandler rejects missing resource`() = TestUtil.test { app, http ->
        assertThatExceptionOfType(IllegalArgumentException::class.java).isThrownBy {
            app.crud("/foo/bar/users", TestUserController())
        }.withMessageStartingWith("CrudHandler requires a path-parameter at the end of the provided path, e.g. '/users/:user-id'")
    }

    @Test
    fun `CrudHandler rejects missing resource base`() = TestUtil.test { app, http ->
        assertThatExceptionOfType(IllegalArgumentException::class.java).isThrownBy {
            app.crud("/:user-id", TestUserController())
        }.withMessageStartingWith("CrudHandler requires a path like '/resource/:resource-id'")
    }

    @Test
    fun `CrudHandler works with wildcards`() = TestUtil.test { app, http ->
        app.path("/s") {
            it.crud("/*/:user-id", TestUserController())
        }
                .crud("*/:user-id", TestUserController())
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

}

