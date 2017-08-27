/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.core.util

import io.javalin.HaltException
import org.slf4j.LoggerFactory
import java.util.*

object Util {

    private val log = LoggerFactory.getLogger(Util::class.java)

    var noServerHasBeenStarted = true

    fun notNull(obj: Any?, message: String) {
        if (obj == null) {
            throw IllegalArgumentException(message)
        }
    }

    fun classExists(className: String): Boolean {
        return try {
            Class.forName(className)
            true
        } catch (e: ClassNotFoundException) {
            false
        }
    }

    private val dependencyCheckCache = HashMap<String, Boolean>()

    fun ensureDependencyPresent(dependencyName: String, className: String, url: String) {
        if (dependencyCheckCache[className] ?: false) {
            return
        }
        if (!classExists(className)) {
            val message = "Missing dependency '$dependencyName'. Please add dependency: https://mvnrepository.com/artifact/$url"
            log.warn(message)
            throw HaltException(500, message)
        }
        dependencyCheckCache[className] = true
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

}
