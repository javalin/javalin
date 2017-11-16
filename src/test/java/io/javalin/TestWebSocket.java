/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.junit.Test;
import static io.javalin.ApiBuilder.ws;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * This test is not very good...
 */
public class TestWebSocket {

    private List<String> testList = new ArrayList<>();

    @Test
    public void test_allListenersWork() throws Exception {

        Javalin app = Javalin.create().contextPath("/websocket").port(1234);

        app.ws("/test-websocket", ws -> {
            ws.onConnect(session -> {
                testList.add("Connected");
            });
            ws.onMessage((session, message) -> {
                testList.add("Received:" + message);
                session.getRemote().sendString("Echo:" + message);
            });
            ws.onClose((session, statusCode, reason) -> {
                testList.add("Closed");
            });
        });
        app.routes(() -> {
            ws("/test-websocket-2", ws -> {
                ws.onConnect(session -> {
                    testList.add("Connected2");
                });
                ws.onClose((session, statusCode, reason) -> {
                    testList.add("Closed2");
                });
            });
        });
        app.start();

        TestClient testClient = new TestClient(URI.create("ws://localhost:1234/websocket/test-websocket"));
        testClient.connect();
        Thread.sleep(200);
        testClient.send("Hello");
        Thread.sleep(200);
        testClient.close();
        Thread.sleep(200);
        TestClient testClient2 = new TestClient(URI.create("ws://localhost:1234/websocket/test-websocket-2"));
        testClient2.connect();
        Thread.sleep(200);
        testClient2.close();
        Thread.sleep(200);
        assertThat(testList.toString(), is("[Connected, Received:Hello, Echo:Hello, Closed, Connected2, Closed2]"));
        app.stop();
    }

    class TestClient extends WebSocketClient {
        public TestClient(URI serverUri) {
            super(serverUri);
        }

        @Override
        public void onOpen(ServerHandshake serverHandshake) {
        }

        @Override
        public void onMessage(String s) {
            testList.add(s);
        }

        @Override
        public void onClose(int i, String s, boolean b) {
        }

        @Override
        public void onError(Exception e) {
        }
    }

}
