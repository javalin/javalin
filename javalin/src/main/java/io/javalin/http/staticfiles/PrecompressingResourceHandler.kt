package io.javalin.http.staticfiles

import io.javalin.http.LeveledBrotliStream
import io.javalin.http.LeveledGzipStream
import org.eclipse.jetty.util.resource.Resource
import java.io.*

enum class CompressType(val type: String, val extension: String) {
    GZIP("gzip", ".gz"),
    BR("br", ".br")
}

class PrecompressingResourceHandler {
    val compressedFiles = HashMap<String, ByteArray>()

    fun handle(resource: Resource, target: String): Boolean {

        return false
    }

    private fun getCompressedFileInputStream(originResource: Resource, target: String, type: CompressType, level: Int): InputStream {
        val key = target + type.extension
        if (!compressedFiles.containsKey(key)) {
            compressedFiles[key] = getCompressedByteArray(originResource, type, level)
        }
        return compressedFiles[key]!!.inputStream()
    }

    private fun getCompressedByteArray(resource: Resource, type: CompressType, level: Int): ByteArray {
        val fileInput = resource.file.inputStream()
        val byteArrayOutputStream = ByteArrayOutputStream()
        val outputStream: OutputStream = when (type) {
            CompressType.GZIP -> {
                LeveledGzipStream(byteArrayOutputStream, level)
            }
            CompressType.BR -> {
                LeveledBrotliStream(byteArrayOutputStream, level)
            }
        }
        val buffer = ByteArray(2048)
        var bytesRead: Int
        while (fileInput.read(buffer).also { bytesRead = it } > 0) {
            outputStream.write(buffer, 0, bytesRead)
        }
        fileInput.close()
        outputStream.close()
        return byteArrayOutputStream.toByteArray()
    }
}
