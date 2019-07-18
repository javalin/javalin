package io.javalin.core.compression;

import io.javalin.core.JavalinConfig;
import org.eclipse.jetty.http.CompressedContentFormat;
import org.eclipse.jetty.http.HttpField;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.MimeTypes;
import org.eclipse.jetty.server.HttpOutput;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.gzip.GzipHandler;
import org.eclipse.jetty.util.URIUtil;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.jetbrains.annotations.NotNull;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.ListIterator;

/**
 * Majority of handle() code is copied from GzipHandler, from which this class also inherits
 *
 * The idea is to keep things as close as possible to Jetty's own behaviour while adding support for other
 * compression formats (Just Brotli for now)
 */
public class StaticCompressionHandler extends GzipHandler {

    private static final Logger LOG = Log.getLogger(StaticCompressionHandler.class);

    @NotNull private CompressionStrategy compressionStrategy = CompressionStrategy.NONE;
    private int brotliLevel = -1;

    public StaticCompressionHandler() {
        super();
    }

    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        if(shouldDoBrotli(baseRequest)) { //Do Brotli
            handleBrotli(target, baseRequest, request, response);
        } else if(shouldDoGzip(baseRequest)) { //Do Gzip, using the original GzipHandler
            super.handle(target, baseRequest, request, response);
        } else { //No Compression, call the ResourceHandler immediately
            _handler.handle(target,baseRequest, request, response);
            return;
        }
    }

    private void handleBrotli(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        ServletContext context = baseRequest.getServletContext();
        String path = context==null?baseRequest.getRequestURI(): URIUtil.addPaths(baseRequest.getServletPath(),baseRequest.getPathInfo());

        LOG.debug("{} handle {} in {}",this,baseRequest,context);

        if (!getDispatcherTypes().contains(baseRequest.getDispatcherType()))
        {
            LOG.debug("{} excluded by dispatcherType {}",this,baseRequest.getDispatcherType());
            _handler.handle(target,baseRequest, request, response);
            return;
        }

        // Are we already being compressed?
        HttpOutput out = baseRequest.getResponse().getHttpOutput();
        HttpOutput.Interceptor interceptor = out.getInterceptor();
        while (interceptor!=null)
        {
            if (interceptor instanceof BrotliHttpOutputInterceptor)
            {
                LOG.debug("{} already intercepting {}",this,request);
                _handler.handle(target,baseRequest, request, response);
                return;
            }
            interceptor=interceptor.getNextInterceptor();
        }

        // Special handling for etags
        for (ListIterator<HttpField> fields = baseRequest.getHttpFields().listIterator(); fields.hasNext();)
        {
            HttpField field = fields.next();
            if (field.getHeader()==HttpHeader.IF_NONE_MATCH || field.getHeader()==HttpHeader.IF_MATCH)
            {
                String etag = field.getValue();
                int i=etag.indexOf(CompressedContentFormat.BR._etagQuote);
                if(i>0)
                {
                    baseRequest.setAttribute("i.j.c.compression.StaticCompressionHandler.etag",etag);
                    while (i>=0)
                    {
                        etag=etag.substring(0,i)+etag.substring(i+CompressedContentFormat.BR._etag.length());
                        i=etag.indexOf(CompressedContentFormat.BR._etagQuote,i);
                    }

                    fields.set(new HttpField(field.getHeader(),etag));
                }
            }
        }

        // If not a supported method - no Vary because no matter what client, this URI is always excluded
        // Needed to change test code due to private access on _methods and no available getter
        //if (!_methods.test(baseRequest.getMethod()))
        if(Arrays.asList(getExcludedMethods()).contains(baseRequest.getMethod()))
        {
            LOG.debug("{} excluded by method {}",this,request);
            _handler.handle(target,baseRequest, request, response);
            return;
        }

        // If not a supported URI- no Vary because no matter what client, this URI is always excluded
        // Despite function name, this tests general compression eligibility, not just gzip
        if (!isPathGzipable(path))
        {
            LOG.debug("{} excluded by path {}",this,request);
            _handler.handle(target,baseRequest, request, response);
            return;
        }

        // Exclude non compressible mime-types known from URI extension. - no Vary because no matter what client, this URI is always excluded
        String mimeType = context==null? MimeTypes.getDefaultMimeByExtension(path):context.getMimeType(path);
        if (mimeType!=null)
        {
            mimeType = MimeTypes.getContentTypeWithoutCharset(mimeType);
            // Despite function name, this tests general compression eligibility, not just gzip
            if (!isMimeTypeGzipable(mimeType))
            {
                LOG.debug("{} excluded by path suffix mime type {}",this,request);
                // handle normally without setting vary header
                _handler.handle(target,baseRequest, request, response);
                return;
            }
        }

        HttpOutput.Interceptor orig_interceptor = out.getInterceptor();
        try
        {
            // install interceptor and handle
            out.setInterceptor(new BrotliHttpOutputInterceptor(this, getVaryField(), baseRequest.getHttpChannel(), orig_interceptor, isSyncFlush()));


            if (_handler!=null)
                _handler.handle(target,baseRequest, request, response);
        }
        finally
        {
            // reset interceptor if request not handled
            if (!baseRequest.isHandled() && !baseRequest.isAsyncStarted())
                out.setInterceptor(orig_interceptor);
        }
    }

    private boolean shouldDoBrotli(Request baseReq) {
        boolean brotliEnabled = compressionStrategy.getBrotli()!=null;
        return baseReq.getHttpFields().getField(HttpHeader.ACCEPT_ENCODING).contains("br") && brotliEnabled;
    }

    private boolean shouldDoGzip(Request baseReq) {
        boolean gzipEnabled = compressionStrategy.getGzip()!=null;
        return baseReq.getHttpFields().getField(HttpHeader.ACCEPT_ENCODING).contains("gzip") && gzipEnabled;
    }

    @NotNull
    public CompressionStrategy getCompressionStrategy() {
        return compressionStrategy;
    }

    public void setCompressionStrategy(@NotNull CompressionStrategy compressionStrategy) {
        int gzipLevel = compressionStrategy.getGzip()!=null ? compressionStrategy.getGzip().getLevel() : -1;
        this.setCompressionLevel(gzipLevel);
        this.brotliLevel = compressionStrategy.getBrotli()!=null ? compressionStrategy.getBrotli().getLevel() : -1;
        this.compressionStrategy = compressionStrategy;
    }
}
