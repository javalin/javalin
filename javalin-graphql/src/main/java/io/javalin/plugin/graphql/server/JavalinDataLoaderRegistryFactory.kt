package io.javalin.plugin.graphql.server

import com.expediagroup.graphql.server.execution.DataLoaderRegistryFactory
import com.expediagroup.graphql.server.execution.KotlinDataLoader
import org.dataloader.DataLoaderRegistry

class JavalinDataLoaderRegistryFactory(private val dataLoaders: List<KotlinDataLoader<*, *>>): DataLoaderRegistryFactory {
    override fun generate(): DataLoaderRegistry {
        val registry = DataLoaderRegistry()
        dataLoaders.forEach { registry.register(it.dataLoaderName, it.getDataLoader()) }
        return registry
    }

}
