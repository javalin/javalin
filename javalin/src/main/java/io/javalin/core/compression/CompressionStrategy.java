package io.javalin.core.compression;

import com.nixxcode.jvmbrotli.common.BrotliLoader;
import io.javalin.Javalin;
import io.javalin.core.util.OptionalDependency;
import io.javalin.core.util.Util;

/**
 * This class is a settings container for Javalin's content compression.
 * <p>
 * It is used by the JavalinResponseWrapper to determine the encoding and parameters that should be used when compressing a response.
 *
 * @see io.javalin.http.JavalinResponseWrapper
 */
public class CompressionStrategy {

    public static final CompressionStrategy NONE = new CompressionStrategy(null, null);
    public static final CompressionStrategy GZIP = new CompressionStrategy(null, new Gzip());

    public final Brotli brotli;
    public final Gzip gzip;

    /**
     * This class is a settings container for Javalin's content compression.
     * <p>
     * It is used by the JavalinResponseWrapper to determine the encoding and parameters that should be used when compressing a response.
     *
     * @param brotli instance of Brotli handler, default = null
     * @param gzip   instance of Gzip handler, default = null
     * @see io.javalin.http.JavalinResponseWrapper
     */
    public CompressionStrategy(Brotli brotli, Gzip gzip) {
        //Enabling brotli requires special handling since jvm-brotli is platform dependent
        this.brotli = tryLoadBrotli(brotli);
        this.gzip = gzip;
    }

    /**
     * When enabling Brotli, we try loading the jvm-brotli native library first.
     * If this fails, we keep Brotli disabled and warn the user.
     */
    private Brotli tryLoadBrotli(Brotli brotli) {
        if (brotli == null) {
            return null;
        }
        Util.INSTANCE.ensureDependencyPresent(OptionalDependency.JVMBROTLI, true);
        if (BrotliLoader.isBrotliAvailable()) {
            return brotli;
        } else {
            if (Javalin.log != null) {
                Javalin.log.warn(failedToEnableBrotli());
            }
            return null;
        }
    }

    private String failedToEnableBrotli() {
        return "Failed to enable Brotli compression, because the jvm-brotli native library couldn't be loaded.\n" +
            "jvm-brotli is currently only supported on Windows, Linux and Mac OSX.\n" +
            "If you are running Javalin on a supported system, but are still getting this error,\n" +
            "try re-importing your Maven and/or Gradle dependencies. If that doesn't resolve it,\n" +
            "please create an issue at https://github.com/tipsy/javalin/\n" +
            "---------------------------------------------------------------\n" +
            "If you still want compression, please ensure GZIP is enabled!\n" +
            "---------------------------------------------------------------\n";
    }
}
