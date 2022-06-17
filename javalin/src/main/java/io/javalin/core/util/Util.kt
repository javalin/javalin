/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.core.util

import java.io.ByteArrayInputStream
import java.io.File
import java.net.URL
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.*
import java.util.zip.Adler32
import java.util.zip.CheckedInputStream

object Util {

    @JvmStatic
    fun normalizeContextPath(contextPath: String) = ("/$contextPath").replace("/{2,}".toRegex(), "/").removeSuffix("/")

    @JvmStatic
    fun prefixContextPath(contextPath: String, path: String) = if (path == "*") path else ("$contextPath/$path").replace("/{2,}".toRegex(), "/")

    fun classExists(className: String) = try {
        Class.forName(className)
        true
    } catch (e: ClassNotFoundException) {
        false
    }

    private fun serviceImplementationExists(className: String) = try {
        val serviceClass = Class.forName(className)
        val loader = ServiceLoader.load(serviceClass)
        loader.any()
    } catch (e: ClassNotFoundException) {
        false
    }

    fun loggingLibraryExists(): Boolean {
        return classExists(CoreDependency.SLF4JSIMPLE.testClass) ||
                serviceImplementationExists(CoreDependency.SLF4J_PROVIDER_API.testClass)
    }

    @JvmStatic
    fun printHelpfulMessageIfLoggerIsMissing() {
        if (!loggingLibraryExists()) {
            System.err.println("""
            |-------------------------------------------------------------------
            |${DependencyUtil.missingDependencyMessage(CoreDependency.SLF4JSIMPLE)}
            |-------------------------------------------------------------------
            |OR
            |-------------------------------------------------------------------
            |${DependencyUtil.missingDependencyMessage(CoreDependency.SLF4J_PROVIDER_API)} and
            |${DependencyUtil.missingDependencyMessage(CoreDependency.SLF4J_PROVIDER_SIMPLE)}
            |-------------------------------------------------------------------
            |Visit https://javalin.io/documentation#logging if you need more help""".trimMargin())
        }
    }

    @JvmStatic
    fun logJavalinBanner(showBanner: Boolean) {
        if (showBanner) JavalinLogger.info("\n" + """
          |       __                      __ _           ______
          |      / /____ _ _   __ ____ _ / /(_)____     / ____/
          | __  / // __ `/| | / // __ `// // // __ \   /___ \
          |/ /_/ // /_/ / | |/ // /_/ // // // / / /  ____/ /
          |\____/ \__,_/  |___/ \__,_//_//_//_/ /_/  /_____/
          |
          |          https://javalin.io/documentation
          |""".trimMargin())
    }

    @JvmStatic
    fun logJavalinVersion() = try {
        val properties = Properties().also {
            val propertiesPath = "META-INF/maven/io.javalin/javalin/pom.properties"
            it.load(this.javaClass.classLoader.getResourceAsStream(propertiesPath))
        }
        val (version, buildTime) = listOf(properties.getProperty("version")!!, properties.getProperty("buildTime")!!)
        JavalinLogger.startup("You are running Javalin $version (released ${formatBuildTime(buildTime)}).")
    } catch (e: Exception) {
        // it's not that important
    }

    private fun formatBuildTime(buildTime: String): String? = try {
        val (release, now) = listOf(Instant.parse(buildTime), Instant.now())
        val formatter = DateTimeFormatter.ofPattern("MMMM d, yyyy").withLocale(Locale.US).withZone(ZoneId.of("Z"))
        formatter.format(release) + if (now.isAfter(release.plus(90, ChronoUnit.DAYS))) {
            ". Your Javalin version is ${ChronoUnit.DAYS.between(release, now)} days old. Consider checking for a newer version."
        } else ""
    } catch (e: Exception) {
        null // it's not that important
    }

    fun getChecksumAndReset(inputStream: ByteArrayInputStream): String {
        inputStream.mark(Int.MAX_VALUE) //it's all in memory so there is no readAheadLimit
        val cis = CheckedInputStream(inputStream, Adler32())
        var byte = cis.read()
        while (byte > -1) {
            byte = cis.read()
        }
        inputStream.reset()
        return cis.checksum.value.toString()
    }

    @JvmStatic
    fun getResourceUrl(path: String): URL? = this.javaClass.classLoader.getResource(path)

    fun getFileUrl(path: String): URL? = if (File(path).exists()) File(path).toURI().toURL() else null

    fun isKotlinClass(clazz: Class<*>): Boolean {
        try {
            for (annotation in clazz.declaredAnnotations) {
                // Note: annotation.simpleClass can be used if kotlin-reflect is available.
                if (annotation.annotationClass.toString().contains("kotlin.Metadata")) {
                    return true
                }
            }
        } catch (ignored: Exception) {
        }
        return false
    }

    @JvmStatic
    fun getPort(e: Exception) = e.message!!.takeLastWhile { it != ':' }

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

}
