package io.javalin.compression

enum class CompressionType(val typeName: String, val extension: String) {
    GZIP("gzip", ".gz"),
    BR("br", ".br"),
    NONE("", "");
}
