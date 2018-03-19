/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import io.javalin.embeddedserver.jetty.websocket.WsSession;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
import org.hamcrest.Matchers;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft_6455;
import org.java_websocket.handshake.ServerHandshake;
import org.junit.Before;
import org.junit.Test;
import static io.javalin.ApiBuilder.ws;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;

/**
 * This test could be better
 */
public class TestWebSocket {

    private List<String> log;

    @Before
    public void setup() {
        log = new ArrayList<>();
    }


    @Test
    public void test_id_generation() throws Exception {
        Javalin app = Javalin.create().contextPath("/websocket").port(0);

        app.ws("/test-websocket-1", ws -> {
            ws.onConnect(session -> log.add(session.getId()));
            ws.onMessage((session, msg) -> log.add(session.getId()));
            ws.onClose((session, statusCode, reason) -> log.add(session.getId()));
        });
        app.routes(() -> {
            ws("/test-websocket-2", ws -> {
                ws.onConnect(session -> log.add(session.getId()));
                ws.onMessage((session, msg) -> log.add(session.getId()));
                ws.onClose((session, statusCode, reason) -> log.add(session.getId()));
            });
        });
        app.start();

        TestClient testClient1_1 = new TestClient(URI.create("ws://localhost:" + app.port() + "/websocket/test-websocket-1"));
        TestClient testClient1_2 = new TestClient(URI.create("ws://localhost:" + app.port() + "/websocket/test-websocket-1"));
        TestClient testClient2_1 = new TestClient(URI.create("ws://localhost:" + app.port() + "/websocket/test-websocket-2"));

        doAndSleepWhile(testClient1_1::connect, () -> !testClient1_1.isOpen());
        doAndSleepWhile(testClient1_2::connect, () -> !testClient1_2.isOpen());
        doAndSleep(() -> testClient1_1.send("A"));
        doAndSleep(() -> testClient1_2.send("B"));
        doAndSleepWhile(testClient1_1::close, testClient1_1::isClosing);
        doAndSleepWhile(testClient1_2::close, testClient1_2::isClosing);
        doAndSleepWhile(testClient2_1::connect, () -> !testClient2_1.isOpen());
        doAndSleepWhile(testClient2_1::close, testClient2_1::isClosing);

        // 3 clients and a lot of operations should only yield three unique identifiers for the clients
        Set<String> uniqueLog = new HashSet<>(log);
        assertThat(uniqueLog, hasSize(3));
        for (String id : uniqueLog) {
            assertThat(1, Matchers.equalTo(Collections.frequency(uniqueLog, id)));
        }

        app.stop();
    }

    @Test
    public void test_everything() throws Exception {
        Javalin app = Javalin.create().contextPath("/websocket").port(0);

        Map<WsSession, Integer> userUsernameMap = new LinkedHashMap<>();
        AtomicInteger atomicInteger = new AtomicInteger();
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

        TestClient testClient1_1 = new TestClient(URI.create("ws://localhost:" + app.port() + "/websocket/test-websocket-1"));
        TestClient testClient1_2 = new TestClient(URI.create("ws://localhost:" + app.port() + "/websocket/test-websocket-1"));
        TestClient testClient2_1 = new TestClient(URI.create("ws://localhost:" + app.port() + "/websocket/test-websocket-2"));

        doAndSleepWhile(testClient1_1::connect, () -> !testClient1_1.isOpen());
        doAndSleepWhile(testClient1_2::connect, () -> !testClient1_2.isOpen());
        doAndSleep(() -> testClient1_1.send("A"));
        doAndSleep(() -> testClient1_2.send("B"));
        doAndSleepWhile(testClient1_1::close, testClient1_1::isClosing);
        doAndSleepWhile(testClient1_2::close, testClient1_2::isClosing);
        doAndSleepWhile(testClient2_1::connect, () -> !testClient2_1.isOpen());
        doAndSleepWhile(testClient2_1::close, testClient2_1::isClosing);
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

    @Test
    public void test_path_params() throws Exception {
        Javalin app = Javalin.create().contextPath("/websocket").port(0);
        app.start();

        app.ws("/params/:1", ws -> {
            ws.onConnect(session -> {
                log.add(session.param("1"));
            });
        });

        app.ws("/params/:1/test/:2/:3", ws -> {
            ws.onConnect(session -> {
                log.add(session.param("1") + " " + session.param("2") + " " + session.param("3"));
            });
        });

        app.ws("/*", ws -> { // this should not be triggered since all calls match more specific handlers
            ws.onConnect(session -> {
                log.add("catchall");
            });
        });

        TestClient testClient1_1 = new TestClient(URI.create("ws://localhost:" + app.port() + "/websocket/params/one"));
        doAndSleepWhile(testClient1_1::connect, () -> !testClient1_1.isOpen());
        doAndSleepWhile(testClient1_1::close, testClient1_1::isClosing);

        TestClient testClient1_2 = new TestClient(URI.create("ws://localhost:" + app.port() + "/websocket/params/%E2%99%94"));
        doAndSleepWhile(testClient1_2::connect, () -> !testClient1_2.isOpen());
        doAndSleepWhile(testClient1_2::close, testClient1_2::isClosing);

        TestClient testClient2_1 = new TestClient(URI.create("ws://localhost:" + app.port() + "/websocket/params/another/test/long/path"));
        doAndSleepWhile(testClient2_1::connect, () -> !testClient2_1.isOpen());
        doAndSleepWhile(testClient2_1::close, testClient2_1::isClosing);

        assertThat(log, containsInAnyOrder(
            "one",
            "♔",
            "another long path"
        ));
        assertThat(log, not(hasItem("catchall")));
        app.stop();
    }

    @Test
    public void test_ws_404() throws Exception {
        Javalin app = Javalin.start(0);
        HttpResponse<String> response = Unirest.get("http://localhost:" + app.port() + "/invalid-path")
            .header("Connection", "Upgrade")
            .header("Upgrade", "websocket")
            .header("Host", "localhost:" + app.port())
            .header("Sec-WebSocket-Key", "SGVsbG8sIHdvcmxkIQ==")
            .header("Sec-WebSocket-Version", "13")
            .asString();
        assertThat(response.getBody(), containsString("WebSocket handler not found"));
        app.stop();
    }

    @Test
    public void test_headers_and_host() throws Exception {
        Javalin app = Javalin.start(0);
        app.ws("websocket", ws -> {
            ws.onConnect(session -> {
                log.add("Header: " + session.header("Test"));
            });
            ws.onClose((session, statusCode, reason) -> {
                log.add("Closed connection from: " + session.host());
            });
        });

        TestClient testClient1_1 = new TestClient(URI.create("ws://localhost:" + app.port() + "/websocket"), Collections.singletonMap("Test", "HeaderParameter"));
        doAndSleepWhile(testClient1_1::connect, () -> !testClient1_1.isOpen());
        doAndSleepWhile(testClient1_1::close, testClient1_1::isClosing);

        assertThat(log, containsInAnyOrder(
            "Header: HeaderParameter"
            , "Closed connection from: localhost"));
        app.stop();
    }

    class TestClient extends WebSocketClient {
        public TestClient(URI serverUri) {
            super(serverUri);
        }

        public TestClient(URI serverUri, Map<String, String> headers) {
            super(serverUri, new Draft_6455(), headers, 0);
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
        long timeMillis = System.currentTimeMillis();
        runnable.run();
        while ((boolean) callable.call() && System.currentTimeMillis() < timeMillis + 1000) {
            Thread.sleep(25);
        }
    }

    public void doAndSleep(Runnable runnable) {
        runnable.run();
        try {
            Thread.sleep(25);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
