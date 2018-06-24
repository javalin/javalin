package io.javalin.serversentevent;

import javax.servlet.AsyncContext;
import javax.servlet.ServletOutputStream;
import java.io.IOException;

public class EmitterImpl implements Emitter {
    private static final String CRLF = "\r\n";
    private final AsyncContext asyncContext;
    private ServletOutputStream output;
    private boolean close = false;


    public EmitterImpl(AsyncContext asyncContext) {
        this.asyncContext = asyncContext;
        try {
            this.output = asyncContext.getResponse().getOutputStream();
        } catch (IOException e) {
            close = true;
        }
    }

    @Override
    public void event(String event, String data) {
        synchronized (this) {
            sendEvent( event, data );
        }
    }

    private void sendEvent(String event, String data) {
        synchronized (this) {
            try {
                event( event );
                data( data );
            } catch (IOException e) {
                close = true;
            }
        }
    }

    @Override
    public boolean isClose() {
        return close;
    }

    private void event(String event) throws IOException {
        synchronized (this) {
            output.println( "event: " + event + CRLF );
        }
    }

    private void data(String data) throws IOException {
        synchronized (this) {
            output.println( "data: " + data + CRLF );
            flush();
        }
    }

    private void flush() throws IOException {
        asyncContext.getResponse().flushBuffer();
    }
}
