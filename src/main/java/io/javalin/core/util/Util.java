/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.core.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.javalin.HaltException;

public class Util {

    private static Logger log = LoggerFactory.getLogger(Util.class);

    private Util() {
    }

    public static void notNull(Object object, String message) {
        if (object == null) {
            throw new IllegalArgumentException(message);
        }
    }

    public static boolean classExists(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private static Map<String, Boolean> dependencyCheckCache = new HashMap<>();

    public static void ensureDependencyPresent(String dependencyName, String className, String url) {
        if (dependencyCheckCache.getOrDefault(className, false)) {
            return;
        }
        if (!classExists(className)) {
            String message = "Missing dependency '" + dependencyName + "'. Please add dependency: https://mvnrepository.com/artifact/" + url;
            log.warn(message);
            throw new HaltException(500, message);
        }
        dependencyCheckCache.put(className, true);
    }

    public static List<String> pathToList(String pathString) {
        return Stream.of(pathString.split("/"))
            .filter(p -> p.length() > 0)
            .collect(Collectors.toList());
    }

    public static void printHelpfulMessageIfLoggerIsMissing() {
        if (!classExists("org.slf4j.impl.StaticLoggerBinder")) {
            String message = ""
                + "-------------------------------------------------------------------\n"
                + "Javalin: In the Java world, it's common to add your own logger.\n"
                + "Javalin: To easily fix the warning above, get the latest version of slf4j-simple:\n"
                + "Javalin: https://mvnrepository.com/artifact/org.slf4j/slf4j-simple\n"
                + "Javalin: then add it to your dependencies (pom.xml or build.gradle)\n"
                + "Javalin: Visit https://javalin.io/documentation#logging if you need more help\n";
            System.err.println(message);
        }
    }

    public static String javalinBanner() {
        return " _________________________________________\n"
            + "|        _                  _ _           |\n"
            + "|       | | __ ___   ____ _| (_)_ __      |\n"
            + "|    _  | |/ _` \\ \\ / / _` | | | '_ \\     |\n"
            + "|   | |_| | (_| |\\ V / (_| | | | | | |    |\n"
            + "|    \\___/ \\__,_| \\_/ \\__,_|_|_|_| |_|    |\n"
            + "|_________________________________________|\n"
            + "|                                         |\n"
            + "|    https://javalin.io/documentation     |\n"
            + "|_________________________________________|\n";
    }

    public static long copyStream(InputStream in, OutputStream out) throws IOException {
        byte[] buf = new byte[8192];
        long totalBytesCopied = 0;
        int bytesReadThisPass = 0;

        while ((bytesReadThisPass = in.read(buf)) > 0) {
            out.write(buf, 0, bytesReadThisPass);
            totalBytesCopied += bytesReadThisPass;
        }

        return totalBytesCopied;
    }
}
