package io.javalin.core.config

import io.javalin.core.security.AccessManager
import io.javalin.http.CONTEXT_RESOLVER_KEY
import io.javalin.http.ContextResolver
import io.javalin.plugin.json.JSON_MAPPER_KEY
import io.javalin.plugin.json.JsonMapper
import java.util.function.Consumer

class CoreConfig(private val pvt: PrivateConfig) {

    // abbreviated `cfg` in source
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
