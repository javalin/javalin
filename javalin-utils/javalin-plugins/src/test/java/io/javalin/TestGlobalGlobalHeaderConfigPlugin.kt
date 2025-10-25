/*
 * Javalin - https://javalin.io
 * Copyright 2021 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin

import io.javalin.http.Header
import io.javalin.http.HttpStatus.OK
import io.javalin.plugin.GlobalHeadersConfig
import io.javalin.plugin.GlobalHeadersConfig.ClearSiteData
import io.javalin.plugin.GlobalHeadersConfig.CrossDomainPolicy
import io.javalin.plugin.GlobalHeadersConfig.CrossOriginEmbedderPolicy
import io.javalin.plugin.GlobalHeadersConfig.CrossOriginOpenerPolicy
import io.javalin.plugin.GlobalHeadersConfig.CrossOriginResourcePolicy
import io.javalin.plugin.GlobalHeadersConfig.ReferrerPolicy
import io.javalin.plugin.GlobalHeadersConfig.XFrameOptions
import io.javalin.plugin.GlobalHeadersPlugin
import io.javalin.testtools.JavalinTest
import io.javalin.testing.header
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Duration

class TestGlobalGlobalHeaderConfigPlugin {

    private val globalHeadersConfig = GlobalHeadersConfig()

    @Test
    fun `header values X-Content-Type-Options NoSniff`() {
        globalHeadersConfig.xContentTypeOptionsNoSniff()
        assertThat(globalHeadersConfig.headers).containsEntry(Header.X_CONTENT_TYPE_OPTIONS, "nosniff")
    }

    @Test
    fun `header values Strict-Transport-Security `() {
        globalHeadersConfig.strictTransportSecurity(Duration.ofSeconds(10), false)
        assertThat(globalHeadersConfig.headers).containsEntry(Header.STRICT_TRANSPORT_SECURITY, "max-age=10")
    }

    @Test
    fun `header values Strict-Transport-Security includeSubDomain`() {
        globalHeadersConfig.strictTransportSecurity(Duration.ofSeconds(10), true)
        assertThat(globalHeadersConfig.headers).containsEntry(Header.STRICT_TRANSPORT_SECURITY, "max-age=10 ; includeSubDomains")
    }

    @Test
    fun `header values X-Frame-Options deny`() {
        globalHeadersConfig.xFrameOptions(XFrameOptions.DENY)
        assertThat(globalHeadersConfig.headers).containsEntry(Header.X_FRAME_OPTIONS, "deny")
    }

    @Test
    fun `header values X-Frame-Options same origin`() {
        globalHeadersConfig.xFrameOptions(XFrameOptions.SAMEORIGIN)
        assertThat(globalHeadersConfig.headers).containsEntry(Header.X_FRAME_OPTIONS, "sameorigin")
    }

    @Test
    fun `header values X-Frame-Options allow-from`() {
        globalHeadersConfig.xFrameOptions("web.de")
        assertThat(globalHeadersConfig.headers).containsEntry(Header.X_FRAME_OPTIONS, "allow-from: web.de")
    }

    @Test
    fun `header values Content-Security-Policy`() {
        globalHeadersConfig.contentSecurityPolicy("foo")
        assertThat(globalHeadersConfig.headers).containsEntry(Header.CONTENT_SECURITY_POLICY, "foo")
    }

    @Test
    fun `header values X-Permitted-Cross-Domain-Policies master-only`() {
        globalHeadersConfig.xPermittedCrossDomainPolicies(CrossDomainPolicy.MASTER_ONLY)
        assertThat(globalHeadersConfig.headers).containsEntry(Header.X_PERMITTED_CROSS_DOMAIN_POLICIES, "master-only")
    }

    @Test
    fun `header values X-Permitted-Cross-Domain-Policies none`() {
        globalHeadersConfig.xPermittedCrossDomainPolicies(CrossDomainPolicy.NONE)
        assertThat(globalHeadersConfig.headers).containsEntry(Header.X_PERMITTED_CROSS_DOMAIN_POLICIES, "none")
    }

    @Test
    fun `header values X-Permitted-Cross-Domain-Policies by-content-type`() {
        globalHeadersConfig.xPermittedCrossDomainPolicies(CrossDomainPolicy.BY_CONTENT_TYPE)
        assertThat(globalHeadersConfig.headers).containsEntry(Header.X_PERMITTED_CROSS_DOMAIN_POLICIES, "by-content-type")
    }

    @Test
    fun `header values Referrer-Policy`() {
        globalHeadersConfig.referrerPolicy(ReferrerPolicy.STRICT_ORIGIN)
        assertThat(globalHeadersConfig.headers).containsEntry(Header.REFERRER_POLICY, "strict-origin")
    }

    @Test
    fun `header values Clear-Site-Data any`() {
        globalHeadersConfig.clearSiteData(ClearSiteData.ANY)
        assertThat(globalHeadersConfig.headers).containsEntry(Header.CLEAR_SITE_DATA, """"*"""")
    }

    @Test
    fun `header values Clear-Site-Data as array`() {
        globalHeadersConfig.clearSiteData(ClearSiteData.ANY, ClearSiteData.EXECUTION_CONTEXTS, ClearSiteData.STORAGE)
        assertThat(globalHeadersConfig.headers).containsEntry(Header.CLEAR_SITE_DATA, """"*","executionContexts","storage"""")
    }

    @Test
    fun `header values Cross-Origin-Embedder-Policy`() {
        globalHeadersConfig.crossOriginEmbedderPolicy(CrossOriginEmbedderPolicy.UNSAFE_NONE)
        assertThat(globalHeadersConfig.headers).containsEntry(Header.CROSS_ORIGIN_EMBEDDER_POLICY, "unsafe-none")
    }

    @Test
    fun `header values Cross-Origin-Opener-Policy`() {
        globalHeadersConfig.crossOriginOpenerPolicy(CrossOriginOpenerPolicy.SAME_ORIGIN_ALLOW_POPUPS)
        assertThat(globalHeadersConfig.headers).containsEntry(Header.CROSS_ORIGIN_OPENER_POLICY, "same-origin-allow-popups")
    }

    @Test
    fun `header values Cross-Origin-Resource-Policy`() {
        globalHeadersConfig.crossOriginResourcePolicy(CrossOriginResourcePolicy.SAME_SITE)
        assertThat(globalHeadersConfig.headers).containsEntry(Header.CROSS_ORIGIN_RESOURCE_POLICY, "same-site")
    }

    @Test
    fun `test headers are set on app`() {
        val testApp = Javalin.create { cfg ->
            cfg.registerPlugin(GlobalHeadersPlugin {
                it.xContentTypeOptionsNoSniff()
                it.clearSiteData(ClearSiteData.ANY)
            })
        }
        JavalinTest.test(testApp) { app, http ->
            app.unsafe.routes.get("/") { it.status(200) }
            val response = http.get("/")
            assertThat(response.header(Header.X_CONTENT_TYPE_OPTIONS)).isEqualTo("nosniff")
            assertThat(response.header(Header.CLEAR_SITE_DATA)).isEqualTo(""""*"""")
        }
    }
}
