package io.javalin.wrapper;

import org.meteogroup.jbrotli.Brotli;
import org.meteogroup.jbrotli.BrotliCompressor;
import org.meteogroup.jbrotli.libloader.BrotliLibraryLoader;

import java.util.Arrays;

public class BrotliWrapper {

    private static BrotliCompressor _brotli_compressor;

    private static final int MY_QUALITY = 4;
    private static final Brotli.Parameter MY_BROTLI_PARAMETER = new Brotli.Parameter(
        Brotli.DEFAULT_MODE,
        MY_QUALITY,
        Brotli.DEFAULT_LGWIN,
        Brotli.DEFAULT_LGBLOCK
    );

    //Being used as a static class, we don't want to initialize instances
    private BrotliWrapper() {}

    private static BrotliCompressor brotliCompressor() {
        if(_brotli_compressor == null) {
            BrotliLibraryLoader.loadBrotli();
            _brotli_compressor = new BrotliCompressor();
        }
        return _brotli_compressor;
    }

    public static byte[] compressBytes(byte[] input) {
        byte[] output = new byte[input.length];

        long start = System.nanoTime();
        int compressedLength = brotliCompressor().compress(MY_BROTLI_PARAMETER, input, output);
        long stop = System.nanoTime();
        System.out.println("Time for COMPRESS was " + (stop - start)/1000000 + " miliseconds.");

        return Arrays.copyOfRange(output, 0, compressedLength);
    }
}
