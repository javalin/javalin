/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.core.util

import io.javalin.InternalServerErrorResponse
import org.slf4j.LoggerFactory
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.URL
import java.util.*
import java.util.zip.Adler32
import java.util.zip.CheckedInputStream

object Util {

    private val log = LoggerFactory.getLogger(Util::class.java)

    var noServerHasBeenStarted = true

    fun normalizeContextPath(contextPath: String) = ("/$contextPath").replace("/{2,}".toRegex(), "/").removeSuffix("/")

    @JvmStatic
    fun prefixContextPath(contextPath: String, path: String) = if (path == "*") path else ("$contextPath/$path").replace("/{2,}".toRegex(), "/")

    private fun classExists(className: String) = try {
        Class.forName(className)
        true
    } catch (e: ClassNotFoundException) {
        false
    }

    private val dependencyCheckCache = HashMap<String, Boolean>()

    fun ensureDependencyPresent(dependency: OptionalDependency) {
        if (dependencyCheckCache[dependency.testClass] == true) {
            return
        }
        if (!classExists(dependency.testClass)) {
            val message = missingDependencyMessage(dependency)
            log.warn(message)
            throw InternalServerErrorResponse(message)
        }
        dependencyCheckCache[dependency.testClass] = true
    }

    internal fun missingDependencyMessage(dependency: OptionalDependency) = """
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
            |compile "${dependency.groupId}:${dependency.artifactId}:${dependency.version}"""".trimMargin()

    fun printHelpfulMessageIfNoServerHasBeenStartedAfterOneSecond() {
        // per instance checks are not considered necessary
        // this helper is not intended for people with more than one instance
        Thread {
            Thread.sleep(1000)
            if (noServerHasBeenStarted) {
                log.info("It looks like you created a Javalin instance, but you never started it.")
                log.info("Try: Javalin app = Javalin.create().start();")
                log.info("For more help, visit https://javalin.io/documentation#starting-and-stopping")
            }
        }.start()
    }

    fun pathToList(pathString: String): List<String> = pathString.split("/").filter { it.isNotEmpty() }

    fun printHelpfulMessageIfLoggerIsMissing() {
        if (!classExists(OptionalDependency.SLF4JSIMPLE.testClass)) {
            System.err.println("""
            |-------------------------------------------------------------------
            |${missingDependencyMessage(OptionalDependency.SLF4JSIMPLE)}
            |-------------------------------------------------------------------
            |Visit https://javalin.io/documentation#logging if you need more help""".trimMargin())
        }
    }

    fun javalinBanner(): String {
        return "\n" + """
             _________________________________________
            |        _                  _ _           |
            |       | | __ ___   ____ _| (_)_ __      |
            |    _  | |/ _` \ \ / / _` | | | '_ \     |
            |   | |_| | (_| |\ V / (_| | | | | | |    |
            |    \___/ \__,_| \_/ \__,_|_|_|_| |_|    |
            |_________________________________________|
            |                                         |
            |    https://javalin.io/documentation     |
            |_________________________________________|""".trimIndent()
    }

    fun getChecksumAndReset(inputStream: InputStream): String {
        val cis = CheckedInputStream(inputStream, Adler32())
        val out = ByteArrayOutputStream()
        cis.copyTo(out)
        inputStream.reset()
        return cis.checksum.value.toString()
    }

    fun getResource(path: String): URL? = this.javaClass.classLoader.getResource(path)

    fun isKotlinClass(clazz: Class<*>): Boolean {
      try {
        for (annotation in clazz.declaredAnnotations) {
          // Note: annotation.simpleClass can be used if kotlin-reflect is available.
          if (annotation.annotationClass.toString().contains("kotlin.Metadata")) {
            return true
          }
        }
      } catch (ignored: Exception) {}
      return false
    }
}
