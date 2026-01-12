/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 *
 * Test-only copy of JavalinGson for core module tests.
 * The real implementation is in javalin-gson module.
 */

package io.javalin.json

import com.google.gson.Gson
import io.javalin.util.ConcurrencyUtil
import io.javalin.util.javalinLazy
import java.io.BufferedWriter
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.lang.reflect.Type
import java.util.stream.Stream

open class JavalinGson(
    private val gson: Gson = Gson(),
    private val useVirtualThreads: Boolean = false,
) : JsonMapper {

    private val executorService by javalinLazy { ConcurrencyUtil.executorService("JavalinJsonThreadPool", useVirtualThreads) }

    override fun toJsonString(obj: Any, type: Type): String = when (obj) {
        is String -> obj
        else -> gson.toJson(obj, type)
    }

    override fun toJsonStream(obj: Any, type: Type): InputStream = when (obj) {
        is String -> obj.byteInputStream()
        else -> getInputStream { pipedOutputStream ->
            BufferedWriter(OutputStreamWriter(pipedOutputStream)).use { bufferedWriter ->
                gson.toJson(obj, type, bufferedWriter)
            }
        }
    }

    override fun writeToOutputStream(stream: Stream<*>, outputStream: OutputStream) {
        BufferedWriter(OutputStreamWriter(outputStream)).use { bufferedWriter ->
            var hasComma = false
            bufferedWriter.write("[")
            stream.forEach {
                if (hasComma) bufferedWriter.write(",") else hasComma = true
                gson.toJson(it, bufferedWriter)
            }
            bufferedWriter.write("]")
        }
    }

    override fun <T : Any> fromJsonString(json: String, targetType: Type): T =
        gson.fromJson(json, targetType)

    override fun <T : Any> fromJsonStream(json: InputStream, targetType: Type): T =
        gson.fromJson(InputStreamReader(json), targetType)

    private fun getInputStream(userCallback: (PipedOutputStream) -> Unit): InputStream {
        val pipedOutputStream = PipedOutputStream()
        val pipedInputStream = object : PipedInputStream(pipedOutputStream) {
            var exception: Exception? = null
            override fun close() = exception?.let { throw it } ?: super.close()
        }
        executorService.execute {
            try {
                userCallback(pipedOutputStream)
            } catch (userException: Exception) {
                pipedInputStream.exception = userException
            } finally {
                pipedOutputStream.close()
            }
        }
        return pipedInputStream
    }
}

