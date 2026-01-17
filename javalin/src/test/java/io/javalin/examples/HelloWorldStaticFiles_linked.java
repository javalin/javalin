/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.examples;

import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class HelloWorldStaticFiles_linked {

    public static void main(String[] args) {
        createSymLink("src/test/external/html.html", "src/test/external/linked_html.html");

        Javalin.create(javalin -> {
            javalin.staticFiles.add(staticFiles -> {
                staticFiles.directory = "src/test/external/";
                staticFiles.location = Location.EXTERNAL;
                staticFiles.aliasCheck = (path, realPath) -> true;
            });
        }).start(7070);
    }

    private static void createSymLink(String targetPath, String linkPath) {
        Path target = Paths.get(targetPath).toAbsolutePath();
        Path link = Paths.get(linkPath).toAbsolutePath();
        try {
            Files.createSymbolicLink(link, target);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
