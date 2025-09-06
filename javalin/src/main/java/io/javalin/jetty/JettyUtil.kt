package io.javalin.jetty

import io.javalin.config.JavalinConfig
import io.javalin.http.servlet.ServletEntry
import io.javalin.util.Util
import org.eclipse.jetty.ee10.websocket.server.config.JettyWebSocketServletContainerInitializer

internal object JettyUtil {

    fun createJettyServletWithWebsocketsIfAvailable(cfg: JavalinConfig): ServletEntry? =
        when {
            Util.classExists("org.eclipse.jetty.ee10.websocket.server.JettyWebSocketServlet") ->
                ServletEntry(JettyWebSocketServletContainerInitializer(null), JavalinJettyServlet(cfg))
            else ->
                null
        }

}
