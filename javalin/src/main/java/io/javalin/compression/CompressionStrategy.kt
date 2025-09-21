package io.javalin.compression

import com.aayushatharva.brotli4j.Brotli4jLoader
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
 * @param bufferSize compression buffer size, default = null (uses system default)
 */
class CompressionStrategy @JvmOverloads constructor(brotli: Brotli? = null, gzip: Gzip? = null, bufferSize: Int? = null) {

    companion object {
        @JvmField
        val NONE = CompressionStrategy()

        @JvmField
        val GZIP = CompressionStrategy(null, Gzip())

        // Check if the dependency is present
        fun brotli4jPresent() = Util.classExists(CoreDependency.BROTLI4J.testClass)

        // Check if the native libraries are available

        fun brotli4jAvailable() = try {
            Brotli4jLoader.isAvailable()
        } catch (t: Throwable) {
            false
        }

        /** @returns true if brotli is can be used */
        fun brotliImplAvailable() =  brotli4jPresent() && brotli4jAvailable()

    }

    val compressors: List<Compressor>

    init {
        val comp: MutableList<Compressor> = mutableListOf()
        //Enabling brotli requires special handling since brotli is platform dependent
        if (brotli != null) tryLoadBrotli(brotli, bufferSize ?: brotli.bufferSize)?.let { comp.add(it) }
        if (gzip != null) comp.add(GzipCompressor(gzip.level, bufferSize ?: gzip.bufferSize))
        compressors = comp.toList()
    }

    /** 1500 is the size of a packet, compressing responses smaller than this serves no purpose */
    var defaultMinSizeForCompression = 1500

    /** these mime types will always be allowed */
    var allowedMimeTypes = listOf(
        "image/svg+xml" // we need to allow svg explicitly, because images in general are excluded
    )

    /** these mime types will be processed using NONE compression strategy */
    var excludedMimeTypes = listOf(
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
    private fun tryLoadBrotli(brotli: Brotli, bufferSize: Int?): Compressor? {
        if (!brotli4jPresent()) {
            throw IllegalStateException(DependencyUtil.missingDependencyMessage(CoreDependency.BROTLI4J))
        }
        return when {
            Brotli4jLoader.isAvailable() -> return Brotli4jCompressor(brotli.level, bufferSize)
            else -> {
                JavalinLogger.warn(
                    """|
                       |Failed to enable Brotli compression, because the brotli4j native library couldn't be loaded.
                       |brotli4j is currently only supported on Windows, Linux and Mac OSX.
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
