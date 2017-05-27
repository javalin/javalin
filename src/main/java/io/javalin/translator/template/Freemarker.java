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

import freemarker.template.Configuration;
import freemarker.template.TemplateException;
import freemarker.template.Version;

public class Freemarker {
    private static freemarker.template.Configuration configuration;

    public static void configure(Configuration staticConfiguration) {
        configuration = staticConfiguration;
    }

    public static String render(String templatePath, Map<String, Object> model) {
        if (configuration == null) {
            configuration = new freemarker.template.Configuration(new Version(2, 3, 26));
            configuration.setClassForTemplateLoading(Freemarker.class, "/");
        }
        try {
            StringWriter stringWriter = new StringWriter();
            configuration.getTemplate(templatePath).process(model, stringWriter);
            return stringWriter.toString();
        } catch (IOException | TemplateException e) {
            throw new RuntimeException(e);
        }
    }
}
