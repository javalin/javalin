package io.javalin.core.config

class RoutingConfig {
    @JvmField
    var ignoreTrailingSlashes = true

    @JvmField
    var treatMultipleSlashesAsSingleSlash = false
}
