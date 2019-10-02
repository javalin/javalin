/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.core.util

enum class OptionalDependency(val displayName: String, val testClass: String, val groupId: String, val artifactId: String, val version: String) {
    JACKSON("Jackson", "com.fasterxml.jackson.databind.ObjectMapper", "com.fasterxml.jackson.core", "jackson-databind", "2.10.0.pr1"),
    JACKSON_KT("JacksonKt", "com.fasterxml.jackson.module.kotlin.KotlinModule", "com.fasterxml.jackson.module", "jackson-module-kotlin", "2.10.0.pr1"),
    VELOCITY("Velocity", "org.apache.velocity.app.VelocityEngine", "org.apache.velocity", "velocity-engine-core", "2.1"),
    FREEMARKER("Freemarker", "freemarker.template.Configuration", "org.freemarker", "freemarker", "2.3.28"),
    THYMELEAF("Thymeleaf", "org.thymeleaf.TemplateEngine", "org.thymeleaf", "thymeleaf", "3.0.9.RELEASE"),
    MUSTACHE("Mustache", "com.github.mustachejava.MustacheFactory", "com.github.spullara.mustache.java", "compiler", "0.9.5"),
    JTWIG("Jtwig", "org.jtwig.environment.EnvironmentConfiguration", "org.jtwig", "jtwig-core", "5.87.0.RELEASE"),
    PEBBLE("Pebble", "com.mitchellbosecke.pebble.PebbleEngine", "io.pebbletemplates", "pebble", "3.1.0"),
    COMMONMARK("Commonmark", "org.commonmark.renderer.html.HtmlRenderer", "com.atlassian.commonmark", "commonmark", "0.11.0"),
    SLF4JSIMPLE("Slf4j simple", "org.slf4j.impl.StaticLoggerBinder", "org.slf4j", "slf4j-simple", "1.7.26"),
    MICROMETER("Micrometer", "io.micrometer.core.instrument.Metrics", "io.micrometer", "micrometer-core", "1.1.3"),
    SWAGGERUI("Swagger UI", "STATIC-FILES", "org.webjars", "swagger-ui", "3.23.8"),
    SWAGGERPARSER("Swagger Parser", "io.swagger.v3.parser.OpenAPIV3Parser", "io.swagger.parser.v3", "swagger-parser", "2.0.14"),
    REDOC("ReDoc", "STATIC-FILES", "org.webjars.npm", "redoc", "2.0.0-rc.2"),
    SWAGGER_CORE("Swagger Core", "io.swagger.v3.oas.models.OpenAPI", "io.swagger.core.v3", "swagger-models", "2.0.9"),
    OPENAPI_KOTLIN_DSL("OpenAPI Kotlin DSL", "cc.vileda.openapi.dsl.OpenApiDslKt", "cc.vileda", "kotlin-openapi3-dsl", "0.20.2"),
    CLASS_GRAPH("ClassGraph", "io.github.classgraph.ClassGraph", "io.github.classgraph", "classgraph", "4.8.34"),
    JVMBROTLI("Jvm-Brotli", "com.nixxcode.jvmbrotli.common.BrotliLoader", "com.nixxcode.jvmbrotli", "jvmbrotli", "0.2.0"),
}
