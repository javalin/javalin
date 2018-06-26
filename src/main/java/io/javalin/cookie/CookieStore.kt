package io.javalin.cookie

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.util.*
import javax.servlet.http.Cookie


class CookieStore(cookieValue: String?) {

    companion object {
        const val NAME = "javalin-cookie-store"
    }

    private val map = stringToMap(cookieValue)

    fun cookie() = Cookie(NAME, mapToString(map))

    @Suppress("UNCHECKED_CAST")
    operator fun <T> get(key: String) = map[key] as T

    operator fun set(key: String, value: Any) {
        map[key] = value
    }

    fun clear() {
        map.clear()
    }

    @Suppress("UNCHECKED_CAST")
    private fun stringToMap(cookieValue: String?): MutableMap<String, Any> {
        if (!cookieValue.isNullOrEmpty()) {
            return ObjectInputStream(
                    ByteArrayInputStream(
                            Base64.getDecoder().decode(cookieValue)
                    )
            ).readObject() as MutableMap<String, Any>
        }

        return mutableMapOf()
    }

    private fun mapToString(map: MutableMap<String, Any>): String {
        val byteArrayOutputStream = ByteArrayOutputStream()
        ObjectOutputStream(byteArrayOutputStream).writeObject(map)
        return Base64.getEncoder().encodeToString(byteArrayOutputStream.toByteArray())
    }
}
