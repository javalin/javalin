/*
 * Javalin - https://javalin.io
 * Copyright 2021 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin

import io.javalin.http.Header
import io.javalin.http.HttpStatus.OK
import io.javalin.plugin.bundled.GlobalHeaderConfig
import io.javalin.plugin.bundled.GlobalHeaderConfig.ClearSiteData
import io.javalin.plugin.bundled.GlobalHeaderConfig.CrossDomainPolicy
import io.javalin.plugin.bundled.GlobalHeaderConfig.CrossOriginEmbedderPolicy
import io.javalin.plugin.bundled.GlobalHeaderConfig.CrossOriginOpenerPolicy
import io.javalin.plugin.bundled.GlobalHeaderConfig.CrossOriginResourcePolicy
import io.javalin.plugin.bundled.GlobalHeaderConfig.ReferrerPolicy
import io.javalin.plugin.bundled.GlobalHeaderConfig.XFrameOptions
import io.javalin.testing.TestUtil
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Duration

class TestGlobalGlobalHeaderConfigPlugin {

    private val globalHeaderConfig = GlobalHeaderConfig()

    @Test
    fun `header values X-Content-Type-Options NoSniff`() {
        globalHeaderConfig.xContentTypeOptionsNoSniff()
        assertThat(globalHeaderConfig.headers).containsEntry(Header.X_CONTENT_TYPE_OPTIONS, "nosniff")
    }

    @Test
    fun `header values Strict-Transport-Security `() {
        globalHeaderConfig.strictTransportSecurity(Duration.ofSeconds(10), false)
        assertThat(globalHeaderConfig.headers).containsEntry(Header.STRICT_TRANSPORT_SECURITY, "max-age=10")
    }

    @Test
    fun `header values Strict-Transport-Security includeSubDomain`() {
        globalHeaderConfig.strictTransportSecurity(Duration.ofSeconds(10), true)
        assertThat(globalHeaderConfig.headers).containsEntry(Header.STRICT_TRANSPORT_SECURITY, "max-age=10 ; includeSubDomains")
    }

    @Test
    fun `header values X-Frame-Options deny`() {
        globalHeaderConfig.xFrameOptions(XFrameOptions.DENY)
        assertThat(globalHeaderConfig.headers).containsEntry(Header.X_FRAME_OPTIONS, "deny")
    }

    @Test
    fun `header values X-Frame-Options same origin`() {
        globalHeaderConfig.xFrameOptions(XFrameOptions.SAMEORIGIN)
        assertThat(globalHeaderConfig.headers).containsEntry(Header.X_FRAME_OPTIONS, "sameorigin")
    }

    @Test
    fun `header values X-Frame-Options allow-from`() {
        globalHeaderConfig.xFrameOptions("web.de")
        assertThat(globalHeaderConfig.headers).containsEntry(Header.X_FRAME_OPTIONS, "allow-from: web.de")
    }

    @Test
    fun `header values Content-Security-Policy`() {
        globalHeaderConfig.contentSecurityPolicy("foo")
        assertThat(globalHeaderConfig.headers).containsEntry(Header.CONTENT_SECURITY_POLICY, "foo")
    }

    @Test
    fun `header values X-Permitted-Cross-Domain-Policies master-only`() {
        globalHeaderConfig.xPermittedCrossDomainPolicies(CrossDomainPolicy.MASTER_ONLY)
        assertThat(globalHeaderConfig.headers).containsEntry(Header.X_PERMITTED_CROSS_DOMAIN_POLICIES, "master-only")
    }

    @Test
    fun `header values X-Permitted-Cross-Domain-Policies none`() {
        globalHeaderConfig.xPermittedCrossDomainPolicies(CrossDomainPolicy.NONE)
        assertThat(globalHeaderConfig.headers).containsEntry(Header.X_PERMITTED_CROSS_DOMAIN_POLICIES, "none")
    }

    @Test
    fun `header values X-Permitted-Cross-Domain-Policies by-content-type`() {
        globalHeaderConfig.xPermittedCrossDomainPolicies(CrossDomainPolicy.BY_CONTENT_TYPE)
        assertThat(globalHeaderConfig.headers).containsEntry(Header.X_PERMITTED_CROSS_DOMAIN_POLICIES, "by-content-type")
    }

    @Test
    fun `header values Referrer-Policy`() {
        globalHeaderConfig.referrerPolicy(ReferrerPolicy.STRICT_ORIGIN)
        assertThat(globalHeaderConfig.headers).containsEntry(Header.REFERRER_POLICY, "strict-origin")
    }

    @Test
    fun `header values Clear-Site-Data any`() {
        globalHeaderConfig.clearSiteData(ClearSiteData.ANY)
        assertThat(globalHeaderConfig.headers).containsEntry(Header.CLEAR_SITE_DATA, """"*"""")
    }

    @Test
    fun `header values Clear-Site-Data as array`() {
        globalHeaderConfig.clearSiteData(ClearSiteData.ANY, ClearSiteData.EXECUTION_CONTEXTS, ClearSiteData.STORAGE)
        assertThat(globalHeaderConfig.headers).containsEntry(Header.CLEAR_SITE_DATA, """"*","executionContexts","storage"""")
    }

    @Test
    fun `header values Cross-Origin-Embedder-Policy`() {
        globalHeaderConfig.crossOriginEmbedderPolicy(CrossOriginEmbedderPolicy.UNSAFE_NONE)
        assertThat(globalHeaderConfig.headers).containsEntry(Header.CROSS_ORIGIN_EMBEDDER_POLICY, "unsafe-none")
    }

    @Test
    fun `header values Cross-Origin-Opener-Policy`() {
        globalHeaderConfig.crossOriginOpenerPolicy(CrossOriginOpenerPolicy.SAME_ORIGIN_ALLOW_POPUPS)
        assertThat(globalHeaderConfig.headers).containsEntry(Header.CROSS_ORIGIN_OPENER_POLICY, "same-origin-allow-popups")
    }

    @Test
    fun `header values Cross-Origin-Resource-Policy`() {
        globalHeaderConfig.crossOriginResourcePolicy(CrossOriginResourcePolicy.SAME_SITE)
        assertThat(globalHeaderConfig.headers).containsEntry(Header.CROSS_ORIGIN_RESOURCE_POLICY, "same-site")
    }

    @Test
    fun `test headers are set on app`() {
        val globalHeaderConfig = GlobalHeaderConfig()
        globalHeaderConfig.xContentTypeOptionsNoSniff()
        globalHeaderConfig.clearSiteData(ClearSiteData.ANY)

        val testApp = Javalin.create { it.plugins.enableGlobalHeaders { globalHeaderConfig } }
        TestUtil.test(testApp) { app, http ->
            app.get("/") { it.status(OK) }
            val returnedHeaders = http.get("/").headers
            assertThat(returnedHeaders.getFirst(Header.X_CONTENT_TYPE_OPTIONS)).isEqualTo("nosniff")
            assertThat(returnedHeaders.getFirst(Header.CLEAR_SITE_DATA)).isEqualTo(""""*"""")
        }
    }
}
