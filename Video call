package io.javalin.omeglin;

import io.javalin.websocket.WsContext;
import io.javalin.websocket.WsConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentLinkedQueue;

public class Matchmaking {
    private static final Logger logger = LoggerFactory.getLogger(Matchmaking.class);
    private static final ConcurrentLinkedQueue<Exchange> queue = new ConcurrentLinkedQueue<>();

    public static void websocket(WsConfig ws) {
        ws.onConnect(user -> user.enableAutomaticPings());
        ws.onClose(user -> pairingAbort(user));
        ws.onMessage(user -> {
            logger.info("Received message: " + user.message());
            var message = user.messageAsClass(Message.class);
            switch (message.name()) {
                case "PAIRING_START" -> pairingStart(user);
                case "PAIRING_ABORT" -> pairingAbort(user);
                case "PAIRING_DONE" -> pairingDone(user);
                case "SDP_OFFER", "SDP_ANSWER", "SDP_ICE_CANDIDATE" -> {
                    var exchange = findExchange(user);
                    if (exchange != null && exchange.a != null && exchange.b != null) {
                        send(exchange.otherUser(user), message); // forward message to other user
                    } else {
                        logger.warn("Received SDP message from unpaired user");
                    }
                }
            }
        });
    }

    private static void pairingStart(WsContext user) {
        queue.removeIf(ex -> ex.a == user || ex.b == user); // prevent double queueing
        var exchange = queue.stream()
                .filter(ex -> ex.b == null)
                .findFirst()
                .orElse(null);
        if (exchange != null) {
            exchange.b = user;
            send(exchange.a, new Message("PARTNER_FOUND", "GO_FIRST"));
            send(exchange.b, new Message("PARTNER_FOUND"));
        } else {
            queue.add(new Exchange(user));
        }
    }

    private static void pairingAbort(WsContext user) {
        var exchange = findExchange(user);
        if (exchange != null) {
            send(exchange.otherUser(user), new Message("PARTNER_LEFT"));
            queue.remove(exchange);
        }
    }

    private static void pairingDone(WsContext user) {
        var exchange = findExchange(user);
        if (exchange != null) {
            exchange.doneCount++;
        }
        queue.removeIf(ex -> ex.doneCount == 2);
    }

    private static Exchange findExchange(WsContext user) {
        return queue.stream()
                .filter(ex -> user.equals(ex.a) || user.equals(ex.b))
                .findFirst()
                .orElse(null);
    }

    private static void send(WsContext user, Message message) { // null safe send method
        if (user != null) {
            user.send(message);
        }
    }

    record Message(String name, String data) {
        public Message(String name) {
            this(name, null);
        }
    }

    static class Exchange {
        public WsContext a;
        public WsContext b;
        public int doneCount = 0;

        public Exchange(WsContext a) {
            this.a = a;
        }

        public WsContext otherUser(WsContext user) {
            return user.equals(a) ? b : a;
        }
    }

}
