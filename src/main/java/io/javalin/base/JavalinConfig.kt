package io.javalin.base

import io.javalin.Javalin
import io.javalin.LogLevel
import io.javalin.core.util.CorsUtil
import io.javalin.embeddedserver.Location
import io.javalin.embeddedserver.StaticFileConfig
import java.nio.charset.StandardCharsets
import java.util.ArrayList

internal abstract class JavalinConfig : JavalinWS() {

    protected val staticFileConfigs = ArrayList<StaticFileConfig>()
    protected var logLevel = LogLevel.OFF
    protected var dynamicGzipEnabled = false
    protected var defaultContentType = "text/plain"
    protected var defaultCharacterEncoding = StandardCharsets.UTF_8.name()
    protected var maxRequestCacheBodySize = java.lang.Long.MAX_VALUE

    override fun enableCorsForOrigin(vararg origin: String): Javalin {
        ensureActionIsPerformedBeforeServerStart("Enabling CORS")
        return CorsUtil.enableCors(this, origin)
    }

    override fun enableCorsForAllOrigins() = enableCorsForOrigin("*")

    override fun enableStaticFiles(classpathPath: String): Javalin {
        return enableStaticFiles(classpathPath, Location.CLASSPATH)
    }

    override fun enableStaticFiles(path: String, location: Location): Javalin {
        ensureActionIsPerformedBeforeServerStart("Enabling static files")
        staticFileConfigs.add(StaticFileConfig(path, location))
        return this
    }

    override fun requestLogLevel(logLevel: LogLevel): Javalin {
        ensureActionIsPerformedBeforeServerStart("Enabling request-logging")
        this.logLevel = logLevel
        return this
    }

    override fun enableStandardRequestLogging() = requestLogLevel(LogLevel.STANDARD)

    override fun enableDynamicGzip(): Javalin {
        ensureActionIsPerformedBeforeServerStart("Enabling dynamic GZIP")
        this.dynamicGzipEnabled = true
        return this
    }

    override fun defaultContentType(contentType: String): Javalin {
        ensureActionIsPerformedBeforeServerStart("Changing default content type")
        this.defaultContentType = contentType
        return this
    }

    override fun defaultCharacterEncoding(characterEncoding: String): Javalin {
        ensureActionIsPerformedBeforeServerStart("Changing default character encoding")
        this.defaultCharacterEncoding = characterEncoding
        return this
    }

    override fun maxBodySizeForRequestCache(value: Long): Javalin {
        ensureActionIsPerformedBeforeServerStart("Changing request cache body size")
        this.maxRequestCacheBodySize = value
        return this
    }

    override fun disableRequestCache() = maxBodySizeForRequestCache(0)

    override fun dontIgnoreTrailingSlashes(): Javalin {
        ensureActionIsPerformedBeforeServerStart("Telling Javalin to not ignore slashes")
        pathMatcher.ignoreTrailingSlashes = false
        return this
    }
}