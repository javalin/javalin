/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 *
 */

package io.javalin.translator.template;

import java.util.HashMap;
import java.util.Map;

public class TemplateUtil {
    public static Map<String, Object> model(Object... args) {
        if (args.length % 2 != 0) {
            throw new RuntimeException("Number of arguments must be even (key value pairs)");
        }
        Map<String, Object> model = new HashMap<>();
        for (int i = 0; i < args.length; i += 2) {
            if (args[i] instanceof String) {
                model.put((String) args[i], args[i + 1]);
            } else {
                throw new RuntimeException("Keys must be Strings");
            }
        }
        return model;
    }
}
