module io.javalin {

    exports io.javalin;
    exports io.javalin.apibuilder;
    exports io.javalin.compression;
    exports io.javalin.config;
    exports io.javalin.event;
    exports io.javalin.http;
    exports io.javalin.http.servlet;
    exports io.javalin.http.sse;
    exports io.javalin.http.staticfiles;
    exports io.javalin.http.util;
    exports io.javalin.jetty;
    exports io.javalin.json;
    exports io.javalin.plugin;
    exports io.javalin.plugin.bundled;
    exports io.javalin.rendering;
    exports io.javalin.router;
    exports io.javalin.security;
    exports io.javalin.util;
    exports io.javalin.util.function;
    exports io.javalin.validation;
    exports io.javalin.vue;
    exports io.javalin.websocket;

    requires transitive kotlin.stdlib;
    requires transitive org.slf4j;
    requires transitive org.eclipse.jetty.server;
    requires transitive org.eclipse.jetty.servlet;
    requires transitive org.eclipse.jetty.util;
    requires transitive org.eclipse.jetty.websocket.core.common;
    requires transitive org.eclipse.jetty.websocket.jetty.api;
    requires transitive org.eclipse.jetty.websocket.jetty.server;

    //optional dependencies
    requires static org.jetbrains.annotations;
    requires static com.aayushatharva.brotli4j;
    requires static com.aayushatharva.brotli4j.service;
    requires static com.fasterxml.jackson.databind;
    requires static com.fasterxml.jackson.kotlin;
    requires static com.google.gson;
    
    //Required to use the Service Loader on this type
    uses org.slf4j.spi.SLF4JServiceProvider
}
