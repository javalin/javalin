package io.javalin.compression

import com.nixxcode.jvmbrotli.common.BrotliLoader
import io.javalin.util.CoreDependency
import io.javalin.util.DependencyUtil
import io.javalin.util.JavalinLogger
import io.javalin.util.Util

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

    companion object {
        @JvmField
        val NONE = CompressionStrategy()

        @JvmField
        val GZIP = CompressionStrategy(null, Gzip())
    }

    val brotli: Brotli?
    val gzip: Gzip?

    init {
        //Enabling brotli requires special handling since jvm-brotli is platform dependent
        this.brotli = if (brotli != null) tryLoadBrotli(brotli) else null
        this.gzip = gzip
    }

    /** 1500 is the size of a packet, compressing responses smaller than this serves no purpose */
    var minSizeForCompression = 1500

    /** Those mime types will be processed using NONE compression strategy */
    var excludedMimeTypesFromCompression = listOf(
        "image/",
        "audio/",
        "video/",
        "application/compress",
        "application/zip",
        "application/gzip",
        "application/bzip2",
        "application/brotli",
        "application/x-xz",
        "application/x-rar-compressed"
    )

    /**
     * When enabling Brotli, we try loading the jvm-brotli native library first.
     * If this fails, we keep Brotli disabled and warn the user.
     */
    private fun tryLoadBrotli(brotli: Brotli): Brotli? {
        if (!Util.classExists(CoreDependency.JVMBROTLI.testClass)) {
            throw IllegalStateException(DependencyUtil.missingDependencyMessage(CoreDependency.JVMBROTLI))
        }
        return if (BrotliLoader.isBrotliAvailable()) {
            brotli
        } else {
            JavalinLogger.warn(
                """|
                   |Failed to enable Brotli compression, because the jvm-brotli native library couldn't be loaded.
                   |jvm-brotli is currently only supported on Windows, Linux and Mac OSX.
                   |If you are running Javalin on a supported system, but are still getting this error,
                   |try re-importing your Maven and/or Gradle dependencies. If that doesn't resolve it,
                   |please create an issue at https://github.com/tipsy/javalin/
                   |---------------------------------------------------------------
                   |If you still want compression, please ensure GZIP is enabled!
                   |---------------------------------------------------------------""".trimMargin()
            )
            null
        }
    }
}
