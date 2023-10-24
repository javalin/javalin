
package io.javalin.util.mock

import jakarta.servlet.AsyncContext
import jakarta.servlet.AsyncEvent
import jakarta.servlet.AsyncListener
import jakarta.servlet.DispatcherType
import jakarta.servlet.ReadListener
import jakarta.servlet.RequestDispatcher
import jakarta.servlet.ServletContext
import jakarta.servlet.ServletInputStream
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import jakarta.servlet.http.HttpSession
import jakarta.servlet.http.HttpUpgradeHandler
import jakarta.servlet.http.Part
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.security.Principal
import java.util.Collections
import java.util.Enumeration
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@Suppress("MemberVisibilityCanBePrivate")
data class HttpServletRequestMock(
    val state: RequestState,
    val response: HttpServletResponseMock
) : HttpServletRequest {

    data class RequestState(
        @JvmField var protocol: String = "HTTP/1.1",
        @JvmField var scheme: String = "http",
        @JvmField var serverName: String = "localhost",
        @JvmField var serverPort: Int = 80,
        @JvmField var remoteAddr: String = "127.0.0.1",
        @JvmField var remoteHost: String = "localhost",
        @JvmField var secure: Boolean = false,
        @JvmField var remotePort: Int = 80,
        @JvmField var localName: String = "localhost",
        @JvmField var localAddr: String = "127.0.0.1",
        @JvmField var localPort: Int = -1,
        @JvmField var realPath: String = "",

        @JvmField var servletPath: String = "",
        @JvmField var requestURL: String = "",
        @JvmField var requestURI: String = "",
        @JvmField var pathInfo: String = "",
        @JvmField var pathTranslated: String = "",
        @JvmField var contextPath: String = "",
        @JvmField var queryString: String = "",
        @JvmField var parameters: MutableMap<String, Array<String>> = mutableMapOf(),

        @JvmField var method: String = "GET",
        @JvmField var headers: MutableMap<String, MutableList<String>> = mutableMapOf(),
        @JvmField var cookies: MutableList<Cookie> = mutableListOf(),
        @JvmField var locale: Locale = Locale.getDefault(),
        @JvmField var remoteUser: String = "",

        @JvmField var inputStream: InputStream = ByteArrayInputStream(ByteArray(0)),
        @JvmField var contentLength: Long = 0L,
        @JvmField var contentType: String? = null,
        @JvmField var characterEncodingValue: String = "UTF-8",
        @JvmField var parts: MutableList<Part> = mutableListOf(),

        @JvmField var attributes: MutableMap<String, Any> = mutableMapOf(),
        @JvmField var dispatcherType: DispatcherType = DispatcherType.REQUEST,
        @JvmField var requestDispatcher: RequestDispatcher? = null,
        @JvmField var servletContext: ServletContext? = null,
        @JvmField var asyncContext: AsyncContext? = null,

        @JvmField var authType: String? = null,
        @JvmField var roles: MutableList<String> = mutableListOf(),
        @JvmField var userPrincipal: Principal? = null,
        @JvmField var requestedSessionId: String? = null,
        @JvmField var session: HttpSession? = null,
    ) {
        val cachedInputStream by lazy { inputStream.buffered() }
    }

    override fun getAttribute(attribute: String?): Any? = state.attributes[attribute]
    override fun getAttributeNames(): Enumeration<String> = Collections.enumeration(state.attributes.keys)
    override fun setAttribute(key: String, value: Any) { state.attributes[key] = value }
    override fun removeAttribute(key: String) { state.attributes.remove(key) }

    override fun getCharacterEncoding(): String = state.characterEncodingValue
    override fun setCharacterEncoding(p0: String) { state.characterEncodingValue = p0 }

    override fun getContentLength(): Int = state.contentLength.toInt()
    override fun getContentLengthLong(): Long = state.contentLength
    override fun getContentType(): String? = state.contentType

    override fun getInputStream(): ServletInputStream = StubServletInputStream(state.cachedInputStream)
    override fun getReader(): BufferedReader = state.inputStream.bufferedReader()

    override fun getParameter(name: String?): String? = state.parameters[name]?.firstOrNull()
    override fun getParameterNames(): Enumeration<String> = Collections.enumeration(state.parameters.keys)
    override fun getParameterValues(name: String?): Array<String>? = state.parameters[name]
    override fun getParameterMap(): Map<String, Array<String>> = state.parameters

    override fun getProtocol(): String = state.protocol
    override fun getScheme(): String = state.scheme
    override fun getServerName(): String = state.serverName
    override fun getServerPort(): Int = state.serverPort
    override fun getRemoteAddr(): String = state.remoteAddr
    override fun getRemoteHost(): String = state.remoteHost
    override fun getLocale(): Locale = state.locale
    override fun getLocales(): Enumeration<Locale> = Collections.enumeration(listOf(state.locale))
    override fun isSecure(): Boolean = state.secure
    @Deprecated("Deprecated")
    override fun getRealPath(p0: String?): String? = state.realPath
    override fun getRemotePort(): Int = state.remotePort
    override fun getLocalName(): String = state.localName
    override fun getLocalAddr(): String = state.localAddr
    override fun getLocalPort(): Int = state.localPort

    override fun getServletContext(): ServletContext = state.servletContext!!
    override fun getRequestDispatcher(p0: String?): RequestDispatcher? { return state.requestDispatcher }
    override fun startAsync(): AsyncContext {
        val asyncContext = AsyncContextMock(this, response)
        state.asyncContext = asyncContext
        return asyncContext
    }
    override fun startAsync(p0: ServletRequest?, p1: ServletResponse?): AsyncContext = startAsync()
    override fun isAsyncStarted(): Boolean = state.asyncContext != null
    override fun isAsyncSupported(): Boolean = true
    override fun getAsyncContext(): AsyncContext? = state.asyncContext
    override fun getDispatcherType(): DispatcherType = state.dispatcherType

    override fun getCookies(): Array<Cookie> = state.cookies.toTypedArray()
    override fun getHeader(header: String): String? = state.headers[header]?.firstOrNull()
    override fun getHeaders(header: String): Enumeration<String> = Collections.enumeration(state.headers[header]!!)
    override fun getHeaderNames(): Enumeration<String> = Collections.enumeration(state.headers.keys)
    override fun getIntHeader(header: String): Int = state.headers[header]!!.firstOrNull()?.toInt() ?: -1
    override fun getDateHeader(header: String): Long = state.headers[header]!!.firstOrNull()?.toLong() ?: -1

    override fun getMethod(): String = state.method
    override fun getPathInfo(): String = state.pathInfo
    override fun getPathTranslated(): String = state.pathTranslated
    override fun getContextPath(): String = state.contextPath
    override fun getQueryString(): String = state.queryString
    override fun getRequestURI(): String = state.requestURI
    override fun getRequestURL(): StringBuffer = StringBuffer(state.requestURL)
    override fun getServletPath(): String = state.servletPath

    override fun getAuthType(): String = state.authType!!
    override fun getRemoteUser(): String = state.remoteUser
    override fun isUserInRole(role: String?): Boolean = state.roles.contains(role)
    override fun getUserPrincipal(): Principal? = state.userPrincipal
    override fun getRequestedSessionId(): String? = state.requestedSessionId

    override fun getSession(p0: Boolean): HttpSession? = state.session
    override fun getSession(): HttpSession? = state.session
    override fun changeSessionId(): String? = throw UnsupportedOperationException("Not implemented")
    override fun isRequestedSessionIdValid(): Boolean = throw UnsupportedOperationException("Not implemented")
    override fun isRequestedSessionIdFromCookie(): Boolean = throw UnsupportedOperationException("Not implemented")
    override fun isRequestedSessionIdFromURL(): Boolean = throw UnsupportedOperationException("Not implemented")
    @Deprecated("Deprecated")
    override fun isRequestedSessionIdFromUrl(): Boolean = throw UnsupportedOperationException("Not implemented")

    override fun authenticate(p0: HttpServletResponse?): Boolean = throw UnsupportedOperationException("Not implemented")
    override fun login(p0: String?, p1: String?) { throw UnsupportedOperationException("Not implemented") }
    override fun logout() { throw UnsupportedOperationException("Not implemented") }

    override fun getParts(): MutableCollection<Part> = state.parts
    override fun getPart(p0: String?): Part = parts.first { it.name == p0 }

    override fun <T : HttpUpgradeHandler?> upgrade(p0: Class<T>?): T { throw UnsupportedOperationException("Not implemented") }
}

