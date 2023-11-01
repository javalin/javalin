
package io.javalin.util.mock.stubs

import jakarta.servlet.ServletContext
import jakarta.servlet.http.HttpSession
import jakarta.servlet.http.HttpSessionContext
import java.util.Collections
import java.util.Enumeration
import java.util.UUID

@Suppress("DeprecatedCallableAddReplaceWith", "MemberVisibilityCanBePrivate")
class HttpSessionMock(val state: HttpSessionState = HttpSessionState()) : HttpSession {

    data class HttpSessionState(
        var creationTime: Long = System.currentTimeMillis(),
        var id: String = "mock-session-${UUID.randomUUID()}",
        var lastAccessedTime: Long = System.currentTimeMillis(),
        var maxInactiveInterval: Int = 0,
        var attributes: MutableMap<String, Any?> = mutableMapOf(),
        var invalidated: Boolean = false,
        var new: Boolean = false
    )

    override fun getCreationTime(): Long = state.creationTime
    override fun getId(): String = state.id
    override fun getLastAccessedTime(): Long = state.lastAccessedTime

    override fun setMaxInactiveInterval(interval: Int) { state.maxInactiveInterval = interval }
    override fun getMaxInactiveInterval(): Int = state.maxInactiveInterval

    override fun getAttribute(name: String?): Any? = state.attributes[name]
    override fun getAttributeNames(): Enumeration<String> = Collections.enumeration(state.attributes.keys)
    override fun setAttribute(name: String, value: Any?) { state.attributes[name] = value!! }
    override fun removeAttribute(name: String?) { state.attributes.remove(name) }

    /* Deprecated */
    @Deprecated("Deprecated in Java")
    override fun getValue(name: String?): Any? = state.attributes[name]
    @Deprecated("Deprecated in Java")
    override fun getValueNames(): Array<String> = state.attributes.keys.toTypedArray()
    @Deprecated("Deprecated in Java")
    override fun putValue(name: String, value: Any?) { state.attributes[name] = value }
    @Deprecated("Deprecated in Java")
    override fun removeValue(name: String?) { state.attributes.remove(name) }

    @Deprecated("Deprecated in Java")
    override fun getSessionContext(): HttpSessionContext = throw UnsupportedOperationException()
    override fun getServletContext(): ServletContext = throw UnsupportedOperationException()

    override fun invalidate() {
        if (state.invalidated) throw IllegalStateException("Session already invalidated")
        state.invalidated = true
    }

    override fun isNew(): Boolean {
        if (state.invalidated) throw IllegalStateException("Session already invalidated")
        return state.new
    }

}
