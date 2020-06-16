package io.javalin.core.compression

import com.nixxcode.jvmbrotli.common.BrotliLoader
import io.javalin.Javalin
import io.javalin.core.util.OptionalDependency
import io.javalin.core.util.Util

/**
 * This class is a settings container for Javalin's content compression.
 *
 * It is used by the JavalinResponseWrapper to determine the encoding and parameters that should be used when compressing a response.
 *
 * @see io.javalin.http.JavalinResponseWrapper
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
        //Enabling brotli requires special handling since jvm-brotli is platform dependent
        this.brotli = if (brotli != null) tryLoadBrotli(brotli) else null
        this.gzip = gzip
    }

    /**
     * When enabling Brotli, we try loading the jvm-brotli native library first.
     * If this fails, we keep Brotli disabled and warn the user.
     */
    private fun tryLoadBrotli(brotli: Brotli): Brotli? {
        Util.ensureDependencyPresent(OptionalDependency.JVMBROTLI, startupCheck = true)
        val brotliAvailable = BrotliLoader.isBrotliAvailable()
        if (brotliAvailable) {
            return brotli
        } else {
            Javalin.log?.warn("""${"\n"}
                Failed to enable Brotli compression, because the jvm-brotli native library couldn't be loaded.
                jvm-brotli is currently only supported on Windows, Linux and Mac OSX.
                If you are running Javalin on a supported system, but are still getting this error,
                try re-importing your Maven and/or Gradle dependencies. If that doesn't resolve it,
                please create an issue at https://github.com/tipsy/javalin/
                ---------------------------------------------------------------
                If you still want compression, please ensure GZIP is enabled!
                ---------------------------------------------------------------
            """.trimIndent())
            return null
        }
    }
}
