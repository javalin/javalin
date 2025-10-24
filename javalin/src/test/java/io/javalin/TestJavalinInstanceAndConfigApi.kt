package io.javalin

import io.javalin.apibuilder.ApiBuilder.get
import io.javalin.compression.CompressionStrategy
import io.javalin.config.Key
import io.javalin.config.MultipartConfig
import io.javalin.json.JavalinJackson
import io.javalin.plugin.Plugin
import org.eclipse.jetty.server.ServerConnector
import org.eclipse.jetty.util.thread.QueuedThreadPool

class TestJavalinInstanceAndConfigApi {

    fun `test javalin instance and config api`() {

        val app = Javalin.create { app ->
            // Jetty
            app.jetty.multipartConfig = MultipartConfig()
            app.jetty.defaultHost = "localhost"
            app.jetty.defaultPort = 8080
            app.jetty.threadPool = QueuedThreadPool()
            app.jetty.addConnector { server, httpConfig -> ServerConnector(server) }
            app.jetty.modifyHttpConfiguration { httpConfig -> }
            app.jetty.modifyServer { server -> }
            app.jetty.modifyServletContextHandler { handler -> }
            app.jetty.modifyWebSocketServletFactory { factory -> }
            // Http and compression
            app.http.defaultContentType = "text/plain"
            app.http.asyncTimeout = 10_000L
            app.http.maxRequestSize = 10_000L
            app.http.generateEtags = true
            app.http.prefer405over404 = true
            app.http.strictContentTypes = true
            app.http.customCompression(CompressionStrategy())
            app.http.brotliAndGzipCompression(3)
            app.http.brotliOnlyCompression(3)
            app.http.gzipOnlyCompression(3)
            app.http.disableCompression()
            // Static files
            app.staticFiles.add("/public")
            app.staticFiles.enableWebjars()
            // Router
            app.router.contextPath = "/api"
            app.router.caseInsensitiveRoutes = true
            app.router.ignoreTrailingSlashes = true
            app.router.treatMultipleSlashesAsSingleSlash = true
            // Routes using new API
            app.routes.before("/hello") { ctx -> }
            app.routes.get("/hello") { ctx -> ctx.result("Hello, World!") }
            app.routes.post("/hello") { ctx -> ctx.result("Hello, World!") }
            app.routes.exception(Exception::class.java) { e, ctx -> }
            app.routes.error(404) { ctx -> ctx.result("Not found") }
            app.routes.after("/hello") { ctx -> }
            app.routes.sse("/sse") { client -> }
            app.routes.ws("/ws") { ws -> }
            app.routes.apiBuilder {
                get("/hello") { ctx -> ctx.result("Hello, World!") }
            }
            // Context resolver
            app.contextResolver.fullUrl = { "Test" }
            app.contextResolver.host = { "Test" }
            app.contextResolver.ip = { "Test" }
            app.contextResolver.url = { "Test" }
            app.contextResolver.scheme = { "Test" }
            // Bundled plugnis
            app.bundledPlugins.enableDevLogging()
            app.bundledPlugins.enableRouteOverview("/overview")
            app.bundledPlugins.enableSslRedirects()
            app.bundledPlugins // etc etc
            // Events
            app.events { event ->
                event.serverStarting { println("Server is starting") }
                event.serverStartFailed { println("Server start failed") }
                event.serverStarted { println("Server is started") }
                event.serverStopping { println("Server is stopping") }
                event.serverStopFailed { println("Server stop failed") }
                event.serverStopped { println("Server is stopped") }
                event.handlerAdded {}
                event.wsHandlerAdded {}
            }
            // Request logger
            app.requestLogger.http { ctx, ms -> }
            app.requestLogger.ws { ctx -> }
            // Validation
            app.validation.register(Any::class.java) { }
            // Spa root
            app.spaRoot.addFile("/", "index.html")
            app.spaRoot.addHandler("/") { ctx -> }
            // Other
            app.showJavalinBanner = false
            app.startupWatcherEnabled = false
            app.useVirtualThreads = true
            app.jsonMapper(JavalinJackson())
            app.appData(Key("Test"), "Test")
            app.fileRenderer { filePath, model, ctx -> "Test" }
            app.registerPlugin(object : Plugin<Any>() {})
        }

        // Events are now configured in the config block above (lines 81-90)
        // The events() method on Javalin instance has been removed as part of Javalin 7 API redesign
        app.start()
        app.javalinServlet()
        app.jettyServer()
        app.port()
        app.unsafe
        app.stop()

    }

}
