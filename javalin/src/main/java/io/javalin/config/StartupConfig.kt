/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */
package io.javalin.config

import org.slf4j.event.Level

/**
 * Miscellaneous application-level configuration options.
 */
class StartupConfig {
    @JvmField var showJavalinBanner = true
    @JvmField var showOldJavalinVersionWarning = true
    @JvmField var startupWatcherEnabled = true
    /**
     * Hides Jetty logs below the specified level during server startup and shutdown (lifecycle events).
     * For example, setting to [Level.WARN] will hide INFO and DEBUG logs, showing only WARN and ERROR.
     * After lifecycle completes, the log level is restored to its previous value.
     * Set to null (default) to leave Jetty's log level unchanged.
     * Works with Logback, Log4j2, and slf4j-simple via reflection. Best-effort - silently ignored if unsupported.
     */
    @JvmField var hideJettyLifecycleLogsBelowLevel: Level? = null
}
