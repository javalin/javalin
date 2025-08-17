package io.javalin.compression

import com.aayushatharva.brotli4j.Brotli4jLoader
import io.javalin.util.CoreDependency
import io.javalin.util.DependencyUtil
import io.javalin.util.JavalinLogger
import io.javalin.util.Util
import java.util.Locale.getDefault

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
        if (brotli != null) tryLoadBrotli(brotli)?.let { comp.add(it) }
        if (gzip != null) comp.add(GzipCompressor(gzip.level))
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

    var preferredCompressors: List<CompressionType> = listOf()

    /**
     * When enabling Brotli, we try loading the jvm-brotli native libraries first.
     * If this fails, we keep Brotli disabled and warn the user.
     */
    private fun tryLoadBrotli(brotli: Brotli): Compressor? {
        if (!brotli4jPresent()) {
            throw IllegalStateException(DependencyUtil.missingDependencyMessage(CoreDependency.BROTLI4J))
        }
        return when {
            Brotli4jLoader.isAvailable() -> return Brotli4jCompressor(brotli.level)
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

    private fun getPreferredCompressorForSupportedCompressors(supportedCompressors: List<String>): Compressor?
    {
        for (preferredCompressor in preferredCompressors) {
            if(supportedCompressors.contains(preferredCompressor.typeName)) {
                val compressor = compressors.forType(preferredCompressor.typeName)

                if(compressor != null) {
                    return compressor
                }
            }
        }

        return null;
    }

    fun findMatchingCompressor(encodingHeaderValue: String): Compressor?
    {
        val supportedCompressors = encodingHeaderValue
            .split(",")
            .map { it.trim() }
            .map { it.lowercase(getDefault()) }

        val compressor = getPreferredCompressorForSupportedCompressors(supportedCompressors)

        if (compressor != null) {
            return compressor
        }

        return supportedCompressors.firstNotNullOfOrNull { compressors.forType(it) }
    }

}

fun List<Compressor>.forType(type: String) = this.firstOrNull { it.encoding().equals(type, ignoreCase = true) }
