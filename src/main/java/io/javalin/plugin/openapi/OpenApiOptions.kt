package io.javalin.plugin.openapi

import io.javalin.core.security.Role
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info

class OpenApiOptions @JvmOverloads constructor(
        /** Creates a GET route to get the schema as a json if not null */
        val path: String? = null,
        val roles: Set<Role> = setOf(),
        val createBaseConfiguration: CreateBaseConfiguration
) {
    constructor(
            createBaseConfiguration: () -> OpenAPI
    ) : this(null, setOf(), CreateBaseConfiguration(createBaseConfiguration))

    constructor(
            path: String? = null,
            createBaseConfiguration: () -> OpenAPI
    ) : this(path, setOf(), CreateBaseConfiguration(createBaseConfiguration))


    constructor(
            path: String? = null,
            roles: Set<Role> = setOf(),
            createBaseConfiguration: () -> OpenAPI
    ) : this(path, roles, CreateBaseConfiguration(createBaseConfiguration))

    @JvmOverloads constructor(
            path: String? = null,
            info: Info
    ) : this(path, setOf(), {
        OpenAPI().info(info)
    })

    constructor(
            path: String? = null,
            roles: Set<Role> = setOf(),
            info: Info
    ) : this(path, roles, {
        OpenAPI().info(info)
    })
}
