package io.javalin;

import io.javalin.core.HandlerType;
import io.javalin.core.util.Util;
import io.javalin.embeddedserver.EmbeddedServerFactory;
import io.javalin.embeddedserver.Location;
import io.javalin.embeddedserver.jetty.websocket.WebSocketConfig;
import io.javalin.event.EventListener;
import io.javalin.event.EventType;
import io.javalin.security.AccessManager;
import io.javalin.security.Role;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@SuppressWarnings({"unused", "UnusedReturnValue"})
public interface Javalin {

    static Javalin create() {
        Util.INSTANCE.printHelpfulMessageIfNoServerHasBeenStartedAfterOneSecond();
        return new JavalinInstance();
    }

    static Javalin start(int port) {
        return create()
            .port(port)
            .start();
    }

    Javalin start();

    Javalin stop();

    Javalin embeddedServer(@NotNull EmbeddedServerFactory embeddedServerFactory);

    String contextPath();

    Javalin contextPath(@NotNull String contextPath);

    int port();

    Javalin port(int port);

    Javalin enableStaticFiles(@NotNull String classpathPath);

    Javalin enableStaticFiles(@NotNull String path, @NotNull Location location);

    Javalin enableStandardRequestLogging();

    Javalin requestLogLevel(@NotNull LogLevel logLevel);

    Javalin enableCorsForOrigin(@NotNull String... origin);

    Javalin enableCorsForAllOrigins();

    Javalin enableDynamicGzip();

    Javalin defaultContentType(String contentType);

    Javalin defaultCharacterEncoding(String characterEncoding);

    Javalin maxBodySizeForRequestCache(long value);

    Javalin disableRequestCache();

    Javalin dontIgnoreTrailingSlashes();

    default Javalin routes(@NotNull ApiBuilder.EndpointGroup endpointGroup) {
        synchronized(ApiBuilder.class) {
            ApiBuilder.setStaticJavalin(this);
            endpointGroup.addEndpoints();
            ApiBuilder.clearStaticJavalin();
        }
        return this;
    }

    // HTTP verbs
    Javalin get(@NotNull String path, @NotNull Handler handler);

    Javalin post(@NotNull String path, @NotNull Handler handler);

    Javalin put(@NotNull String path, @NotNull Handler handler);

    Javalin patch(@NotNull String path, @NotNull Handler handler);

    Javalin delete(@NotNull String path, @NotNull Handler handler);

    Javalin head(@NotNull String path, @NotNull Handler handler);

    Javalin trace(@NotNull String path, @NotNull Handler handler);

    Javalin connect(@NotNull String path, @NotNull Handler handler);

    Javalin options(@NotNull String path, @NotNull Handler handler);

    // Secured HTTP verbs
    Javalin accessManager(@NotNull AccessManager accessManager);

    Javalin get(@NotNull String path, @NotNull Handler handler, @NotNull List<Role> permittedRoles);

    Javalin post(@NotNull String path, @NotNull Handler handler, @NotNull List<Role> permittedRoles);

    Javalin put(@NotNull String path, @NotNull Handler handler, @NotNull List<Role> permittedRoles);

    Javalin patch(@NotNull String path, @NotNull Handler handler, @NotNull List<Role> permittedRoles);

    Javalin delete(@NotNull String path, @NotNull Handler handler, @NotNull List<Role> permittedRoles);

    Javalin head(@NotNull String path, @NotNull Handler handler, @NotNull List<Role> permittedRoles);

    Javalin trace(@NotNull String path, @NotNull Handler handler, @NotNull List<Role> permittedRoles);

    Javalin connect(@NotNull String path, @NotNull Handler handler, @NotNull List<Role> permittedRoles);

    Javalin options(@NotNull String path, @NotNull Handler handler, @NotNull List<Role> permittedRoles);

    // Filters
    Javalin before(@NotNull String path, @NotNull Handler handler);

    Javalin before(@NotNull Handler handler);

    Javalin after(@NotNull String path, @NotNull Handler handler);

    Javalin after(@NotNull Handler handler);

    // Reverse routing
    String pathFinder(@NotNull Handler handler);

    String pathFinder(@NotNull Handler handler, @NotNull HandlerType handlerType);

    <T extends Exception> Javalin exception(Class<T> exceptionClass, ExceptionHandler<? super T> exceptionHandler);

    Javalin error(int statusCode, ErrorHandler errorHandler);

    Javalin event(EventType eventType, EventListener eventListener);

    // WebSockets
    // Only available via Jetty, as there is no WebSocket interface in Java to build on top of

    Javalin ws(@NotNull String path, @NotNull WebSocketConfig ws);

    Javalin ws(@NotNull String path, @NotNull Class webSocketClass);

    Javalin ws(@NotNull String path, @NotNull Object webSocketObject);
}
