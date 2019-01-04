package io.javalin;

import com.mashape.unirest.http.Headers;
import io.javalin.serversentevent.EventSource;
import io.javalin.util.TestUtil;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
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

    @Test
    public void headers() {
        TestUtil.test( ((server, httpUtil) -> {

            server.sse( ssePath, sse -> sse.sendEvent( event, data ) );

            final Headers headers = httpUtil.sse(ssePath).get().getHeaders(); // Headers extends HashMap<String, List<String>>

            assertTrue(headers.containsKey("Connection"));
            assertTrue(headers.containsKey("Cache-Control"));
            assertTrue(headers.containsKey("Content-Type"));

            final String connection = headers.get("Connection").get(headers.get("Connection").size() - 1).toLowerCase();
            final String contentType = headers.get("Content-Type").get(headers.get("Content-Type").size() - 1).toLowerCase();
            final String cacheControl = headers.get("Cache-Control").get(headers.get("Cache-Control").size() - 1).toLowerCase();

            assertTrue(connection.contains("keep-alive"));          // should be "keep-alive" NOT "close"
            assertTrue(cacheControl.contains("no-cache"));          // should be sent
            assertTrue(contentType.contains("text/event-stream"));  // passes
            assertTrue(contentType.contains("charset=utf-8"));      // passes

        }) );
    }

    @Test
    public void http_status() {
        TestUtil.test( ((server, httpUtil) -> {
            server.sse( ssePath, sse -> sse.sendEvent( event, data ) );
            final int status = httpUtil.sse(ssePath).get().getStatus();
            assertThat(status, equalTo(200));
        }) );
    }
}
