package io.javalin;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class TestSSE {
    final String event = "hi";
    final String data = "hello world";
    final String ssePath = "/sse";

    @Test
    public void happy_path() throws Exception {
        Javalin server = Javalin.create().port(0).start();

        server.sse( ssePath, sse -> {
            sse.onOpen( eventSource -> {
                eventSource.sendEvent(event, data);
                return null;
            });
        });
        int port = server.port();

        HttpResponse<String> response = Unirest.get("http://localhost:" + port + ssePath )
                .header("Accept", "text/event-stream")
                .header("Connection", "keep-alive")
                .header("Cache-Control", "no-cache")
                .asString();

        assertTrue(response.getBody().contains("event: " + event));
        assertTrue(response.getBody().contains("data: " + data));
        server.stop();
    }
}
