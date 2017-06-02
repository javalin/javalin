/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 *
 */

package io.javalin.core;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import io.javalin.HaltException;
import io.javalin.Handler;
import io.javalin.Request;
import io.javalin.Response;
import io.javalin.core.util.RequestUtil;
import io.javalin.core.util.Util;
import io.javalin.embeddedserver.StaticResourceHandler;

public class JavalinServlet implements Servlet {

    private PathMatcher pathMatcher;
    private ExceptionMapper exceptionMapper;
    private ErrorMapper errorMapper;
    private StaticResourceHandler staticResourceHandler;

    public JavalinServlet(PathMatcher pathMatcher, ExceptionMapper exceptionMapper, ErrorMapper errorMapper, StaticResourceHandler staticResourceHandler) {
        this.pathMatcher = pathMatcher;
        this.exceptionMapper = exceptionMapper;
        this.errorMapper = errorMapper;
        this.staticResourceHandler = staticResourceHandler;
    }

    @Override
    public void service(ServletRequest servletRequest, ServletResponse servletResponse) throws ServletException, IOException {

        HttpServletRequest httpRequest = (HttpServletRequest) servletRequest;
        HttpServletResponse httpResponse = (HttpServletResponse) servletResponse;
        Handler.Type type = Handler.Type.Companion.fromServletRequest(httpRequest);
        String requestUri = httpRequest.getRequestURI();
        Request request = RequestUtil.create(httpRequest);
        Response response = new Response(httpResponse);

        response.header("Server", "Javalin");

        try { // before-handlers, endpoint-handlers, static-files

            for (HandlerMatch beforeHandler : pathMatcher.findHandlers(Handler.Type.BEFORE, requestUri)) {
                beforeHandler.handler.handle(RequestUtil.create(httpRequest, beforeHandler), response);
            }

            List<HandlerMatch> matches = pathMatcher.findHandlers(type, requestUri);
            if (!matches.isEmpty()) {
                for (HandlerMatch endpointHandler : matches) {
                    Request currentRequest = RequestUtil.create(httpRequest, endpointHandler);
                    endpointHandler.handler.handle(currentRequest, response);
                    if (!currentRequest.nexted()) {
                        break;
                    }
                }
            } else if (type != Handler.Type.HEAD || (type == Handler.Type.HEAD && pathMatcher.findHandlers(Handler.Type.GET, requestUri).isEmpty())) {
                if (staticResourceHandler.handle(httpRequest, httpResponse)) {
                    return;
                }
                throw new HaltException(404, "Not found");
            }

        } catch (Exception e) {
            // both before-handlers and endpoint-handlers can throw Exception,
            // we need to handle those here in order to run after-filters even if an exception was thrown
            exceptionMapper.handle(e, request, response);
        }

        try { // after-handlers
            for (HandlerMatch afterHandler : pathMatcher.findHandlers(Handler.Type.AFTER, requestUri)) {
                afterHandler.handler.handle(RequestUtil.create(httpRequest, afterHandler), response);
            }
        } catch (Exception e) {
            // after filters can also throw exceptions
            exceptionMapper.handle(e, request, response);
        }

        try { // error mapping (turning status codes into standardized messages/pages)
            errorMapper.handle(response.status(), request, response);
        } catch (RuntimeException e) {
            // depressingly, the error mapping itself could throw a runtime exception
            // we need to handle these last... but that's it.
            exceptionMapper.handle(e, request, response);
        }

        // javalin is done doing stuff, write result to servlet-response
        if (response.contentType() == null) {
            httpResponse.setContentType("text/plain");
        }
        if (response.encoding() == null) {
            httpResponse.setCharacterEncoding(StandardCharsets.UTF_8.name());
        }
        if (response.body() != null) {
            httpResponse.getWriter().write(response.body());
            httpResponse.getWriter().flush();
            httpResponse.getWriter().close();
        } else if (response.bodyStream() != null) {
            Util.copyStream(response.bodyStream(), httpResponse.getOutputStream());
        }

    }

    @Override
    public void init(ServletConfig config) throws ServletException {
    }

    @Override
    public ServletConfig getServletConfig() {
        return null;
    }

    @Override
    public String getServletInfo() {
        return null;
    }

    @Override
    public void destroy() {
    }

}
