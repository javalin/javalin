/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin;

import org.junit.jupiter.api.Test;

import static io.javalin.apibuilder.ApiBuilder.*;
import static org.assertj.core.api.Assertions.assertThat;

public class TestApiBuilderWebSocket {

    @Test
    public void testWsBeforeAndAfterInApiBuilder() {
        Javalin app = Javalin.create(config -> {
            config.routes.apiBuilder(() -> {
                wsBefore("/ws/*", ws -> ws.onConnect(ctx -> {}));
                wsBefore(ws -> ws.onConnect(ctx -> {}));
                ws("/ws/test", ws -> ws.onConnect(ctx -> {}));
                wsAfter("/ws/*", ws -> ws.onClose(ctx -> {}));
                wsAfter(ws -> ws.onClose(ctx -> {}));
            });
        });

        assertThat(app.unsafe.pvt.internalRouter.allWsHandlers()).hasSize(5);
    }
}
