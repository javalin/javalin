package io.javalin.json

import io.avaje.jsonb.Jsonb
import io.javalin.http.InternalServerErrorResponse
import io.javalin.util.CoreDependency
import io.javalin.util.DependencyUtil
import io.javalin.util.JavalinLogger
import io.javalin.util.Util
import io.javalin.util.javalinLazy
import java.io.InputStream
import java.io.OutputStream
import java.lang.reflect.Type
import java.util.stream.Stream

class JavalinJsonb(
    private var jsonb: Jsonb = Jsonb.instance(),
    private val useVirtualThreads: Boolean = false,
) : JsonMapper {

    private val pipedStreamExecutor: PipedStreamExecutor by javalinLazy { PipedStreamExecutor(useVirtualThreads) }

    init {
        if (!Util.classExists(CoreDependency.AVAJE_JSONB.testClass)) {
            val message =
                """|It looks like you don't have avaje-jsonb dependency on classpath.
                   |The easiest way to fix this is to simply add the '${CoreDependency.AVAJE_JSONB.artifactId}' dependency:
                   |
                   |${DependencyUtil.mavenAndGradleSnippets(CoreDependency.AVAJE_JSONB)}
                   |""".trimMargin()
            JavalinLogger.warn(DependencyUtil.wrapInSeparators(message))
            throw InternalServerErrorResponse(message)
        }
    }

    override fun toJsonString(obj: Any, type: Type): String = when (obj) {
        is String -> obj
        else -> jsonb.type<Any>(type).toJson(obj)
    }

    override fun toJsonStream(obj: Any, type: Type): InputStream = when (obj) {
        is String -> obj.byteInputStream()
        else -> pipedStreamExecutor.getInputStream { pipedOutputStream ->
            jsonb.type<Any>(type).toJson(obj, pipedOutputStream)
        }
    }

    override fun writeToOutputStream(stream: Stream<*>, outputStream: OutputStream) {
        jsonb.writer(outputStream).use { writer ->
            writer.beginArray()
            stream.forEach {
                jsonb.toJson(it, writer)
            }
            writer.endArray()
        }
    }

    override fun <T : Any> fromJsonString(json: String, targetType: Type): T =
        jsonb.type<T>(targetType).fromJson(json)

    override fun <T : Any> fromJsonStream(json: InputStream, targetType: Type): T =
        jsonb.type<T>(targetType).fromJson(json)

}
