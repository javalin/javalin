package io.javalin.http

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class TestContentDisposition {

    private fun attachment(name: String) = ContentDisposition.attachment(name).stringifyValue()
    private fun attachmentRfc5987(name: String) = ContentDisposition.attachmentRfc5987(name).stringifyValue()

    @Test
    fun `inline and attachment without filename`() {
        assertThat(ContentDisposition.inline().stringifyValue()).isEqualTo("inline")
        assertThat(ContentDisposition.attachment().stringifyValue()).isEqualTo("attachment")
    }

    @Test
    fun `attachment with plain filename`() {
        assertThat(attachment("report.pdf")).isEqualTo("attachment; filename=\"report.pdf\"")
        assertThat(attachmentRfc5987("report.pdf")).isEqualTo("attachment; filename=\"report.pdf\"; filename*=UTF-8''report.pdf")
    }

    @Test
    fun `non-ascii filename is transliterated in filename and percent-encoded in filename*`() {
        assertThat(attachmentRfc5987("résumé €.pdf"))
            .isEqualTo("attachment; filename=\"resume _.pdf\"; filename*=UTF-8''r%C3%A9sum%C3%A9%20%E2%82%AC.pdf")
        assertThat(attachmentRfc5987("日本語.txt")).endsWith("filename*=UTF-8''%E6%97%A5%E6%9C%AC%E8%AA%9E.txt")
    }

    @Test
    fun `quotes and line breaks cannot escape the header value`() {
        val value = attachmentRfc5987("evil\"\r\nSet-Cookie: a=b.txt")
        assertThat(value).doesNotContain("\r", "\n")
        assertThat(value).isEqualTo("attachment; filename=\"evil_Set-Cookie_ a_b.txt\"; filename*=UTF-8''evil%22%0D%0ASet-Cookie%3A%20a%3Db.txt")
    }

    @Test
    fun `path separators are removed`() {
        assertThat(attachment("../../etc/passwd")).isEqualTo("attachment; filename=\"_.._etc_passwd\"")
        assertThat(attachment("C:\\Windows\\win.ini")).isEqualTo("attachment; filename=\"C_Windows_win.ini\"")
    }

    @Test
    fun `windows reserved names are prefixed`() {
        assertThat(attachment("CON")).isEqualTo("attachment; filename=\"_CON\"")
        assertThat(attachment("nul.txt")).isEqualTo("attachment; filename=\"_nul.txt\"")
        assertThat(attachment("LPT1.log")).isEqualTo("attachment; filename=\"_LPT1.log\"")
        assertThat(attachment("console.txt")).isEqualTo("attachment; filename=\"console.txt\"")
    }

    @Test
    fun `filename that sanitizes to nothing falls back to download`() {
        assertThat(attachment("...")).isEqualTo("attachment; filename=\"download\"")
        assertThat(attachment(" . ")).isEqualTo("attachment; filename=\"download\"")
    }

    @Test
    fun `blank filename is rejected`() {
        assertThatThrownBy { ContentDisposition.attachment("  ") }.isInstanceOf(IllegalArgumentException::class.java)
        assertThatThrownBy { ContentDisposition.attachmentRfc5987("") }.isInstanceOf(IllegalArgumentException::class.java)
    }
}
