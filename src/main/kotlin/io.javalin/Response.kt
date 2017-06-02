/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin

import java.io.IOException
import java.io.InputStream

import javax.servlet.http.Cookie
import javax.servlet.http.HttpServletResponse

import org.slf4j.LoggerFactory

import io.javalin.builder.CookieBuilder
import io.javalin.core.util.Util
import io.javalin.translator.json.Jackson
import io.javalin.translator.template.Freemarker
import io.javalin.translator.template.Mustache
import io.javalin.translator.template.Thymeleaf
import io.javalin.translator.template.Velocity

class Response(private val servletResponse: HttpServletResponse) {

    private val log = LoggerFactory.getLogger(Response::class.java)

    private var body: String? = null
    private var bodyStream: InputStream? = null

    private var encoding: String? = null

    fun unwrap(): HttpServletResponse {
        return servletResponse
    }

    fun contentType(): String? {
        return servletResponse.contentType
    }

    fun contentType(contentType: String): Response {
        servletResponse.contentType = contentType
        return this
    }

    fun body(): String? {
        return body
    }

    fun body(body: String): Response {
        this.body = body
        this.bodyStream = null // can only have one or the other
        return this
    }

    fun bodyStream(): InputStream? {
        return bodyStream
    }

    fun body(bodyStream: InputStream): Response {
        this.body = null // can only have one or the other
        this.bodyStream = bodyStream
        return this
    }

    fun encoding(): String? {
        return encoding
    }

    fun encoding(charset: String): Response {
        encoding = charset
        return this
    }

    fun header(headerName: String) {
        servletResponse.getHeader(headerName)
    }

    fun header(headerName: String, headerValue: String): Response {
        servletResponse.setHeader(headerName, headerValue)
        return this
    }

    fun html(html: String): Response {
        return body(html).contentType("text/html")
    }

    fun redirect(location: String): Response {
        try {
            servletResponse.sendRedirect(location)
        } catch (e: IOException) {
            log.warn("Exception while trying to redirect", e)
        }

        return this
    }

    fun redirect(location: String, httpStatusCode: Int): Response {
        servletResponse.status = httpStatusCode
        servletResponse.setHeader("Location", location)
        servletResponse.setHeader("Connection", "close")
        try {
            servletResponse.sendError(httpStatusCode)
        } catch (e: IOException) {
            log.warn("Exception while trying to redirect", e)
        }

        return this
    }

    fun status(): Int {
        return servletResponse.status
    }

    fun status(statusCode: Int): Response {
        servletResponse.status = statusCode
        return this
    }

    // cookie methods

    fun cookie(name: String, value: String): Response {
        return cookie(CookieBuilder.cookieBuilder(name, value))
    }

    fun cookie(name: String, value: String, maxAge: Int): Response {
        return cookie(CookieBuilder.cookieBuilder(name, value).maxAge(maxAge))
    }

    fun cookie(cookieBuilder: CookieBuilder): Response {
        val cookie = Cookie(cookieBuilder.name(), cookieBuilder.value())
        cookie.path = cookieBuilder.path()
        cookie.domain = cookieBuilder.domain()
        cookie.maxAge = cookieBuilder.maxAge()
        cookie.secure = cookieBuilder.secure()
        cookie.isHttpOnly = cookieBuilder.httpOnly()
        servletResponse.addCookie(cookie)
        return this
    }

    fun removeCookie(name: String): Response {
        return removeCookie(null, name)
    }

    fun removeCookie(path: String?, name: String): Response {
        val cookie = Cookie(name, "")
        cookie.path = path
        cookie.maxAge = 0
        servletResponse.addCookie(cookie)
        return this
    }

    // Translator methods

    fun json(`object`: Any): Response {
        Util.ensureDependencyPresent("Jackson", "com.fasterxml.jackson.databind.ObjectMapper", "com.fasterxml.jackson.core/jackson-databind")
        return body(Jackson.toJson(`object`)).contentType("application/json")
    }

    fun renderVelocity(templatePath: String, model: Map<String, Any>): Response {
        Util.ensureDependencyPresent("Apache Velocity", "org.apache.velocity.Template", "org.apache.velocity/velocity")
        return html(Velocity.render(templatePath, model))
    }

    fun renderFreemarker(templatePath: String, model: Map<String, Any>): Response {
        Util.ensureDependencyPresent("Apache Freemarker", "freemarker.template.Configuration", "org.freemarker/freemarker")
        return html(Freemarker.render(templatePath, model))
    }

    fun renderThymeleaf(templatePath: String, model: Map<String, Any>): Response {
        Util.ensureDependencyPresent("Thymeleaf", "org.thymeleaf.TemplateEngine", "org.thymeleaf/thymeleaf-spring3")
        return html(Thymeleaf.render(templatePath, model))
    }

    fun renderMustache(templatePath: String, model: Map<String, Any>): Response {
        Util.ensureDependencyPresent("Mustache", "com.github.mustachejava.Mustache", "com.github.spullara.mustache.java/compiler")
        return html(Mustache.render(templatePath, model))
    }

}
