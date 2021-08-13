package io.javalin

/** Base representation of context and shared between them properties **/
interface BaseContext {

    /** Gets request path (uri) */
    fun uri(): String

    /** Gets map with all headers **/
    fun headerMap(): Map<String, String>

    /** Gets detected ip of client **/
    fun ip(): String

}
