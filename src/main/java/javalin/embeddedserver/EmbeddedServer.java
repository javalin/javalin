// Javalin - https://javalin.io
// Copyright 2017 David Åse
// Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE

package javalin.embeddedserver;

public interface EmbeddedServer {

    int start(String host, int port) throws Exception;

    void join() throws InterruptedException;

    void stop();

    int activeThreadCount();

    Object attribute(String key);
}
