/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.util

import java.io.ByteArrayInputStream
import java.io.File
import java.net.URL
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.*
import java.util.function.Supplier
import java.util.stream.Stream
import java.util.zip.Adler32
import java.util.zip.CheckedInputStream

object Util {

    @JvmStatic
    fun <T> createLazy(init: Supplier<T>): Lazy<T> = lazy { init.get() }

    @JvmStatic
    fun normalizeContextPath(contextPath: String) = when {
        contextPath == "/" -> "/"
        else -> ("/$contextPath").replace("/{2,}".toRegex(), "/").removeSuffix("/")
    }

    @JvmStatic
    fun prefixContextPath(contextPath: String, path: String) = if (path == "*") path else ("$contextPath/$path").replace("/{2,}".toRegex(), "/")

    fun classExists(className: String) = try {
        Class.forName(className)
        true
    } catch (e: ClassNotFoundException) {
        false
    }

    private fun slf4jServiceImplementationExists() = try {
        val serviceClass = Class.forName("org.slf4j.spi.SLF4JServiceProvider")
        val loader = ServiceLoader.load(serviceClass)
        loader.any()
    } catch (e: ClassNotFoundException) {
        false
    }

    @JvmStatic
    fun printHelpfulMessageIfLoggerIsMissing() {
        val hasLogger = classExists(CoreDependency.SLF4JSIMPLE.testClass) || slf4jServiceImplementationExists()
        if (!hasLogger) {
            System.err.println(
                DependencyUtil.wrapInSeparators(
                    """|Javalin: It looks like you don't have a logger in your project.
                       |The easiest way to fix this is to add '${CoreDependency.SLF4JSIMPLE.artifactId}':
                       |
                       |${DependencyUtil.mavenAndGradleSnippets(CoreDependency.SLF4JSIMPLE)}
                       |
                       |Visit https://javalin.io/documentation#logging if you need more help""".trimMargin()
                )
            )
        }
    }

    @JvmStatic
    fun logJavalinVersion(showVersionWarning: Boolean = true) = try {
        val properties = Properties().also {
            val propertiesPath = "META-INF/maven/io.javalin/javalin/pom.properties"
            it.load(this.javaClass.classLoader.getResourceAsStream(propertiesPath))
        }
        val (version, buildTime) = listOf(properties.getProperty("version")!!, properties.getProperty("buildTime")!!)
        JavalinLogger.startup("You are running Javalin $version (released ${formatBuildTime(buildTime, showVersionWarning)}).")
    } catch (e: Exception) {
        // it's not that important
    }

    private fun formatBuildTime(buildTime: String, showVersionWarning: Boolean = true): String? = try {
        val (release, now) = listOf(Instant.parse(buildTime), Instant.now())
        val formatter = DateTimeFormatter.ofPattern("MMMM d, yyyy").withLocale(Locale.US).withZone(ZoneId.of("Z"))
        formatter.format(release) + if (showVersionWarning && now.isAfter(release.plus(120, ChronoUnit.DAYS))) {
            ". Your Javalin version is ${ChronoUnit.DAYS.between(release, now)} days old. Consider checking for a newer version."
        } else ""
    } catch (e: Exception) {
        null // it's not that important
    }

    fun checksumAndReset(inputStream: ByteArrayInputStream): String {
        val cis = CheckedInputStream(inputStream, Adler32())
        var byte = cis.read()
        while (byte > -1) {
            byte = cis.read()
        }
        inputStream.reset()
        return cis.checksum.value.toString()
    }

    @JvmStatic
    fun resourceUrl(path: String): URL? = this.javaClass.classLoader.getResource(path)

    fun fileUrl(path: String): URL? = if (File(path).exists()) File(path).toURI().toURL() else null

    @JvmStatic
    fun port(e: Exception) = e.message!!.takeLastWhile { it != ':' }

    fun <T : Any?> findByClass(map: Map<Class<out Exception>, T>, exceptionClass: Class<out Exception>): T? = map.getOrElse(exceptionClass) {
        var superclass = exceptionClass.superclass
        while (superclass != null) {
            if (map.containsKey(superclass)) {
                return map[superclass]
            }
            superclass = superclass.superclass
        }
        return null
    }

    fun <T : Any> Stream<T>.firstOrNull(): T? = findFirst().orElse(null)

    inline fun <T : Any> Stream<T>.firstOrNull(body: (T) -> Unit): T? {
        val value = firstOrNull()
        if (value != null) body(value)
        return value
    }

}
