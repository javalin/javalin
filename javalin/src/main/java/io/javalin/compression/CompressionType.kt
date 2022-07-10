package io.javalin.compression

enum class CompressionType(val typeName: String, val extension: String) {
    GZIP("gzip", ".gz"),
    BR("br", ".br"),
    NONE("", "");

    fun acceptEncoding(acceptEncoding: String): Boolean {
        return acceptEncoding.contains(typeName, ignoreCase = true)
    }

    companion object {
        fun getByAcceptEncoding(acceptEncoding: String): CompressionType {
            return values().find { it.acceptEncoding(acceptEncoding) } ?: NONE
        }
    }
}
