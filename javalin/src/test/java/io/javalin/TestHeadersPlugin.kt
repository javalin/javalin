/*
 * Javalin - https://javalin.io
 * Copyright 2021 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin

import io.javalin.http.Header
import io.javalin.plugin.Headers
import io.javalin.plugin.Headers.ClearSiteData
import io.javalin.plugin.Headers.CrossDomainPolicy
import io.javalin.plugin.Headers.CrossOriginEmbedderPolicy
import io.javalin.plugin.Headers.CrossOriginOpenerPolicy
import io.javalin.plugin.Headers.CrossOriginResourcePolicy
import io.javalin.plugin.Headers.ReferrerPolicy
import io.javalin.plugin.Headers.XFrameOptions
import io.javalin.testing.TestUtil
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Duration

class TestHeadersPlugin {

    private val headers = Headers()

    @Test
    fun `header values X-Content-Type-Options NoSniff`() {
        headers.xContentTypeOptionsNoSniff()
        assertThat(headers.headers).containsEntry(Header.X_CONTENT_TYPE_OPTIONS, "nosniff")
    }

    @Test
    fun `header values Strict-Transport-Security `() {
        headers.strictTransportSecurity(Duration.ofSeconds(10), false)
        assertThat(headers.headers).containsEntry(Header.STRICT_TRANSPORT_SECURITY, "max-age=10")
    }

    @Test
    fun `header values Strict-Transport-Security includeSubDomain`() {
        headers.strictTransportSecurity(Duration.ofSeconds(10), true)
        assertThat(headers.headers).containsEntry(Header.STRICT_TRANSPORT_SECURITY, "max-age=10 ; includeSubDomains")
    }

    @Test
    fun `header values X-Frame-Options deny`() {
        headers.xFrameOptions(XFrameOptions.DENY)
        assertThat(headers.headers).containsEntry(Header.X_FRAME_OPTIONS, "deny")
    }

    @Test
    fun `header values X-Frame-Options same origin`() {
        headers.xFrameOptions(XFrameOptions.SAMEORIGIN)
        assertThat(headers.headers).containsEntry(Header.X_FRAME_OPTIONS, "sameorigin")
    }

    @Test
    fun `header values X-Frame-Options allow-from`() {
        headers.xFrameOptions("web.de")
        assertThat(headers.headers).containsEntry(Header.X_FRAME_OPTIONS, "allow-from: web.de")
    }

    @Test
    fun `header values Content-Security-Policy`() {
        headers.contentSecurityPolicy("foo")
        assertThat(headers.headers).containsEntry(Header.CONTENT_SECURITY_POLICY, "foo")
    }

    @Test
    fun `header values X-Permitted-Cross-Domain-Policies master-only`() {
        headers.xPermittedCrossDomainPolicies(CrossDomainPolicy.MASTER_ONLY)
        assertThat(headers.headers).containsEntry(Header.X_PERMITTED_CROSS_DOMAIN_POLICIES, "master-only")
    }

    @Test
    fun `header values X-Permitted-Cross-Domain-Policies none`() {
        headers.xPermittedCrossDomainPolicies(CrossDomainPolicy.NONE)
        assertThat(headers.headers).containsEntry(Header.X_PERMITTED_CROSS_DOMAIN_POLICIES, "none")
    }

    @Test
    fun `header values X-Permitted-Cross-Domain-Policies by-content-type`() {
        headers.xPermittedCrossDomainPolicies(CrossDomainPolicy.BY_CONTENT_TYPE)
        assertThat(headers.headers).containsEntry(Header.X_PERMITTED_CROSS_DOMAIN_POLICIES, "by-content-type")
    }

    @Test
    fun `header values Referrer-Policy`() {
        headers.referrerPolicy(ReferrerPolicy.STRICT_ORIGIN)
        assertThat(headers.headers).containsEntry(Header.REFERRER_POLICY, "strict-origin")
    }

    @Test
    fun `header values Clear-Site-Data any`() {
        headers.clearSiteData(ClearSiteData.ANY)
        assertThat(headers.headers).containsEntry(Header.CLEAR_SITE_DATA, "\"*\"")
    }

    @Test
    fun `header values Clear-Site-Data as array`() {
        headers.clearSiteData(ClearSiteData.ANY, ClearSiteData.EXECUTION_CONTEXTS, ClearSiteData.STORAGE)
        assertThat(headers.headers).containsEntry(Header.CLEAR_SITE_DATA, "\"*\",\"executionContexts\",\"storage\"")
    }

    @Test
    fun `header values Cross-Origin-Embedder-Policy`() {
        headers.crossOriginEmbedderPolicy(CrossOriginEmbedderPolicy.UNSAFE_NONE)
        assertThat(headers.headers).containsEntry(Header.CROSS_ORIGIN_EMBEDDER_POLICY, "unsafe-none")
    }

    @Test
    fun `header values Cross-Origin-Opener-Policy`() {
        headers.crossOriginOpenerPolicy(CrossOriginOpenerPolicy.SAME_ORIGIN_ALLOW_POPUPS)
        assertThat(headers.headers).containsEntry(Header.CROSS_ORIGIN_OPENER_POLICY, "same-origin-allow-popups")
    }

    @Test
    fun `header values Cross-Origin-Resource-Policy`() {
        headers.crossOriginResourcePolicy(CrossOriginResourcePolicy.SAME_SITE)
        assertThat(headers.headers).containsEntry(Header.CROSS_ORIGIN_RESOURCE_POLICY, "same-site")
    }

    @Test
    fun `test headers are set on app`() {
        val headers = Headers()
        headers.xContentTypeOptionsNoSniff()
        headers.clearSiteData(ClearSiteData.ANY)

        val testApp = Javalin.create { it.plugins.enableGlobalHeaders { headers } }
        TestUtil.test(testApp) { app, http ->
            app.get("/") { it.status(200) }
            val returnedHeaders = http.get("/").headers
            assertThat(returnedHeaders.getFirst(Header.X_CONTENT_TYPE_OPTIONS.name)).isEqualTo("nosniff")
            assertThat(returnedHeaders.getFirst(Header.CLEAR_SITE_DATA.name)).isEqualTo("\"*\"")
        }
    }
}
