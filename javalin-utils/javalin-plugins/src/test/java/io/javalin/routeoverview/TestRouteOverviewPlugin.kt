package io.javalin.routeoverview

import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder.get
import io.javalin.apibuilder.ApiBuilder.post
import io.javalin.plugin.RouteOverviewPlugin
import io.javalin.testtools.HttpClient
import io.javalin.testtools.JavalinTest
import io.javalin.testing.getBody
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.*

class TestRouteOverviewPlugin {

    private fun uuid() = UUID.randomUUID().toString()

    @Test
    fun `exposes routes added after server start`() {
        val path1 = uuid()
        val path2 = uuid()
        val app = Javalin.create { config ->
            config.registerPlugin(RouteOverviewPlugin { it.path = "/overview" })
            config.routes.get(path1) { }
            config.routes.post(path2) { }
        }
        JavalinTest.test(app) { _, http ->
            assertAllPathsPresent(app, http)
        }
    }

    @Test
    fun `exposes routes added before server start`() = JavalinTest.test(unstartedJavalinWithRoutes()) { app, http ->
        assertAllPathsPresent(app, http)
    }

    @Test
    fun `exposes routes added through router#apibuilder`() = JavalinTest.test(Javalin.create { config ->
        config.registerPlugin(RouteOverviewPlugin { it.path = "/overview" })
        config.routes.apiBuilder {
            get(uuid()) {}
            post(uuid()) {}
        }
    }) { app, http ->
        assertAllPathsPresent(app, http)
    }

    @Test
    fun `exposes routes added through routes config`() = JavalinTest.test(Javalin.create { config ->
        config.registerPlugin(RouteOverviewPlugin { it.path = "/overview" })
        config.routes.get(uuid()) {}
        config.routes.post(uuid()) {}
    }) { app, http ->
        assertAllPathsPresent(app, http)
    }

    private fun assertAllPathsPresent(app: Javalin, http: HttpClient) {
        val router = app.unsafe.pvt.internalRouter
        val allPaths = router.allHttpHandlers().map { it.endpoint.path } + router.allWsHandlers().map { it.path }
        val overviewHtml = http.getBody("/overview")
        allPaths.forEach { assertThat(overviewHtml).contains(it) }
    }

    private fun routeOverviewJavalin(): Javalin = Javalin.create { config ->
        config.registerPlugin(RouteOverviewPlugin { it.path = "/overview" })
    }

    private fun unstartedJavalinWithRoutes(): Javalin {
        return Javalin.create { config ->
            config.registerPlugin(RouteOverviewPlugin { it.path = "/overview" })
            config.routes.get(uuid()) { }
            config.routes.post(uuid()) { }
        }
    }

}
