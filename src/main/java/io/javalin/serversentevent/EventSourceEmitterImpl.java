package io.javalin.serversentevent;

import javax.servlet.AsyncContext;
import javax.servlet.ServletOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

public class EventSourceEmitterImpl implements EventSourceEmitter {
    private static final Charset UTF_8 = Charset.forName("UTF-8");
    private static final byte[] CRLF = new byte[]{'\r', '\n'};
    private static final byte[] ID_FIELD;
    private static final byte[] EVENT_FIELD;
    private static final byte[] DATA_FIELD;
    private static final byte[] COMMENT_FIELD;
    static
    {
        try
        {
            ID_FIELD = "id: ".getBytes(UTF_8.name());
            EVENT_FIELD = "event: ".getBytes(UTF_8.name());
            DATA_FIELD = "data: ".getBytes(UTF_8.name());
            COMMENT_FIELD = ": ".getBytes(UTF_8.name());
        }
        catch (UnsupportedEncodingException x)
        {
            throw new RuntimeException(x);
        }
    }
    private final ServletOutputStream output;
    private final AsyncContext asyncContext;

    public EventSourceEmitterImpl(AsyncContext asyncContext) throws IOException {
        this.asyncContext = asyncContext;
        this.output = asyncContext.getResponse().getOutputStream();
    }

    @Override
    public void emmit(String event, String data) throws IOException {
        synchronized (this) {
            event( event );
            data( data );
        }
    }

    @Override
    public void emmit(String id, String event, String data) throws IOException {
        synchronized (this) {
            id( id );
            emmit( event, data );
        }
    }

    private void id(String id) throws IOException {
        synchronized (this) {
            output.write( ID_FIELD );
            output.write( id.getBytes( UTF_8.name() ) );
            output.write( CRLF );
        }
    }

    private void event(String event) throws IOException {
        synchronized (this) {
            output.write( EVENT_FIELD );
            output.write( event.getBytes( UTF_8.name() ) );
            output.write( CRLF );
        }
    }

    public void data(String data) throws IOException {
        synchronized (this) {
            BufferedReader reader = new BufferedReader( new StringReader( data ) );
            String line;
            while ((line = reader.readLine()) != null) {
                output.write( DATA_FIELD );
                output.write( line.getBytes( UTF_8.name() ) );
                output.write( CRLF );
            }
            output.write( CRLF );
            flush();
        }
    }

    protected void flush() throws IOException {
        asyncContext.getResponse().flushBuffer();
    }
}
