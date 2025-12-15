@file:Suppress("internal", "INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")

package io.javalin.config

import io.javalin.http.HttpStatus.INTERNAL_SERVER_ERROR
import io.javalin.router.exception.JavaLangErrorHandler
import io.javalin.router.HandlerWrapper
import io.javalin.util.JavalinLogger

/**
 * Configuration for the Router.
 *
 * @param cfg the parent Javalin Configuration
 * @see [JavalinState.router]
 */
class RouterConfig() {

    @JvmSynthetic
    internal var handlerWrapper: HandlerWrapper? = null

    @JvmSynthetic
    internal var javaLangErrorHandler: JavaLangErrorHandler = JavaLangErrorHandler { res, error ->
        res.status = INTERNAL_SERVER_ERROR.code
        JavalinLogger.error("Fatal error occurred while servicing http-request", error)
    }

    // @formatter:off
    /** The context path (ex '/blog' if you are hosting an app on a subpath, like 'mydomain.com/blog') */
    @JvmField var contextPath = "/"
    /** If true, treat '/path' and '/path/' as the same path (default: true). */
    @JvmField var ignoreTrailingSlashes = true
    /** If true, treat '/path//subpath' and '/path/subpath' as the same path (default: false). */
    @JvmField var treatMultipleSlashesAsSingleSlash = false
    /** If true, treat '/PATH' and '/path' as the same path (default: false). */
    @JvmField var caseInsensitiveRoutes = false
    // @formatter:on

    fun handlerWrapper(handlerWrapper: HandlerWrapper) {
        this.handlerWrapper = handlerWrapper
    }

    fun javaLangErrorHandler(handler: JavaLangErrorHandler) {
        this.javaLangErrorHandler = handler
    }

}
