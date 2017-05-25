// Javalin - https://javalin.io
// Copyright 2017 David Åse
// Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE

package javalin;

import java.io.IOException;
import java.util.Map;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javalin.builder.CookieBuilder;
import javalin.core.util.Util;

import static javalin.ResponseMapper.TemplateUtil.*;
import static javalin.builder.CookieBuilder.*;

public class Response {

    private static Logger log = LoggerFactory.getLogger(Response.class);

    private HttpServletResponse servletResponse;
    private String body;
    private String encoding;

    public Response(HttpServletResponse servletResponse) {
        this.servletResponse = servletResponse;
    }

    public HttpServletResponse unwrap() {
        return servletResponse;
    }

    public String contentType() {
        return servletResponse.getContentType();
    }

    public Response contentType(String contentType) {
        servletResponse.setContentType(contentType);
        return this;
    }

    public String body() {
        return body;
    }

    public Response body(String body) {
        this.body = body;
        return this;
    }

    public String encoding() {
        return encoding;
    }

    public Response encoding(String charset) {
        encoding = charset;
        return this;
    }

    public void header(String headerName) {
        servletResponse.getHeader(headerName);
    }

    public Response header(String headerName, String headerValue) {
        servletResponse.setHeader(headerName, headerValue);
        return this;
    }

    public Response html(String html) {
        return body(html).contentType("text/html");
    }

    public Response redirect(String location) {
        try {
            servletResponse.sendRedirect(location);
        } catch (IOException e) {
            log.warn("Exception while trying to redirect", e);
        }
        return this;
    }

    public Response redirect(String location, int httpStatusCode) {
        servletResponse.setStatus(httpStatusCode);
        servletResponse.setHeader("Location", location);
        servletResponse.setHeader("Connection", "close");
        try {
            servletResponse.sendError(httpStatusCode);
        } catch (IOException e) {
            log.warn("Exception while trying to redirect", e);
        }
        return this;
    }

    public int status() {
        return servletResponse.getStatus();
    }

    public Response status(int statusCode) {
        servletResponse.setStatus(statusCode);
        return this;
    }

    // cookie methods

    public Response cookie(String name, String value) {
        return cookie(cookieBuilder(name, value));
    }

    public Response cookie(String name, String value, int maxAge) {
        return cookie(cookieBuilder(name, value).maxAge(maxAge));
    }

    public Response cookie(CookieBuilder cookieBuilder) {
        Cookie cookie = new Cookie(cookieBuilder.name(), cookieBuilder.value());
        cookie.setPath(cookieBuilder.path());
        cookie.setDomain(cookieBuilder.domain());
        cookie.setMaxAge(cookieBuilder.maxAge());
        cookie.setSecure(cookieBuilder.secure());
        cookie.setHttpOnly(cookieBuilder.httpOnly());
        servletResponse.addCookie(cookie);
        return this;
    }

    public Response removeCookie(String name) {
        return removeCookie(null, name);
    }

    public Response removeCookie(String path, String name) {
        Cookie cookie = new Cookie(name, "");
        cookie.setPath(path);
        cookie.setMaxAge(0);
        servletResponse.addCookie(cookie);
        return this;
    }

    // ResponseMapping methods

    public Response json(Object object) {
        ResponseMapper.ensureDependencyPresent("Jackson", "com.fasterxml.jackson.databind.ObjectMapper", "com.fasterxml.jackson.core/jackson-databind");
        return body(ResponseMapper.Jackson.toJson(object)).contentType("application/json");
    }

    public Response renderVelocity(String templatePath, Map<String, Object> model) {
        ResponseMapper.ensureDependencyPresent("Apache Velocity", "org.apache.velocity.Template", "org.apache.velocity/velocity");
        return html(ResponseMapper.Velocity.renderVelocityTemplate(templatePath, model));
    }

    public Response renderFreemarker(String templatePath, Map<String, Object> model) {
        ResponseMapper.ensureDependencyPresent("Apache Freemarker", "freemarker.template.Configuration", "org.freemarker/freemarker");
        return html(ResponseMapper.Freemarker.renderFreemarkerTemplate(templatePath, model));
    }

}
