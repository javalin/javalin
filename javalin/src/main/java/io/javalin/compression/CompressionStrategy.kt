package io.javalin.compression

import com.nixxcode.jvmbrotli.common.BrotliLoader
import io.javalin.compression.impl.JvmBrotliCompressor
import io.javalin.compression.impl.GzipCompressor
import io.javalin.util.CoreDependency
import io.javalin.util.DependencyUtil
import io.javalin.util.JavalinLogger
import io.javalin.util.Util

/**
 * This class is a settings container for Javalin's content compression.
 *
 * It is used by the CompressedOutputStream and JettyPrecompressingResourceHandler to determine the encoding and parameters that should be used when compressing a response.
 *
 * @see io.javalin.compression.CompressedOutputStream
 * @see io.javalin.jetty.JettyPrecompressingResourceHandler
 *
 * @param brotli instance of Brotli config, default = null
 * @param gzip   instance of Gzip config, default = null
 */
class CompressionStrategy(brotli: Brotli? = null, gzip: Gzip? = null) {

    companion object {
        @JvmField
        val NONE = CompressionStrategy()

        @JvmField
        val GZIP = CompressionStrategy(null, Gzip())
        fun brotliPresent() = Util.classExists(CoreDependency.JVMBROTLI.testClass)
        fun brotliJvmAvailable() = try {BrotliLoader.isBrotliAvailable()} catch (t: Throwable) {false}
    }
    val compressors : List<Compressor>

    init {
        val comp : MutableList<Compressor> = mutableListOf()
        //Enabling brotli requires special handling since brotli is platform dependent
        if (brotli != null) tryLoadBrotli(brotli)?.let { comp.add(it) }
        if (gzip != null) comp.add(GzipCompressor(gzip.level))
        compressors = comp.toList()
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
     * When enabling Brotli, we try loading the jvm-brotli native libraries first.
     * If this fails, we keep Brotli disabled and warn the user.
     */
    private fun tryLoadBrotli(brotli: Brotli): Compressor? {
        if (!brotliPresent()) {
            throw IllegalStateException(DependencyUtil.missingDependencyMessage(CoreDependency.JVMBROTLI))
        }
        return when {
            BrotliLoader.isBrotliAvailable() -> return JvmBrotliCompressor(brotli.level)
            else -> {
                JavalinLogger.warn(
                    """|
                       |Failed to enable Brotli compression, because the jvm-brotli native library couldn't be loaded.
                       |jvm-brotli is currently only supported on Windows, Linux and Mac OSX.
                       |If you are running Javalin on a supported system, but are still getting this error,
                       |try re-importing your Maven and/or Gradle dependencies. If that doesn't resolve it,
                       |please create an issue at https://github.com/javalin/javalin/
                       |---------------------------------------------------------------
                       |If you still want compression, please ensure GZIP is enabled!
                       |---------------------------------------------------------------""".trimMargin()
                )
                null
            }
        }
    }

}

fun List<Compressor>.forType(type: String) = this.firstOrNull { it.encoding().equals(type, ignoreCase = true) }
