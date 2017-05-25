// Javalin - https://javalin.io
// Copyright 2017 David Åse
// Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE

package javalin;

import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javalin.core.util.Util;

import com.fasterxml.jackson.databind.ObjectMapper;

public class ResponseMapper {

    private static Logger log = LoggerFactory.getLogger(ResponseMapper.class);

    public static void ensureDependencyPresent(String dependencyName, String className, String url) {
        if (!Util.classExists(className)) {
            String message = "Missing dependency '" + dependencyName + "'. Please add dependency: https://mvnrepository.com/artifact/" + url;
            log.warn(message);
            throw new HaltException(500, message);
        }
    }

    // TODO: Add GSON or other alternatives?
    static class Jackson {
        private static ObjectMapper objectMapper;

        static String toJson(Object object) {
            if (objectMapper == null) {
                objectMapper = new ObjectMapper();
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
        private static VelocityEngine velocityEngine;

        public static void setEngine(VelocityEngine staticVelocityEngine) {
            velocityEngine = staticVelocityEngine;
        }

        static String renderVelocityTemplate(String templatePath, Map<String, Object> model) {
            if (velocityEngine == null) {
                Properties properties = new Properties();
                properties.setProperty("resource.loader", "class");
                properties.setProperty("class.resource.loader.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
                velocityEngine = new org.apache.velocity.app.VelocityEngine(properties);
            }
            Template template = velocityEngine.getTemplate(templatePath, StandardCharsets.UTF_8.name());
            VelocityContext context = new VelocityContext(model);
            StringWriter writer = new StringWriter();
            template.merge(context, writer);
            return writer.toString();
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
