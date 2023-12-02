@file:Suppress("DEPRECATION")

package io.javalin.component

import io.javalin.http.Context
import java.util.function.Consumer
import kotlin.reflect.KClass

class ComponentManager {

    private val resolvers: MutableMap<ComponentHandle<*>, ComponentResolver<*>> = mutableMapOf()
    private val classHandles: MutableMap<KClass<*>, ComponentHandle<*>> = mutableMapOf()

    // For java
    fun <COMPONENT: Any> register(klass: Class<COMPONENT>, component: COMPONENT, skipIfExists: Boolean = false) {
        registerResolver(klass, { component }, skipIfExists)
    }

    fun <COMPONENT: Any> registerResolver(klass: Class<COMPONENT>, resolver: ComponentResolver<COMPONENT>, skipIfExists: Boolean = false) {
        registerResolver(klass.kotlin, resolver, skipIfExists)
    }

    // For kotlin
    fun <COMPONENT: Any> register(klass: KClass<COMPONENT>, component: COMPONENT, skipIfExists: Boolean = false) {
        registerResolver(klass, { component }, skipIfExists)
    }

    fun <COMPONENT: Any> registerResolver(klass: KClass<COMPONENT>, resolver: ComponentResolver<COMPONENT>, skipIfExists: Boolean = false) {
        if (classHandles.containsKey(klass)) {
            if(skipIfExists) {
                return
            }
            throw ComponentAlreadyExistsException(ComponentHandle<COMPONENT>())
        }

        val handle = ComponentHandle<COMPONENT>()
        classHandles[klass] = handle
        registerResolver(handle, resolver)
    }

    fun <COMPONENT> register(handle: ComponentHandle<COMPONENT>, component: COMPONENT, skipIfExists: Boolean = false) {
        registerResolver(handle, { component }, skipIfExists)
    }

    fun <COMPONENT> registerResolver(handle: ComponentHandle<COMPONENT>, resolver: ComponentResolver<COMPONENT>, skipIfExists: Boolean = false) {
        if (resolvers.containsKey(handle)) {
            if(skipIfExists) {
                return
            }
            throw ComponentAlreadyExistsException(handle)
        }

        resolvers[handle] = resolver
    }

    fun <COMPONENT : Any> resolve(klass: Class<COMPONENT>, ctx: Context?): COMPONENT =
        resolve(klass.kotlin, ctx)

    fun <COMPONENT : Any> resolve(klass: KClass<COMPONENT>, ctx: Context?): COMPONENT =
        classHandles[klass]
            ?.let {
                @Suppress("UNCHECKED_CAST")
                it as ComponentHandle<COMPONENT>
            }
            ?.let { resolve(it, ctx) }
            ?: throw ComponentNotFoundException(ComponentHandle<COMPONENT>())

    fun <COMPONENT> resolve(handle: ComponentHandle<COMPONENT>, ctx: Context?): COMPONENT =
        resolvers[handle]
            ?.let {
                @Suppress("UNCHECKED_CAST")
                it as ComponentResolver<COMPONENT>
            }
            ?.resolve(ctx)
            ?: throw ComponentNotFoundException(handle)

}
