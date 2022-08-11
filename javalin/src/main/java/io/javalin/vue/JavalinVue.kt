/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.vue

import io.javalin.http.Context
import io.javalin.http.isLocalhost
import io.javalin.http.staticfiles.Location
import java.nio.file.FileSystem
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.stream.Collectors

object JavalinVue {
    // @formatter:off
    internal var rootDirectory: Path? = null // is set on first request (if not configured)
    @JvmStatic fun rootDirectory(path: Path) {
        this.rootDirectory = path
    }
    @JvmStatic @JvmOverloads fun rootDirectory(path: String, location: Location = Location.CLASSPATH, resourcesJarClass: Class<*> = PathMaster::class.java) {
        this.rootDirectory = if (location == Location.CLASSPATH) PathMaster.classpathPath(path, resourcesJarClass) else Paths.get(path)
    }

    @JvmField var vueAppName: String? = null // only relevant for Vue 3 apps

    internal var isDev: Boolean? = null // cached and easily accessible, is set on first request (can't be configured directly by end user)
    @JvmField var isDevFunction: (Context) -> Boolean = { it.isLocalhost() } // used to set isDev, will be called once

    @JvmField var optimizeDependencies = true // only include required components for the route component

    @JvmField var stateFunction: (Context) -> Any = { mapOf<String, String>() } // global state that is injected into all VueComponents

    @JvmField var cacheControl = "no-cache, no-store, must-revalidate"

    @JvmField var enableCspAndNonces = false
    // @formatter:on
    fun walkPaths(): Set<Path> = Files.walk(rootDirectory, 20).use { it.collect(Collectors.toSet()) }
    internal val cachedPaths by lazy { walkPaths() }
    internal val cachedDependencyResolver by lazy { VueDependencyResolver(cachedPaths, vueAppName) }
}

object PathMaster {
    /** We create a filesystem to "walk" the jar ([JavalinVue.walkPaths]) to find all the .vue files. */
    private lateinit var fileSystem: FileSystem // we need to keep this variable around to keep the file system open ?

    private fun getFileSystem(jarClass: Class<*>): FileSystem {
        if (!this::fileSystem.isInitialized) {
            this.fileSystem = FileSystems.newFileSystem(jarClass.getResource("")!!.toURI(), emptyMap<String, Any>())
        }
        return this.fileSystem
    }

    fun classpathPath(path: String, jarClass: Class<*>): Path = when {
        jarClass.getResource(path)!!.toURI().scheme == "jar" -> getFileSystem(jarClass).getPath(path) // we're inside a jar
        else -> Paths.get(jarClass.getResource(path)!!.toURI()) // we're not in jar (probably running from IDE)
    }

    fun defaultLocation(isDev: Boolean): Path =
        if (isDev) Paths.get("src/main/resources/vue") else classpathPath("/vue", PathMaster::class.java)
}
