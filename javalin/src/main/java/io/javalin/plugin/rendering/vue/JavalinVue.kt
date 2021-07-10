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
import java.util.function.Consumer
import java.util.stream.Collectors
import io.javalin.plugin.rendering.vue.JavalinVue.resourcesJarClass as jarClass

enum class VueVersion { VUE_2, VUE_3 }

object JavalinVue {
    // @formatter:off
    internal var resourcesJarClass: Class<*> = PathMaster::class.java // can be any class in the jar to look for resources in
    internal var rootDirectory: Path? = null // is set on first request (if not configured)
    @JvmStatic fun rootDirectory(config: Consumer<VueDirConfig>) = config.accept(VueDirConfig()) // configures the two internal variables above

    internal var vueVersion = VueVersion.VUE_2
    internal var vueAppName = "Vue" // only relevant for Vue 3 apps
    @JvmStatic fun vueVersion(config: Consumer<VueVersionConfig>) = config.accept(VueVersionConfig()) // configures the two internal variables above

    internal var isDev: Boolean? = null // cached and easily accessible, is set on first request (can't be configured directly by end user)
    @JvmField var isDevFunction: (Context) -> Boolean = { it.isLocalhost() } // used to set isDev, will be called once

    @JvmField var optimizeDependencies = true // only include required components for the route component

    @JvmField var stateFunction: (Context) -> Any = { mapOf<String, String>() } // global state that is injected into all VueComponents

    @JvmField var cacheControl = "no-cache, no-store, must-revalidate"
    // @formatter:on
    fun walkPaths(): Set<Path> = Files.walk(rootDirectory, 20).collect(Collectors.toSet())
    internal val cachedPaths by lazy { walkPaths() }
    internal val cachedDependencyResolver by lazy { VueDependencyResolver(cachedPaths, vueAppName) }
}

/**
 * By default, [jarClass] is PathMaster::class, which means this code will only
 * work if the resources are in the same jar as Javalin (i.e. in a fat-jar/uber-jar).
 * You can change resourcesJarClass to whatever class suits your needs.
 */
object PathMaster {
    /** We create a filesystem to "walk" the jar ([JavalinVue.walkPaths]) to find all the .vue files. */
    private val fileSystem by lazy { FileSystems.newFileSystem(jarClass.getResource("").toURI(), emptyMap<String, Any>()) }

    fun classpathPath(path: String): Path = when {
        jarClass.getResource(path).toURI().scheme == "jar" -> fileSystem.getPath(path) // we're inside a jar
        else -> Paths.get(jarClass.getResource(path).toURI()) // we're not in jar (probably running from IDE)
    }

    fun defaultLocation(isDev: Boolean?) = if (isDev == true) Paths.get("src/main/resources/vue") else classpathPath("/vue")
}
