/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 *
 */

package io.javalin.core.util;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Util {

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

}
