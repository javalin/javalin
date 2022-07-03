package io.javalin.core.config

class RoutingConfig {

    @JvmField
    var contextPath = "/"

    @JvmField
    var ignoreTrailingSlashes = true

    @JvmField
    var treatMultipleSlashesAsSingleSlash = false

}
