/*
 * Javalin - https://javalin.io
 * Copyright 2021 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.plugin.bundled

import io.javalin.config.JavalinConfig
import io.javalin.http.Header
import io.javalin.plugin.Plugin
import java.time.Duration
import java.util.*
import java.util.function.Consumer

/**
 * A plugin to configure arbitrary headers, with a focus on the OWASP secure headers project
 * https://owasp.org/www-project-secure-headers/
 */
class GlobalHeadersPlugin(userConfig: Consumer<GlobalHeadersConfig>? = null) : Plugin<GlobalHeadersConfig>(userConfig, GlobalHeadersConfig()) {

    override fun onStart(config: JavalinConfig) {
        config.routes.before { ctx ->
            pluginConfig.headers.forEach { (name, value) ->
                ctx.header(name, value)
            }
        }
    }

}

/** The Configuration for the [GlobalHeadersPlugin]. */
class GlobalHeadersConfig {

    /** The headers to add to each request. */
    val headers = mutableMapOf<String, String>()

    /**
     * Adds a Strict-Transport-Security header.
     *
     * The HTTP Strict-Transport-Security response header (often abbreviated as HSTS) informs browsers
     * that the site should only be accessed using HTTPS, and that any future attempts to access it
     * using HTTP should automatically be converted to HTTPS.
     * e.g.: `Strict-Transport-Security: max-age=31536000 ; includeSubDomains`
     *
     * @param duration The time, in seconds, that the browser should remember that a site is only
     * to be accessed using HTTPS.
     * @param includeSubdomains if true, this rule applies to all of the site's subdomains as well.
     */
    fun strictTransportSecurity(duration: Duration, includeSubdomains: Boolean) {
        headers[Header.STRICT_TRANSPORT_SECURITY] = "max-age=" + duration.seconds +
            if (includeSubdomains) " ; includeSubDomains" else ""
    }

    /** X-Frame-Options policy */
    enum class XFrameOptions {
        /** The page cannot be displayed in a frame, regardless of the site attempting to do so. */
        DENY,
        /** The page can only be displayed if all ancestor frames are same origin to the page itself. */
        SAMEORIGIN;
    }

    /**
     * Adds an X-Frame-Options header.
     *
     * The X-Frame-Options HTTP response header can be used to indicate whether or not a browser
     * should be allowed to render a page in a <frame>, <iframe>, <embed> or <object>. Sites can use
     * this to avoid click-jacking attacks, by ensuring that their content is not embedded into other sites.
     *
     * e.g.: `X-Frame-Options DENY | SAMEORIGIN`
     *
     * @param xFrameOptions the option to use
     */
    fun xFrameOptions(xFrameOptions: XFrameOptions) {
        headers[Header.X_FRAME_OPTIONS] = xFrameOptions.name.toHttpHeaderValue()
    }

    /**
     * Adds an X-Frame-Options header.
     *
     * The X-Frame-Options HTTP response header can be used to indicate whether or not a browser
     * should be allowed to render a page in a <frame>, <iframe>, <embed> or <object>. Sites can use
     * this to avoid click-jacking attacks, by ensuring that their content is not embedded into other sites.
     *
     * e.g.: `X-Frame-Options ALLOW-FROM origin`
     *
     * @param domain the domain to allow
     */
    @Deprecated("This is an obsolete directive that no longer works in modern browsers.")
    fun xFrameOptions(domain: String) {
        headers[Header.X_FRAME_OPTIONS] = "allow-from: $domain"
    }

    /**
     * Adds a "No Sniff" X-Content-Type-Options header.
     *
     * The X-Content-Type-Options response HTTP header is a marker used by the server to indicate
     * that the MIME types advertised in the Content-Type headers should be followed and not be
     * changed. The header allows you to avoid MIME type sniffing by saying that the MIME types
     * are deliberately configured.
     *
     * Blocks a request if the request destination is of type style and the MIME type is not text/css,
     * or of type script and the MIME type is not a JavaScript MIME type.
     *
     * i.e.: `X-Content-Type-Options: nosniff`
     */
    fun xContentTypeOptionsNoSniff() {
        headers[Header.X_CONTENT_TYPE_OPTIONS] = "nosniff"
    }

    /**
     * Adds the Content-Security-Policy header.
     *
     * The HTTP Content-Security-Policy response header allows website administrators to control
     * resources the user agent is allowed to load for a given page. With a few exceptions, policies
     * mostly involve specifying server origins and script endpoints. This helps guard against cross-site
     * scripting attacks (Cross-site_scripting).
     *
     * e.g.: `Content-Security-Policy: <policy-directive>; <policy-directive>`
     */
    fun contentSecurityPolicy(contentSecurityPolicy: String) {
        headers[Header.CONTENT_SECURITY_POLICY] = contentSecurityPolicy
    }

