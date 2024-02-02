/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 *
 */

package io.javalin

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
import io.javalin.http.HttpStatus.CREATED
import io.javalin.http.HttpStatus.NO_CONTENT
import io.javalin.http.HttpStatus.OK
import io.javalin.testing.TestUtil
import io.javalin.testing.TestUtil.okHandler
import io.javalin.testing.httpCode
import kong.unirest.core.HttpMethod
import kong.unirest.core.Unirest
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.Test

class TestApiBuilder {

    @Test
    fun `ApiBuilder prefixes paths with slash`() = TestUtil.test(
        Javalin.create {
            it.router.apiBuilder {
                path("level-1") {
                    get("hello", simpleAnswer("Hello from level 1"))
                }
            }
        }
    ) { app, http ->
        assertThat(http.getBody("/level-1/hello")).isEqualTo("Hello from level 1")
    }

    @Test
    fun `pathless routes are handled properly`() = TestUtil.test(
        Javalin.create {
            it.router.apiBuilder {
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
        }
    ) { app, http ->
        val httpMethods = arrayOf(HttpMethod.GET, HttpMethod.POST, HttpMethod.PUT, HttpMethod.DELETE, HttpMethod.PATCH)
        for (httpMethod in httpMethods) {
            assertThat(http.call(httpMethod, "/api").httpCode()).isEqualTo(OK)
            assertThat(http.call(httpMethod, "/api/user").httpCode()).isEqualTo(OK)
        }
    }

    @Test
    fun `multiple nested path-method calls works`() = TestUtil.test(
        Javalin.create {
            it.router.apiBuilder {
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
        }
    ) { app, http ->
        assertThat(http.getBody("/hello")).isEqualTo("Hello from level 0")
        assertThat(http.getBody("/level-1/hello")).isEqualTo("Hello from level 1")
        assertThat(http.getBody("/level-1/level-2/hello")).isEqualTo("Hello from level 2")
        assertThat(http.getBody("/level-1/level-2/level-3/hello")).isEqualTo("Hello from level 3")
    }

    @Test
    fun `filters work as expected`() = TestUtil.test(
        Javalin.create {
            it.router.apiBuilder {
                path("level-1") {
                    before { it.result("1") }
                    path("level-2") {
                        path("level-3") { get("/hello", updateAnswer("Hello")) }
                        after(updateAnswer("2"))
                    }
                }
            }
        }
    ) { app, http ->
        assertThat(http.getBody("/level-1/level-2/level-3/hello")).isEqualTo("1Hello2")
    }

    @Test
    fun `slashes can be omitted for both path-method and verbs`() = TestUtil.test(
        Javalin.create {
            it.router.apiBuilder {
                path("level-1") {
                    get { it.result("level-1") }
                    get("hello") { it.result("Hello") }
                }
            }
        }
    ) { app, http ->
        assertThat(http.getBody("/level-1")).isEqualTo("level-1")
        assertThat(http.getBody("/level-1/hello")).isEqualTo("Hello")
    }

    @Test
    fun `pathless routes do not require a trailing slash`() = TestUtil.test(
        Javalin.create {
            it.router.apiBuilder {
                path("api") {
                    before { it.result("before") }
                    get(updateAnswer("get"))
                    after(updateAnswer("after"))
                }
            }
        }
    ) { app, http ->
        assertThat(http.getBody("/api")).isEqualTo("beforegetafter")
        assertThat(http.getBody("/api/")).isEqualTo("beforegetafter")
    }

    @Test
    fun `star routes do not require a trailing slash`() = TestUtil.test(
        Javalin.create {
            it.router.apiBuilder {
                path("api") {
                    before("*") { it.result("before") }
                    get(updateAnswer("get"))
                    after("*", updateAnswer("after"))
                }
            }
        }
    ) { app, http ->
        assertThat(http.getBody("/api")).isEqualTo("beforegetafter")
        assertThat(http.getBody("/api/")).isEqualTo("beforegetafter")
    }

    @Test
    fun `slash star routes do require a trailing slash`() = TestUtil.test(
        Javalin.create {
            it.router.apiBuilder {
                path("api") {
                    before("/*") { it.result("before") }
                    get { it.result((it.result() ?: "") + "get") }
                    after("/*", updateAnswer("after"))
                }
            }
        }
    ) { app, http ->
        assertThat(http.getBody("/api")).isEqualTo("get")
        assertThat(http.getBody("/api/")).isEqualTo("beforegetafter")
    }

    private fun simpleAnswer(body: String) = Handler { it.result(body) }
    private fun updateAnswer(body: String) = Handler { it.result(it.result()!! + body) }

    @Test
    fun `ApiBuilder throws if used outside of routes{} call`() = TestUtil.test { _, _ ->
        assertThatExceptionOfType(IllegalStateException::class.java)
            .isThrownBy { io.javalin.apibuilder.ApiBuilder.get("/") { it.result("") } }
            .withMessageStartingWith("The static API can only be used within a routes() call.")
    }

    @Test
    fun `ApiBuilder works with two services at once`() = TestUtil.runLogLess {
        val app = Javalin
            .create { cfg1 ->
                cfg1.router.apiBuilder {
                    get("/hello-1") { it.result("Hello-1") }
                }

                val app2 = Javalin
                    .create { cfg2 ->
                        cfg2.router.apiBuilder {
                            get("/hello-2") { it.result("Hello-2") }
                            get("/hello-3") { it.result("Hello-3") }
                        }
                    }
                    .start(0)
                    .also { app2 ->
                        assertThat(Unirest.get("http://localhost:${app2.port()}/hello-2").asString().body).isEqualTo("Hello-2")
                        assertThat(Unirest.get("http://localhost:${app2.port()}/hello-3").asString().body).isEqualTo("Hello-3")
                        app2.stop()
                    }

                cfg1.router.apiBuilder {
                    get("/hello-4") { it.result("Hello-4") }
                }
            }
            .start(0)
            .also { app1 ->
                assertThat(Unirest.get("http://localhost:${app1.port()}/hello-1").asString().body).isEqualTo("Hello-1")
                assertThat(Unirest.get("http://localhost:${app1.port()}/hello-4").asString().body).isEqualTo("Hello-4")
                app1.stop()
            }
    }

    @Test
    fun `CrudHandler works`() = TestUtil.test(
        Javalin.create {
            it.router.apiBuilder {
                crud("users/{user-id}", UserController())
                path("/s") {
                    crud("/users/{user-id}", UserController())
                }
            }
        }
    ) { app, http ->
        assertThat(Unirest.get(http.origin + "/users").asString().body).isEqualTo("All my users")
        assertThat(Unirest.post(http.origin + "/users").asString().httpCode()).isEqualTo(CREATED)
        assertThat(Unirest.get(http.origin + "/users/myUser").asString().body).isEqualTo("My single user: myUser")
        assertThat(Unirest.patch(http.origin + "/users/myUser").asString().httpCode()).isEqualTo(NO_CONTENT)
        assertThat(Unirest.delete(http.origin + "/users/myUser").asString().httpCode()).isEqualTo(NO_CONTENT)

        assertThat(Unirest.get(http.origin + "/s/users").asString().body).isEqualTo("All my users")
        assertThat(Unirest.post(http.origin + "/s/users").asString().httpCode()).isEqualTo(CREATED)
        assertThat(Unirest.get(http.origin + "/s/users/myUser").asString().body).isEqualTo("My single user: myUser")
        assertThat(Unirest.patch(http.origin + "/s/users/myUser").asString().httpCode()).isEqualTo(NO_CONTENT)
        assertThat(Unirest.delete(http.origin + "/s/users/myUser").asString().httpCode()).isEqualTo(NO_CONTENT)
    }

    @Test
    fun `CrudHandler works with long nested resources`() = TestUtil.test(
        Javalin.create {
            it.router.apiBuilder {
                crud("/foo/bar/users/{user-id}", UserController())
                path("/foo/baz") {
                    crud("/users/{user-id}", UserController())
                }
            }
        }
    ) { app, http ->
        assertThat(Unirest.get(http.origin + "/foo/bar/users").asString().body).isEqualTo("All my users")
        assertThat(Unirest.post(http.origin + "/foo/bar/users").asString().httpCode()).isEqualTo(CREATED)
        assertThat(Unirest.get(http.origin + "/foo/bar/users/myUser").asString().body).isEqualTo("My single user: myUser")
        assertThat(Unirest.patch(http.origin + "/foo/bar/users/myUser").asString().httpCode()).isEqualTo(NO_CONTENT)
        assertThat(Unirest.delete(http.origin + "/foo/bar/users/myUser").asString().httpCode()).isEqualTo(NO_CONTENT)

        assertThat(Unirest.get(http.origin + "/foo/baz/users").asString().body).isEqualTo("All my users")
        assertThat(Unirest.post(http.origin + "/foo/baz/users").asString().httpCode()).isEqualTo(CREATED)
        assertThat(Unirest.get(http.origin + "/foo/baz/users/myUser").asString().body).isEqualTo("My single user: myUser")
        assertThat(Unirest.patch(http.origin + "/foo/baz/users/myUser").asString().httpCode()).isEqualTo(NO_CONTENT)
        assertThat(Unirest.delete(http.origin + "/foo/baz/users/myUser").asString().httpCode()).isEqualTo(NO_CONTENT)
    }

    @Test
    fun `pathless CrudHandler works`() = TestUtil.test(
        Javalin.create {
            it.router.apiBuilder {
                path("/foo/bar/users/{user-id}") {
                    crud(UserController())
                }
            }
        }
    ) { app, http ->
        assertThat(Unirest.get(http.origin + "/foo/bar/users").asString().body).isEqualTo("All my users")
        assertThat(Unirest.post(http.origin + "/foo/bar/users").asString().httpCode()).isEqualTo(CREATED)
        assertThat(Unirest.get(http.origin + "/foo/bar/users/myUser").asString().body).isEqualTo("My single user: myUser")
        assertThat(Unirest.patch(http.origin + "/foo/bar/users/myUser").asString().httpCode()).isEqualTo(NO_CONTENT)
        assertThat(Unirest.delete(http.origin + "/foo/bar/users/myUser").asString().httpCode()).isEqualTo(NO_CONTENT)
    }

    @Test
    fun `CrudHandler rejects resource in the middle`() {
        assertThatExceptionOfType(IllegalArgumentException::class.java)
            .isThrownBy {
                Javalin.create {
                    it.router.apiBuilder {
                        crud("/foo/bar/{user-id}/users", UserController())
                    }
                }
            }
            .withMessageStartingWith("CrudHandler requires a path-parameter at the end of the provided path, e.g. '/users/{user-id}'")
    }

    @Test
    fun `CrudHandler rejects missing resource`() {
        assertThatExceptionOfType(IllegalArgumentException::class.java)
            .isThrownBy {
                Javalin.create {
                    it.router.apiBuilder {
                        crud("/foo/bar/users", UserController())
                    }
                }
            }
            .withMessageStartingWith("CrudHandler requires a path-parameter at the end of the provided path, e.g. '/users/{user-id}'")
    }

    @Test
    fun `CrudHandler rejects missing resource base`() = TestUtil.test { app, _ ->
        assertThatExceptionOfType(IllegalArgumentException::class.java)
            .isThrownBy {
                Javalin.create {
                    it.router.apiBuilder {
                        crud("/{user-id}", UserController())
                    }
                }
            }
            .withMessageStartingWith("CrudHandler requires a path like '/resource/{resource-id}'")
    }

    @Test
    fun `CrudHandler works with wildcards`() = TestUtil.test(
        Javalin.create {
            it.router.apiBuilder {
                path("/s") {
                    crud("/*/{user-id}", UserController())
                }
                crud("*/{user-id}", UserController())
            }
        }
    ) { app, http ->
        assertThat(Unirest.get(http.origin + "/users").asString().body).isEqualTo("All my users")
        assertThat(Unirest.post(http.origin + "/users").asString().httpCode()).isEqualTo(CREATED)
        assertThat(Unirest.get(http.origin + "/users/myUser").asString().body).isEqualTo("My single user: myUser")
        assertThat(Unirest.patch(http.origin + "/users/myUser").asString().httpCode()).isEqualTo(NO_CONTENT)
        assertThat(Unirest.delete(http.origin + "/users/myUser").asString().httpCode()).isEqualTo(NO_CONTENT)

        assertThat(Unirest.get(http.origin + "/s/users").asString().body).isEqualTo("All my users")
        assertThat(Unirest.post(http.origin + "/s/users").asString().httpCode()).isEqualTo(CREATED)
        assertThat(Unirest.get(http.origin + "/s/users/myUser").asString().body).isEqualTo("My single user: myUser")
        assertThat(Unirest.patch(http.origin + "/s/users/myUser").asString().httpCode()).isEqualTo(NO_CONTENT)
        assertThat(Unirest.delete(http.origin + "/s/users/myUser").asString().httpCode()).isEqualTo(NO_CONTENT)
    }

    class UserController : CrudHandler {

        override fun getAll(ctx: Context) {
            ctx.result("All my users")
        }

        override fun getOne(ctx: Context, resourceId: String) {
            ctx.result("My single user: $resourceId")
        }

        override fun create(ctx: Context) {
            ctx.status(CREATED)
        }

        override fun update(ctx: Context, resourceId: String) {
            ctx.status(NO_CONTENT)
        }

        override fun delete(ctx: Context, resourceId: String) {
            ctx.status(NO_CONTENT)
        }
    }

}
