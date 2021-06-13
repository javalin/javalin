/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.core.util

import io.javalin.core.JavalinServer
import io.javalin.http.Context
import io.javalin.http.InternalServerErrorResponse
import java.io.ByteArrayInputStream
import java.io.File
import java.net.URL
import java.net.URLEncoder
import java.util.*
import java.util.ServiceLoader
import java.util.zip.Adler32
import java.util.zip.CheckedInputStream
import javax.servlet.http.HttpServletResponse

object Util {

    @JvmStatic
    fun normalizeContextPath(contextPath: String) = ("/$contextPath").replace("/{2,}".toRegex(), "/").removeSuffix("/")

    @JvmStatic
    fun prefixContextPath(contextPath: String, path: String) = if (path == "*") path else ("$contextPath/$path").replace("/{2,}".toRegex(), "/")

    @JvmStatic
    fun isNonSubPathWildcard(path: String) = path.length > 1 && path.endsWith("*") && !path.endsWith("/*") // e.g. /my/:path*

    private fun classExists(className: String) = try {
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

    fun dependencyIsPresent(dependency: OptionalDependency) = try {
        ensureDependencyPresent(dependency)
        true
    } catch (e: Exception) {
        false
    }

    private val dependencyCheckCache = HashMap<String, Boolean>()

    fun ensureDependencyPresent(dependency: OptionalDependency, startupCheck: Boolean = false) {
        if (dependencyCheckCache[dependency.testClass] == true) {
            return
        }
        if (!classExists(dependency.testClass)) {
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
            |compile "${dependency.groupId}:${dependency.artifactId}:${dependency.version}"
            |
            |Find the latest version here:
            |https://search.maven.org/search?q=${URLEncoder.encode("g:" + dependency.groupId + " AND a:" + dependency.artifactId, "UTF-8")}
            |-------------------------------------------------------------------""".trimMargin()

    fun pathToList(pathString: String): List<String> = pathString.split("/").filter { it.isNotEmpty() }

    @JvmStatic
    fun printHelpfulMessageIfLoggerIsMissing() {
        if (!loggingLibraryExists()) {
            System.err.println("""
            |-------------------------------------------------------------------
            |${missingDependencyMessage(OptionalDependency.SLF4JSIMPLE)}
            |-------------------------------------------------------------------
            |OR
            |-------------------------------------------------------------------
            |${missingDependencyMessage(OptionalDependency.SLF4J_PROVIDER_API)} and
            |${missingDependencyMessage(OptionalDependency.SLF4J_PROVIDER_SIMPLE)}
            |-------------------------------------------------------------------
            |Visit https://javalin.io/documentation#logging if you need more help""".trimMargin())
        }
    }

    fun loggingLibraryExists(): Boolean {
        return classExists(OptionalDependency.SLF4JSIMPLE.testClass) ||
                serviceImplementationExists(OptionalDependency.SLF4J_PROVIDER_API.testClass)
    }

    @JvmStatic
    fun logJavalinBanner(showBanner: Boolean) {
        if (showBanner) JavalinLogger.info("\n" + """
          |       __                      __ _            __ __
          |      / /____ _ _   __ ____ _ / /(_)____      / // /
          | __  / // __ `/| | / // __ `// // // __ \    / // /_
          |/ /_/ // /_/ / | |/ // /_/ // // // / / /   /__  __/
          |\____/ \__,_/  |___/ \__,_//_//_//_/ /_/      /_/
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
        JavalinLogger.info("You are running Javalin ${properties.getProperty("version")!!}.") // throw if null
    } catch (e: Exception) {
        // it's not that important
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

    @JvmStatic
    fun getWebjarPublicPath(ctx: Context, dependency: OptionalDependency): String {
        return "${ctx.contextPath()}/webjars/${dependency.artifactId}/${dependency.version}"
    }

    @JvmStatic
    fun assertWebjarInstalled(dependency: OptionalDependency) = try {
        getWebjarResourceUrl(dependency)
    } catch (e: Exception) {
        JavalinLogger.warn(missingDependencyMessage(dependency))
    }

    @JvmStatic
    fun getWebjarResourceUrl(dependency: OptionalDependency): URL? {
        val webjarBaseUrl = "META-INF/resources/webjars"
        return getResourceUrl("$webjarBaseUrl/${dependency.artifactId}/${dependency.version}")
    }

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

    fun writeResponse(response: HttpServletResponse, responseBody: String, status: Int) {
        response.status = status
        ByteArrayInputStream(responseBody.toByteArray()).copyTo(response.outputStream)
        response.outputStream.close()
    }

    var logIfNotStarted = true

    @JvmStatic
    fun logIfServerNotStarted(server: JavalinServer) = Thread {
        Thread.sleep(5000)
        if (!server.started && logIfNotStarted) {
            JavalinLogger.info("It looks like you created a Javalin instance, but you never started it.")
            JavalinLogger.info("Try: Javalin app = Javalin.create().start();")
            JavalinLogger.info("For more help, visit https://javalin.io/documentation#starting-and-stopping")
        }
    }.start()

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

    // jetty throws if client aborts during response writing. testing name avoids hard dependency on jetty.
    fun isClientAbortException(t: Throwable) = t::class.java.name == "org.eclipse.jetty.io.EofException"

    // Jetty may timeout connections to avoid having broken connections that remain open forever
    // This is rare, but intended (see issues #163 and #1277)
    fun isJettyTimeoutException(t: Throwable) = t::class.java.name == "java.util.concurrent.TimeoutException"

}
