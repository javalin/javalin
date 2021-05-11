/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.core.util

enum class OptionalDependency(val displayName: String, val testClass: String, val groupId: String, val artifactId: String, val version: String) {
    JACKSON("Jackson", "com.fasterxml.jackson.databind.ObjectMapper", "com.fasterxml.jackson.core", "jackson-databind", "2.10.3"),
    JACKSON_KT("JacksonKt", "com.fasterxml.jackson.module.kotlin.KotlinModule", "com.fasterxml.jackson.module", "jackson-module-kotlin", "2.10.3"),
    VELOCITY("Velocity", "org.apache.velocity.app.VelocityEngine", "org.apache.velocity", "velocity-engine-core", "2.2"),
    FREEMARKER("Freemarker", "freemarker.template.Configuration", "org.freemarker", "freemarker", "2.3.30"),
    THYMELEAF("Thymeleaf", "org.thymeleaf.TemplateEngine", "org.thymeleaf", "thymeleaf", "3.0.12.RELEASE"),
    MUSTACHE("Mustache", "com.github.mustachejava.MustacheFactory", "com.github.spullara.mustache.java", "compiler", "0.9.7"),
    JTWIG("Jtwig", "org.jtwig.environment.EnvironmentConfiguration", "org.jtwig", "jtwig-core", "5.87.0.RELEASE"),
    PEBBLE("Pebble", "com.mitchellbosecke.pebble.PebbleEngine", "io.pebbletemplates", "pebble", "3.1.5"),
    COMMONMARK("Commonmark", "org.commonmark.renderer.html.HtmlRenderer", "org.commonmark", "commonmark", "0.17.1"),
    SLF4JSIMPLE("Slf4j simple", "org.slf4j.impl.StaticLoggerBinder", "org.slf4j", "slf4j-simple", "1.7.30"),
    SLF4J_PROVIDER_SIMPLE("Slf4j simple with Provider", "org.slf4j.simple.SimpleServiceProvider", "org.slf4j", "slf4j-simple", "1.8.0-beta4"),
    SLF4J_PROVIDER_API("Slf4j simple with Provider", "org.slf4j.spi.SLF4JServiceProvider", "org.slf4j", "slf4j-api", "1.8.0-beta4"),
    MICROMETER("Micrometer", "io.micrometer.core.instrument.Metrics", "io.micrometer", "micrometer-core", "1.6.3"),
    SWAGGERUI("Swagger UI", "STATIC-FILES", "org.webjars", "swagger-ui", "3.43.0"),
    SWAGGERPARSER("Swagger Parser", "io.swagger.v3.parser.OpenAPIV3Parser", "io.swagger.parser.v3", "swagger-parser", "2.0.24"),
    REDOC("ReDoc", "STATIC-FILES", "org.webjars.npm", "redoc", "2.0.0-rc.45"),
    SWAGGER_CORE("Swagger Core", "io.swagger.v3.oas.models.OpenAPI", "io.swagger.core.v3", "swagger-models", "2.1.6"),
    CLASS_GRAPH("ClassGraph", "io.github.classgraph.ClassGraph", "io.github.classgraph", "classgraph", "4.8.102"),
    JVMBROTLI("Jvm-Brotli", "com.nixxcode.jvmbrotli.common.BrotliLoader", "com.nixxcode.jvmbrotli", "jvmbrotli", "0.2.0"),
    JTE("jte", "gg.jte.TemplateEngine", "gg.jte", "jte", "1.10.0"),
    JTE_KOTLIN("jte-kotlin", "gg.jte.compiler.kotlin.KotlinClassCompiler", "gg.jte", "jte-kotlin", "1.10.0"),
}
