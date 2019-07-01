package io.javalin.wrapper;

import haxe.lang.EmptyObject;
import haxe.root.Brotli;

public class BrotliWrapper {

    private static Brotli _brotli;

    private BrotliWrapper() {
    }

    private static Brotli brotli() {
        if(_brotli == null) _brotli = new Brotli(EmptyObject.EMPTY);
        //if(_brotli == null) _brotli = new Brotli("dictionary.txt");
        return _brotli;
    }

    public static Object compress(Object input, int quality) {
        //return _brotli.compress(input, quality).toString().getBytes(Charset.forName("UTF-8"));
        return brotli().compress(input, quality);
    }
}
