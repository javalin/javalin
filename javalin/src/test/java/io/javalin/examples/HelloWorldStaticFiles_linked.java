/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.examples;

import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;
import org.eclipse.jetty.server.handler.ContextHandler;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;

public class HelloWorldStaticFiles_linked {

    public static void main(String[] args) {
        createSimLink("src/test/external/html.html", "src/test/external/linked_html.html");

        Javalin.create(config -> {
            config.addStaticFiles("src/test/external/", Location.EXTERNAL, Collections.singletonList(new ContextHandler.ApproveAliases()));
        }).start(7070);
    }

    private static void createSimLink(String targetPath, String linkPath) {
        Path target = Paths.get(targetPath).toAbsolutePath();
        Path link = Paths.get(linkPath).toAbsolutePath();
        try {
            Files.createSymbolicLink(link, target);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
