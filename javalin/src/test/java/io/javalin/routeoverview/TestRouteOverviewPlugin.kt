package io.javalin.routeoverview

import io.javalin.Javalin
import io.javalin.http.HandlerType
import io.javalin.plugin.bundled.RouteOverviewPlugin.Companion.RouteOverview
import io.javalin.testing.TestUtil
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class TestRouteOverviewPlugin {

    @Test
    fun `should properly register and expose route overview`() = TestUtil.test(
        Javalin.create { config ->
            config.registerPlugin(RouteOverview) { it.path = "/overview" }
        }
    ) { app, http ->
        VisualTest.setupJavalinRoutes(app)

        val allPaths = HandlerType.values()
            .flatMap { app.cfg.pvt.pathMatcher.getAllEntriesOfType(it).map { entry -> entry.path } }

        assertThat(allPaths).isNotEmpty
        assertThat(http.getBody("/overview")).contains(allPaths)
    }

}
