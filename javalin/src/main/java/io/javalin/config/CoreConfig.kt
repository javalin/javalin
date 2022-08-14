package io.javalin.config

import io.javalin.json.JSON_MAPPER_KEY
import io.javalin.json.JsonMapper
import io.javalin.security.AccessManager
import java.util.function.Consumer

class CoreConfig(private val pvt: PrivateConfig) {

    var showJavalinBanner = true

    var contextResolver = ContextResolver()

    fun accessManager(accessManager: AccessManager) {
        pvt.accessManager = accessManager
    }

    fun jsonMapper(jsonMapper: JsonMapper) {
        pvt.appAttributes[JSON_MAPPER_KEY] = jsonMapper
    }

}
