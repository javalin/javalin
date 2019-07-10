package io.javalin.core.compression;

import org.meteogroup.jbrotli.Brotli;
import org.meteogroup.jbrotli.BrotliCompressor;

import java.io.Closeable;
import java.io.IOException;
import java.util.Arrays;


/**
 * This class acts as a middleman between JBrotli and Javalin.
 *
 * Intentionally left without public modifier, as we want Javalin API users implementing compression via
 * @see DynamicCompressionStrategy, rather than accessing this class directly.
 */
class BrotliWrapper implements Closeable {

    public static final Brotli.Mode DEFAULT_MODE = Brotli.DEFAULT_MODE;
    public static final int DEFAULT_LEVEL = 4;
    public static final int DEFAULT_LGWIN = Brotli.DEFAULT_LGWIN;
    public static final int DEFAULT_LGBLOCK = Brotli.DEFAULT_LGBLOCK;

    private BrotliCompressor _brotliCompressor = new BrotliCompressor();
    private Brotli.Parameter _brotliParameter;

    /**
     * Default parameter
     */
    public BrotliWrapper() {
        _brotliParameter = createParameter();
    }

    /**
     * Default parameter with custom compression level
     *
     * Valid level range is 0..11
     */
    public BrotliWrapper(int level) {
        _brotliParameter = createParameter(level);
    }

    /**
     * Fully custom parameter
     */
    public BrotliWrapper(Brotli.Mode mode, int level, int lgWin, int lgBlock) {
        _brotliParameter = createParameter(mode, level, lgWin, lgBlock);
    }

    public Brotli.Parameter getParameter() {
        return _brotliParameter;
    }


    //PROTECTED methods
    protected Brotli.Parameter createParameter() {
        return createParameter(DEFAULT_MODE, DEFAULT_LEVEL, DEFAULT_LGWIN, DEFAULT_LGBLOCK);
    }

    protected Brotli.Parameter createParameter(int level) {
        return createParameter(DEFAULT_MODE, level, DEFAULT_LGWIN, DEFAULT_LGBLOCK);
    }

    protected Brotli.Parameter createParameter(Brotli.Mode mode, int level, int lgWin, int lgBlock) {
        return new Brotli.Parameter(mode, level, lgWin, lgBlock);
    }

    /**
     * @param input Byte array to compress
     * @return Compressed byte array
     */
    public byte[] compressByteArray(byte[] input) {
        byte[] output = new byte[input.length];
        int compressedLength = _brotliCompressor.compress(_brotliParameter, input, output);
        return Arrays.copyOfRange(output, 0, compressedLength);
    }

    //Not used, but needs to be overriden because the class implements Closeable interface
    @Override
    public void close() throws IOException { }
}
