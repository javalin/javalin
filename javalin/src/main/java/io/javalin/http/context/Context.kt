package io.javalin.http.context

import io.javalin.http.Context

/**
 * Maps a JSON body to a Java/Kotlin class using JavalinJson.
 * JavalinJson can be configured to use any mapping library.
 * @return The mapped object
 */
inline fun <reified T : Any> Context.body(): T = bodyAsClass(T::class.java)

/** Reified version of [header] (Kotlin only) */
inline fun <reified T : Any> Context.header(header: String) = header(header, T::class.java)

/** Reified version of [formParam] (Kotlin only) */
inline fun <reified T : Any> Context.formParam(key: String, default: String? = null) = formParam(key, T::class.java, default)

/** Reified version of [pathParam] (Kotlin only) */
inline fun <reified T : Any> Context.pathParam(key: String) = pathParam(key, T::class.java)

/** Reified version of [queryParam] (Kotlin only) */
inline fun <reified T : Any> Context.queryParam(key: String, default: String? = null) = queryParam(key, T::class.java, default)
