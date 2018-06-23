package io.javalin.serversentevent;

import io.javalin.Handler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Enumeration;
import java.util.List;

public class SSEHandler {
    private static final Charset UTF_8 = Charset.forName( "UTF-8" );

    public static Handler start(List<EventSourceEmitter> emitters) {
        return ctx -> {
            HttpServletRequest request = ctx.request();
            HttpServletResponse response = ctx.response();
            Enumeration<String> acceptValues = request.getHeaders( "Accept" );
            while (acceptValues.hasMoreElements()) {
                String accept = acceptValues.nextElement();
                if (accept.equals( "text/event-stream" )) {
                    respond( request, response );
                    request.startAsync( request, response );
                    EventSourceEmitter emitter = new EventSourceEmitterImpl( request.getAsyncContext() );
                    emitters.add( emitter );
                    return;
                }
            }

        };
    }

    private static void respond(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setStatus( HttpServletResponse.SC_OK );
        response.setCharacterEncoding( UTF_8.name() );
        response.setContentType( "text/event-stream" );
        response.addHeader( "Connection", "close" );
        response.flushBuffer();
    }
}
