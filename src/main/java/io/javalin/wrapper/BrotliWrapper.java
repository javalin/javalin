package io.javalin.wrapper;

import haxe.lang.EmptyObject;
import haxe.root.Brotli;

public class BrotliWrapper {

    private static Brotli _brotli;

    private BrotliWrapper() {
    }

    private static Brotli brotli() {
        if(_brotli == null) _brotli = new Brotli(EmptyObject.EMPTY);
        return _brotli;
    }

    /*
    public static String compress(String input, int quality) {
        return (String)brotli().compress(input, quality);
    }
    */

    public static byte[] compressArray(byte[] input, int quality) {
        return brotli().compressArray(input, quality);
    }
}
