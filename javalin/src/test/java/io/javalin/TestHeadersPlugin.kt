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
import org.junit.Test
import java.time.Duration

class TestHeadersPlugin {

    @Test
    fun `ensure header value are set correct`() {
        val headers = Headers()
        headers.xContentTypeOptionsNoSniff()
        assertThat(headers.headers).containsEntry("X-Content-Type-Options", "nosniff")
        headers.strictTransportSecurity(Duration.ofSeconds(10), false)
        assertThat(headers.headers).containsEntry("Strict-Transport-Security", "max-age=10")
        headers.strictTransportSecurity(Duration.ofSeconds(10), true)
        assertThat(headers.headers).containsEntry("Strict-Transport-Security", "max-age=10 ; includeSubDomains")
        headers.xFrameOptions(XFrameOptions.DENY)
        assertThat(headers.headers).containsEntry("X-Frame-Options", "deny")
        headers.xFrameOptions(XFrameOptions.SAMEORIGIN)
        assertThat(headers.headers).containsEntry("X-Frame-Options", "sameorigin")
        headers.xFrameOptions("web.de")
        assertThat(headers.headers).containsEntry("X-Frame-Options", "allow-from: web.de")
        headers.contentSecurityPolicy("foo")
        assertThat(headers.headers).containsEntry("Content-Security-Policy", "foo")
        headers.xPermittedCrossDomainPolicies(CrossDomainPolicy.MASTER_ONLY)
        assertThat(headers.headers).containsEntry("X-Permitted-Cross-Domain-Policies", "master-only")
        headers.xPermittedCrossDomainPolicies(CrossDomainPolicy.NONE)
        assertThat(headers.headers).containsEntry("X-Permitted-Cross-Domain-Policies", "none")
        headers.xPermittedCrossDomainPolicies(CrossDomainPolicy.BY_CONTENT_TYPE)
        assertThat(headers.headers).containsEntry("X-Permitted-Cross-Domain-Policies", "by-content-type")
        headers.referrerPolicy(ReferrerPolicy.STRICT_ORIGIN)
        assertThat(headers.headers).containsEntry("Referrer-Policy", "strict-origin")
        headers.clearSiteData(ClearSiteData.ANY)
        assertThat(headers.headers).containsEntry("Clear-Site-Data", "\"*\"")
        headers.clearSiteData(ClearSiteData.ANY, ClearSiteData.EXECUTION_CONTEXTS, ClearSiteData.STORAGE)
        assertThat(headers.headers).containsEntry("Clear-Site-Data", "\"*\",\"executionContexts\",\"storage\"")
        headers.crossOriginEmbedderPolicy(CrossOriginEmbedderPolicy.UNSAFE_NONE)
        assertThat(headers.headers).containsEntry("Cross-Origin-Embedder-Policy", "unsafe-none")
        headers.crossOriginOpenerPolicy(CrossOriginOpenerPolicy.SAME_ORIGIN_ALLOW_POPUPS)
        assertThat(headers.headers).containsEntry("Cross-Origin-Opener-Policy", "same-origin-allow-popups")
        headers.crossOriginResourcePolicy(CrossOriginResourcePolicy.SAME_SITE)
        assertThat(headers.headers).containsEntry("Cross-Origin-Resource-Policy", "same-site")
    }

    @Test
    fun `test plugin is registered when calling headers method`() {
        val javalin = Javalin.create { config: JavalinConfig -> config.headers { Headers() } }
        val retrievedPlugin: HeadersPlugin = javalin._conf.getPlugin(HeadersPlugin::class.java)
        assertThat(retrievedPlugin).isSameAs(retrievedPlugin)
    }

    @Test
    fun `test headers are set when startung`() {
        val headers = Headers()
        headers.xContentTypeOptionsNoSniff()
        headers.clearSiteData(ClearSiteData.ANY)

        val testApp = Javalin.create { config: JavalinConfig -> config.headers { headers } }
        TestUtil.test(testApp) { app, http ->
            app.get("/") { it.status(200) }
            val returnedHeaders = http.get("/").headers
            assertThat(returnedHeaders.getFirst(Header.X_CONTENT_TYPE_OPTIONS)).isEqualTo("nosniff")
            assertThat(returnedHeaders.getFirst(Header.CLEAR_SITE_DATA)).isEqualTo("\"*\"")
        }
    }
}
