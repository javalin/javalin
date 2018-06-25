/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.core.util

import io.javalin.HaltException
import io.javalin.Handler
import org.slf4j.LoggerFactory
import java.io.ByteArrayOutputStream
import java.io.ObjectOutputStream
import java.util.*

object Util {

    private val log = LoggerFactory.getLogger(Util::class.java)

    var noServerHasBeenStarted = true

    fun normalizeContextPath(contextPath: String) = ("/$contextPath").replace("/{2,}".toRegex(), "/").removeSuffix("/")
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
            val message = """
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
            log.warn(message)
            throw HaltException(500, message)
        }
        dependencyCheckCache[dependency.testClass] = true
    }

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
        if (!classExists("org.slf4j.impl.StaticLoggerBinder")) {
            val message = """
            -------------------------------------------------------------------
            Javalin: In the Java world, it's common to add your own logger.
            Javalin: To easily fix the warning above, get the latest version of slf4j-simple:
            Javalin: https://mvnrepository.com/artifact/org.slf4j/slf4j-simple
            Javalin: then add it to your dependencies (pom.xml or build.gradle)
            Javalin: Visit https://javalin.io/documentation#logging if you need more help
            """.trimIndent()
            System.err.println(message)
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

    fun handlerEquals(h1: Handler, h2: Handler) = h1 == h2 || probablyEqual(h1, h2)

    private fun probablyEqual(h1: Handler, h2: Handler): Boolean { // lol
        val h1Bytes = serialize(h1)
        val h2Bytes = serialize(h2)
        if (h1Bytes.size != h2Bytes.size) {
            return false;
        }
        if (h1Bytes.contentEquals(h2Bytes)) {
            return true;
        }
        var count = 0
        for (i in 0 until h1Bytes.size) {
            if (h1Bytes[i] == h2Bytes[i]) {
                count++
            }
        }
        val match = count.toDouble() / h1Bytes.size
        return match > 0.95
    }

    private fun serialize(obj: Any): Array<Byte> {
        val byteArrayOutputStream = ByteArrayOutputStream()
        ObjectOutputStream(byteArrayOutputStream).writeObject(obj)
        return byteArrayOutputStream.toByteArray().toTypedArray()
    }

}
