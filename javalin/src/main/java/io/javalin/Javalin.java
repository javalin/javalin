/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 *
 */

package io.javalin;

import io.javalin.router.InternalRouter;
import io.javalin.router.JavalinDefaultRoutingApi;
import io.javalin.config.JavalinConfig;
import io.javalin.config.EventConfig;
import io.javalin.http.Context;
import io.javalin.http.servlet.JavalinServlet;
import io.javalin.jetty.JavalinJettyServlet;
import io.javalin.jetty.JettyServer;
import io.javalin.util.Util;
import java.util.function.Consumer;
import kotlin.Lazy;
import org.jetbrains.annotations.NotNull;

import static io.javalin.util.Util.createLazy;

@SuppressWarnings("unchecked")
public class Javalin implements AutoCloseable, JavalinDefaultRoutingApi<Javalin> {

    /**
     * Do not use this field unless you know what you're doing.
     * Application config should be declared in {@link Javalin#create(Consumer)}.
     */
    private final JavalinConfig cfg;
    protected final JavalinServlet javalinServlet;
    protected final Lazy<JavalinJettyServlet> javalinJettyServlet;
    protected final Lazy<JettyServer> jettyServer;

    protected Javalin(JavalinConfig config) {
        this.cfg = config;
        this.javalinServlet = new JavalinServlet(cfg);
        this.javalinJettyServlet = createLazy(() -> new JavalinJettyServlet(cfg, javalinServlet));
        this.jettyServer = createLazy(() -> new JettyServer(this.cfg, javalinJettyServlet.getValue()));
    }

    @NotNull
    public JavalinConfig unsafeConfig() {
        return cfg;
    }

    @NotNull
    public InternalRouter internalRouter() {
        return cfg.pvt.internalRouter;
    }

    public JettyServer jettyServer() {
        return jettyServer.getValue();
    }

    /**
     * Creates a new instance without any custom configuration.
     *
     * @see Javalin#create(Consumer)
     */
    public static Javalin create() {
        return create(config -> {});
    }

    /**
     * Creates a new instance with the user provided configuration.
     * The server does not run until {@link Javalin#start()} is called.
     *
     * @return application instance.
     * @see Javalin#start()
     * @see Javalin#start(int)
     */
    public static Javalin create(Consumer<JavalinConfig> config) {
        JavalinConfig cfg = new JavalinConfig();
        Javalin app = new Javalin(cfg);
        JavalinConfig.applyUserConfig(app, app.cfg, config); // mutates app.config and app (adds http-handlers)
        app.jettyServer.getValue(); // initialize server if no plugin already did
        return app;
    }

    // Get JavalinServlet (can be attached to other servlet containers)
    public JavalinServlet javalinServlet() {
        return this.javalinServlet;
    }

    /**
     * Synchronously starts the application instance on the specified port
     * with the given host IP to bind to.
     *
     * @param host The host IP to bind to
     * @param port to run on
     * @return running application instance.
     * @see Javalin#create()
     * @see Javalin#start()
     */
    public Javalin start(String host, int port) {
        Util.printHelpfulMessageIfLoggerIsMissing();
        jettyServer.getValue().start(host, port);
        return this;
    }

    /**
     * Synchronously starts the application instance on the specified port.
     * Use port 0 to start the application instance on a random available port.
     *
     * @param port to run on
     * @return running application instance.
     * @see Javalin#create()
     * @see Javalin#start()
     */
    public Javalin start(int port) {
        return start(null, port);
    }

    /**
     * Synchronously starts the application instance on the configured port, or on
     * the configured ServerConnectors if the Jetty server has been manually configured.
     * If no port or connector is configured, the instance will start on port 8080.
     *
     * @return running application instance.
     * @see Javalin#create()
     */
    public Javalin start() {
        return start(null, -1);
    }

    /**
     * Synchronously stops the application instance.
     * Recommended to use {@link Javalin#close} instead with Java's try-with-resources
     * or Kotlin's {@code use}. This differs from {@link Javalin#close} by
     * firing lifecycle events even if the server is stopping or already stopped.
     * This could cause your listeners to observe nonsensical state transitions.
     * E.g. started -> stopping -> stopped -> stopping -> stopped.
     *
     * @return stopped application instance.
     * @see Javalin#close()
     */
    public Javalin stop() {
        jettyServer.getValue().stop();
        return this;
    }

    /**
     * Synchronously stops the application instance.
     * Can safely be called multiple times.
     */
    @Override
    public void close() {
        if (jettyServer.getValue().server().isStopping() || jettyServer.getValue().server().isStopped()) {
            return;
        }
        stop();
    }

    public Javalin events(Consumer<EventConfig> listener) {
        listener.accept(cfg.events);
        return this;
    }

    /**
     * Get which port instance is running on
     * Mostly useful if you start the instance with port(0) (random port)
     */
    public int port() {
        return jettyServer.getValue().port();
    }

    /**
     * Registers an attribute on the instance.
     * Instance is available on the {@link Context} through {@link Context#appAttribute}.
     * Ex: app.attribute(MyExt.class, myExtInstance())
     * The method must be called before {@link Javalin#start()}.
     */
    public Javalin attribute(String key, Object value) {
        cfg.pvt.appAttributes.put(key, value);
        return this;
    }

    /**
     * Retrieve an attribute stored on the instance.
     * Available on the {@link Context} through {@link Context#appAttribute}.
     * Ex: app.attribute(MyExt.class).myMethod()
     * Ex: ctx.appAttribute(MyExt.class).myMethod()
     */
    public <T> T attribute(String key) {
        return (T) cfg.pvt.appAttributes.get(key);
    }

}
