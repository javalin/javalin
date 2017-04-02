// Javalin - https://javalin.io
// Copyright 2017 David Åse
// Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE

package javalin.builder;

import javalin.core.util.Util;

public class CookieBuilder {

    private String name;
    private String value;
    private String domain = "";
    private String path = "";
    private int maxAge = -1;
    private boolean secure = false;
    private boolean httpOnly = false;

    public static CookieBuilder cookieBuilder(String name, String value) {
        return new CookieBuilder(name, value);
    }

    private CookieBuilder(String name, String value) {
        Util.notNull(name, "Cookie name cannot be null");
        Util.notNull(value, "Cookie value cannot be null");
        this.name = name;
        this.value = value;
    }

    public CookieBuilder domain(String domain) {
        this.domain = domain;
        return this;
    }

    public CookieBuilder path(String path) {
        this.path = path;
        return this;
    }

    public CookieBuilder maxAge(int maxAge) {
        this.maxAge = maxAge;
        return this;
    }

    public CookieBuilder secure(boolean secure) {
        this.secure = secure;
        return this;
    }

    public CookieBuilder httpOnly(boolean httpOnly) {
        this.httpOnly = httpOnly;
        return this;
    }

    // getters
    public String name() {
        return name;
    }

    public String value() {
        return value;
    }

    public String domain() {
        return domain;
    }

    public String path() {
        return path;
    }

    public int maxAge() {
        return maxAge;
    }

    public boolean secure() {
        return secure;
    }

    public boolean httpOnly() {
        return httpOnly;
    }
}