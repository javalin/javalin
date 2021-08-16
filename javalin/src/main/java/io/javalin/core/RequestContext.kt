package io.javalin.core

import io.javalin.core.validation.Validator

/** Base representation of context and shared between them properties */
interface RequestContext {

    /** Sets an attribute on the request. Attributes are available to other handlers in the request lifecycle */
    fun attribute(key: String, value: Any?)
    /** Gets the specified attribute from the request. */
    fun <T> attribute(key: String): T?
    /** Gets a map with all the attribute keys and values on the request. */
    fun attributeMap(): Map<String, Any?>

    /** Gets specified attribute from the user session, or null. */
    fun <T> sessionAttribute(key: String): T?
    /** Gets a map of all the attributes in the user session. */
    fun sessionAttributeMap(): Map<String, Any?>

    /** Gets a request cookie by name, or null. */
    fun cookie(name: String): String?
    /** Gets a map with all the cookie keys and values on the request. */
    fun cookieMap(): Map<String, String>

    /** Gets a request header by name, or null. */
    fun header(header: String): String?
    /** Gets map with all headers **/
    fun headerMap(): Map<String, String>

    /** * Gets a path param by name (ex: pathParam("param"). */
    fun pathParam(key: String): String
    /** Creates a typed [Validator] for the pathParam() value */
    fun <T> pathParamAsClass(key: String, clazz: Class<T>): Validator<T>
    /** Gets a map of all the [pathParamAsClass] keys and values. */
    fun pathParamMap(): Map<String, String>

    /** Gets the request query string, or null. */
    fun queryString(): String?
    /** Gets a map with all the query param keys and values. */
    fun queryParamMap(): Map<String, List<String>>
    /** Gets a list of query params for the specified key, or empty list. */
    fun queryParams(key: String): List<String>
    /** Gets a query param if it exists, else null */
    fun queryParam(key: String): String?
    /** Creates a typed [Validator] for the queryParam() value */
    fun <T> queryParamAsClass(key: String, clazz: Class<T>): Validator<T>

    /** Gets detected ip of client **/
    fun ip(): String
    /** Gets request path (uri) */
    fun uri(): String
    /** Gets the path that was used to match request (also includes before/after paths */
    fun matchedPath(): String

}
