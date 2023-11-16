package io.javalin.routeoverview

import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder.get
import io.javalin.apibuilder.ApiBuilder.post
import io.javalin.plugin.bundled.RouteOverviewPlugin
import io.javalin.testing.HttpUtil
import io.javalin.testing.TestUtil
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.UUID

class TestRouteOverviewPlugin {

    private fun uuid() = UUID.randomUUID().toString()

    @Test
    fun `exposes routes added after server start`() = TestUtil.test(routeOverviewJavalin()) { app, http ->
        app.get(uuid()) { }
        app.post(uuid()) { }
        assertAllPathsPresent(app, http)
    }

    @Test
    fun `exposes routes added before server start`() = TestUtil.test(unstartedJavalinWithRoutes()) { app, http ->
        assertAllPathsPresent(app, http)
    }

    @Test
    fun `exposes routes added through router#apibuilder`() = TestUtil.test(Javalin.create { config ->
        config.registerPlugin(RouteOverviewPlugin { it.path = "/overview" })
        config.router.apiBuilder {
            get(uuid()) {}
            post(uuid()) {}
        }
    }) { app, http ->
        assertAllPathsPresent(app, http)
    }

    @Test
    fun `exposes routes added through router#mount`() = TestUtil.test(Javalin.create { config ->
        config.registerPlugin(RouteOverviewPlugin { it.path = "/overview" })
        config.router.mount {
            it.get(uuid()) {}
            it.post(uuid()) {}
        }
    }) { app, http ->
        assertAllPathsPresent(app, http)
    }

    private fun assertAllPathsPresent(app: Javalin, http: HttpUtil) {
        val router = app.unsafeConfig().pvt.internalRouter
        val allPaths = router.allHttpHandlers().map { it.path } + router.allWsHandlers().map { it.path }
        val overviewHtml = http.getBody("/overview")
        allPaths.forEach { assertThat(overviewHtml).contains(it) }
    }

    private fun routeOverviewJavalin(): Javalin = Javalin.create { config ->
        config.registerPlugin(RouteOverviewPlugin { it.path = "/overview" })
    }

    private fun unstartedJavalinWithRoutes() = routeOverviewJavalin().apply {
        this.get(uuid()) { }
        this.post(uuid()) { }
    }

}
