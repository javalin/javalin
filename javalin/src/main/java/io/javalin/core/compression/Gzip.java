package io.javalin.core.compression;

public class Gzip {

    private final int level;

    /**
     * Create with default compression level of 6.
     */
    public Gzip() {
        this.level = 6;
    }

    /**
     * @param level Compression level. Higher yields better (but slower) compression. Range 0..9, default = 6
     */
    public Gzip(int level) {
        if (level < 0 || level > 9) {
            throw new IllegalArgumentException("Valid range for parameter level is 0 to 9");
        }
        this.level = level;
    }

    public int getLevel() {
        return level;
    }
}
