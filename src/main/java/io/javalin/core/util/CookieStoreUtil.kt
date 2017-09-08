/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.core.util

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.util.*

object CookieStoreUtil {

    const val name = "javalin-cookie-store"

    fun mapToString(cookieStore: MutableMap<String, Any>): String {
        val byteArrayOutputStream = ByteArrayOutputStream()
        ObjectOutputStream(byteArrayOutputStream).writeObject(cookieStore)
        return Base64.getEncoder().encodeToString(byteArrayOutputStream.toByteArray())
    }

    @Suppress("UNCHECKED_CAST")
    fun stringToMap(cookie: String?): MutableMap<String, Any> {
        if (cookie.isNullOrEmpty()) return mutableMapOf()
        return ObjectInputStream(
                ByteArrayInputStream(
                        Base64.getDecoder().decode(cookie)
                )
        ).readObject() as MutableMap<String, Any>
    }

}
