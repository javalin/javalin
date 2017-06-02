/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.core.util

import io.javalin.HaltException
import org.slf4j.LoggerFactory
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.*

object Util {

    private val log = LoggerFactory.getLogger(Util::class.java)

    fun notNull(obj: Any?, message: String) {
        if (obj == null) {
            throw IllegalArgumentException(message)
        }
    }

    fun classExists(className: String): Boolean {
        try {
            Class.forName(className)
            return true
        } catch (e: ClassNotFoundException) {
            return false
        }

    }

    private val dependencyCheckCache = HashMap<String, Boolean>()

    fun ensureDependencyPresent(dependencyName: String, className: String, url: String) {
        if (dependencyCheckCache.getOrDefault(className, false)) {
            return
        }
        if (!classExists(className)) {
            val message = "Missing dependency '$dependencyName'. Please add dependency: https://mvnrepository.com/artifact/$url"
            log.warn(message)
            throw HaltException(500, message)
        }
        dependencyCheckCache.put(className, true)
    }

    fun pathToList(pathString: String): List<String> {
        return pathString.split("/").filter { it.isNotEmpty() }.toList();
    }

    fun printHelpfulMessageIfLoggerIsMissing() {
        if (!classExists("org.slf4j.impl.StaticLoggerBinder")) {
            val message = """
            "-------------------------------------------------------------------"
            "Javalin: In the Java world, it's common to add your own logger."
            "Javalin: To easily fix the warning above, get the latest version of slf4j-simple:"
            "Javalin: https://mvnrepository.com/artifact/org.slf4j/slf4j-simple"
            "Javalin: then add it to your dependencies (pom.xml or build.gradle)"
            "Javalin: Visit https://javalin.io/documentation#logging if you need more help"
            """.trimIndent();
            System.err.println(message)
        }
    }

    fun javalinBanner(): String {
        return """
             _________________________________________
            |        _                  _ _           |
            |       | | __ ___   ____ _| (_)_ __      |
            |    _  | |/ _` \ \ / / _` | | | '_ \     |
            |   | |_| | (_| |\ V / (_| | | | | | |    |
            |    \___/ \__,_| \_/ \__,_|_|_|_| |_|    |
            |_________________________________________|
            |                                         |
            |    https://javalin.io/documentation     |
            |_________________________________________|

            """.trimIndent()
    }

    @Throws(IOException::class)
    fun copyStream(inputStream: InputStream, out: OutputStream): Long {
        val buf = ByteArray(8192)
        var totalBytesCopied: Long = 0
        var bytesReadThisPass = inputStream.read(buf)
        while (bytesReadThisPass > 0) {
            out.write(buf, 0, bytesReadThisPass)
            totalBytesCopied += bytesReadThisPass.toLong()
            bytesReadThisPass = inputStream.read(buf)
        }
        return totalBytesCopied
    }
}
