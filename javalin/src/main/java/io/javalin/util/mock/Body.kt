package io.javalin.util.mock

import io.javalin.http.ContentType
import io.javalin.json.JSON_MAPPER_KEY
import io.javalin.json.JsonMapper
import io.javalin.json.toJsonString
import java.io.InputStream

interface Body {

    fun init(mockConfig: MockConfig) {}
    fun toInputStream(): InputStream
    fun getContentType(): String?
    fun getContentLength(): Long?

    companion object {

        private abstract class AbstractBody(private val contentType: String?, private val contentLength: Long? = null) : Body {
            override fun getContentType(): String? = contentType
            override fun getContentLength(): Long? = contentLength
        }

        @JvmStatic
        @JvmOverloads
        fun ofString(
            body: String,
            contentType: String? = ContentType.PLAIN,
            contentLength: Long? = body.length.toLong()
        ): Body =
            object : AbstractBody(contentType = contentType, contentLength = contentLength) {
                override fun toInputStream(): InputStream = body.byteInputStream()
            }

        @JvmStatic
        @JvmOverloads
        fun ofInputStream(
            body: InputStream,
            contentType: String? = ContentType.OCTET_STREAM,
            contentLength: Long? = null
        ): Body =
            object : AbstractBody(contentType = contentType) {
                val content = body.use { it.readAllBytes() }
                override fun toInputStream(): InputStream = content.inputStream()
                override fun getContentLength(): Long = contentLength ?: content.size.toLong()
            }

        @JvmStatic
        @JvmOverloads
        fun ofObject(
            body: Any,
            contentType: String? = ContentType.JSON,
            contentLength: Long? = null
        ): Body =
            object : AbstractBody(contentType = contentType, contentLength = contentLength) {
                private lateinit var content: ByteArray
                override fun init(mockConfig: MockConfig) {
                    this.content = (mockConfig.javalinConfig.pvt.appAttributes[JSON_MAPPER_KEY] as JsonMapper).toJsonString(body).toByteArray()
                }
                override fun toInputStream(): InputStream = content.inputStream()
                override fun getContentLength(): Long = content.size.toLong()
            }

    }

}
