package io.javalin.core.compression

import org.meteogroup.jbrotli.libloader.BrotliLibraryLoader
import io.javalin.Javalin.log

/**
 * This class is a settings container for Javalin's dynamic content compression.
 *
 * It is used by DynamicCompressionHandler to determine the encoding and parameters that should be used
 * when encoding a dynamic response from the server.
 *
 * @see DynamicCompressionHandler
 *
 * @param brotliEnabled Default: false
 * @param gzipEnabled   Default: true
 * @param brotliLevel   Brotli compression level. Higher means better (but slower) compression.
 *                      Range 0..11, Default: 4
 * @param gzipLevel     Gzip compression level. Higher means better (but slower) compression.
 *                      Range 0..9, Default: 6
 */
class DynamicCompressionStrategy @JvmOverloads constructor(
        brotliEnabled: Boolean = false,
        gzipEnabled: Boolean = true,
        brotliLevel: Int = 4,
        gzipLevel: Int = 6) {

    val brotliEnabled: Boolean
    val gzipEnabled: Boolean
    val brotliLevel: Int
    val gzipLevel: Int

    companion object {
        //Used to init the corresponding object in JavalinConfig
        @JvmField val NONE = DynamicCompressionStrategy(false, false)
    }

    init {
        require(brotliLevel in 0..11) {
            "Valid range for parameter brotliLevel is 0 to 11"
        }
        require(gzipLevel in 0..9) {
            "Valid range for parameter gzipLevel is 0 to 9"
        }

        //Enabling brotli requires special handling since jbrotli libs are platform dependent
        this.brotliEnabled = if (brotliEnabled) tryLoadBrotli() else false

        this.gzipEnabled = gzipEnabled
        this.brotliLevel = brotliLevel
        this.gzipLevel = gzipLevel
    }

    /**
     * When enabling Brotli, we try loading the jbrotli native library first.
     * If this fails, we keep Brotli disabled and warn the user.
     */
    private fun tryEnableBrotli(brotliEnabled: Boolean): Boolean {
        if (!brotliEnabled) {
            return false
        } else {
            return if (tryLoadBrotli()) {
                true
            } else {
                log.warn(
                        "Failed to enable Brotli compression, because we couldn't load the JBrotli native library\n" +
                        "Brotli is currently only supported on Windows, Linux and Mac OSX.\n" +
                        "If you are running Javalin on a supported system, but are still getting this error,\n" +
                        "try re-importing your Maven and/or Gradle dependencies. If that doesn't resolve it,\n" +
                        "please report the issue at https://github.com/tipsy/javalin/\n" +
                        "---------------------------------------------------------------\n" +
                        "If you still want dynamic compression, please ensure GZIP is enabled!\n" +
                        "---------------------------------------------------------------\n"
                )
                false
            }
        }
    }

    private fun tryLoadBrotli(): Boolean {
        return try {
            BrotliLibraryLoader.loadBrotli()
            true
        } catch (t: Throwable) {
            false
        }
    }
}
