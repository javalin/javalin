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
            app.http.strictFormContentTypes = true
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
            app.router.mount { router ->
                router.before("/hello") { ctx -> }
                router.get("/hello") { ctx -> ctx.result("Hello, World!") }
                router.post("/hello") { ctx -> ctx.result("Hello, World!") }
                router.exception(Exception::class.java) { e, ctx -> }
                router.error(404) { ctx -> ctx.result("Not found") }
                router.after("/hello") { ctx -> }
                router.sse("/sse") { client -> }
                router.ws("/ws") { ws -> }
            }
            app.router.apiBuilder {
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
            // Vue
            app.vue.cacheControl = "Test"
            app.vue.enableCspAndNonces = true
            app.vue.vueInstanceNameInJs = "Test"
            app.vue.isDevFunction = { true }
            app.vue.optimizeDependencies = true
            app.vue.stateFunction = { "Test" }
            app.vue.rootDirectory("Test")
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
        app.before("/hello") { ctx -> }
        app.get("/hello") { ctx -> ctx.result("Hello, World!") }
        app.post("/hello") { ctx -> ctx.result("Hello, World!") }
        app.exception(Exception::class.java) { e, ctx -> }
        app.error(404) { ctx -> ctx.result("Not found") }
        app.after("/hello") { ctx -> }
        app.sse("/sse") { client -> }
        app.ws("/ws") { ws -> }
        app.start()
        app.javalinServlet()
        app.jettyServer()
        app.port()
        app.unsafeConfig()
        app.stop()

    }

}
