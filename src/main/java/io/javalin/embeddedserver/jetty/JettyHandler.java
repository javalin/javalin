/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 *
 */

package io.javalin.embeddedserver.jetty;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.servlet.AsyncContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.session.SessionHandler;

import io.javalin.core.JavalinServlet;
import io.javalin.embeddedserver.CachedRequestWrapper;

public class JettyHandler extends SessionHandler {

    private JavalinServlet javalinServlet;

    private ThreadPoolExecutor executor;

    public JettyHandler(JavalinServlet javalinServlet) {
        this.javalinServlet = javalinServlet;
        this.executor = new ThreadPoolExecutor(200, 200, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<>());
        executor.allowCoreThreadTimeOut(true);
    }

    @Override
    public void doHandle(String target, Request jettyRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

        CachedRequestWrapper cachedRequest = new CachedRequestWrapper(request);
        cachedRequest.setAttribute("jetty-target", target);
        cachedRequest.setAttribute("jetty-request", jettyRequest);

        AsyncContext asyncContext = request.startAsync();
        CompletableFuture.runAsync(() -> {
            try {
                javalinServlet.service(cachedRequest, response);
                request.getAsyncContext().complete();
            } catch (IOException | ServletException e) {
                throw new RuntimeException(e);
            }
        }, executor).thenAccept((Void) -> {
            jettyRequest.setHandled(true);
            asyncContext.complete();
        });

    }

}
