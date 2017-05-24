// Javalin - https://javalin.io
// Copyright 2017 David Åse
// Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE

package javalin.core.util;

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
