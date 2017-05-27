/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 *
 */

package io.javalin.translator.template;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.MustacheFactory;

public class Mustache {

    private static MustacheFactory mustacheFactory;

    public static void configure(MustacheFactory staticMustacheFactory) {
        mustacheFactory = staticMustacheFactory;
    }

    public static String render(String templatePath, Map<String, Object> model) {
        if (mustacheFactory == null) {
            mustacheFactory = new DefaultMustacheFactory("./");
        }
        try {
            StringWriter stringWriter = new StringWriter();
            mustacheFactory.compile(templatePath).execute(stringWriter, model).close();
            return stringWriter.toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
