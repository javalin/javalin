/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.plugin.rendering.vue

import io.javalin.http.Context
import io.javalin.http.util.ContextUtil.isLocalhost
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.stream.Collectors
import io.javalin.plugin.rendering.vue.JavalinVue.resourcesJarClass as jarClass

object JavalinVue {
    // @formatter:off
    internal var isDev: Boolean? = null // cached and easily accessible, is set on first request (can't be configured directly by end user)
    @JvmField var isDevFunction: (Context) -> Boolean = { it.isLocalhost() } // used to set isDev, will be called once
    @JvmField var optimizeDependencies = true // only include required components for the route component
    @JvmField var stateFunction: (Context) -> Any = { mapOf<String, String>() } // global state that is injected into all VueComponents
    @JvmField var cacheControl = "no-cache, no-store, must-revalidate"
    @JvmField var rootDirectory: Path? = null // is set on first request (if not configured)
    // Balage's note:
    // I think it is dangerous for CLASSPATH mode: if you set first the rootDirectory it will be calculated using
    // the Path finder as "jarClass". And even if you override the resourcesJarClass, it will not be recalculated!
    // If you wish to keep it backward compatible, this may be made deprecated instead
//    @JvmStatic fun rootDirectory(path: String, location: Location) {
//        rootDirectory = if (location == Location.CLASSPATH) PathMaster.classpathPath(path) else Paths.get(path)
//    }

    // Making this private and only settable by rootDirectory()
    @JvmStatic internal var resourcesJarClass: Class<*> = PathMaster::class.java // can be any class in the jar to look for resources in

    // For completeness, you may add this convenient version for String based path setup
    @JvmStatic fun rootDirectory(path: String) { rootDirectory = Paths.get(path) }

    // The user can either use the property value setter for external path or this function for classpath related values, but giving the
    // class and the path at the same time
    @JvmStatic fun rootDirectory(path: String, resourcesJarClass: Class<*> = PathMaster::class.java) {
        this.resourcesJarClass = resourcesJarClass
        rootDirectory = PathMaster.classpathPath(path)
    }

    internal fun walkPaths(): Set<Path> = Files.walk(rootDirectory, 20).collect(Collectors.toSet())
    internal val cachedPaths by lazy { walkPaths() }
    internal val cachedDependencyResolver by lazy { VueDependencyResolver(cachedPaths) }
    // @formatter:on
}

/**
 * By default, [jarClass] is PathMaster::class, which means this code will only
 * work if the resources are in the same jar as Javalin (i.e. in a fat-jar/uber-jar).
 * You can change resourcesJarClass to whatever class suits your needs.
 */
object PathMaster {
    /** We create a filesystem to "walk" the jar ([JavalinVue.walkPaths]) to find all the .vue files. */
    private val fileSystem by lazy {
        FileSystems.newFileSystem(
            jarClass.getResource("").toURI(),
            emptyMap<String, Any>()
        )
    }

    fun classpathPath(path: String): Path = when {
        jarClass.getResource(path).toURI().scheme == "jar" -> fileSystem.getPath(path) // we're inside a jar
        else -> Paths.get(jarClass.getResource(path).toURI()) // we're not in jar (probably running from IDE)
    }

    fun defaultLocation(isDev: Boolean?) =
        if (isDev == true) Paths.get("src/main/resources/vue") else classpathPath("/vue")
}
