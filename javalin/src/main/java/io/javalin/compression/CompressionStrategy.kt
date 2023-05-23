package io.javalin.compression

import com.aayushatharva.brotli4j.Brotli4jLoader
import com.nixxcode.jvmbrotli.common.BrotliLoader
import io.javalin.compression.impl.Brotli4j
import io.javalin.compression.impl.BrotliJvm
import io.javalin.compression.impl.GzipCompressor
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
    val compressors : List<Compressor>

    init {
        //Enabling brotli requires special handling since brotli is platform dependent
        val comp : MutableList<Compressor> = mutableListOf()
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
     * When enabling Brotli, we try loading the brotli4j or jvm-brotli native libraries first.
     * If this fails, we keep Brotli disabled and warn the user.
     */
    private fun tryLoadBrotli(brotli: Brotli): Compressor? {
        if (!Util.classExists(CoreDependency.BROTLI4J.testClass) &&
            !Util.classExists(CoreDependency.JVMBROTLI.testClass)) {
            throw IllegalStateException(DependencyUtil.missingDependencyMessage(CoreDependency.BROTLI4J))
        }
        return when {
            Brotli4jLoader.isAvailable() -> return Brotli4j(brotli.level)
            BrotliLoader.isBrotliAvailable() -> return BrotliJvm(brotli.level)
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

fun List<Compressor>.forType(type: CompressionType) = this.firstOrNull { it.type() == type }
