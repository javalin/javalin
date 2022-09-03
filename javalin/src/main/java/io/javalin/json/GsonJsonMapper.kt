package io.javalin.json

import com.google.gson.Gson
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.lang.reflect.Type

open class GsonJsonMapper(private val gson: Gson = Gson()) : JsonMapper {

    override fun toJsonString(obj: Any, type: Type): String = when (obj) {
        is String -> obj
        else -> gson.toJson(obj, type)
    }

    override fun toJsonStream(obj: Any, type: Type): InputStream = when {
        obj is String -> obj.byteInputStream()
        else -> PipedStreamUtil.getInputStream { gson.toJson(obj, type, OutputStreamWriter(it)) }
    }

    override fun <T : Any> fromJsonString(json: String, targetType: Type): T =
        gson.fromJson(json, targetType)

    override fun <T : Any> fromJsonStream(json: InputStream, targetType: Type): T =
        gson.fromJson(InputStreamReader(json), targetType)

}