private class AsyncContextMock(
    private val request: HttpServletRequestMock,
    private val response: HttpServletResponseMock,
    private val scheduler: ExecutorService = Executors.newSingleThreadExecutor()
) : AsyncContext {
    override fun getRequest(): ServletRequest = request
    override fun getResponse(): ServletResponse = response
    override fun hasOriginalRequestAndResponse(): Boolean = true
    override fun dispatch() { throw NotImplementedError("dispatch()") }
    override fun dispatch(path: String) { throw NotImplementedError("dispatch(path)") }
    override fun dispatch(context: ServletContext, path: String) { throw NotImplementedError("dispatch(context, path)") }
    override fun complete() { listeners.forEach { it.onComplete(AsyncEvent(this)) } }
    override fun start(run: Runnable) {
        scheduler.submit {
            run.run()
            listeners.forEach { it.onComplete(AsyncEvent(this)) }
        }
    }
    private var listeners = mutableListOf<AsyncListener>()
    override fun addListener(listener: AsyncListener) { listeners.add(listener) }
    override fun addListener(listener: AsyncListener, servletRequest: ServletRequest, servletResponse: ServletResponse) { addListener(listener) }
    override fun <T : AsyncListener?> createListener(clazz: Class<T>): T = clazz.getConstructor().newInstance()
    private var timeout: Long = 0
    override fun setTimeout(timeout: Long) { this.timeout = timeout; }
    override fun getTimeout(): Long = timeout
}

class StubServletInputStream(private val inputStream: InputStream) : ServletInputStream() {
    override fun read(): Int = inputStream.read()
    override fun isFinished(): Boolean = inputStream.available() == 0
    override fun isReady(): Boolean = inputStream.available() > 0
    override fun setReadListener(listener: ReadListener?) = throw UnsupportedOperationException("Not implemented")
}