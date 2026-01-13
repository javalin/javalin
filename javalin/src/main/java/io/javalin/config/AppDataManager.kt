package io.javalin.config

data class Key<T>(val id: String)

class KeyAlreadyExistsException(key: Key<*>) : IllegalStateException("Key '$key' already exists")
class NoValueForKeyException(key: Key<*>) : IllegalStateException("No value for key '$key'")

class AppDataManager {

    private val data: MutableMap<Key<*>, Any?> = mutableMapOf()

    fun <T> register(key: Key<T>, value: T) {
        if (data.containsKey(key)) {
            throw KeyAlreadyExistsException(key)
        }
        data[key] = value
    }

    fun <T> registerIfAbsent(key: Key<T>, value: T) {
        data.putIfAbsent(key, value)
    }

    fun <T> get(key: Key<T>): T {
        return data[key] as T? ?: throw NoValueForKeyException(key)
    }

}