    /** X-Permitted-Cross-Domain-Policies */
    enum class CrossDomainPolicy {
        /** Will prevent the browser from MIME-sniffing a response away from the declared content-type. */
        NONE,
        /** Only this master policy file is allowed. */
        MASTER_ONLY,
        /** Only policy files served with Content-Type: text/x-cross-domain-policy are allowed. */
        BY_CONTENT_TYPE,
        /** Only policy files whose filenames are crossdomain.xml (i.e. URLs ending in /crossdomain.xml) are allowed. */
        BY_FTP_FILENAME,
        /** All policy files on this target domain are allowed. */
        ALL;
    }

    /**
     * Adds the X-Permitted-Cross-Domain-Policies header.
     *
     * This header is used to limit which data external resources, such as Adobe Flash and PDF documents,
     * can access on the domain. Failure to set the X-Permitted- Cross-Domain-Policies header to “none”
     * value allows other domains to embed the application’s data in their content.
     *
     * e.g.: `X-Permitted-Cross-Domain-Policies: none | master-only | by-content-type | by-ftp-filename | all`
     *
     * @param policy the policy to use
     */
    fun xPermittedCrossDomainPolicies(policy: CrossDomainPolicy) {
        headers[Header.X_PERMITTED_CROSS_DOMAIN_POLICIES] = policy.name.toHttpHeaderValue()
    }

    /** Referrer-Policy */
    enum class ReferrerPolicy {
        /** The Referer header will be omitted: sent requests do not include any referrer information. */
        NO_REFERRER,
        /**
         * Send the origin, path, and querystring in Referer when the protocol security level stays
         * the same or improves (HTTP→HTTP, HTTP→HTTPS, HTTPS→HTTPS). Don't send the Referer header
         * for requests to less secure destinations (HTTPS→HTTP, HTTPS→file).
         */
        NO_REFERRER_WHEN_DOWNGRADE,
        /** Send only the origin in the Referer header. For example, a document at https://example.com/page.html will send the referrer https://example.com/. */
        ORIGIN,
        /**
         * When performing a same-origin request to the same protocol level (HTTP→HTTP, HTTPS→HTTPS),
         * send the origin, path, and query string. Send only the origin for cross-origin requests
         * and requests to less secure destinations (HTTPS→HTTP).
         */
        ORIGIN_WHEN_CROSS_ORIGIN,
        /** Send the origin, path, and query string for same-origin requests. Don't send the Referer header for cross-origin requests. */
        SAME_ORIGIN,
        /** Send only the origin when the protocol security level stays the same (HTTPS→HTTPS). Don't send the Referer header to less secure destinations (HTTPS→HTTP). */
        STRICT_ORIGIN,
        /**
         * Send the origin, path, and querystring when performing a same-origin request. For cross-origin
         * requests send the origin (only) when the protocol security level stays same (HTTPS→HTTPS).
         * Don't send the Referer header to less secure destinations (HTTPS→HTTP).
         */
        STRICT_ORIGIN_WHEN_CROSS_ORIGIN,
        /** Send the origin, path, and query string when performing any request, regardless of security. */
        UNSAFE_URL;
    }

    /**
     * Adds a Referrer-Policy header.
     *
     * The Referrer-Policy HTTP header controls how much referrer information (sent with the Referer header)
     * should be included with requests. Aside from the HTTP header, you can set this policy in HTML.
     *
     * e.g.: `Referrer-Policy: no-referrer`
     *
     * @param policy the policy to use
     */
    fun referrerPolicy(policy: ReferrerPolicy) {
        headers[Header.REFERRER_POLICY] = policy.name.toHttpHeaderValue()
    }

    /** Directive for the Clear-Site-Data header. */
    enum class ClearSiteData(name: String, val headerValue: String = '"' + name + '"') {
        /**
         * Indicates that the server wishes to remove locally cached data (the browser cache) for
         * the origin of the response URL. Depending on the browser, this might also clear out things
         * like pre-rendered pages, script caches, WebGL shader caches, or address bar suggestions.
         */
        CACHE("cache"),
        /**
         * Indicates that the server wishes to remove all cookies for the origin of the response URL.
         * HTTP authentication credentials are also cleared out. This affects the entire registered
         * domain, including subdomains. So https://example.com as well as https://stage.example.com,
         * will have cookies cleared.
         */
        COOKIES("cookies"),
        /**
         * Indicates that the server wishes to remove all DOM storage for the origin of the response URL.
         * This includes storage mechanisms such as:localStorage, sessionStorage, IndexedDB,
         * Service worker registrations, Web SQL databases (deprecated), FileSystem API data,
         * Plugin data.
         */
        STORAGE("storage"),
        /** Indicates that the server wishes to reload all browsing contexts for the origin of the response. */
        EXECUTION_CONTEXTS("executionContexts"),
        /**
         * Indicates that the server wishes to clear all types of data for the origin of
         * the response. If more data types are added in future versions of this header,
         * they will also be covered by it.
         */
        ANY("*");
    }

