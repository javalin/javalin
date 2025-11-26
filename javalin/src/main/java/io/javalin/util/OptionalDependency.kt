/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.util

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
    val testClass: () -> Any?
    val groupId: String
    val artifactId: String
    val version: String
}

enum class CoreDependency(
    override val displayName: String,
    override val testClass: () -> Any?,
    override val groupId: String,
    override val artifactId: String,
    override val version: String
) : OptionalDependency {

    // JSON (Jackson) handling
    JACKSON("Jackson", { com.fasterxml.jackson.databind.ObjectMapper::class }, "com.fasterxml.jackson.core", "jackson-databind", "2.17.2"),
    JACKSON_KT("JacksonKt", { com.fasterxml.jackson.module.kotlin.KotlinModule::class }, "com.fasterxml.jackson.module", "jackson-module-kotlin", "2.17.2"),
    JACKSON_JSR_310("JacksonJsr310", { com.fasterxml.jackson.datatype.jsr310.JavaTimeModule::class }, "com.fasterxml.jackson.datatype", "jackson-datatype-jsr310", "2.17.2"),
    JACKSON_ECLIPSE_COLLECTIONS("JacksonEclipseCollections", { com.fasterxml.jackson.datatype.eclipsecollections.EclipseCollectionsModule::class }, "com.fasterxml.jackson.datatype", "jackson-datatype-eclipse-collections", "2.17.2"),
    JACKSON_KTORM("Jackson Ktorm", { org.ktorm.jackson.KtormModule::class }, "org.ktorm", "ktorm-jackson", "3.6.0"),

    // JSON (Gson)
    GSON("Gson", { com.google.gson.Gson::class }, "com.google.code.gson", "gson", "2.11.0"),

    // Logging
    SLF4JSIMPLE("Slf4j simple", { org.slf4j.impl.StaticLoggerBinder::class }, "org.slf4j", "slf4j-simple", "2.0.16"),

    // Compression
    BROTLI4J("Brotli4j", { com.aayushatharva.brotli4j.Brotli4jLoader::class }, "com.aayushatharva.brotli4j", "brotli4j", "1.20.0"),
    ZSTD_JNI("Zstd-jni", { com.github.luben.zstd.Zstd::class }, "com.github.luben", "zstd-jni", "1.5.7-4"),
}
