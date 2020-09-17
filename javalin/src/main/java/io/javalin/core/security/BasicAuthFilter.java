/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */
package io.javalin.core.security;

import io.javalin.Javalin;
import io.javalin.core.plugin.Plugin;
import io.javalin.core.util.Header;
import io.javalin.http.UnauthorizedResponse;
import org.jetbrains.annotations.NotNull;

/**
 * Adds a filter that runs before every http request (does not apply to websocket upgrade requests)
 */
public class BasicAuthFilter implements Plugin {

    private final String username;
    private final String password;

    public BasicAuthFilter(String username, String password) {
        this.username = username;
        this.password = password;
    }

    @Override
    public void apply(@NotNull Javalin app) {
        app.before(ctx -> {
            try {
                final BasicAuthCredentials credentials = ctx.basicAuthCredentials();
                if (!credentials.getUsername().equals(username) || !credentials.getPassword().equals(password)) {
                    throw new RuntimeException("Incorrect username or password");
                }
            } catch (Exception e) { // badly formatted header OR incorrect credentials
                ctx.header(Header.WWW_AUTHENTICATE, "Basic");
                throw new UnauthorizedResponse();
            }
        });
    }

}
