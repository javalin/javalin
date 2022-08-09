package io.javalin.config

import io.javalin.json.JSON_MAPPER_KEY
import io.javalin.json.JsonMapper
import io.javalin.security.AccessManager
import java.util.function.Consumer

class CoreConfig(private val pvt: PrivateConfig) {

    var showJavalinBanner = true

    fun contextResolvers(userResolver: Consumer<ContextResolver>) {
        val finalResolver = ContextResolver()
        userResolver.accept(finalResolver)
        pvt.appAttributes[CONTEXT_RESOLVER_KEY] = finalResolver
    }

    fun accessManager(accessManager: AccessManager) {
        pvt.accessManager = accessManager
    }

    fun jsonMapper(jsonMapper: JsonMapper) {
        pvt.appAttributes[JSON_MAPPER_KEY] = jsonMapper
    }

}
