package io.javalin.http;

import java.nio.charset.StandardCharsets;
import java.text.Normalizer;

/**
 * Class representing the Content-Disposition header value.
 * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Content-Disposition">MDN Web Docs: Content-Disposition</a>
 */
public class ContentDisposition {

    private final boolean inline;
    private final boolean multipart;
    private final boolean usingRfc5987;
    private final String fileName, name, value;

    private ContentDisposition(boolean inline, boolean multipart, boolean usingRfc5987, String fileName, String name, String value) {
        this.inline = inline;
        this.multipart = multipart;
        this.usingRfc5987 = usingRfc5987;
        this.fileName = fileName;
        this.name = name;
        this.value = value;
    }

    /**
     * Converts a filename to the RFC 5987 format for use in HTTP headers.
     * @param filename The filename to convert.
     * @return The filename in RFC 5987 format.
     */
    private String toRfc5987(String filename) {
        StringBuilder result = new StringBuilder("UTF-8''");

        for (byte b : filename.getBytes(StandardCharsets.UTF_8)) {
            int c = b & 0xff;

            if (isAttrChar(c)) {
                result.append((char) c);
            } else {
                result.append('%');
                result.append(Character.toUpperCase(Character.forDigit((c >>> 4) & 0xf, 16)));
                result.append(Character.toUpperCase(Character.forDigit(c & 0xf, 16)));
            }
        }

        return result.toString();
    }

    /**
     * Checks if a character is valid for use in HTTP header attributes according to RFC 5987.
     * @param c The character to check.
     * @return True if the character is valid, false otherwise.
     */
    private boolean isAttrChar(int c) {
        return (c >= 'a' && c <= 'z')
            || (c >= 'A' && c <= 'Z')
            || (c >= '0' && c <= '9')
            || "!#$&+-.^_`|~".indexOf(c) >= 0;
    }

    /**
     * Converts a filename to a legacy-safe format for use in HTTP headers, ensuring compatibility with older systems.
     * @param filename The filename to convert.
     * @return The filename in a legacy-safe format.
     */
    private String toLegacySafeFilename(String filename) {
        if (filename == null || filename.isEmpty())
            return "download";

        String s = Normalizer.normalize(filename, Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "")
            .replaceAll("[^A-Za-z0-9._ -]", "_")
            .trim()
            .replaceAll("^[. ]+", "")
            .replaceAll("[. ]+$", "")
            .replaceAll("_+", "_");

        if (s.isEmpty())
            return "download";

        // Windows reserved device names.
        if (s.matches("(?i)^(CON|PRN|AUX|NUL|COM[1-9]|LPT[1-9])(?:\\..*)?$"))
            s = "_" + s;

        return s;
    }

    /**
     * Stringifies the Content-Disposition value based on the specified parameters.
     * @return The stringified Content-Disposition value.
     */
    public String stringifyValue() {
        if (multipart) {
            if (value == null)
                return "form-data; name=" + name;
            return "form-data; name=" + name + "; value=" + value;
        }
        if (inline)
            return "inline";
        if (fileName == null)
            return "attachment";

        String str = "attachment; filename=\"" + toLegacySafeFilename(fileName) + "\"";
        if (usingRfc5987)
            str += "; filename*=" + toRfc5987(fileName);
        return str;
    }

    /**
     * Creates a ContentDisposition instance for inline content.
     * @return A ContentDisposition instance representing inline content.
     */
    public static ContentDisposition inline() {
        return new ContentDisposition(true, false, false, null, null, null);
    }

    /**
     * Creates a ContentDisposition instance for attachment content.
     * @return A ContentDisposition instance representing attachment content.
     */
    public static ContentDisposition attachment() {
        return new ContentDisposition(false, false, false, null, null, null);
    }

    /**
     * Creates a ContentDisposition instance for attachment content with a specified filename.
     * @param fileName The filename to be used in the Content-Disposition header.
     * @return A ContentDisposition instance representing attachment content with the specified filename.
     */
    public static ContentDisposition attachment(String fileName) {
        if (fileName == null || fileName.isBlank())
            throw new IllegalArgumentException("fileName cannot be null or blank");

        return new ContentDisposition(false, false, false, fileName, null, null);
    }

    /**
     * Creates a ContentDisposition instance for attachment content with a specified filename using RFC 5987 encoding.
     * @param fileName The filename to be used in the Content-Disposition header.
     * @return A ContentDisposition instance representing attachment content with the specified filename and RFC 5987 encoding.
     */
    public static ContentDisposition attachmentRfc5987(String fileName) {
        if (fileName == null || fileName.isBlank())
            throw new IllegalArgumentException("fileName cannot be null or blank");

        return new ContentDisposition(false, false, true, fileName, null, null);
    }

    /**
     * Creates a ContentDisposition instance for multipart/form-data content with a specified name.
     * @param name The name to be used in the Content-Disposition header for multipart/form-data.
     * @return A ContentDisposition instance representing multipart/form-data content with the specified name.
     */
    public static ContentDisposition multipart(String name) {
        if (name == null || name.isBlank())
            throw new IllegalArgumentException("name cannot be null or blank");

        return new ContentDisposition(false, true, false, null, name, null);
    }

    /**
     * Creates a ContentDisposition instance for multipart/form-data content with a specified name and value.
     * @param name The name to be used in the Content-Disposition header for multipart/form-data.
     * @param value The value to be used in the Content-Disposition header for multipart/form-data.
     * @return A ContentDisposition instance representing multipart/form-data content with the specified name and value.
     */
    public static ContentDisposition multipart(String name, String value) {
        if (name == null || name.isBlank())
            throw new IllegalArgumentException();

        if (value == null || value.isBlank())
            throw new IllegalArgumentException();

        return new ContentDisposition(false, true, false, null, name, value);
    }
}
