/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 *
 */

package io.javalin.translator.template;

import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;

public class Velocity {

    private static VelocityEngine velocityEngine;

    public static void configure(VelocityEngine staticVelocityEngine) {
        velocityEngine = staticVelocityEngine;
    }

    public static String render(String templatePath, Map<String, Object> model) {
        if (velocityEngine == null) {
            velocityEngine = new VelocityEngine();
            velocityEngine.setProperty("resource.loader", "class");
            velocityEngine.setProperty("class.resource.loader.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
        }
        StringWriter stringWriter = new StringWriter();
        velocityEngine.getTemplate(templatePath, StandardCharsets.UTF_8.name()).merge(
            new VelocityContext(model), stringWriter
        );
        return stringWriter.toString();
    }
}
