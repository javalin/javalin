package io.javalin.json

import com.google.gson.Gson
import io.javalin.http.InternalServerErrorResponse
import io.javalin.util.CoreDependency
import io.javalin.util.DependencyUtil
import io.javalin.util.JavalinLogger
import io.javalin.util.Util
import java.io.*
import java.lang.reflect.Type
import java.util.stream.Stream

open class JavalinGson(private val gson: Gson = Gson()) : JsonMapper {

    init {
        if (!Util.classExists(CoreDependency.GSON.testClass)) {
            val message =
                """|It looks like you don't have Gson dependency on classpath.
                   |The easiest way to fix this is to simply add the '${CoreDependency.GSON.artifactId}' dependency:
                   |
                   |${DependencyUtil.mavenAndGradleSnippets(CoreDependency.GSON)}
                   |""".trimMargin()
            JavalinLogger.warn(DependencyUtil.wrapInSeparators(message))
            throw InternalServerErrorResponse(message)
        }
    }

    override fun toJsonString(obj: Any, type: Type): String = when (obj) {
        is String -> obj
        else -> gson.toJson(obj, type)
    }

    override fun toJsonStream(obj: Any, type: Type): InputStream = when (obj) {
        is String -> obj.byteInputStream()
        else -> PipedStreamUtil.getInputStream { pipedOutputStream ->
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

}
