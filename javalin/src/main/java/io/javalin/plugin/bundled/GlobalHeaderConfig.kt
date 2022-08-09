/*
 * Javalin - https://javalin.io
 * Copyright 2021 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.plugin.bundled

import io.javalin.http.Header
import java.time.Duration
import java.util.*

/**
 * A plugin to configure arbitrary headers, with a focus on the OWASP secure headers project
 * https://owasp.org/www-project-secure-headers/
 */
class GlobalHeaderConfig {

    val headers = mutableMapOf<String, String>()

    // Strict-Transport-Security: max-age=31536000 ; includeSubDomains
    fun strictTransportSecurity(duration: Duration, includeSubdomains: Boolean) {
        headers[Header.STRICT_TRANSPORT_SECURITY] = "max-age=" + duration.seconds +
            if (includeSubdomains) " ; includeSubDomains" else ""
    }

    // X-Frame-Options: deny | sameorigin | allow-from: DOMAIN
    enum class XFrameOptions {
        DENY, SAMEORIGIN;
    }

    fun xFrameOptions(xFrameOptions: XFrameOptions) {
        headers[Header.X_FRAME_OPTIONS] = xFrameOptions.name.toHttpHeaderValue()
    }

    fun xFrameOptions(domain: String) {
        headers[Header.X_FRAME_OPTIONS] = "allow-from: $domain"
    }

    // X-Content-Type-Options: nosniff
    fun xContentTypeOptionsNoSniff() {
        headers[Header.X_CONTENT_TYPE_OPTIONS] = "nosniff"
    }

    // Content-Security-Policy: String... + JAVADOC
    fun contentSecurityPolicy(contentSecurityPolicy: String) {
        headers[Header.CONTENT_SECURITY_POLICY] = contentSecurityPolicy
    }

    // X-Permitted-Cross-Domain-Policies: none | master-only | by-content-type | by-ftp-filename | all
    enum class CrossDomainPolicy {
        NONE, MASTER_ONLY, BY_CONTENT_TYPE, BY_FTP_FILENAME, ALL;
    }

    fun xPermittedCrossDomainPolicies(policy: CrossDomainPolicy) {
        headers[Header.X_PERMITTED_CROSS_DOMAIN_POLICIES] = policy.name.toHttpHeaderValue()
    }

    // Referrer-Policy: no-referrer | no-referrer-when-downgrade | origin | origin-when-cross-origin | same-origin | strict-origin | strict-origin-when-cross-origin | unsafe-url
    enum class ReferrerPolicy {
        NO_REFERRER, NO_REFERRER_WHEN_DOWNGRADE, ORIGIN, ORIGIN_WHEN_CROSS_ORIGIN, SAME_ORIGIN, STRICT_ORIGIN, STRICT_ORIGIN_WHEN_CROSS_ORIGIN, UNSAFE_URL;
    }

    fun referrerPolicy(policy: ReferrerPolicy) {
        headers[Header.REFERRER_POLICY] = policy.name.toHttpHeaderValue()
    }

    // Clear-Site-Data: "cache" | "cookies" | "storage" | "executionContexts" | "*"
    enum class ClearSiteData(name: String, val headerValue: String = '"' + name + '"') {
        CACHE("cache"),
        COOKIES("cookies"),
        STORAGE("storage"),
        EXECUTION_CONTEXTS("executionContexts"),
        ANY("*");
    }

    fun clearSiteData(vararg data: ClearSiteData) {
        headers[Header.CLEAR_SITE_DATA] = data.joinToString(",", transform = ClearSiteData::headerValue)
    }

    // Cross-Origin-Embedder-Policy: require-corp | unsafe-none
    enum class CrossOriginEmbedderPolicy {
        UNSAFE_NONE, REQUIRE_CORP;
    }

    fun crossOriginEmbedderPolicy(policy: CrossOriginEmbedderPolicy) {
        headers[Header.CROSS_ORIGIN_EMBEDDER_POLICY] = policy.name.toHttpHeaderValue()
    }

    // Cross-Origin-Opener-Policy: unsafe-none	| same-origin-allow-popups	| same-origin
    enum class CrossOriginOpenerPolicy {
        UNSAFE_NONE, SAME_ORIGIN_ALLOW_POPUPS, SAME_ORIGIN;
    }

    fun crossOriginOpenerPolicy(policy: CrossOriginOpenerPolicy) {
        headers[Header.CROSS_ORIGIN_OPENER_POLICY] = policy.name.toHttpHeaderValue()
    }

    // Cross-Origin-Resource-Policy: same-site | same-origin | cross-origin
    enum class CrossOriginResourcePolicy {
        SAME_SITE, SAME_ORIGIN, CROSS_ORIGIN;
    }

    fun crossOriginResourcePolicy(policy: CrossOriginResourcePolicy) {
        headers[Header.CROSS_ORIGIN_RESOURCE_POLICY] = policy.name.toHttpHeaderValue()
    }

    private fun String.toHttpHeaderValue(): String =
        this.lowercase(Locale.ROOT).replace("_", "-")

}
