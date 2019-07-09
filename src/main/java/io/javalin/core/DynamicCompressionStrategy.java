package io.javalin.core;

public class DynamicCompressionStrategy {

    public static final int BROTLI_DEFAULT_LEVEL = 4;
    public static final int GZIP_DEFAULT_LEVEL = 6;

    private boolean _brotliEnabled = false;
    private boolean _gzipEnabled = true;
    private int _brotliLevel = BROTLI_DEFAULT_LEVEL;
    private int _gzipLevel = GZIP_DEFAULT_LEVEL;

    public DynamicCompressionStrategy() { }

    public DynamicCompressionStrategy(boolean brotliEnabled, boolean gzipEnabled) {
        this(brotliEnabled, BROTLI_DEFAULT_LEVEL, gzipEnabled, GZIP_DEFAULT_LEVEL);
    }

    public DynamicCompressionStrategy(boolean brotliEnabled, int brotliLevel, boolean gzipEnabled, int gzipLevel) {
        setBrotliEnabled(brotliEnabled);
        setGzipEnabled(gzipEnabled);
        setBrotliLevel(brotliLevel);
        setGzipLevel(gzipLevel);
    }



    public DynamicCompressionStrategy setGzipLevel(int gzipLevel) {
        if(gzipLevel < 0) gzipLevel = 0;
        if(gzipLevel > 9) gzipLevel = 9;
        this._gzipLevel = gzipLevel;
        return this;
    }

    public DynamicCompressionStrategy setBrotliLevel(int brotliLevel) {
        if(brotliLevel < 0) brotliLevel = 0;
        if(brotliLevel > 11) brotliLevel = 11;
        this._brotliLevel = brotliLevel;
        return this;
    }

    public DynamicCompressionStrategy setGzipEnabled(boolean gzipEnabled) {
        this._gzipEnabled = gzipEnabled;
        return this;
    }

    public DynamicCompressionStrategy setBrotliEnabled(boolean brotliEnabled) {
        this._brotliEnabled = brotliEnabled;
        return this;
    }

    public int getGzipLevel() {
        return _gzipLevel;
    }

    public int getBrotliLevel() {
        return _brotliLevel;
    }

    public boolean isBrotliEnabled() {
        return _brotliEnabled;
    }

    public boolean isGzipEnabled() {
        return _gzipEnabled;
    }
}
