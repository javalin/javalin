package io.javalin.core;

import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.GZIPOutputStream;

public class GZIPWrapper extends GZIPOutputStream {

    public GZIPWrapper(OutputStream out) throws IOException {
        super(out);
    }

    public GZIPWrapper(OutputStream out, boolean syncFlush) throws IOException {
        super(out, syncFlush);
    }

    public GZIPWrapper setLevel(int level) {
        this.def.setLevel(level);
        return this;
    }
}
