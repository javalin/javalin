package io.javalin.routeoverview

import com.fasterxml.jackson.databind.ObjectMapper
import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder.get
import io.javalin.apibuilder.ApiBuilder.post
import io.javalin.plugin.bundled.RouteOverviewPlugin
import io.javalin.security.RouteRole
import io.javalin.testing.HttpUtil
import io.javalin.testing.TestUtil
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.*

class TestRouteOverviewPlugin {

    private enum class Role : RouteRole { WS_ADMIN }

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
        TestUtil.test(app) { _, http ->
            assertAllPathsPresent(app, http)
        }
    }

    @Test
    fun `exposes routes added before server start`() = TestUtil.test(unstartedJavalinWithRoutes()) { app, http ->
        assertAllPathsPresent(app, http)
    }

    @Test
    fun `exposes routes added through router#apibuilder`() = TestUtil.test(Javalin.create { config ->
        config.registerPlugin(RouteOverviewPlugin { it.path = "/overview" })
        config.routes.apiBuilder {
            get(uuid()) {}
            post(uuid()) {}
        }
    }) { app, http ->
        assertAllPathsPresent(app, http)
    }

    @Test
    fun `exposes routes added through routes config`() = TestUtil.test(Javalin.create { config ->
        config.registerPlugin(RouteOverviewPlugin { it.path = "/overview" })
        config.routes.get(uuid()) {}
        config.routes.post(uuid()) {}
    }) { app, http ->
        assertAllPathsPresent(app, http)
    }

    @Test
    fun `html overview shows roles of websocket routes`() = TestUtil.test(javalinWithRoleProtectedWebsocket()) { _, http ->
        val html = http.htmlGet("/overview").body
        val socketRow = Regex("""<tr class="method WEBSOCKET">(.*?)</tr>""", RegexOption.DOT_MATCHES_ALL)
            .findAll(html).map { it.value }
            .single { it.contains("<td>/socket</td>") }
        assertThat(socketRow).contains("WS_ADMIN")
    }

    @Test
    fun `json overview shows roles of websocket routes`() = TestUtil.test(javalinWithRoleProtectedWebsocket()) { _, http ->
        val overview = ObjectMapper().readTree(http.jsonGet("/overview").body)
        val socket = overview["wsHandlers"].single { it["path"].asText() == "/socket" }
        assertThat(socket["handlerType"].asText()).isEqualTo("WEBSOCKET")
        assertThat(socket["roles"].toString()).contains("WS_ADMIN")
    }

    private fun assertAllPathsPresent(app: Javalin, http: HttpUtil) {
        val router = app.unsafe.internalRouter
        val allPaths = router.allHttpHandlers().map { it.endpoint.path } + router.allWsHandlers().map { it.path }
        val overviewHtml = http.getBody("/overview")
        allPaths.forEach { assertThat(overviewHtml).contains(it) }
    }

    private fun routeOverviewJavalin(): Javalin = Javalin.create { config ->
        config.registerPlugin(RouteOverviewPlugin { it.path = "/overview" })
    }

    private fun javalinWithRoleProtectedWebsocket() = Javalin.create { config ->
        config.registerPlugin(RouteOverviewPlugin { it.path = "/overview" })
        config.routes.ws("/socket", {}, Role.WS_ADMIN)
    }

    private fun unstartedJavalinWithRoutes(): Javalin {
        return Javalin.create { config ->
            config.registerPlugin(RouteOverviewPlugin { it.path = "/overview" })
            config.routes.get(uuid()) { }
            config.routes.post(uuid()) { }
        }
    }

}
