package io.javalin.plugin.openapi

import io.javalin.core.security.Role

class OpenApiOptions @JvmOverloads constructor(
        val path: String? = null,
        val roles: Set<Role> = setOf(),
        val createBaseConfiguration: CreateBaseConfiguration
)
