@file:Suppress("UNCHECKED_CAST")

package io.javalin.component

import java.util.IdentityHashMap

class ComponentNotFoundException(clazz: Class<*>) : IllegalStateException("Component ${clazz.simpleName} not found")
class ComponentAlreadyRegisteredException(component: Component) : IllegalStateException("Component ${component.javaClass.simpleName} already registered")

interface Component // marker interface

class ComponentManager {

    private val componentResolvers = IdentityHashMap<Class<*>, Component>()

    fun register(component: Component, key: Class<*> = component::class.java) {
        if (componentResolvers.containsKey(component::class.java)) {
            throw ComponentAlreadyRegisteredException(component)
        }
        componentResolvers[key] = component
    }

    fun registerIfAbsent(component: Component, key: Class<*> = component::class.java) {
        componentResolvers.putIfAbsent(key, component)
    }

    fun <T> get(clazz: Class<T>): T =
        componentResolvers[clazz] as T? ?: throw ComponentNotFoundException(clazz)

    inline fun <reified T> get(): T = get(T::class.java)

}
