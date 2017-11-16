/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin;

import io.javalin.embeddedserver.jetty.websocket.WsSession;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.junit.Test;
import static io.javalin.ApiBuilder.ws;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;

/**
 * This test could be better
 */
public class TestWebSocket {

    private static Map<WsSession, Integer> userUsernameMap = new LinkedHashMap<>();
    private AtomicInteger atomicInteger = new AtomicInteger();
    private TestClient testClient1_1 = new TestClient(URI.create("ws://localhost:1234/websocket/test-websocket-1"));
    private TestClient testClient1_2 = new TestClient(URI.create("ws://localhost:1234/websocket/test-websocket-1"));
    private TestClient testClient2_1 = new TestClient(URI.create("ws://localhost:1234/websocket/test-websocket-2"));
    private List<String> log = new ArrayList<>();

    @Test
    public void test_allListenersWork() throws Exception {

        Javalin app = Javalin.create().contextPath("/websocket").port(1234);

        app.ws("/test-websocket-1", ws -> {
            ws.onConnect(session -> {
                userUsernameMap.put(session, atomicInteger.getAndIncrement());
                log.add(userUsernameMap.get(session) + " connected");
            });
            ws.onMessage((session, message) -> {
                log.add(userUsernameMap.get(session) + " sent '" + message + "' to server");
                userUsernameMap.forEach((client, i) -> {
                    doAndSleep(() -> client.send("Server sent '" + message + "' to " + userUsernameMap.get(client)));
                });
            });
            ws.onClose((session, statusCode, reason) -> {
                log.add(userUsernameMap.get(session) + " disconnected");
            });
        });
        app.routes(() -> {
            ws("/test-websocket-2", ws -> {
                ws.onConnect(session -> log.add("Connected to other endpoint"));
                ws.onClose((session, statusCode, reason) -> log.add("Disconnected from other endpoint"));
            });
        });
        app.start();
        doAndSleepWhile(() -> testClient1_1.connect(), () -> !testClient1_1.isOpen());
        doAndSleepWhile(() -> testClient1_2.connect(), () -> !testClient1_2.isOpen());
        doAndSleep(() -> testClient1_1.send("A"));
        doAndSleep(() -> testClient1_2.send("B"));
        doAndSleepWhile(() -> testClient1_1.close(), () -> testClient1_1.isClosing());
        doAndSleepWhile(() -> testClient1_2.close(), () -> testClient1_2.isClosing());
        doAndSleepWhile(() -> testClient2_1.connect(), () -> !testClient2_1.isOpen());
        doAndSleepWhile(() -> testClient2_1.close(), testClient2_1::isClosing);
        assertThat(log, containsInAnyOrder(
            "0 connected",
            "1 connected",
            "0 sent 'A' to server",
            "Server sent 'A' to 0",
            "Server sent 'A' to 1",
            "1 sent 'B' to server",
            "Server sent 'B' to 0",
            "Server sent 'B' to 1",
            "0 disconnected",
            "1 disconnected",
            "Connected to other endpoint",
            "Disconnected from other endpoint"
        ));
        app.stop();
    }

    class TestClient extends WebSocketClient {
        public TestClient(URI serverUri) {
            super(serverUri);
        }

        public void onOpen(ServerHandshake serverHandshake) {
        }

        public void onMessage(String s) {
            log.add(s);
        }

        public void onClose(int i, String s, boolean b) {
        }

        public void onError(Exception e) {
        }
    }

    public void doAndSleepWhile(Runnable runnable, Callable callable) throws Exception {
        runnable.run();
        while ((boolean) callable.call()) {
            Thread.sleep(15);
        }
    }

    public void doAndSleep(Runnable runnable) {
        runnable.run();
        try {
            Thread.sleep(15);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
