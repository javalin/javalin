/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.core.util

enum class OptionalDependency(val displayName: String, val testClass: String, val groupId: String, val artifactId: String, val version: String) {
    JACKSON("Jackson", "com.fasterxml.jackson.databind.ObjectMapper", "com.fasterxml.jackson.core", "jackson-databind", "2.9.6"),
    JACKSON_KT("JacksonKt", "com.fasterxml.jackson.module.kotlin.KotlinModule", "com.fasterxml.jackson.module", "jackson-module-kotlin", "2.9.4.1"),
    VELOCITY("Velocity", "org.apache.velocity.app.VelocityEngine", "org.apache.velocity", "velocity-engine-core", "2.0"),
    FREEMARKER("Freemarker", "freemarker.template.Configuration", "org.freemarker", "freemarker", "2.3.28"),
    THYMELEAF("Thymeleaf", "org.thymeleaf.TemplateEngine", "org.thymeleaf", "thymeleaf", "3.0.9.RELEASE"),
    MUSTACHE("Mustache", "com.github.mustachejava.MustacheFactory", "com.github.spullara.mustache.java", "compiler", "0.9.5"),
    JTWIG("Jtwig", "org.jtwig.environment.EnvironmentConfiguration", "org.jtwig", "jtwig-core", "5.87.0.RELEASE"),
    PEBBLE("Pebble", "com.mitchellbosecke.pebble.PebbleEngine", "com.mitchellbosecke", "pebble", "2.4.0"),
    COMMONMARK("Commonmark", "org.commonmark.renderer.html.HtmlRenderer", "com.atlassian.commonmark", "commonmark", "0.11.0"),
    SLF4JSIMPLE("Slf4j simple", "org.slf4j.impl.StaticLoggerBinder", "org.slf4j", "slf4j-simple", "1.7.25"),
    SWAGGERUI("Swagger UI", "STATIC-FILES", "org.webjars", "swagger-ui", "3.17.6"),
}

