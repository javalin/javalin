package io.javalin;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import io.javalin.serversentevent.EventSource;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

public class TestSSE {
    final String event = "hi";
    final String data = "hello world";
    final String ssePath = "/sse";
    Javalin server;
    int port;

    @Before
    public void setUp() throws Exception {
        server = Javalin.create().port(0).start();
        port = server.port();
    }

    @Test
    public void happy_path() throws Exception {

        server.sse( ssePath, sse -> {
            sse.onOpen( eventSource -> {
                eventSource.sendEvent(event, data);
                return null;
            });
        });

        Future<HttpResponse<String>> client = Unirest.get("http://localhost:" + port + ssePath )
                .header("Accept", "text/event-stream")
                .header("Connection", "keep-alive")
                .header("Cache-Control", "no-cache")
                .asStringAsync();

        final String body = client.get().getBody();
        assertTrue(body.contains("event: " + event));
        assertTrue(body.contains("data: " + data));
        server.stop();
    }

    @Test
    public void multiple_clients() throws Exception {
        List<EventSource> eventsources = new ArrayList<>();

        server.sse( ssePath, sse -> {
            sse.onOpen( eventSource -> {
                eventsources.add(eventSource);
                eventSource.sendEvent(event, data + eventsources.size());
                return null;
            });
        });
        int port = server.port();

        Future<HttpResponse<String>> client = Unirest.get("http://localhost:" + port + ssePath )
                .header("Accept", "text/event-stream")
                .header("Connection", "keep-alive")
                .header("Cache-Control", "no-cache")
                .asStringAsync();
        Future<HttpResponse<String>> client2 = Unirest.get("http://localhost:" + port + ssePath )
                .header("Accept", "text/event-stream")
                .header("Connection", "keep-alive")
                .header("Cache-Control", "no-cache")
                .asStringAsync();

        assertNotEquals(client.get().getBody(), client2.get().getBody());
        assertNotEquals(eventsources.get(0), eventsources.get(1));
        server.stop();
    }
}
