package io.javalin.core;

import org.meteogroup.jbrotli.Brotli;
import org.meteogroup.jbrotli.BrotliCompressor;

import java.io.Closeable;
import java.io.IOException;
import java.util.Arrays;

public class BrotliWrapper implements Closeable {

    //DEFAULTS
    public static final Brotli.Mode DEFAULT_MODE = Brotli.DEFAULT_MODE;
    public static final int DEFAULT_LEVEL = 4;
    public static final int DEFAULT_LGWIN = Brotli.DEFAULT_LGWIN;
    public static final int DEFAULT_LGBLOCK = Brotli.DEFAULT_LGBLOCK;


    //FIELDS
    private BrotliCompressor _brotliCompressor = new BrotliCompressor();
    private Brotli.Parameter _brotliParameter;


    //CONSTRUCTORS
    public BrotliWrapper() {
        _brotliParameter = createParameter();
    }

    public BrotliWrapper(int level) {
        _brotliParameter = createParameter(level);
    }

    public BrotliWrapper(Brotli.Mode mode, int level, int lgWin, int lgBlock) {
        _brotliParameter = createParameter(mode, level, lgWin, lgBlock);
    }


    //GETTERS
    public Brotli.Parameter getParameter() {
        return _brotliParameter;
    }


    //METHODS
    protected Brotli.Parameter createParameter() {
        return createParameter(DEFAULT_MODE, DEFAULT_LEVEL, DEFAULT_LGWIN, DEFAULT_LGBLOCK);
    }

    protected Brotli.Parameter createParameter(int level) {
        return createParameter(DEFAULT_MODE, level, DEFAULT_LGWIN, DEFAULT_LGBLOCK);
    }

    protected Brotli.Parameter createParameter(Brotli.Mode mode, int level, int lgWin, int lgBlock) {
        return new Brotli.Parameter(mode, level, lgWin, lgBlock);
    }

    public byte[] compressByteArray(byte[] input) {
        byte[] output = new byte[input.length];
        int compressedLength = _brotliCompressor.compress(_brotliParameter, input, output);
        return Arrays.copyOfRange(output, 0, compressedLength);
    }

    @Override
    public void close() throws IOException {

    }
}
