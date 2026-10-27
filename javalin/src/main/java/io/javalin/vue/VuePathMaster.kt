/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.vue

import io.javalin.util.javalinLazy
import java.net.URI
import java.nio.file.FileSystem
import java.nio.file.FileSystemNotFoundException
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.stream.Collectors

internal class VuePathMaster(val cfg: JavalinVueConfig) {

    internal val cachedPaths by javalinLazy { walkPaths() }
    internal val cachedDependencyResolver by javalinLazy { VueDependencyResolver(cachedPaths, cfg.vueInstanceNameInJs) }

    fun walkPaths(): Set<Path> = Files.walk(cfg.rootDirectory, 20).use { it.collect(Collectors.toSet()) }

    fun classpathPath(path: String, jarClass: Class<*>): Path {
        val resourceUri = jarClass.getResource(path)!!.toURI()
        return when (resourceUri.scheme) {
            "jar" -> getFileSystem(resourceUri).provider().getPath(resourceUri)
            else -> Paths.get(resourceUri)
        }
    }

    fun defaultLocation(isDev: Boolean): Path =
        if (isDev) Paths.get("src/main/resources/vue") else classpathPath("/vue", VuePathMaster::class.java)

    /** We create a filesystem to "walk" the jar ([walkPaths]) to find all the .vue files. */
    private lateinit var fileSystem: FileSystem // we need to keep this variable around to keep the file system open ?
    private fun getFileSystem(resourceUri: URI): FileSystem {
        if (!this::fileSystem.isInitialized) {
            try {
                this.fileSystem = FileSystems.getFileSystem(resourceUri)
            } catch (e: FileSystemNotFoundException) {
                this.fileSystem = FileSystems.newFileSystem(resourceUri, emptyMap<String, Any>())
            }
        }
        return this.fileSystem
    }

}
