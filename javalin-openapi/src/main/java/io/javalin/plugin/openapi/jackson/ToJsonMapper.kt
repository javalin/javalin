package io.javalin.plugin.openapi.jackson

@FunctionalInterface
interface ToJsonMapper {
    fun map(obj: Any): String
}
