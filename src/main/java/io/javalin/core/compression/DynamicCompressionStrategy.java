package io.javalin.core.compression;

import org.meteogroup.jbrotli.libloader.BrotliLibraryLoader;

import static io.javalin.Javalin.log;

/**
 * This class is a settings container for Javalin's dynamic content compression.
 *
 * It is used by DynamicCompressionHandler to determine the encoding and parameters that should be used
 * when encoding a dynamic response from the server.
 *
 * @see DynamicCompressionHandler
 */
public class DynamicCompressionStrategy {

    public static final int BROTLI_DEFAULT_LEVEL = 4;
    public static final int GZIP_DEFAULT_LEVEL = 6;

    private boolean _brotliEnabled = false;
    private boolean _gzipEnabled = true;
    private int _brotliLevel = BROTLI_DEFAULT_LEVEL;
    private int _gzipLevel = GZIP_DEFAULT_LEVEL;

    /**
     * Default settings, nothing is changed
     */
    public DynamicCompressionStrategy() { }

    /**
     * Enable or disable GZIP and Brotli. Default compression levels are used
     */
    public DynamicCompressionStrategy(boolean brotliEnabled, boolean gzipEnabled) {
        this(brotliEnabled, BROTLI_DEFAULT_LEVEL, gzipEnabled, GZIP_DEFAULT_LEVEL);
    }

    /**
     * Enable or disable GZIP and Brotli with custom compression levels
     */
    public DynamicCompressionStrategy(boolean brotliEnabled, int brotliLevel, boolean gzipEnabled, int gzipLevel) {
        setBrotliEnabled(brotliEnabled);
        setGzipEnabled(gzipEnabled);
        setBrotliLevel(brotliLevel);
        setGzipLevel(gzipLevel);
    }

    /**
     * @param gzipLevel GZIP compression level. Valid range is 0..9
     */
    public DynamicCompressionStrategy setGzipLevel(int gzipLevel) {
        if(gzipLevel < 0) gzipLevel = 0;
        if(gzipLevel > 9) gzipLevel = 9;
        _gzipLevel = gzipLevel;
        return this;
    }

    /**
     * @param brotliLevel Brotli compression level. Valid range is 0..11
     */
    public DynamicCompressionStrategy setBrotliLevel(int brotliLevel) {
        if(brotliLevel < 0) brotliLevel = 0;
        if(brotliLevel > 11) brotliLevel = 11;
        _brotliLevel = brotliLevel;
        return this;
    }

    public DynamicCompressionStrategy setGzipEnabled(boolean gzipEnabled) {
        _gzipEnabled = gzipEnabled;
        return this;
    }

    /**
     * When enabling Brotli, we try loading the JBrotli native library first.
     * If this fails, we keep Brotli disabled and warn the user.
     */
    public DynamicCompressionStrategy setBrotliEnabled(boolean brotliEnabled) {
        if(!_brotliEnabled && brotliEnabled == true) {
            if(tryLoadBrotli()) {
                _brotliEnabled = true;
            } else {
                log.warn("Failed to enable Brotli compression, because we couldn't load the JBrotli native library.");
                log.warn("Brotli is currently only supported on Windows, Linux and Mac OSX.");
                log.warn("If you are running Javalin on a supported system, but are still getting this error,");
                log.warn("try re-importing your Maven and/or Gradle dependencies. If that doesn't resolve it,");
                log.warn("please report the issue at https://github.com/tipsy/javalin/");
                log.warn("---------------------------------------------------------------");
                log.warn("If you still want dynamic compression, please ensure GZIP is enabled!");
                log.warn("---------------------------------------------------------------");
                log.warn("");
            }
        } else {
            _brotliEnabled = brotliEnabled;
        }
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


    //PRIVATE methods
    private boolean tryLoadBrotli() {
        try {
            BrotliLibraryLoader.loadBrotli();
            return true;
        } catch (Throwable t) {
            return false;
        }
    }
}
