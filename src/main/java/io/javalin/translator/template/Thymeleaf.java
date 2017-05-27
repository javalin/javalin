/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 *
 */

package io.javalin.translator.template;

import java.util.Map;

import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

public class Thymeleaf {

    private static org.thymeleaf.TemplateEngine templateEngine;

    public static void configure(TemplateEngine staticTemplateEngine) {
        templateEngine = staticTemplateEngine;
    }

    public static String render(String templatePath, Map<String, Object> model) {
        if (templateEngine == null) {
            templateEngine = new TemplateEngine();
            ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();
            templateResolver.setTemplateMode(TemplateMode.HTML);
            templateEngine.setTemplateResolver(templateResolver);
        }
        Context context = new Context();
        context.setVariables(model);
        return templateEngine.process(templatePath, context);
    }
}
