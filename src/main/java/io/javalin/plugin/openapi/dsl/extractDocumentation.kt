package io.javalin.plugin.openapi.dsl

import io.javalin.core.event.HandlerMetaInfo

fun HandlerMetaInfo.extractDocumentation(): OpenApiDocumentation? {
    return if (handler is DocumentedHandler) {
        handler.documentation
    } else {
        null
    }
}
