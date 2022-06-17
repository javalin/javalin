/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.core.util

enum class OptionalDependency(val displayName: String, val testClass: String, val groupId: String, val artifactId: String, val version: String) {

    // JSON handling
    JACKSON("Jackson", "com.fasterxml.jackson.databind.ObjectMapper", "com.fasterxml.jackson.core", "jackson-databind", "2.13.3"),
    JACKSON_KT("JacksonKt", "com.fasterxml.jackson.module.kotlin.KotlinModule", "com.fasterxml.jackson.module", "jackson-module-kotlin", "2.13.3"),
    JACKSON_JSR_310("JacksonJsr310", "com.fasterxml.jackson.datatype.jsr310.JavaTimeModule", "com.fasterxml.jackson.datatype", "jackson-datatype-jsr310", "2.13.3"),
    JACKSON_KTORM("Jackson Ktorm", "org.ktorm.jackson.KtormModule", "org.ktorm","ktorm-jackson", "3.4.1"),

    // Logging
    SLF4JSIMPLE("Slf4j simple", "org.slf4j.impl.StaticLoggerBinder", "org.slf4j", "slf4j-simple", "1.7.31"),
    SLF4J_PROVIDER_SIMPLE("Slf4j simple with Provider", "org.slf4j.simple.SimpleServiceProvider", "org.slf4j", "slf4j-simple", "1.8.0-beta4"),
    SLF4J_PROVIDER_API("Slf4j simple with Provider", "org.slf4j.spi.SLF4JServiceProvider", "org.slf4j", "slf4j-api", "1.8.0-beta4"),

    // Monitoring
    MICROMETER("Micrometer", "io.micrometer.core.instrument.Metrics", "io.micrometer", "micrometer-core", "1.7.3"),

    // Compression
    JVMBROTLI("Jvm-Brotli", "com.nixxcode.jvmbrotli.common.BrotliLoader", "com.nixxcode.jvmbrotli", "jvmbrotli", "0.2.0"),
}
