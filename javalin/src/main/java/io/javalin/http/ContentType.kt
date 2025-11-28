package io.javalin.http

/**
 * List of mime types for the most common file types.
 *
 * Sources:
 * * [Mozilla / Common Types](https://developer.mozilla.org/en-US/docs/Web/HTTP/Basics_of_HTTP/MIME_types/Common_types)
 * * [Apache / Mime Types](http://svn.apache.org/repos/asf/httpd/httpd/trunk/docs/conf/mime.types)
 */
enum class ContentType(
    /** Standardized mime-type */
    val mimeType: String,
    /**
     *  Utility property describes if given format is readable as a text
     *  or visible as media file by common tools, like e.g. browsers
     */
    val isHumanReadable: Boolean,
    /** Common file extensions for this type */
    vararg val extensions: String,
) {

    /* Text */

    TEXT_PLAIN("text/plain", true, "txt"),
    TEXT_CSS("text/css", true, "css"),
    TEXT_CSV("text/csv", false, "csv"),
    TEXT_HTML("text/html", true, "html", "htm"),
    TEXT_JS("text/javascript", true, "js", "mjs"),
    TEXT_MARKDOWN("text/markdown", true, "md"),
    TEXT_PROPERTIES("text/x-java-properties", true, "properties"),
    TEXT_XML("text/xml", true, "xml"),

    /* Image */

    IMAGE_AVIF("image/avif", true, "avif"),
    IMAGE_BMP("image/bmp", true, "bmp"),
    IMAGE_GIF("image/gif", true, "gif"),
    IMAGE_ICO("image/vnd.microsoft.icon", true, "ico"),
    IMAGE_JPEG("image/jpeg", true, "jpeg", "jpg"),
    IMAGE_PNG("image/png", true, "png"),
    IMAGE_SVG("image/svg+xml", true, "svg"),
    IMAGE_TIFF("image/tiff", true, "tiff", "tif"),
    IMAGE_WEBP("image/webp", true, "webp"),

    /* Audio */

    AUDIO_AAC("audio/aac", true, "aac"),
    AUDIO_MIDI("audio/midi", true, "mid", "midi"),
    AUDIO_MPEG("audio/mpeg", true, "mp3"),
    AUDIO_OGA("audio/ogg", true, "oga"),
    AUDIO_OPUS("audio/opus", true, "opus"),
    AUDIO_WAV("audio/wav", true, "wav"),
    AUDIO_WEBA("audio/weba", true, "waba"),

    /* Video */

    VIDEO_AVI("video/x-msvideo", true, "avi"),
    VIDEO_MP4("video/mp4", true, "mp4"),
    VIDEO_MPEG("video/mpeg", true, "mpeg"),
    VIDEO_OGG("video/ogg", true, "ogg"),
    VIDEO_WEBM("video/webm", true, "webm"),

    /* Font */

    FONT_OTF("font/otf", false, "otf"),
    FONT_TTF("font/ttf", false, "ttf"),
    FONT_WOFF("font/woff", false, "woff"),
    FONT_WOFF2("font/woff2", false, "woff2"),

    /* Application */

    APPLICATION_OCTET_STREAM("application/octet-stream", false, "bin"),
    APPLICATION_BZ("application/x-bzip", false, "bz"),
    APPLICATION_BZ2("application/x-bzip2", false, "bz2"),
    APPLICATION_CDN("application/cdn", true, "cdn"),
    APPLICATION_DOC("application/msword", false, "doc"),
    APPLICATION_DOCX("application/vnd.openxmlformats-officedocument.wordprocessingml.document", false, "docx"),
    APPLICATION_EPUB("application/epub+zip", false, "epub"),
    APPLICATION_GZ("application/gzip", false, "gz"),
    APPLICATION_JSON("application/json", true, "json"),
    APPLICATION_MPKG("application/vnd.apple.installer+xml", false, "mpkg"),
    APPLICATION_JAR("application/java-archive", false, "jar"),
    APPLICATION_PDF("application/pdf", true, "pdf"),
    APPLICATION_POM("application/xml", true, "pom"),
    APPLICATION_RAR("application/vnd.rar", false, "rar"),
    APPLICATION_SH("application/x-sh", true, "sh"),
    APPLICATION_SWF("application/x-shockwave-flash", false, "swf"),
    APPLICATION_TAR("application/x-tar", false, "tar"),
    APPLICATION_XHTML("application/xhtml+xml", true, "xhtml"),
    APPLICATION_YAML("application/yaml", true, "yaml", "yml"),
    APPLICATION_ZIP("application/zip", false, "zip"),
    APPLICATION_7Z("application/x-7z-compressed", false, "7z"),

    /* Other */

    MULTIPART_FORM_DATA("multipart/form-data", false, "multipart/form-data");

    override fun toString(): String =
        mimeType

    companion object {

        /* Compile time common constants - useful for annotations & as raw string values */

        const val PLAIN = "text/plain"
        const val CSS = "text/css"
        const val HTML = "text/html"
        const val XML = "text/xml"
        const val OCTET_STREAM = "application/octet-stream"
        const val JAVASCRIPT = "text/javascript"
        const val JSON = "application/json"
        const val FORM_DATA = "multipart/form-data"

        @JvmStatic
        fun contentType(mimeType: String): ContentType? =
            values().find { it.mimeType.equals(mimeType, ignoreCase = true) }

        @JvmStatic
        fun contentTypeByExtension(extension: String): ContentType? =
            values().firstOrNull { type ->
                type.extensions.any {
                    extension.equals(it, ignoreCase = true)
                }
            }

        @JvmStatic
        fun mimeTypeByExtension(extensions: String): String? =
            contentTypeByExtension(extensions)?.mimeType

    }

}
