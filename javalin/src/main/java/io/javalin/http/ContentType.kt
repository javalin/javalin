package io.javalin.http

enum class ContentType(
    val mimeType: String,
    val isHumanReadable: Boolean,
    vararg val extensions: String,
) {

    // Fallback list of basic mime types used by Maven repository
    // ~ https://developer.mozilla.org/en-US/docs/Web/HTTP/Basics_of_HTTP/MIME_types/Common_types
    // Extra types
    // ~ http://svn.apache.org/repos/asf/httpd/httpd/trunk/docs/conf/mime.types

    /* Text */

    TEXT_PLAIN("text/plain", true, "txt"),
    TEXT_CSS("text/css", true, "css"),
    TEXT_CSV("text/csv", false, "csv"),
    TEXT_HTML("text/html", true, "html", "htm"),
    TEXT_MARKDOWN("text/markdown", true, "md"),
    TEXT_PROPERTIES("text/x-java-properties", true, "properties"),
    TEXT_XML("text/xml", true, "xml"),

    /* Image */

    IMAGE_ICO("image/vnd.microsoft.icon", true, "ico"),
    IMAGE_JPEG( "image/jpeg", true, "jpeg"),
    IMAGE_JPG("image/jpg", true, "jpg"),
    IMAGE_PNG("image/png", true, "png"),
    IMAGE_TIFF( "image/tiff", true, "tiff", "tif"),

    /* Font */

    FONT_OTF("font/otf", false, "otf",),
    FONT_TTF("font/ttf", false, "ttf"),

    /* Application */

    APPLICATION_OCTET_STREAM("application/octet-stream", false, "bin"),
    APPLICATION_BZ("application/x-bzip", false, "bz"),
    APPLICATION_BZ2("application/x-bzip2", false, "bz2"),
    APPLICATION_CDN("application/cdn", true, "cdn"),
    APPLICATION_GZ("application/gzip", false, "gz"),
    APPLICATION_JS("application/javascript", true, "js"),
    APPLICATION_JSON("application/json", true, "json"),
    APPLICATION_MPKG("application/vnd.apple.installer+xml", false, "mpkg"),
    APPLICATION_JAR("application/java-archive", false, "jar"),
    APPLICATION_PDF("application/pdf", false, "pdf"),
    APPLICATION_POM("application/xml", true, "pom"),
    APPLICATION_RAR("application/vnd.rar", false, "rar"),
    APPLICATION_SH("application/x-sh", true, "sh"),
    APPLICATION_TAR("application/x-tar", false, "tar"),
    APPLICATION_XHTML("application/xhtml+xml", true, "xhtml"),
    APPLICATION_YAML( "application/yaml", true, "yaml", "yml"),
    APPLICATION_ZIP("application/zip", false, "zip"),
    APPLICATION_7Z("application/x-7z-compressed", false, "7z"),

    /* Other */

    MULTIPART_FORM_DATA("multipart/form-data", false, "multipart/form-data");

    override fun toString(): String =
        mimeType

    companion object {

        /* Compile time common constants - useful for annotations & as raw string values */

        const val PLAIN = "text/plain"
        const val HTML = "text/html"
        const val XML = "text/xml"
        const val OCTET_STREAM = "application/octet-stream"
        const val JAVASCRIPT = "application/javascript"
        const val JSON = "application/json"
        const val FORM_DATA = "multipart/form-data"

        @JvmStatic
        fun getContentType(mimeType: String): ContentType? =
            values().find { it.mimeType.equals(mimeType, ignoreCase = true) }

        @JvmStatic
        fun getContentTypeByExtension(extension: String): ContentType? =
            values().firstOrNull { type ->
                type.extensions.any {
                    extension.equals(it, ignoreCase = true)
                }
            }

        @JvmStatic
        fun getMimeTypeByExtension(extensions: String): String? =
            getContentTypeByExtension(extensions)?.mimeType

    }

}
