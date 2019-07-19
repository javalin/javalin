package io.javalin.core.compression

import org.eclipse.jetty.http.HttpHeader
import org.eclipse.jetty.server.HttpOutput
import org.eclipse.jetty.server.Request
import org.eclipse.jetty.server.handler.gzip.GzipHandler
import org.eclipse.jetty.util.log.Log

import javax.servlet.ServletException
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import java.io.IOException

/**
 * Majority of handle() code is copied from GzipHandler, from which this class also inherits
 *
 * The idea is to keep things as close as possible to Jetty's own behaviour while adding support for other
 * compression formats (Just Brotli for now)
 */
class StaticCompressionHandler : GzipHandler() {
    var compressionStrategy = CompressionStrategy.NONE
        set(compressionStrategy) {
            val gzipLevel = if (compressionStrategy.gzip != null) compressionStrategy.gzip.level else -1
            this.compressionLevel = gzipLevel
            this.brotliLevel = if (compressionStrategy.brotli != null) compressionStrategy.brotli.level else -1
            field = compressionStrategy
        }
    private var brotliLevel = -1

    @Throws(IOException::class, ServletException::class)
    override fun handle(target: String, baseRequest: Request, request: HttpServletRequest, response: HttpServletResponse) {
        when {
            shouldDoBrotli(baseRequest) -> { //Do Brotli
                handleBrotli(target, baseRequest, request, response)
            }
            shouldDoGzip(baseRequest) -> { //Do Gzip, using the original GzipHandler
                super.handle(target, baseRequest, request, response)
            }
            else -> { //No Compression, call the ResourceHandler immediately
                _handler.handle(target, baseRequest, request, response)
                return
            }
        }
    }

    @Throws(IOException::class, ServletException::class)
    private fun handleBrotli(target: String, baseRequest: Request, request: HttpServletRequest, response: HttpServletResponse) {
        val context = baseRequest.servletContext
        val out = baseRequest.response.httpOutput
        LOG.debug("{} handle {} in {}", this, baseRequest, context)

        var interceptor: HttpOutput.Interceptor? = out.interceptor
        while (interceptor != null) { // Are we already being compressed?
            if (interceptor is BrotliHttpOutputInterceptor) {
                LOG.debug("{} already intercepting {}", this, request)
                _handler.handle(target, baseRequest, request, response)
                return
            }
            interceptor = interceptor.nextInterceptor
        }

        val originalInterceptor = out.interceptor
        try { // install interceptor and handle
            out.interceptor = BrotliHttpOutputInterceptor(this, varyField, baseRequest.httpChannel, originalInterceptor, isSyncFlush)
            _handler.handle(target, baseRequest, request, response)
        } finally { // reset interceptor if request not handled
            if (!baseRequest.isHandled && !baseRequest.isAsyncStarted) {
                out.interceptor = originalInterceptor
            }
        }
    }

    private fun shouldDoBrotli(baseReq: Request) =
        baseReq.httpFields.getField(HttpHeader.ACCEPT_ENCODING).contains("br") && this.compressionStrategy.brotli != null


    private fun shouldDoGzip(baseReq: Request) =
        baseReq.httpFields.getField(HttpHeader.ACCEPT_ENCODING).contains("gzip") && this.compressionStrategy.gzip != null

    companion object {
        private val LOG = Log.getLogger(StaticCompressionHandler::class.java)
    }
}
