/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.core.util

import io.javalin.http.InternalServerErrorResponse
import java.net.URLEncoder
import java.util.HashMap

object DependencyUtil {

    private val dependencyCheckCache = HashMap<String, Boolean>()

    fun isPresent(dependency: OptionalDependency) = try {
        ensurePresence(dependency)
        true
    } catch (e: Exception) {
        false
    }

    fun ensurePresence(dependency: OptionalDependency, startupCheck: Boolean = false) {
        if (dependencyCheckCache[dependency.testClass] == true) {
            return
        }
        if (!Util.classExists(dependency.testClass)) {
            val message = missingDependencyMessage(dependency)
            if (startupCheck) {
                throw IllegalStateException(message)
            } else {
                JavalinLogger.warn(message)
                throw InternalServerErrorResponse(message)
            }
        }
        dependencyCheckCache[dependency.testClass] = true
    }

    internal fun missingDependencyMessage(dependency: OptionalDependency) = """|
        |-------------------------------------------------------------------
        |Missing dependency '${dependency.displayName}'. Add the dependency.
        |
        |pom.xml:
        |<dependency>
        |    <groupId>${dependency.groupId}</groupId>
        |    <artifactId>${dependency.artifactId}</artifactId>
        |    <version>${dependency.version}</version>
        |</dependency>
        |
        |build.gradle:
        |implementation group: '${dependency.groupId}', name: '${dependency.artifactId}', version: '${dependency.version}'
        |
        |Find the latest version here:
        |https://search.maven.org/search?q=${URLEncoder.encode("g:" + dependency.groupId + " AND a:" + dependency.artifactId, "UTF-8")}
        |-------------------------------------------------------------------""".trimMargin()

}

interface OptionalDependency{
    val displayName: String
    val testClass: String
    val groupId: String
    val artifactId: String
    val version: String
}

enum class CoreDependency(
    override val displayName: String,
    override val testClass: String,
    override val groupId: String,
    override val artifactId: String,
    override val version: String
) : OptionalDependency {

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
