package io.javalin.core.compression

import io.javalin.Javalin.log
import org.meteogroup.jbrotli.libloader.BrotliLibraryLoader

/**
 * This class is a settings container for Javalin's dynamic content compression.
 *
 * It is used by DynamicCompressionHandler to determine the encoding and parameters that should be used
 * when encoding a dynamic response from the server.
 *
 * @see DynamicCompressionHandler
 *
 * @param brotli instance of Brotli handler, default = null
 * @param gzip   instance of Gzip handler, default = null
 */
class CompressionStrategy(brotli: Brotli? = null, gzip: Gzip? = null) {

    val brotli: Brotli?
    val gzip: Gzip?

    companion object {
        @JvmField
        val NONE = CompressionStrategy()
        @JvmField
        val GZIP = CompressionStrategy(null, Gzip())
    }

    init {
        //Enabling brotli requires special handling since jbrotli libs are platform dependent
        this.brotli = if (brotli != null) tryLoadBrotli(brotli) else null
        this.gzip = gzip
    }

    /**
     * When enabling Brotli, we try loading the jbrotli native library first.
     * If this fails, we keep Brotli disabled and warn the user.
     */
    private fun tryLoadBrotli(brotli: Brotli) = try {
        BrotliLibraryLoader.loadBrotli()
        brotli
    } catch (t: Throwable) {
        log.warn("""
            |Failed to enable Brotli compression, because we couldn't load the JBrotli native library
            |Brotli is currently only supported on Windows, Linux and Mac OSX.
            |If you are running Javalin on a supported system, but are still getting this error,
            |try re-importing your Maven and/or Gradle dependencies. If that doesn't resolve it,
            |please report the issue at https://github.com/tipsy/javalin/
            |---------------------------------------------------------------
            |If you still want dynamic compression, please ensure GZIP is enabled!
            |---------------------------------------------------------------
        """.trimIndent())
        null
    }
}
