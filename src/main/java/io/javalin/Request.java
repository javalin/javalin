/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 *
 */

package io.javalin;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.servlet.AsyncContext;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import io.javalin.core.util.RequestUtil;
import io.javalin.core.util.Util;
import io.javalin.translator.json.Jackson;

public class Request {

    private boolean passedToNextHandler;
    private HttpServletRequest servletRequest;
    private Map<String, String> paramMap; // cache (different for each handler)
    private List<String> splatList; // cache (different for each handler)

    public Request(HttpServletRequest httpRequest, Map<String, String> paramMap, List<String> splatList) {
        this.servletRequest = httpRequest;
        this.paramMap = paramMap;
        this.splatList = splatList;
    }

    public HttpServletRequest unwrap() {
        return servletRequest;
    }

    @FunctionalInterface
    public interface AsyncHandler {
        CompletableFuture<Void> handle();
    }

    public void async(AsyncHandler asyncHandler) {
        AsyncContext asyncContext = servletRequest.startAsync();
        asyncHandler.handle()
            .thenAccept((Void) -> asyncContext.complete())
            .exceptionally(e -> {
                asyncContext.complete();
                throw new RuntimeException(e);
            });
    }

    public String body() {
        return RequestUtil.INSTANCE.byteArrayToString(bodyAsBytes(), servletRequest.getCharacterEncoding());
    }

    public byte[] bodyAsBytes() {
        try {
            return RequestUtil.INSTANCE.toByteArray(servletRequest.getInputStream());
        } catch (IOException e) {
            return null;
        }
    }

    public <T> T bodyAsClass(Class<T> clazz) {
        Util.ensureDependencyPresent("Jackson", "com.fasterxml.jackson.databind.ObjectMapper", "com.fasterxml.jackson.core/jackson-databind");
        return Jackson.INSTANCE.toObject(body(), clazz);
    }

    // yeah, this is probably not the best solution
    public String bodyParam(String bodyParam) {
        for (String keyValuePair : body().split("&")) {
            String[] pair = keyValuePair.split("=");
            if (pair[0].equalsIgnoreCase(bodyParam)) {
                return pair[1];
            }
        }
        return null;
    }

    public String formParam(String formParam) {
        return bodyParam(formParam);
    }

    public String param(String param) {
        if (param == null) {
            return null;
        }
        if (!param.startsWith(":")) {
            param = ":" + param;
        }
        return paramMap.get(param.toLowerCase());
    }

    public Map<String, String> paramMap() {
        return Collections.unmodifiableMap(paramMap);
    }

    public String splat(int splatNr) {
        return splatList.get(splatNr);
    }

    public String[] splats() {
        return splatList.toArray(new String[splatList.size()]);
    }

    // wrapper methods for HttpServletRequest

    public void attribute(String attribute, Object value) {
        servletRequest.setAttribute(attribute, value);
    }

    public <T> T attribute(String attribute) {
        return (T) servletRequest.getAttribute(attribute);
    }

    public Map<String, String> attributeMap() {
        return Collections.list(servletRequest.getAttributeNames()).stream().collect(Collectors.toMap(a -> a, this::attribute));
    }

    public int contentLength() {
        return servletRequest.getContentLength();
    }

    public String contentType() {
        return servletRequest.getContentType();
    }

    public String cookie(String name) {
        return Stream.of(Optional.ofNullable(servletRequest.getCookies()).orElse(new Cookie[] {}))
            .filter(cookie -> cookie.getName().equals(name))
            .map(Cookie::getValue)
            .findFirst()
            .orElse(null);
    }

    public Map<String, String> cookieMap() {
        Cookie[] cookies = Optional.ofNullable(servletRequest.getCookies()).orElse(new Cookie[] {});
        return Stream.of(cookies).collect(Collectors.toMap(Cookie::getName, Cookie::getValue));
    }

    public String header(String header) {
        return servletRequest.getHeader(header);
    }

    public Map<String, String> headerMap() {
        return Collections.list(servletRequest.getHeaderNames()).stream().collect(Collectors.toMap(h -> h, this::header));
    }

    public String host() {
        return servletRequest.getHeader("host");
    }

    public String ip() {
        return servletRequest.getRemoteAddr();
    }

    public void next() {
        passedToNextHandler = true;
    }

    public boolean nexted() {
        return passedToNextHandler;
    }

    public String path() {
        return servletRequest.getPathInfo();
    }

    public int port() {
        return servletRequest.getServerPort();
    }

    public String protocol() {
        return servletRequest.getProtocol();
    }

    public String queryParam(String queryParam) {
        return servletRequest.getParameter(queryParam);
    }

    public String queryParamOrDefault(String queryParam, String defaultValue) {
        return Optional.ofNullable(servletRequest.getParameter(queryParam)).orElse(defaultValue);
    }

    public String[] queryParams(String queryParam) {
        return servletRequest.getParameterValues(queryParam);
    }

    public Map<String, String[]> queryParamMap() {
        return servletRequest.getParameterMap();
    }

    public String queryString() {
        return servletRequest.getQueryString();
    }

    public String requestMethod() {
        return servletRequest.getMethod();
    }

    public String scheme() {
        return servletRequest.getScheme();
    }

    public String uri() {
        return servletRequest.getRequestURI();
    }

    public String url() {
        return servletRequest.getRequestURL().toString();
    }

    public String userAgent() {
        return servletRequest.getHeader("user-agent");
    }

}
