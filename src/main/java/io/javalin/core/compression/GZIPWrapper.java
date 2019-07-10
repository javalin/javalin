package io.javalin.core.compression;

import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.GZIPOutputStream;


/**
 * This class acts as a middleman between GZIP and Javalin.
 */
public class GZIPWrapper extends GZIPOutputStream {

    public GZIPWrapper(OutputStream out) throws IOException {
        super(out);
    }

    public GZIPWrapper(OutputStream out, boolean syncFlush) throws IOException {
        super(out, syncFlush);
    }

    /**
     * @param level GZIP compression level. Valid range is 0..9
     */
    public GZIPWrapper setLevel(int level) {
        this.def.setLevel(level);
        return this;
    }
}
