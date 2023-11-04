package io.javalin.mock.servlet

import jakarta.servlet.http.Part
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Paths

class InMemoryPart(private val state: PartState) : Part {

    class PartState @JvmOverloads constructor(
        val name: String,
        val fileName: String,
        val content: ByteArray,
        val size: Long = content.size.toLong(),
        val contentType: String = "application/octet-stream",
        val headers: Map<String, List<String>> = emptyMap()
    )

    override fun getInputStream(): InputStream = state.content.inputStream()
    override fun getContentType(): String = state.contentType
    override fun getName(): String = state.name
    override fun getSubmittedFileName(): String = state.fileName
    override fun getSize(): Long = state.size
    override fun write(fileName: String) { Files.write(Paths.get(fileName), state.content) }
    override fun delete() {}
    override fun getHeader(name: String): String? = state.headers[name]?.firstOrNull()
    override fun getHeaders(name: String): Collection<String> = state.headers[name] ?: emptyList()
    override fun getHeaderNames(): Collection<String> = state.headers.keys

}
