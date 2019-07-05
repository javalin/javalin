package io.javalin.wrapper;

import org.meteogroup.jbrotli.Brotli;
import org.meteogroup.jbrotli.BrotliCompressor;
import org.meteogroup.jbrotli.BrotliStreamCompressor;
import org.meteogroup.jbrotli.libloader.BrotliLibraryLoader;

import java.util.Arrays;

public class BrotliWrapper {

    private static BrotliCompressor _brotli_compressor;
    private static BrotliStreamCompressor _brotli_stream_compressor;

    private static final int QUALITY = 4;
    private static final Brotli.Parameter MY_BROTLI_PARAMETER = new Brotli.Parameter(Brotli.DEFAULT_MODE, QUALITY, 22, 0);

    private BrotliWrapper() {
    }

    private static BrotliCompressor brotliCompressor() {
        if(_brotli_compressor == null) {
            BrotliLibraryLoader.loadBrotli();
            _brotli_compressor = new BrotliCompressor();
        }
        return _brotli_compressor;
    }

    private static BrotliStreamCompressor brotliStreamCompressor() {
        if(_brotli_stream_compressor == null) {
            BrotliLibraryLoader.loadBrotli();
            _brotli_stream_compressor = new BrotliStreamCompressor(MY_BROTLI_PARAMETER);
        }
        return _brotli_stream_compressor;
    }

    public static byte[] compress(byte[] input, int quality) {
        byte[] outBuff = new byte[16384];

        long start = System.nanoTime();
        int len = brotliCompressor().compress(MY_BROTLI_PARAMETER, input, outBuff);
        long stop = System.nanoTime();
        System.out.println("Time for COMPRESS was " + (stop - start)/1000000 + " miliseconds.");

        byte[] output = Arrays.copyOfRange(outBuff, 0, len);
        return output;
    }

    public static byte[] compressStream(byte[] input, int quality) {
        long start = System.nanoTime();
        byte[] output = brotliStreamCompressor().compressArray(input, false);
        long stop = System.nanoTime();
        System.out.println("Time for STREAM COMPRESS was " + (stop - start)/1000000 + " miliseconds.");

        return output;
    }
}
