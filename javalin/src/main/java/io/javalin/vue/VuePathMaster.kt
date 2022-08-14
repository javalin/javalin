/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.vue

import java.nio.file.FileSystem
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.stream.Collectors

internal class VuePathMaster(val cfg: JavalinVueConfig) {

    internal val cachedPaths by lazy { walkPaths() }
    internal val cachedDependencyResolver by lazy { VueDependencyResolver(cachedPaths, cfg.vueAppName) }

    fun walkPaths(): Set<Path> = Files.walk(cfg.rootDirectory, 20).use { it.collect(Collectors.toSet()) }

    fun classpathPath(path: String, jarClass: Class<*>): Path = when {
        jarClass.getResource(path)!!.toURI().scheme == "jar" -> getFileSystem(jarClass).getPath(path) // we're inside a jar
        else -> Paths.get(jarClass.getResource(path)!!.toURI()) // we're not in jar (probably running from IDE)
    }

    fun defaultLocation(isDev: Boolean): Path =
        if (isDev) Paths.get("src/main/resources/vue") else classpathPath("/vue", VuePathMaster::class.java)

    /** We create a filesystem to "walk" the jar ([walkPaths]) to find all the .vue files. */
    private lateinit var fileSystem: FileSystem // we need to keep this variable around to keep the file system open ?
    private fun getFileSystem(jarClass: Class<*>): FileSystem {
        if (!this::fileSystem.isInitialized) {
            this.fileSystem = FileSystems.newFileSystem(jarClass.getResource("")!!.toURI(), emptyMap<String, Any>())
        }
        return this.fileSystem
    }

}
