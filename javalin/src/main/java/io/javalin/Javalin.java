/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 *
 */

package io.javalin;

import io.javalin.config.JavalinConfig;
import io.javalin.config.JavalinState;
import io.javalin.jetty.JettyServer;
import jakarta.servlet.Servlet;
import kotlin.Lazy;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

import static io.javalin.util.Util.createLazy;

public class Javalin {

    private final JavalinState state;
    protected final Lazy<JettyServer> jettyServer;

    protected Javalin(JavalinState state) {
        this.state = state;
        this.jettyServer = createLazy(() -> new JettyServer(this.state));
        unsafe = this.state;
    }

    /**
     * Advanced/unsafe API providing access to internal Javalin configuration.
     * This exposes powerful but potentially dangerous APIs. Use with caution.
     */
    @NotNull
    public JavalinState unsafe;

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
     * @param config configuration consumer accepting {@link JavalinConfig}
     * @return application instance
     * @see Javalin#start()
     * @see Javalin#start(int)
     */
    public static Javalin create(Consumer<JavalinConfig> config) {
        JavalinState state = new JavalinState();
        JavalinState.applyUserConfig(state, config); // mutates app.config and app (adds http-handlers)
        Javalin app = new Javalin(state);
        app.jettyServer.getValue(); // initialize server if no plugin already did
        return app;
    }

    /**
     * Creates and starts a new instance with the user provided configuration.
     *
     * @param config configuration consumer accepting {@link JavalinConfig}
     * @return running application instance
     */
    public static Javalin start(Consumer<JavalinConfig> config) {
        return create(config).start();
    }

    // Get JavalinServlet (can be attached to other servlet containers)
    public Servlet javalinServlet() {
        return state.servlet.getValue().getServlet();
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
        state.jetty.host = host;
        state.jetty.port = port;
        jettyServer.getValue().start();
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
        state.jetty.port = port;
        jettyServer.getValue().start();
        return this;
    }

    /**
     * Synchronously starts the application instance on the configured port, or on
     * the configured ServerConnectors if the Jetty server has been manually
     * configured.
     * If no port or connector is configured, the instance will start on port 8080.
     *
     * @return running application instance.
     * @see Javalin#create()
     */
    public Javalin start() {
        jettyServer.getValue().start();
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

    /**
     * Get which port instance is running on
     * Mostly useful if you start the instance with port(0) (random port)
     */
    public int port() {
        return jettyServer.getValue().port();
    }

}
