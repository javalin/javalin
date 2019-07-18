package io.javalin.core.compression;

import org.eclipse.jetty.http.HttpField;
import org.eclipse.jetty.http.HttpFields;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.MimeTypes;
import org.eclipse.jetty.server.HttpChannel;
import org.eclipse.jetty.server.HttpOutput;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.util.StringUtil;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

import java.nio.ByteBuffer;
import java.nio.channels.WritePendingException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.CRC32;

import static org.eclipse.jetty.http.CompressedContentFormat.BR;

/**
 * Majority of this code is copied from GzipHttpOutputInterceptor
 */
public class BrotliHttpOutputInterceptor implements HttpOutput.Interceptor {

    public static Logger LOG = Log.getLogger(BrotliHttpOutputInterceptor.class);

    private enum BRState {  MIGHT_COMPRESS, NOT_COMPRESSING, COMMITTING, COMPRESSING, FINISHED}
    private final AtomicReference<BRState> _state = new AtomicReference<>(BRState.MIGHT_COMPRESS);

    //GzipFactory is fine here, because StaticCompressionHandler inherits from GzipHandler (who implements this interface)
    //private final GzipFactory _factory;
    private final StaticCompressionHandler _handler;
    private final HttpOutput.Interceptor _interceptor;
    private final HttpChannel _channel;
    private final HttpField _vary;
    private final int _bufferSize;
    private final boolean _syncFlush;

    private final CRC32 _crc = new CRC32();
    private ByteBuffer _buffer;

    public BrotliHttpOutputInterceptor(StaticCompressionHandler handler, HttpField vary, HttpChannel channel, HttpOutput.Interceptor next, boolean syncFlush) {
        this(handler,vary,channel.getHttpConfiguration().getOutputBufferSize(),channel,next,syncFlush);
    }

    public BrotliHttpOutputInterceptor(StaticCompressionHandler handler, HttpField vary, int bufferSize, HttpChannel channel, HttpOutput.Interceptor next,boolean syncFlush) {
        _handler=handler;
        _channel=channel;
        _interceptor=next;
        _vary=vary;
        _bufferSize=bufferSize;
        _syncFlush=syncFlush;
    }

    @Override
    public HttpOutput.Interceptor getNextInterceptor() {
        return _interceptor;
    }

    @Override
    public boolean isOptimizedForDirectBuffers() {
        return false;
    }

    @Override
    public void write(ByteBuffer content, boolean complete, Callback callback) {
        switch (_state.get())
        {
            case MIGHT_COMPRESS:
                commit(content,complete,callback);
                break;

            case NOT_COMPRESSING:
                _interceptor.write(content, complete, callback);
                return;

            case COMMITTING:
                callback.failed(new WritePendingException());
                break;

            case COMPRESSING:
                //brotli(content,complete,callback);
                break;

            default:
                callback.failed(new IllegalStateException("state="+_state.get()));
                break;
        }
    }

    private void brotli(ByteBuffer content, boolean complete, final Callback callback) {
        //if (content.hasRemaining() || complete) {
            ByteBuffer outBuff = _handler.getCompressionStrategy().getBrotli().compressBuffer(content);
            _interceptor.write(outBuff, true, callback);
        //}
    }

    private String etagBrotli(String etag)
    {
        int end = etag.length()-1;
        return (etag.charAt(end)=='"')?etag.substring(0,end)+ BR._etag+'"':etag+BR._etag;
    }

    protected void commit(ByteBuffer content, boolean complete, Callback callback)
    {
        // Are we excluding because of status?
        Response response = _channel.getResponse();
        int sc = response.getStatus();
        if (sc>0 && (sc<200 || sc==204 || sc==205 || sc>=300))
        {
            LOG.debug("{} exclude by status {}",this,sc);
            noCompression();

            if (sc==304)
            {
                String request_etags = (String)_channel.getRequest().getAttribute("i.j.c.compression.StaticCompressionHandler.etag");
                String response_etag = response.getHttpFields().get(HttpHeader.ETAG);
                if (request_etags!=null && response_etag!=null)
                {
                    String response_etag_brotli=etagBrotli(response_etag);
                    if (request_etags.contains(response_etag_brotli))
                        response.getHttpFields().put(HttpHeader.ETAG,response_etag_brotli);
                }
            }

            _interceptor.write(content, complete, callback);
            return;
        }

        // Are we excluding because of mime-type?
        String ct = response.getContentType();
        if (ct!=null)
        {
            ct= MimeTypes.getContentTypeWithoutCharset(ct);
            // Despite function name, this tests general compression eligibility, not just gzip
            if (!_handler.isMimeTypeGzipable(StringUtil.asciiToLowerCase(ct)))
            {
                LOG.debug("{} exclude by mimeType {}",this,ct);
                noCompression();
                _interceptor.write(content, complete, callback);
                return;
            }
        }

        // Has the Content-Encoding header already been set?
        HttpFields fields = response.getHttpFields();
        String ce=fields.get(HttpHeader.CONTENT_ENCODING);
        if (ce != null)
        {
            LOG.debug("{} exclude by content-encoding {}",this,ce);
            noCompression();
            _interceptor.write(content, complete, callback);
            return;
        }

        // Are we the thread that commits?
        if (_state.compareAndSet(BrotliHttpOutputInterceptor.BRState.MIGHT_COMPRESS, BrotliHttpOutputInterceptor.BRState.COMMITTING))
        {
            // We are varying the response due to accept encoding header.
            if (_vary != null)
            {
                if (fields.contains(HttpHeader.VARY))
                    fields.addCSV(HttpHeader.VARY,_vary.getValues());
                else
                    fields.add(_vary);
            }


            /*
            long content_length = response.getContentLength();
            if (content_length<0 && complete)
                content_length=content.remaining();

            _deflater = _factory.getDeflater(_channel.getRequest(),content_length);
            if (_deflater==null)
            {
                LOG.debug("{} exclude no deflater",this);
                _state.set(GzipHttpOutputInterceptor.GZState.NOT_COMPRESSING);
                _interceptor.write(content, complete, callback);
                return;
            }
            */

            fields.put(BR._contentEncoding);
            _crc.reset();
            _buffer=_channel.getByteBufferPool().acquire(_bufferSize,false);
            //BufferUtil.fill(_buffer,GZIP_HEADER,0,GZIP_HEADER.length);

            // Adjust headers
            response.setContentLength(-1);
            String etag=fields.get(HttpHeader.ETAG);
            if (etag!=null)
                fields.put(HttpHeader.ETAG,etagBrotli(etag));

            //LOG.debug("{} compressing {}",this,_deflater);
            _state.set(BrotliHttpOutputInterceptor.BRState.COMPRESSING);

            brotli(content,complete,callback);
        }
        else
            callback.failed(new WritePendingException());
    }

    public void noCompression()
    {
        while (true)
        {
            switch (_state.get())
            {
                case NOT_COMPRESSING:
                    return;

                case MIGHT_COMPRESS:
                    if (_state.compareAndSet(BRState.MIGHT_COMPRESS,BRState.NOT_COMPRESSING))
                        return;
                    break;

                default:
                    throw new IllegalStateException(_state.get().toString());
            }
        }
    }
}
