package io.javalin.cookie

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.util.*
import javax.servlet.http.Cookie


class CookieStore(cookie: String?) {

    companion object {
        const val COOKIE_NAME = "javalin-cookie-store"
    }

    private val cookieMap = deserialize(cookie)

    fun serializeToCookie() = Cookie(COOKIE_NAME, serialize(cookieMap))

    @Suppress("UNCHECKED_CAST")
    operator fun <T> get(key: String) = cookieMap[key] as T

    operator fun set(key: String, value: Any) = cookieMap.put(key, value)

    fun clear() = cookieMap.clear()

    @Suppress("UNCHECKED_CAST")
    private fun deserialize(cookie: String?) = if (!cookie.isNullOrEmpty()) {
        ObjectInputStream(ByteArrayInputStream(
                Base64.getDecoder().decode(cookie)
        )).readObject() as MutableMap<String, Any>
    } else mutableMapOf()

    private fun serialize(map: MutableMap<String, Any>): String {
        val byteArrayOutputStream = ByteArrayOutputStream()
        ObjectOutputStream(byteArrayOutputStream).writeObject(map)
        return Base64.getEncoder().encodeToString(byteArrayOutputStream.toByteArray())
    }
}
