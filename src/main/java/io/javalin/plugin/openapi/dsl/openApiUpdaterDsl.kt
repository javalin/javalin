/**
 * This file contains helper methods to update openapi items without overriding the original
 */
package io.javalin.plugin.openapi.dsl

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.PathItem
import io.swagger.v3.oas.models.Paths
import io.swagger.v3.oas.models.media.Content
import io.swagger.v3.oas.models.parameters.RequestBody
import io.swagger.v3.oas.models.responses.ApiResponse

internal fun OpenAPI.updateComponents(apply: Components.() -> Unit) {
    components = components ?: Components()
    components.apply(apply)
}

internal fun OpenAPI.updatePaths(apply: Paths.() -> Unit) {
    paths = paths ?: Paths()
    paths.apply(apply)
}

internal fun Paths.updatePath(name: String, apply: PathItem.() -> Unit) {
    if (get(name) == null) {
        addPathItem(name, PathItem())
    }
    get(name)!!.apply(apply)
}

internal fun RequestBody.updateContent(apply: Content.() -> Unit) {
    content = content ?: Content()
    content.apply(apply)
}

internal fun ApiResponse.updateContent(apply: Content.() -> Unit) {
    content = content ?: Content()
    content.apply(apply)
}
