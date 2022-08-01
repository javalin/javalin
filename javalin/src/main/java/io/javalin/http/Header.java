package io.javalin.http;

import java.util.Set;
import org.jetbrains.annotations.NotNull;

public class Header {

    @NotNull
    public final String name;

    private Header(@NotNull String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public boolean equals(@NotNull Object o) {
        if (this == o) return true;
        if (this.getClass() != o.getClass()) return false;
        return this.name.equals(((Header) o).name);
    }

    //@formatter:off
    @NotNull public static final Header ACCEPT = new Header("Accept");
    @NotNull public static final Header ACCEPT_CHARSET = new Header("Accept-Charset");
    @NotNull public static final Header ACCEPT_ENCODING = new Header("Accept-Encoding");
    @NotNull public static final Header ACCEPT_LANGUAGE = new Header("Accept-Language");
    @NotNull public static final Header ACCEPT_RANGES = new Header("Accept-Ranges");
    @NotNull public static final Header ACCESS_CONTROL_ALLOW_CREDENTIALS = new Header("Access-Control-Allow-Credentials");
    @NotNull public static final Header ACCESS_CONTROL_ALLOW_HEADERS = new Header("Access-Control-Allow-Headers");
    @NotNull public static final Header ACCESS_CONTROL_ALLOW_METHODS = new Header("Access-Control-Allow-Methods");
    @NotNull public static final Header ACCESS_CONTROL_ALLOW_ORIGIN = new Header("Access-Control-Allow-Origin");
    @NotNull public static final Header ACCESS_CONTROL_EXPOSE_HEADERS = new Header("Access-Control-Expose-Headers");
    @NotNull public static final Header ACCESS_CONTROL_MAX_AGE = new Header("Access-Control-Max-Age");
    @NotNull public static final Header ACCESS_CONTROL_REQUEST_HEADERS = new Header("Access-Control-Request-Headers");
    @NotNull public static final Header ACCESS_CONTROL_REQUEST_METHOD = new Header("Access-Control-Request-Method");
    @NotNull public static final Header AGE = new Header("Age");
    @NotNull public static final Header ALLOW = new Header("Allow");
    @NotNull public static final Header AUTHORIZATION = new Header("Authorization");
    @NotNull public static final Header CACHE_CONTROL = new Header("Cache-Control");
    @NotNull public static final Header CLEAR_SITE_DATA = new Header("Clear-Site-Data");
    @NotNull public static final Header CONNECTION = new Header("Connection");
    @NotNull public static final Header CONTENT_ENCODING = new Header("Content-Encoding");
    @NotNull public static final Header CONTENT_DISPOSITION = new Header("Content-Disposition");
    @NotNull public static final Header CONTENT_LANGUAGE = new Header("Content-Language");
    @NotNull public static final Header CONTENT_LENGTH = new Header("Content-Length");
    @NotNull public static final Header CONTENT_LOCATION = new Header("Content-Location");
    @NotNull public static final Header CONTENT_RANGE = new Header("Content-Range");
    @NotNull public static final Header CONTENT_SECURITY_POLICY = new Header("Content-Security-Policy");
    @NotNull public static final Header CONTENT_TYPE = new Header("Content-Type");
    @NotNull public static final Header COOKIE = new Header("Cookie");
    @NotNull public static final Header CROSS_ORIGIN_EMBEDDER_POLICY = new Header("Cross-Origin-Embedder-Policy");
    @NotNull public static final Header CROSS_ORIGIN_OPENER_POLICY = new Header("Cross-Origin-Opener-Policy");
    @NotNull public static final Header CROSS_ORIGIN_RESOURCE_POLICY = new Header("Cross-Origin-Resource-Policy");
    @NotNull public static final Header DATE = new Header("Date");
    @NotNull public static final Header ETAG = new Header("ETag");
    @NotNull public static final Header EXPECT = new Header("Expect");
    @NotNull public static final Header EXPIRES = new Header("Expires");
    @NotNull public static final Header FROM = new Header("From");
    @NotNull public static final Header HOST = new Header("Host");
    @NotNull public static final Header IF_MATCH = new Header("If-Match");
    @NotNull public static final Header IF_MODIFIED_SINCE = new Header("If-Modified-Since");
    @NotNull public static final Header IF_NONE_MATCH = new Header("If-None-Match");
    @NotNull public static final Header IF_RANGE = new Header("If-Range");
    @NotNull public static final Header IF_UNMODIFIED_SINCE = new Header("If-Unmodified-Since");
    @NotNull public static final Header LAST_MODIFIED = new Header("Last-Modified");
    @NotNull public static final Header LINK = new Header("Link");
    @NotNull public static final Header LOCATION = new Header("Location");
    @NotNull public static final Header MAX_FORWARDS = new Header("Max-Forwards");
    @NotNull public static final Header ORIGIN = new Header("Origin");
    @NotNull public static final Header PRAGMA = new Header("Pragma");
    @NotNull public static final Header PROXY_AUTHENTICATE = new Header("Proxy-Authenticate");
    @NotNull public static final Header PROXY_AUTHORIZATION = new Header("Proxy-Authorization");
    @NotNull public static final Header RANGE = new Header("Range");
    @NotNull public static final Header REFERER = new Header("Referer");
    @NotNull public static final Header REFERRER_POLICY = new Header("Referrer-Policy");
    @NotNull public static final Header RETRY_AFTER = new Header("Retry-After");
    @NotNull public static final Header SERVER = new Header("Server");
    @NotNull public static final Header SET_COOKIE = new Header("Set-Cookie");
    @NotNull public static final Header SEC_WEBSOCKET_KEY = new Header("Sec-WebSocket-Key");
    @NotNull public static final Header STRICT_TRANSPORT_SECURITY = new Header("Strict-Transport-Security");
    @NotNull public static final Header TE = new Header("TE");
    @NotNull public static final Header TRAILER = new Header("Trailer");
    @NotNull public static final Header TRANSFER_ENCODING = new Header("Transfer-Encoding");
    @NotNull public static final Header UPGRADE = new Header("Upgrade");
    @NotNull public static final Header USER_AGENT = new Header("User-Agent");
    @NotNull public static final Header VARY = new Header("Vary");
    @NotNull public static final Header VIA = new Header("Via");
    @NotNull public static final Header WARNING = new Header("Warning");
    @NotNull public static final Header WWW_AUTHENTICATE = new Header("WWW-Authenticate");
    @NotNull public static final Header X_FORWARDED_FOR = new Header("X-Forwarded-For");
    @NotNull public static final Header X_FORWARDED_PROTO = new Header("X-Forwarded-Proto");
    @NotNull public static final Header X_FRAME_OPTIONS = new Header("X-Frame-Options");
    @NotNull public static final Header X_CONTENT_TYPE_OPTIONS = new Header("X-Content-Type-Options");
    @NotNull public static final Header X_HTTP_METHOD_OVERRIDE = new Header("X-HTTP-Method-Override");
    @NotNull public static final Header X_PERMITTED_CROSS_DOMAIN_POLICIES = new Header("X-Permitted-Cross-Domain-Policies");
    @NotNull public static final Header X_ACCEL_BUFFERING = new Header("X-Accel-Buffering");
    //@formatter:on

    private static final Set<Header> values = Set.of(
        ACCEPT,
        ACCEPT_CHARSET,
        ACCEPT_ENCODING,
        ACCEPT_LANGUAGE,
        ACCEPT_RANGES,
        ACCESS_CONTROL_ALLOW_CREDENTIALS,
        ACCESS_CONTROL_ALLOW_HEADERS,
        ACCESS_CONTROL_ALLOW_METHODS,
        ACCESS_CONTROL_ALLOW_ORIGIN,
        ACCESS_CONTROL_EXPOSE_HEADERS,
        ACCESS_CONTROL_MAX_AGE,
        ACCESS_CONTROL_REQUEST_HEADERS,
        ACCESS_CONTROL_REQUEST_METHOD,
        AGE,
        ALLOW,
        AUTHORIZATION,
        CACHE_CONTROL,
        CLEAR_SITE_DATA,
        CONNECTION,
        CONTENT_ENCODING,
        CONTENT_DISPOSITION,
        CONTENT_LANGUAGE,
        CONTENT_LENGTH,
        CONTENT_LOCATION,
        CONTENT_RANGE,
        CONTENT_SECURITY_POLICY,
        CONTENT_TYPE,
        COOKIE,
        CROSS_ORIGIN_EMBEDDER_POLICY,
        CROSS_ORIGIN_OPENER_POLICY,
        CROSS_ORIGIN_RESOURCE_POLICY,
        DATE,
        ETAG,
        EXPECT,
        EXPIRES,
        FROM,
        HOST,
        IF_MATCH,
        IF_MODIFIED_SINCE,
        IF_NONE_MATCH,
        IF_RANGE,
        IF_UNMODIFIED_SINCE,
        LAST_MODIFIED,
        LINK,
        LOCATION,
        MAX_FORWARDS,
        ORIGIN,
        PRAGMA,
        PROXY_AUTHENTICATE,
        PROXY_AUTHORIZATION,
        RANGE,
        REFERER,
        REFERRER_POLICY,
        RETRY_AFTER,
        SERVER,
        SET_COOKIE,
        SEC_WEBSOCKET_KEY,
        STRICT_TRANSPORT_SECURITY,
        TE,
        TRAILER,
        TRANSFER_ENCODING,
        UPGRADE,
        USER_AGENT,
        VARY,
        VIA,
        WARNING,
        WWW_AUTHENTICATE,
        X_FORWARDED_FOR,
        X_FORWARDED_PROTO,
        X_FRAME_OPTIONS,
        X_CONTENT_TYPE_OPTIONS,
        X_HTTP_METHOD_OVERRIDE,
        X_PERMITTED_CROSS_DOMAIN_POLICIES,
        X_ACCEL_BUFFERING
    );

    public static Set<Header> values() {
        return values;
    }

    public static Header from(String name) {
        return new Header(name);
    }
}
