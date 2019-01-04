package io.javalin;

import io.javalin.serversentevent.EventSource;
import io.javalin.util.TestUtil;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

public class TestSse {
    String event = "hi";
    String data = "hello world";
    String ssePath = "/sse";


    @Test
    public void happy_path() {
        TestUtil.test( ((server, httpUtil) -> {
            server.sse( ssePath, sse -> sse.sendEvent( event, data ) );

            final String body = httpUtil.sse( ssePath ).get().getBody();
            assertTrue( body.contains( "event: " + event ) );
            assertTrue( body.contains( "data: " + data ) );
        }) );
    }

    @Test
    public void multiple_clients() {
        TestUtil.test( ((server, httpUtil) -> {
            List<EventSource> eventsources = new ArrayList<>();

            server.sse(ssePath, sse -> {
                eventsources.add(sse);
                sse.sendEvent(event, data + eventsources.size());
            } );

            final String bodyClient1 = httpUtil.sse(ssePath).get().getBody();
            final String bodyClient2 = httpUtil.sse(ssePath).get().getBody();

            assertNotEquals(bodyClient1, bodyClient2);
            assertNotEquals(eventsources.get(0), eventsources.get(1));
        }) );
    }
}
