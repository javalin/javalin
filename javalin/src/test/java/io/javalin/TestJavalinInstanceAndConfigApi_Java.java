package io.javalin;

import io.javalin.apibuilder.ApiBuilder;
import io.javalin.compression.CompressionStrategy;
import io.javalin.config.Key;
import io.javalin.config.MultipartConfig;
import io.javalin.json.JavalinJackson;
import io.javalin.plugin.Plugin;
import io.javalin.websocket.WsConfig;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

import java.util.function.Consumer;

import static io.javalin.testing.JavalinTestUtil.after;
import static io.javalin.testing.JavalinTestUtil.before;
import static io.javalin.testing.JavalinTestUtil.error;
import static io.javalin.testing.JavalinTestUtil.exception;
import static io.javalin.testing.JavalinTestUtil.get;
import static io.javalin.testing.JavalinTestUtil.post;
import static io.javalin.testing.JavalinTestUtil.sse;
import static io.javalin.testing.JavalinTestUtil.ws;

public class TestJavalinInstanceAndConfigApi_Java {

    public void testJavalinInstanceAndConfigApi() {

        Javalin javalin = Javalin.create(app -> {
            // Jetty
            app.jetty.multipartConfig = new MultipartConfig();
            app.jetty.defaultHost = "localhost";
            app.jetty.defaultPort = 8080;
            app.jetty.threadPool = new QueuedThreadPool();
            app.jetty.addConnector((server, config) -> new ServerConnector(server));
            app.jetty.modifyHttpConfiguration(httpConfig -> {});
            app.jetty.modifyServer(server -> {});
            app.jetty.modifyServletContextHandler(handler -> {});
            app.jetty.modifyWebSocketServletFactory(factory -> {});
            // Http and compression
            app.http.defaultContentType = "text/plain";
            app.http.asyncTimeout = 10_000L;
            app.http.maxRequestSize = 10_000L;
            app.http.generateEtags = true;
            app.http.prefer405over404 = true;
            app.http.strictContentTypes = true;
            app.http.customCompression(new CompressionStrategy());
            app.http.brotliAndGzipCompression(3);
            app.http.brotliOnlyCompression(3);
            app.http.gzipOnlyCompression(3);
            app.http.disableCompression();
            // Static files
            app.staticFiles.add("/public");
            app.staticFiles.enableWebjars();
            // Router
            app.router.contextPath = "/api";
            app.router.caseInsensitiveRoutes = true;
            app.router.ignoreTrailingSlashes = true;
            app.router.treatMultipleSlashesAsSingleSlash = true;
            app.routes.before("/hello", ctx -> {});
            app.routes.get("/hello", ctx -> ctx.result("Hello, World!"));
            app.routes.post("/hello", ctx -> ctx.result("Hello, World!"));
            app.routes.exception(Exception.class, (e, ctx) -> {});
            app.routes.error(404, ctx -> ctx.result("Not found"));
            app.routes.after("/hello", ctx -> {});
            app.routes.sse("/sse", client -> {});
            app.routes.ws("/ws", ws -> {});
            app.routes.apiBuilder(() -> {
                ApiBuilder.get("/hello", ctx -> ctx.result("Hello, World!"));
            });
            // Context resolver
            app.contextResolver.fullUrl = ctx -> "Test";
            app.contextResolver.host = ctx -> "Test";
            app.contextResolver.ip = ctx -> "Test";
            app.contextResolver.url = ctx -> "Test";
            app.contextResolver.scheme = ctx -> "Test";
            // Bundled plugins
            app.bundledPlugins.enableDevLogging();
            app.bundledPlugins.enableRouteOverview("/overview");
            app.bundledPlugins.enableSslRedirects();
            // Events
            app.events(event -> {
                event.serverStarting(() -> System.out.println("Server is starting"));
                event.serverStartFailed(() -> System.out.println("Server start failed"));
                event.serverStarted(() -> System.out.println("Server is started"));
                event.serverStopping(() -> System.out.println("Server is stopping"));
                event.serverStopFailed(() -> System.out.println("Server stop failed"));
                event.serverStopped(() -> System.out.println("Server is stopped"));
                event.handlerAdded(handlerMetaInfo -> {});
                event.wsHandlerAdded(wsHandlerMetaInfo -> {});
            });
            // Request logger
            app.requestLogger.http((ctx, ms) -> {});
            Consumer<WsConfig> requestLogger = ws -> {};
            app.routes.ws("/path", requestLogger);
            // Validation
            app.validation.register(Object.class, o -> new Object());
            // Spa root
            app.spaRoot.addFile("/", "index.html");
            app.spaRoot.addHandler("/", ctx -> {});
            // Other
            app.showJavalinBanner = false;
            app.startupWatcherEnabled = false;
            app.useVirtualThreads = true;
            app.jsonMapper(new JavalinJackson());
            app.appData(new Key<>("Test"), "Test");
            app.fileRenderer((filePath, model, ctx) -> "Test");
            app.registerPlugin(new Plugin<>() {});
        });

        // Events are now configured in the config block above (lines 81-90)
        // The events() method on Javalin instance has been removed as part of Javalin 7 API redesign
        before(javalin, "/hello", ctx -> {});
        get(javalin, "/hello", ctx -> ctx.result("Hello, World!"));
        post(javalin, "/hello", ctx -> ctx.result("Hello, World!"));
        exception(javalin, Exception.class, (e, ctx) -> {});
        error(javalin, 404, ctx -> ctx.result("Not found"));
        after(javalin, "/hello", ctx -> {});
        sse(javalin, "/sse", client -> {});
        ws(javalin, "/ws", ws -> {});
        javalin.start();
        javalin.javalinServlet();
        javalin.jettyServer();
        javalin.port();
        javalin.stop();
    }

}
