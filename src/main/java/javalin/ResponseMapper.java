// Javalin - https://javalin.io
// Copyright 2017 David Åse
// Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE

package javalin;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javalin.core.util.Util;

public class ResponseMapper {

    private static Logger log = LoggerFactory.getLogger(ResponseMapper.class);

    private static Map<String, Boolean> dependencyCheckCache = new HashMap<>();

    public static void ensureDependencyPresent(String dependencyName, String className, String url) {
        if (dependencyCheckCache.getOrDefault(className, false)) {
            return;
        }
        if (!Util.classExists(className)) {
            String message = "Missing dependency '" + dependencyName + "'. Please add dependency: https://mvnrepository.com/artifact/" + url;
            log.warn(message);
            throw new HaltException(500, message);
        }
        dependencyCheckCache.put(className, true);
    }

    // TODO: Add GSON or other alternatives?
    static class Jackson {
        private static com.fasterxml.jackson.databind.ObjectMapper objectMapper;

        static String toJson(Object object) {
            if (objectMapper == null) {
                objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
            }
            try {
                return objectMapper.writeValueAsString(object);
            } catch (Throwable t) {
                String message = "Failed to write object as JSON";
                log.warn(message, t);
                throw new HaltException(500, message);
            }
        }
    }

    static class Velocity {
        private static org.apache.velocity.app.VelocityEngine velocityEngine;

        public static void configure(org.apache.velocity.app.VelocityEngine staticVelocityEngine) {
            velocityEngine = staticVelocityEngine;
        }

        static String render(String templatePath, Map<String, Object> model) {
            if (velocityEngine == null) {
                velocityEngine = new org.apache.velocity.app.VelocityEngine();
                velocityEngine.setProperty("resource.loader", "class");
                velocityEngine.setProperty("class.resource.loader.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
            }
            org.apache.velocity.Template template = velocityEngine.getTemplate(templatePath, StandardCharsets.UTF_8.name());
            org.apache.velocity.VelocityContext context = new org.apache.velocity.VelocityContext(model);
            StringWriter writer = new StringWriter();
            template.merge(context, writer);
            return writer.toString();
        }
    }

    static class Freemarker {
        private static freemarker.template.Configuration configuration;

        public static void configure(freemarker.template.Configuration staticConfiguration) {
            configuration = staticConfiguration;
        }

        static String render(String templatePath, Map<String, Object> model) {
            if (configuration == null) {
                configuration = new freemarker.template.Configuration(new freemarker.template.Version(2, 3, 26));
                configuration.setClassForTemplateLoading(Freemarker.class, "/");
            }
            try {
                StringWriter stringWriter = new StringWriter();
                freemarker.template.Template template = configuration.getTemplate(templatePath);
                template.process(model, stringWriter);
                return stringWriter.toString();
            } catch (IOException | freemarker.template.TemplateException e) {
                throw new RuntimeException(e);
            }
        }
    }

    static class Thymeleaf {
        private static org.thymeleaf.TemplateEngine templateEngine;

        public static void configure(org.thymeleaf.TemplateEngine staticTemplateEngine) {
            templateEngine = staticTemplateEngine;
        }

        static String render(String templatePath, Map<String, Object> model) {
            if (templateEngine == null) {
                templateEngine = new org.thymeleaf.TemplateEngine();
                org.thymeleaf.templateresolver.ClassLoaderTemplateResolver templateResolver = new org.thymeleaf.templateresolver.ClassLoaderTemplateResolver();
                templateResolver.setTemplateMode(org.thymeleaf.templatemode.TemplateMode.HTML);
                templateEngine.setTemplateResolver(templateResolver);
            }
            org.thymeleaf.context.Context context = new org.thymeleaf.context.Context();
            context.setVariables(model);
            return templateEngine.process(templatePath, context);
        }
    }

    static class Mustache {
        private static com.github.mustachejava.MustacheFactory mustacheFactory;

        public static void configure(com.github.mustachejava.MustacheFactory staticMustacheFactory) {
            mustacheFactory = staticMustacheFactory;
        }

        static String render(String templatePath, Map<String, Object> model) {
            if (mustacheFactory == null) {
                mustacheFactory = new com.github.mustachejava.DefaultMustacheFactory("./");
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

    static class TemplateUtil {
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

}
