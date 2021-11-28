/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 *
 */

package io.javalin;

import org.eclipse.jetty.server.Server;
import org.junit.jupiter.api.Test;

import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

class TestClose_Java {

    @Test
    void tryWithResourcesStopsServer() {
        final Javalin app = Javalin.create();

        //noinspection EmptyTryBlock
        try (final Javalin ignored = app.start(0)) {
            // do nothing
        }

        final Server server = Objects.requireNonNull(app.jettyServer()).server();
        assertThat(server.isStopped()).isTrue();
    }

    @Test
    void tryWithResourcesCallsLifecycleEvents() {
        final StringBuilder log = new StringBuilder();
        final Javalin app = Javalin.create().events(event -> {
            event.serverStopping(() -> log.append("Stopping"));
            event.serverStopped(() -> log.append("Stopped"));
        });

        //noinspection EmptyTryBlock
        try (final Javalin ignored = app.start(0)) {
            // do nothing
        }

        assertThat(log.toString()).isEqualTo("StoppingStopped");
    }

    @Test
    void closingInsideTryWithResourcesIsIdempotent() {
        final StringBuilder log = new StringBuilder();
        final Javalin app = Javalin.create().events(event -> {
            event.serverStopping(() -> log.append("Stopping"));
            event.serverStopped(() -> log.append("Stopped"));
        });
        try (final Javalin startedApp = app.start(0)) {
            //noinspection RedundantExplicitClose
            startedApp.close();
        }

        final Server server = Objects.requireNonNull(app.jettyServer()).server();
        assertThat(server.isStopped()).isTrue();
        assertThat(log.toString()).isEqualTo("StoppingStopped");
    }
}
