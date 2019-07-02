package io.javalin.wrapper;

import haxe.lang.EmptyObject;
import haxe.root.Brotli;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class BrotliWrapper {

    private static Brotli _brotli;

    private BrotliWrapper() {
    }

    private static Brotli brotli() {
        if(_brotli == null) _brotli = new Brotli(EmptyObject.EMPTY);
        return _brotli;
    }

    public static String compress(String input, int quality) {
        //String s = (String)brotli().compress(input, quality);
        //byte[] b = s.getBytes(StandardCharsets.US_ASCII);
        //return b;
        return (String)brotli().compress(input, quality);
    }

    public static byte[] compressArray(byte[] input, int quality) {
        return brotli().compressArray(input, quality);
    }
}
