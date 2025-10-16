package io.javalin.mock.servlet

import jakarta.servlet.ServletOutputStream
import jakarta.servlet.WriteListener
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletResponse
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.io.PrintWriter
import java.net.URLEncoder
import java.util.*

// @formatter:off
@Suppress("MemberVisibilityCanBePrivate")
data class HttpServletResponseMock(val state: ResponseState = ResponseState()) : HttpServletResponse {

    data class ResponseState(
        @JvmField var characterEncoding: String = "UTF-8",
        @JvmField var contentType: String? = null,
        @JvmField var outputStream: ByteArrayOutputStream = ByteArrayOutputStream(),
        @JvmField var contentLength: Long = 0L,
        @JvmField var bufferSize: Int = 0,
        @JvmField var locale: Locale? = null,
        @JvmField var headers: MutableMap<String, String> = mutableMapOf(),
        @JvmField var error: Error? = null,
        @JvmField var redirectUrl: String? = null,
        @JvmField var status: Int = 200,
    ) {
        val servletOutputStream by lazy { ServletOutputStreamImpl(outputStream) }
    }

    override fun getCharacterEncoding(): String = state.characterEncoding
    override fun setCharacterEncoding(encoding: String?) { state.characterEncoding = encoding!! }
    override fun getContentType(): String? = state.contentType
    override fun setContentType(contentType: String?) { state.contentType = contentType }
    override fun setContentLength(contentLength: Int) { state.contentLength = contentLength.toLong() }
    override fun setContentLengthLong(contentLength: Long) { state.contentLength = contentLength }

    override fun getOutputStream(): ServletOutputStream = state.servletOutputStream
    override fun getWriter(): PrintWriter = PrintWriter(state.servletOutputStream)
    override fun setBufferSize(bufferSize: Int) { state.bufferSize = bufferSize }
    override fun getBufferSize(): Int = state.bufferSize
    override fun flushBuffer() {}
    override fun resetBuffer() {}
    override fun isCommitted(): Boolean = true
    override fun reset() {}

    override fun setLocale(locale: Locale?) { state.locale = locale!! }
    override fun getLocale(): Locale? = state.locale

    @JvmField var cookies = mutableListOf<Cookie>()
    override fun addCookie(p0: Cookie?) { cookies.add(p0!!) }

    override fun containsHeader(header: String): Boolean = state.headers.containsKey(header)
    override fun setDateHeader(header: String, value: Long) { state.headers[header] = value.toString() }
    override fun addDateHeader(header: String, value: Long) { state.headers[header] = value.toString() }
    override fun setHeader(header: String, value: String?) { state.headers[header] = value!! }
    override fun addHeader(header: String, value: String?) { state.headers[header] = value!! }
    override fun setIntHeader(header: String, value: Int) { state.headers[header] = value.toString() }
    override fun addIntHeader(header: String, value: Int) { state.headers[header] = value.toString() }
    override fun getHeader(header: String): String? = state.headers[header]
    override fun getHeaders(header: String): MutableCollection<String> = mutableListOf(state.headers[header]!!)
    override fun getHeaderNames(): MutableCollection<String> = state.headers.keys

    override fun encodeURL(url: String?): String = URLEncoder.encode(url, state.characterEncoding)
    override fun encodeRedirectURL(url: String?): String = URLEncoder.encode(url, state.characterEncoding)
    @Deprecated("Deprecated", ReplaceWith("URLEncoder.encode(url, state.characterEncoding)", "java.net.URLEncoder"))
    override fun encodeUrl(url: String?): String = URLEncoder.encode(url, state.characterEncoding)
    @Deprecated("Deprecated", ReplaceWith("URLEncoder.encode(p0, state.characterEncoding)", "java.net.URLEncoder"))
    override fun encodeRedirectUrl(url: String?): String = URLEncoder.encode(url, state.characterEncoding)

    data class Error(val code: Int, val message: String?)
    override fun sendError(status: Int, message: String?) { state.error = Error(status, message) }
    override fun sendError(status: Int) { state.error = Error(status, null) }
    override fun sendRedirect(url: String?) { state.redirectUrl = url }
    override fun setStatus(status: Int) { state.status = status }
    @Deprecated("Deprecated", ReplaceWith("throw UnsupportedOperationException()"))
    override fun setStatus(status: Int, message: String?) { throw UnsupportedOperationException() }
    override fun getStatus(): Int = state.status

}

class ServletOutputStreamImpl(private val outputStream: OutputStream) : ServletOutputStream() {
    private var writeListener: WriteListener? = null
    override fun isReady(): Boolean = true
    override fun setWriteListener(listener: WriteListener?) {
        this.writeListener = listener
        listener?.onWritePossible()
    }
    override fun write(data: Int) { outputStream.write(data) }
}
// @formatter:on
