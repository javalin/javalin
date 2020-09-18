package io.javalin.core.compression;

public class Brotli {

    private final int level;

    /**
     * Create with default compression level of 4.
     */
    public Brotli() {
        this.level = 4;
    }

    /**
     * @param level Compression level. Higher yields better (but slower) compression. Range 0..11, default = 4
     */
    public Brotli(int level) {
        if (level < 0 || level > 11) {
            throw new IllegalArgumentException("Valid range for parameter level is 0 to 11");
        }
        this.level = level;
    }

    public int getLevel() {
        return level;
    }
}
