/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 *
 */

package io.javalin;

import io.javalin.config.JavalinConfig;
import io.javalin.config.EventConfig;
import io.javalin.jetty.JettyServer;
import java.util.function.Consumer;
import jakarta.servlet.Servlet;
import kotlin.Lazy;
import org.jetbrains.annotations.NotNull;

import static io.javalin.util.Util.createLazy;

public class Javalin {

    /**
     * Do not use this field unless you know what you're doing.
     * Application config should be declared in {@link Javalin#create(Consumer)}.
     */
    private final JavalinConfig cfg;
    protected final Lazy<JettyServer> jettyServer;

    protected Javalin(JavalinConfig config) {
        this.cfg = config;
        this.jettyServer = createLazy(() -> new JettyServer(this.cfg));
    }

    @NotNull
    public JavalinConfig unsafeConfig() {
        return cfg;
    }

    public JettyServer jettyServer() {
        return jettyServer.getValue();
    }

    /**
     * Creates a new instance without any custom configuration.
     * The server does not run until {@link Javalin#start()} is called.
     *
     * @return application instance
     * @see Javalin#create(Consumer)
     */
    public static Javalin create() {
        return create(config -> {
        });
    }

    /**
     * Creates a new instance with the user provided configuration.
     * The server does not run until {@link Javalin#start()} is called.
     *
     * @return application instance
     * @see Javalin#start()
     * @see Javalin#start(int)
     */
    public static Javalin create(Consumer<JavalinConfig> config) {
        JavalinConfig cfg = new JavalinConfig();
        JavalinConfig.applyUserConfig(cfg, config); // mutates app.config and app (adds http-handlers)
        Javalin app = new Javalin(cfg);
        app.jettyServer.getValue(); // initialize server if no plugin already did
        return app;
    }

    /**
     * Creates a new instance with the user provided configuration and starts it immediately.
     *
     * @return running application instance
     * @see io.javalin.Javalin#create(java.util.function.Consumer)
     * @see Javalin#start()
     */
    public static Javalin createAndStart(Consumer<JavalinConfig> config) {
        return create(cfg -> {
            cfg.startupWatcherEnabled = false;
            config.accept(cfg);
        }).start();
    }

    // Get JavalinServlet (can be attached to other servlet containers)
    public Servlet javalinServlet() {
        return cfg.pvt.servlet.getValue().getServlet();
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
        jettyServer.getValue().start(null, port);
        return this;
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
        jettyServer.getValue().start(null, null);
        return this;
    }

    /**
     * Synchronously stops the application instance.
     *
     * @return stopped application instance.
     */
    public Javalin stop() {
        if (jettyServer.getValue().server().isStopping() || jettyServer.getValue().server().isStopped()) {
            return this;
        }
        jettyServer.getValue().stop();
        return this;
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


}
