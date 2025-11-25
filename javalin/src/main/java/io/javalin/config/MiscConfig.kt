/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */
package io.javalin.config

import io.javalin.http.servlet.DefaultTasks
import io.javalin.http.servlet.TaskInitializer
import io.javalin.http.servlet.JavalinServletContext

/**
 * Miscellaneous application-level configuration options.
 */
class MiscConfig {
    @JvmField var useVirtualThreads = false
    @JvmField var showJavalinBanner = true
    @JvmField var showOldJavalinVersionWarning = true
    @JvmField var startupWatcherEnabled = true
}
