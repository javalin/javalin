package io.javalin.wrapper;

import org.meteogroup.jbrotli.Brotli;
import org.meteogroup.jbrotli.BrotliCompressor;
import org.meteogroup.jbrotli.libloader.BrotliLibraryLoader;

import java.util.Arrays;

public class BrotliWrapper {

    private static BrotliCompressor _brotli;

    private BrotliWrapper() {
    }

    private static BrotliCompressor brotli() {
        if(_brotli == null) {
            BrotliLibraryLoader.loadBrotli();
            _brotli = new BrotliCompressor();
        }
        return _brotli;
    }

    public static byte[] compress(byte[] input, int quality) {
        byte[] outBuff = new byte[8192];
        int len = brotli().compress(Brotli.DEFAULT_PARAMETER, input, outBuff);
        byte[] output = Arrays.copyOfRange(outBuff, 0, len);
        return output;
    }
}
