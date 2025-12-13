package io.javalin

import io.javalin.apibuilder.ApiBuilder.crud
import io.javalin.apibuilder.ApiBuilder.get
import io.javalin.config.JavalinConfig
import io.javalin.http.HttpStatus.UNAUTHORIZED
import io.javalin.http.staticfiles.Location
import io.javalin.security.RouteRole
import io.javalin.testing.TestUtil
import io.javalin.util.legacy.legacyAccessManager
import kong.unirest.Unirest
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalStateException
import org.junit.jupiter.api.Test

class TestLegacyAccessManager {

    enum class R : RouteRole { ROLE_ONE, ROLE_TWO, ROLE_THREE }

    private fun managedApp(cfg: ((JavalinConfig) -> Unit)? = null) = Javalin.create { config ->
        cfg?.invoke(config)
    }.legacyAccessManager { handler, ctx, routeRoles ->
        val role: RouteRole? = ctx.queryParam("role")?.let { R.valueOf(it) }

        when (role) {
            in routeRoles -> handler.handle(ctx)
            else -> ctx.status(UNAUTHORIZED).result(UNAUTHORIZED.message)
        }
    }

    @Test
    fun `AccessManager throws if app is started`() = TestUtil.test { app, http ->
        assertThatIllegalStateException()
            .isThrownBy { app.legacyAccessManager { _, _, _ -> } }
            .withMessage("AccessManager must be set before server start")
    }

    @Test
    fun `AccessManager does not run if roles are not present`() = TestUtil.test(
        Javalin.create().legacyAccessManager { _, _, _ ->
            throw RuntimeException()
        }) { app, http ->
        app.unsafe.routes.get("/unsecured") { it.result("Hello") }
        assertThat(http.getBody("/unsecured")).isEqualTo("Hello")
    }

    @Test
    fun `handler doesn't run if not explicitly called`() = TestUtil.test(
        Javalin.create().legacyAccessManager { _, _, _ -> }) { app, http ->
        app.unsafe.routes.get("/", { it.result("Hello") }, R.ROLE_ONE)
        assertThat(http.getBody("/")).isEqualTo("")
    }


    @Test
    fun `redirect works in access manager`() = TestUtil.test(
        Javalin.create().legacyAccessManager { _, ctx, _ ->
            ctx.redirect("/redirected")
        }) { app, http ->
        app.unsafe.routes.get("/", { it.result("Hello") }, R.ROLE_ONE)
        app.unsafe.routes.get("/redirected") { it.result("${it.result() ?: ""}Redirected") }
        assertThat(http.getBody("/")).isEqualTo("Redirected")
    }

    @Test
    fun `AccessManager can restrict access for instance`() = TestUtil.test(managedApp()) { app, http ->
        app.unsafe.routes.get("/secured", { it.result("Hello") }, R.ROLE_ONE, R.ROLE_TWO)
        assertThat(callWithRole(http.origin, "/secured", "ROLE_ONE")).isEqualTo("Hello")
        assertThat(callWithRole(http.origin, "/secured", "ROLE_TWO")).isEqualTo("Hello")
        assertThat(callWithRole(http.origin, "/secured", "ROLE_THREE")).isEqualTo(UNAUTHORIZED.message)
    }

    @Test
    fun `AccessManager can restrict access for ApiBuilder`() = TestUtil.test(managedApp { cfg ->
        cfg.routes.apiBuilder {
            get("/static-secured", { it.result("Hello") }, R.ROLE_ONE, R.ROLE_TWO)
        }
    }) { app, http ->
        assertThat(callWithRole(http.origin, "/static-secured", "ROLE_ONE")).isEqualTo("Hello")
        assertThat(callWithRole(http.origin, "/static-secured", "ROLE_TWO")).isEqualTo("Hello")
        assertThat(callWithRole(http.origin, "/static-secured", "ROLE_THREE")).isEqualTo(UNAUTHORIZED.message)
    }

    @Test
    fun `AccessManager can restrict access for ApiBuilder crud`() = TestUtil.test(managedApp { cfg ->
        cfg.routes.apiBuilder {
            crud("/users/{userId}", TestApiBuilder.UserController(), R.ROLE_ONE, R.ROLE_TWO)
        }
    }) { app, http ->
        assertThat(callWithRole(http.origin, "/users/1", "ROLE_ONE")).isEqualTo("My single user: 1")
        assertThat(callWithRole(http.origin, "/users/2", "ROLE_TWO")).isEqualTo("My single user: 2")
        assertThat(callWithRole(http.origin, "/users/3", "ROLE_THREE")).isEqualTo(UNAUTHORIZED.message)
    }

    @Test
    fun `AccessManager supports path params`() = TestUtil.test(Javalin.create {}.legacyAccessManager { _, ctx, _ ->
        ctx.result(ctx.pathParam("userId"));
    }) { app, http ->
        app.unsafe.routes.get("/user/{userId}", {}, R.ROLE_ONE)
        assertThat(http.get("/user/123").body).isEqualTo("123")
    }

    @Test
    fun `AccessManager does not affect static files`() = TestUtil.test(managedApp()) { app, http ->
        app.unsafe.staticFiles.add("/public", Location.CLASSPATH)
        assertThat(http.get("/styles.css").body).contains("CSS works")
    }

    private fun callWithRole(origin: String, path: String, role: String) =
        Unirest.get(origin + path).queryString("role", role).asString().body

}
