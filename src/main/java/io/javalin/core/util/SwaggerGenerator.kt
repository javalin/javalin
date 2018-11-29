/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.core.util

import java.io.File

data class EndpointData(
        var description: MutableList<String> = mutableListOf(),
        var path: String = "",
        val pathParams: MutableList<Pair<String, String>> = mutableListOf(),
        val queryParams: MutableList<Pair<String, String>> = mutableListOf(),
        val formParams: MutableList<Pair<String, String>> = mutableListOf(),
        var result: String = ""
)

object SwaggerGenerator {

    fun generateFromSourceFiles() = generateFromDirectory("src")

    fun generateFromDirectory(path: String) = generateSwagger(File(path).walkTopDown().filter { it.isFile }.toSet())

    private fun generateSwagger(files: Set<File>) = files.forEach { currentFile ->
        Regex("/\\*\\*([\\s\\S]*?)\\*/")
                .findAll(currentFile.readText())
                .map { it.groupValues[0] }
                .filter { it.contains("@path ") }
                .forEach { javadoc ->
                    val data = EndpointData()
                    javadoc.lines().asSequence()
                            .filter { it.length > 3 } // content
                            .map { it.substring(3) } // remove margin
                            .forEach { line ->
                                when {
                                    line.startsWith("@path ") -> data.path = line.split(" ")[1]
                                    line.startsWith("@pathParam ") -> data.pathParams.add(line.split(" ", limit = 3).let { it[1] to it[2] })
                                    line.startsWith("@queryParam ") -> data.queryParams.add(line.split(" ", limit = 3).let { it[1] to it[2] })
                                    line.startsWith("@formParam ") -> data.formParams.add(line.split(" ", limit = 3).let { it[1] to it[2] })
                                    line.startsWith("@result") -> data.result = line.split(" ")[1]
                                    else -> data.description.add(line)
                                }
                            }
                    println(data.description.joinToString(" "))
                    println(data.path)
                    println(data.pathParams)
                    println(data.queryParams)
                    println(data.formParams)
                    println(data.result)
                }
    }

}
