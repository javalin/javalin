/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 *
 */

package io.javalin.examples;

import org.eclipse.jetty.util.ssl.SslContextFactory;

import io.javalin.Javalin;
import io.javalin.embeddedserver.EmbeddedServer;
import io.javalin.embeddedserver.jetty.EmbeddedJettyFactory;

public class HelloWorldSecure {

    // This is a very basic example, a better one can be found at:
    // https://github.com/eclipse/jetty.project/blob/jetty-9.4.x/examples/embedded/src/main/java/org/eclipse/jetty/embedded/LikeJettyXml.java#L139-L163
    public static void main(String[] args) {
        Javalin.create()
            .embeddedServer(new EmbeddedJettyFactory())
            .get("/", (req, res) -> res.body("Hello World")); // valid endpoint for both connectors
    }

    private static SslContextFactory getSslContextFactory() {
        SslContextFactory sslContextFactory = new SslContextFactory();
        sslContextFactory.setKeyStorePath(EmbeddedServer.class.getResource("/keystore.jks").toExternalForm());
        sslContextFactory.setKeyStorePassword("password");
        return sslContextFactory;
    }

}
