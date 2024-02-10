package io.javalin;

import io.javalin.apibuilder.ApiBuilder;
import io.javalin.compression.CompressionStrategy;
import io.javalin.config.Key;
import io.javalin.config.MultipartConfig;
import io.javalin.json.JavalinJackson;
import io.javalin.plugin.Plugin;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

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
            app.router.mount(router -> {
                router.before("/hello", ctx -> {});
                router.get("/hello", ctx -> ctx.result("Hello, World!"));
                router.post("/hello", ctx -> ctx.result("Hello, World!"));
                router.exception(Exception.class, (e, ctx) -> {});
                router.error(404, ctx -> ctx.result("Not found"));
                router.after("/hello", ctx -> {});
                router.sse("/sse", client -> {});
                router.ws("/ws", ws -> {});
            });
            app.router.apiBuilder(() -> {
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
            app.requestLogger.ws(ctx -> {});
            // Validation
            app.validation.register(Object.class, o -> new Object());
            // Vue
            app.vue.cacheControl = "Test";
            app.vue.enableCspAndNonces = true;
            app.vue.vueInstanceNameInJs = "Test";
            app.vue.isDevFunction = ctx -> true;
            app.vue.optimizeDependencies = true;
            app.vue.stateFunction = ctx -> "Test";
            app.vue.rootDirectory("Test");
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

        javalin.events(event -> {
            event.serverStarting(() -> System.out.println("Server is starting"));
            event.serverStartFailed(() -> System.out.println("Server start failed"));
            event.serverStarted(() -> System.out.println("Server is started"));
            event.serverStopping(() -> System.out.println("Server is stopping"));
            event.serverStopFailed(() -> System.out.println("Server stop failed"));
            event.serverStopped(() -> System.out.println("Server is stopped"));
            event.handlerAdded(handlerMetaInfo -> {});
            event.wsHandlerAdded(wsHandlerMetaInfo -> {});
        });
        javalin.before("/hello", ctx -> {});
        javalin.get("/hello", ctx -> ctx.result("Hello, World!"));
        javalin.post("/hello", ctx -> ctx.result("Hello, World!"));
        javalin.exception(Exception.class, (e, ctx) -> {});
        javalin.error(404, ctx -> ctx.result("Not found"));
        javalin.after("/hello", ctx -> {});
        javalin.sse("/sse", client -> {});
        javalin.ws("/ws", ws -> {});
        javalin.start();
        javalin.javalinServlet();
        javalin.jettyServer();
        javalin.port();
        javalin.unsafeConfig();
        javalin.stop();

    }

}
