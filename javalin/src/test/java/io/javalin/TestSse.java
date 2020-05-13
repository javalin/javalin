package io.javalin;

import com.mashape.unirest.http.Headers;
import io.javalin.http.sse.SseClient;
import io.javalin.testing.TestUtil;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

public class TestSse {

    private String event = "hi";
    private String data = "hello world";
    private String ssePath = "/sse";

    private Javalin shortTimeoutServer() {
        return Javalin.create().after(ctx -> ctx.req.getAsyncContext().setTimeout(10));
    }

    @Test
    public void happy_path() {
        TestUtil.test(shortTimeoutServer(), ((server, httpUtil) -> {
            server.sse(ssePath, sse -> sse.sendEvent(event, data));
            String body = httpUtil.sse(ssePath).get().getBody();
            assertTrue(body.contains("event: " + event));
            assertTrue(body.contains("data: " + data));
        }));
    }

    @Test
    public void happy_path_with_id() {
        TestUtil.test(shortTimeoutServer(), ((server, httpUtil) -> {
            int id = 1;
            server.sse(ssePath, sse -> sse.sendEvent(event, data, String.valueOf(id)));
            String body = httpUtil.sse(ssePath).get().getBody();
            assertTrue(body.contains("id: " + id));
            assertTrue(body.contains("event: " + event));
            assertTrue(body.contains("data: " + data));
        }));
    }

    @Test
    public void multiple_clients() {
        TestUtil.test(shortTimeoutServer(), ((server, httpUtil) -> {
            List<SseClient> eventsources = new ArrayList<>();
            server.sse(ssePath, sse -> {
                eventsources.add(sse);
                sse.sendEvent(event, data + eventsources.size());
            });
            String bodyClient1 = httpUtil.sse(ssePath).get().getBody();
            String bodyClient2 = httpUtil.sse(ssePath).get().getBody();
            assertNotEquals(bodyClient1, bodyClient2);
            assertNotEquals(eventsources.get(0), eventsources.get(1));
        }));
    }

    @Test
    public void headers() {
        TestUtil.test(shortTimeoutServer(), ((server, httpUtil) -> {
            server.sse(ssePath, sse -> sse.sendEvent(event, data));
            Headers headers = httpUtil.sse(ssePath).get().getHeaders(); // Headers extends HashMap<String, List<String>>
            assertTrue(headers.containsKey("Connection"));
            assertTrue(headers.containsKey("Cache-Control"));
            assertTrue(headers.containsKey("Content-Type"));
            String connection = headers.get("Connection").get(headers.get("Connection").size() - 1).toLowerCase();
            String contentType = headers.get("Content-Type").get(headers.get("Content-Type").size() - 1).toLowerCase();
            String cacheControl = headers.get("Cache-Control").get(headers.get("Cache-Control").size() - 1).toLowerCase();
            assertTrue(connection.contains("close"));
            assertTrue(cacheControl.contains("no-cache"));
            assertTrue(contentType.contains("text/event-stream"));
            assertTrue(contentType.contains("charset=utf-8"));
        }));
    }

    @Test
    public void http_status() {
        TestUtil.test(shortTimeoutServer(), ((server, httpUtil) -> {
            server.sse(ssePath, sse -> sse.sendEvent(event, data));
            int status = httpUtil.sse(ssePath).get().getStatus();
            assertEquals(200, status);
        }));
    }

    @Test
    public void getting_queryparam() {
        TestUtil.test(shortTimeoutServer(), ((server, httpUtil) -> {
            server.sse(ssePath, sse -> sse.sendEvent(event, sse.ctx.queryParam("qp")));
            String body = httpUtil.sse(ssePath + "?qp=my-qp").get().getBody();
            assertTrue(body.contains("event: " + event));
            assertTrue(body.contains("data: " + "my-qp"));
        }));
    }

}
