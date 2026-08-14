/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.util

import java.util.Properties

object DependencyUtil {

    fun missingDependencyMessage(dependency: OptionalDependency) = wrapInSeparators(
        """|You're missing the '${dependency.displayName}' dependency in your project. Add the dependency:
           |
           |${mavenAndGradleSnippets(dependency)}""".trimMargin()
    )

    fun mavenAndGradleSnippets(dependency: OptionalDependency) = """
           |pom.xml:
           |<dependency>
           |    <groupId>${dependency.groupId}</groupId>
           |    <artifactId>${dependency.artifactId}</artifactId>
           |    <version>${dependency.version}</version>
           |</dependency>
           |
           |build.gradle or build.gradle.kts:
           |implementation("${dependency.groupId}:${dependency.artifactId}:${dependency.version}")""".trimMargin()

    fun wrapInSeparators(msg: String) = """
        |
        |#########################################################################
        |${msg}
        |#########################################################################""".trimMargin()

}

interface OptionalDependency {
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
    private val versionProperty: String,
) : OptionalDependency {

    // JSON (Jackson) handling
    JACKSON("Jackson", "com.fasterxml.jackson.databind.ObjectMapper", "com.fasterxml.jackson.core", "jackson-databind", "jackson.databind.version"),
    JACKSON_KT("JacksonKt", "com.fasterxml.jackson.module.kotlin.KotlinModule", "com.fasterxml.jackson.module", "jackson-module-kotlin", "jackson.version"),
    JACKSON_JSR_310("JacksonJsr310", "com.fasterxml.jackson.datatype.jsr310.JavaTimeModule", "com.fasterxml.jackson.datatype", "jackson-datatype-jsr310", "jackson.version"),
    JACKSON_ECLIPSE_COLLECTIONS("JacksonEclipseCollections", "com.fasterxml.jackson.datatype.eclipsecollections.EclipseCollectionsModule", "com.fasterxml.jackson.datatype", "jackson-datatype-eclipse-collections", "jackson.version"),
    JACKSON_KTORM("Jackson Ktorm", "org.ktorm.jackson.KtormModule", "org.ktorm", "ktorm-jackson", "ktorm.version"),

    // JSON (Jackson 3) handling
    JACKSON3("Jackson3", "tools.jackson.databind.json.JsonMapper", "tools.jackson.core", "jackson-databind", "jackson3.version"),
    JACKSON3_KT("Jackson3Kt", "tools.jackson.module.kotlin.KotlinModule", "tools.jackson.module", "jackson-module-kotlin", "jackson3.version"),
    JACKSON3_ECLIPSE_COLLECTIONS("Jackson3EclipseCollections", "tools.jackson.datatype.eclipsecollections.EclipseCollectionsModule", "tools.jackson.datatype", "jackson-datatype-eclipse-collections", "jackson3.version"),

    // JSON (Gson)
    GSON("Gson", "com.google.gson.Gson", "com.google.code.gson", "gson", "gson.version"),

    // Logging
    SLF4JSIMPLE("Slf4j simple", "org.slf4j.impl.StaticLoggerBinder", "org.slf4j", "slf4j-simple", "slf4j.version"),

    // Compression
    BROTLI4J("Brotli4j", "com.aayushatharva.brotli4j.Brotli4jLoader", "com.aayushatharva.brotli4j", "brotli4j", "brotli4j.version"),
    ZSTD_JNI("Zstd-jni", "com.github.luben.zstd.Zstd", "com.github.luben", "zstd-jni", "zstd.jni.version");

    // baked from the pom into the jar's pom.properties; "..." only when running from source
    override val version: String get() = BuildProperties[versionProperty] ?: "..."
}

private object BuildProperties {
    private val properties: Properties by lazy {
        Properties().apply {
            OptionalDependency::class.java.classLoader
                .getResourceAsStream("META-INF/maven/io.javalin/javalin/pom.properties")?.use { load(it) }
        }
    }

    operator fun get(key: String): String? = properties.getProperty(key)
}
