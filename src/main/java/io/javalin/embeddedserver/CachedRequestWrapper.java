/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 *
 */

package io.javalin.embeddedserver;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import io.javalin.core.util.RequestUtil;

public class CachedRequestWrapper extends HttpServletRequestWrapper {

    private byte[] cachedBytes;

    public CachedRequestWrapper(HttpServletRequest request) throws IOException {
        super(request);
        cachedBytes = RequestUtil.toByteArray(super.getInputStream());
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
        if (chunkedTransferEncoding()) { // this could blow up memory if cached
            return super.getInputStream();
        }
        return new CachedServletInputStream();
    }

    private boolean chunkedTransferEncoding() {
        return "chunked".equals(((HttpServletRequest) super.getRequest()).getHeader("Transfer-Encoding"));
    }

    private class CachedServletInputStream extends ServletInputStream {
        private ByteArrayInputStream byteArrayInputStream;

        public CachedServletInputStream() {
            this.byteArrayInputStream = new ByteArrayInputStream(cachedBytes);
        }

        @Override
        public int read() {
            return byteArrayInputStream.read();
        }

        @Override
        public int available() {
            return byteArrayInputStream.available();
        }

        @Override
        public boolean isFinished() {
            return available() <= 0;
        }

        @Override
        public boolean isReady() {
            return available() >= 0;
        }

        @Override
        public void setReadListener(ReadListener readListener) {
        }
    }
}
