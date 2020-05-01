/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.core.util

import java.io.File
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption

object FileUtil {

    @JvmStatic
    fun streamToFile(inputStream: InputStream, path: String) {
        val newFile = File(path)
        newFile.parentFile.mkdirs() // create parent dirs if necessary
        newFile.createNewFile() // create file if necessary
        Files.copy(inputStream, newFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
    }

    @JvmStatic
    fun readResource(path: String) = FileUtil::class.java.getResource(path).readText()

    @JvmStatic
    fun readFile(path: String) = File(path).readText()

}
