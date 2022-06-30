/*
 * Javalin - https://javalin.io
 * Copyright 2021 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin

import io.javalin.core.JavalinConfig
import io.javalin.core.util.Header
import io.javalin.core.util.Headers
import io.javalin.core.util.Headers.ClearSiteData
import io.javalin.core.util.Headers.CrossDomainPolicy
import io.javalin.core.util.Headers.CrossOriginEmbedderPolicy
import io.javalin.core.util.Headers.CrossOriginOpenerPolicy
import io.javalin.core.util.Headers.CrossOriginResourcePolicy
import io.javalin.core.util.Headers.ReferrerPolicy
import io.javalin.core.util.Headers.XFrameOptions
import io.javalin.core.util.HeadersPlugin
import io.javalin.testing.TestUtil
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Duration

class TestHeadersPlugin {

    private val headers = Headers()

    @Test
    fun `header values X-Content-Type-Options NoSniff`() {
        headers.xContentTypeOptionsNoSniff()
        assertThat(headers.headers).containsEntry("X-Content-Type-Options", "nosniff")
    }

    @Test
    fun `header values Strict-Transport-Security `() {
        headers.strictTransportSecurity(Duration.ofSeconds(10), false)
        assertThat(headers.headers).containsEntry("Strict-Transport-Security", "max-age=10")
    }

    @Test
    fun `header values Strict-Transport-Security includeSubDomain`() {
        headers.strictTransportSecurity(Duration.ofSeconds(10), true)
        assertThat(headers.headers).containsEntry("Strict-Transport-Security", "max-age=10 ; includeSubDomains")
    }

    @Test
    fun `header values X-Frame-Options deny`() {
        headers.xFrameOptions(XFrameOptions.DENY)
        assertThat(headers.headers).containsEntry("X-Frame-Options", "deny")
    }

    @Test
    fun `header values X-Frame-Options same origin`() {
        headers.xFrameOptions(XFrameOptions.SAMEORIGIN)
        assertThat(headers.headers).containsEntry("X-Frame-Options", "sameorigin")
    }

    @Test
    fun `header values X-Frame-Options allow-from`() {
        headers.xFrameOptions("web.de")
        assertThat(headers.headers).containsEntry("X-Frame-Options", "allow-from: web.de")
    }

    @Test
    fun `header values Content-Security-Policy`() {
        headers.contentSecurityPolicy("foo")
        assertThat(headers.headers).containsEntry("Content-Security-Policy", "foo")
    }

    @Test
    fun `header values X-Permitted-Cross-Domain-Policies master-only`() {
        headers.xPermittedCrossDomainPolicies(CrossDomainPolicy.MASTER_ONLY)
        assertThat(headers.headers).containsEntry("X-Permitted-Cross-Domain-Policies", "master-only")
    }

    @Test
    fun `header values X-Permitted-Cross-Domain-Policies none`() {
        headers.xPermittedCrossDomainPolicies(CrossDomainPolicy.NONE)
        assertThat(headers.headers).containsEntry("X-Permitted-Cross-Domain-Policies", "none")
    }

    @Test
    fun `header values X-Permitted-Cross-Domain-Policies by-content-type`() {
        headers.xPermittedCrossDomainPolicies(CrossDomainPolicy.BY_CONTENT_TYPE)
        assertThat(headers.headers).containsEntry("X-Permitted-Cross-Domain-Policies", "by-content-type")
    }

    @Test
    fun `header values Referrer-Policy`() {
        headers.referrerPolicy(ReferrerPolicy.STRICT_ORIGIN)
        assertThat(headers.headers).containsEntry("Referrer-Policy", "strict-origin")
    }

    @Test
    fun `header values Clear-Site-Data any`() {
        headers.clearSiteData(ClearSiteData.ANY)
        assertThat(headers.headers).containsEntry("Clear-Site-Data", "\"*\"")
    }

    @Test
    fun `header values Clear-Site-Data as array`() {
        headers.clearSiteData(ClearSiteData.ANY, ClearSiteData.EXECUTION_CONTEXTS, ClearSiteData.STORAGE)
        assertThat(headers.headers).containsEntry("Clear-Site-Data", "\"*\",\"executionContexts\",\"storage\"")
    }

    @Test
    fun `header values Cross-Origin-Embedder-Policy`() {
        headers.crossOriginEmbedderPolicy(CrossOriginEmbedderPolicy.UNSAFE_NONE)
        assertThat(headers.headers).containsEntry("Cross-Origin-Embedder-Policy", "unsafe-none")
    }

    @Test
    fun `header values Cross-Origin-Opener-Policy`() {
        headers.crossOriginOpenerPolicy(CrossOriginOpenerPolicy.SAME_ORIGIN_ALLOW_POPUPS)
        assertThat(headers.headers).containsEntry("Cross-Origin-Opener-Policy", "same-origin-allow-popups")
    }

    @Test
    fun `header values Cross-Origin-Resource-Policy`() {
        headers.crossOriginResourcePolicy(CrossOriginResourcePolicy.SAME_SITE)
        assertThat(headers.headers).containsEntry("Cross-Origin-Resource-Policy", "same-site")
    }

    @Test
    fun `test plugin is registered when calling headers method`() {
        val javalin = Javalin.create { config: JavalinConfig -> config.globalHeaders { Headers() } }
        val retrievedPlugin: HeadersPlugin = javalin.cfg.getPlugin(HeadersPlugin::class.java)
        assertThat(retrievedPlugin).isSameAs(retrievedPlugin)
    }

    @Test
    fun `test headers are set when startung`() {
        val headers = Headers()
        headers.xContentTypeOptionsNoSniff()
        headers.clearSiteData(ClearSiteData.ANY)

        val testApp = Javalin.create { config: JavalinConfig -> config.globalHeaders { headers } }
        TestUtil.test(testApp) { app, http ->
            app.get("/") { it.status(200) }
            val returnedHeaders = http.get("/").headers
            assertThat(returnedHeaders.getFirst(Header.X_CONTENT_TYPE_OPTIONS)).isEqualTo("nosniff")
            assertThat(returnedHeaders.getFirst(Header.CLEAR_SITE_DATA)).isEqualTo("\"*\"")
        }
    }
}