    /**
     * Adds a Clear-Site-Data header.
     *
     * The Clear-Site-Data header clears browsing data (cookies, storage, cache) associated with the
     * requesting website. It allows web developers to have more control over the data stored by a
     * client browser for their origins.
     *
     * e.g.: `Clear-Site-Data: "cache", "cookies"`
     *
     * @param data a vararg list of directives about which data should be cleared
     */
    fun clearSiteData(vararg data: ClearSiteData) {
        headers[Header.CLEAR_SITE_DATA] = data.joinToString(",", transform = ClearSiteData::headerValue)
    }

    /** Cross-Origin-Embedder-Policy */
    enum class CrossOriginEmbedderPolicy {
        /** This is the default value. Allows the document to fetch cross-origin resources without giving explicit permission through the CORS protocol or the Cross-Origin-Resource-Policy header. */
        UNSAFE_NONE,
        /**
         * A document can only load resources from the same origin, or resources explicitly marked as
         * loadable from another origin. If a cross-origin resource supports CORS, the cross-origin
         * attribute or the Cross-Origin-Resource-Policy header must be used to load it without
         * being blocked by COEP.
         */
        REQUIRE_CORP;
    }

    /**
     * Adds a Cross-Origin-Embedder-Policy (COEP) header.
     *
     * The HTTP Cross-Origin-Embedder-Policy (COEP) response header configures embedding cross-origin
     * resources into the document.
     *
     * e.g.: `Cross-Origin-Embedder-Policy: require-corp | unsafe-none`
     *
     * @param policy the policy to use
     */
    fun crossOriginEmbedderPolicy(policy: CrossOriginEmbedderPolicy) {
        headers[Header.CROSS_ORIGIN_EMBEDDER_POLICY] = policy.name.toHttpHeaderValue()
    }

    /** Cross-Origin-Opener-Policy */
    enum class CrossOriginOpenerPolicy {
        /** This is the default value. Allows the document to be added to its opener's browsing context group unless the opener itself has a COOP of same-origin or same-origin-allow-popups. */
        UNSAFE_NONE,
        /** Retains references to newly opened windows or tabs that either don't set COOP or that opt out of isolation by setting a COOP of unsafe-none. */
        SAME_ORIGIN_ALLOW_POPUPS,
        /** Isolates the browsing context exclusively to same-origin documents. Cross-origin documents are not loaded in the same browsing context. */
        SAME_ORIGIN;
    }

    /**
     * Adds a Cross-Origin-Opener-Policy (COOP) header.
     *
     * The HTTP Cross-Origin-Opener-Policy (COOP) response header allows you to ensure a top-level
     * document does not share a browsing context group with cross-origin documents.
     *
     * e.g.: Cross-Origin-Opener-Policy: unsafe-none | same-origin-allow-popups | same-origin`
     *
     * @param policy the policy to use
     */
    fun crossOriginOpenerPolicy(policy: CrossOriginOpenerPolicy) {
        headers[Header.CROSS_ORIGIN_OPENER_POLICY] = policy.name.toHttpHeaderValue()
    }

    /** Cross-Origin Resource Policy */
    enum class CrossOriginResourcePolicy {
        /** Only requests from the same Site can read the resource. */
        SAME_SITE,
        /** Only requests from the same origin (i.e. scheme + host + port) can read the resource. */
        SAME_ORIGIN,
        /** Requests from any origin (both same-site and cross-site) can read the resource. This is useful when COEP is used. */
        CROSS_ORIGIN;
    }

    /**
     * Adds a Cross-Origin Resource Policy (CORP) header.
     *
     * Cross-Origin Resource Policy is a policy set by the Cross-Origin-Resource-Policy HTTP header
     * that lets websites and applications opt in to protection against certain requests from other
     * origins (such as those issued with elements like <script> and <img>), to mitigate speculative
     * side-channel attacks, like Spectre, as well as Cross-Site Script Inclusion attacks.
     *
     * e.g.: `Cross-Origin-Resource-Policy: same-site | same-origin | cross-origin`
     *
     * @param policy the policy to use
     */
    fun crossOriginResourcePolicy(policy: CrossOriginResourcePolicy) {
        headers[Header.CROSS_ORIGIN_RESOURCE_POLICY] = policy.name.toHttpHeaderValue()
    }

    private fun String.toHttpHeaderValue(): String =
        this.lowercase(Locale.ROOT).replace("_", "-")

}
