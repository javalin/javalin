/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.json

import com.fasterxml.jackson.databind.Module
import com.fasterxml.jackson.databind.ObjectMapper
import io.javalin.util.CoreDependency
import io.javalin.util.Util
import java.io.InputStream
import java.lang.reflect.Type

open class JacksonJsonMapper(private var objectMapper: ObjectMapper = defaultMapper()) : JsonMapper {

    override fun toJsonString(obj: Any, type: Type): String = when (obj) {
        is String -> obj // the default mapper treats strings as if they are already JSON
        else -> objectMapper.writeValueAsString(obj) // convert object to JSON
    }

    override fun toJsonStream(obj: Any, type: Type): InputStream = when (obj) {
        is String -> obj.byteInputStream() // the default mapper treats strings as if they are already JSON
        else -> PipedStreamUtil.getInputStream { objectMapper.factory.createGenerator(it).writeObject(obj) }
    }

    override fun <T : Any> fromJsonString(json: String, targetType: Type): T =
        objectMapper.readValue(json, objectMapper.typeFactory.constructType(targetType))

    override fun <T : Any> fromJsonStream(json: InputStream, targetType: Type): T =
        objectMapper.readValue(json, objectMapper.typeFactory.constructType(targetType))

}

fun defaultMapper(): ObjectMapper = ObjectMapper()
    .registerOptionalModule(CoreDependency.JACKSON_KT.testClass)
    .registerOptionalModule(CoreDependency.JACKSON_JSR_310.testClass)
    .registerOptionalModule(CoreDependency.JACKSON_KTORM.testClass) // very optional module for ktorm (a kotlin orm)

private fun ObjectMapper.registerOptionalModule(classString: String): ObjectMapper = also {
    if (Util.classExists(classString)) {
        registerModule(Class.forName(classString).getConstructor().newInstance() as Module)
    }
}
