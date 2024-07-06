package io.javalin

import io.javalin.plugin.bundled.CorsUtils
import io.javalin.plugin.bundled.OriginParts
import io.javalin.plugin.bundled.PortResult
import io.javalin.plugin.bundled.WildcardResult
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.assertj.core.api.Assertions.assertThatIllegalArgumentException
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.EmptySource
import org.junit.jupiter.params.provider.MethodSource
import java.net.URISyntaxException
import java.util.stream.Stream

private const val SHAN_ZERO: String = "\u1090" // ႐ MYANMAR SHAN DIGIT ZERO
private const val BOLD_ZERO: String = "\uD835\uDFCE" // 𝟎 MATHEMATICAL BOLD DIGIT ZERO

internal object CorsArguments {
    @JvmStatic
    fun singleSpace(): Stream<String> = Stream.of(" ")
}


class TestCorsUtils {
    @Nested
    inner class IsSchemeValid {

        @ParameterizedTest
        @CsvSource(value = ["http", "https", "hi", "ftp", "sftp", "c007", "a.b.c", "a+b", "a-b"])
        fun `accepts valid schemes`(scheme: String) {
            assertThat(CorsUtils.isSchemeValid(scheme)).describedAs(scheme).isTrue
        }

        @ParameterizedTest
        @EmptySource
        @MethodSource("io.javalin.CorsArguments#singleSpace")
        @CsvSource(value = ["forbidden_underscore", "no-#", "no%", "c-${SHAN_ZERO}", "c-${BOLD_ZERO}"])
        fun `rejects invalid schemes`(scheme: String) {
            assertThat(CorsUtils.isSchemeValid(scheme)).describedAs(scheme).isFalse
        }
    }

    @Nested
    inner class IsValidOrigin {
        @ParameterizedTest
        @CsvSource(value = ["null", "https://example.com", "https://example.com:8443"])
        fun `accepts valid origins`(origin: String) {
            assertThat(CorsUtils.isValidOrigin(origin)).describedAs(origin).isTrue
        }

        @ParameterizedTest
        @EmptySource
        @CsvSource(value = ["://no-scheme", "o_O://illegal-underscore", "https://example.com/", "https://example.com?query=true", "https://example.com:fakeport", "https://example.com:8${SHAN_ZERO}", "https://example.com:8${BOLD_ZERO}", "https://example.com#fragment"])
        fun `rejects invalid origins`(it: String) {
            assertThat(CorsUtils.isValidOrigin(it)).describedAs(it).isFalse
        }

        @ParameterizedTest
        @CsvSource(value = ["null", "https://example.com", "https://example.com:8443"])
        fun `accepts valid origins JDK`(origin: String) {
            assertThat(CorsUtils.isValidOriginJdk(origin)).describedAs(origin).isTrue
        }

        @ParameterizedTest
        @EmptySource
        @CsvSource(value = ["://no-scheme", "o_O://illegal-underscore", "https://example.com/", "https://example.com?query=true", "https://example.com:fakeport", "https://example.com:8${SHAN_ZERO}", "https://example.com:8${BOLD_ZERO}", "https://example.com#fragment"])
        fun `rejects invalid origins JDK`(it: String) {
            assertThat(CorsUtils.isValidOriginJdk(it)).describedAs(it).isFalse
        }
    }

    @Nested
    inner class ExtractPort {
        @ParameterizedTest
        @CsvSource(
            value = ["https://example.com:80,80", "https://example.com:8443,8443"]
        )
        fun `can extract port if specified`(origin: String, port: Int) {
            val portResult = CorsUtils.extractPort(origin) as? PortResult.PortSpecified
            assertThat(portResult).describedAs("cast successful").isNotNull
            assertThat(portResult!!.port).describedAs("port").isEqualTo(port)
            assertThat(portResult.fromSchemeDefault).describedAs("scheme default").isFalse
        }

        @ParameterizedTest
        @EmptySource
        @CsvSource(value = ["example.com"])
        fun `returns errors for invalid origins`(origin: String) {
            assertThat(CorsUtils.extractPort(origin)).isEqualTo(PortResult.ErrorState.InvalidOrigin)
        }

        @ParameterizedTest
        @CsvSource(
            value = ["https://example.com:fakeport", "https://example.com:8${SHAN_ZERO}", "https://example.com:8${BOLD_ZERO}"]
        )
        fun `returns special error for invalid port values`(origin: String) {
            assertThat(CorsUtils.extractPort(origin)).describedAs(origin).isEqualTo(PortResult.ErrorState.InvalidPort)
        }

        @Test
        fun `a valid origin without explicit port returns NoPortSpecified`() {
            assertThat(CorsUtils.extractPort("https://example.com")).isEqualTo(PortResult.NoPortSpecified)
        }

        @Test
        fun `http fallback works`() {
            val portResult = CorsUtils.extractPortOrSchemeDefault("http://example.com") as? PortResult.PortSpecified
            assertThat(portResult).isNotNull
            assertThat(portResult!!.port).isEqualTo(80)
            assertThat(portResult.fromSchemeDefault).isTrue
        }

        @Test
        fun `https fallback works`() {
            val portResult = CorsUtils.extractPortOrSchemeDefault("https://example.com") as? PortResult.PortSpecified
            assertThat(portResult).isNotNull
            assertThat(portResult!!.port).isEqualTo(443)
            assertThat(portResult.fromSchemeDefault).isTrue
        }
    }

    @Nested
    inner class NormalizeOrigin {
        @Test
        fun `adds a port if not specified`() {
            assertThat(CorsUtils.normalizeOrigin("https://example.com")).isEqualTo("https://example.com:443")
            assertThat(CorsUtils.normalizeOrigin("http://example.com")).isEqualTo("http://example.com:80")
        }

        @Test
        fun `does not add a default port if one is already there`() {
            assertThat(CorsUtils.normalizeOrigin("https://example.com:8443")).isEqualTo("https://example.com:8443")
        }

        @Test
        fun `does not touch special values`() {
            assertThat(CorsUtils.normalizeOrigin("*")).isEqualTo("*")
            assertThat(CorsUtils.normalizeOrigin("null")).isEqualTo("null")
        }
    }

    @Nested
    inner class TestOriginParts {
        @Test
        fun `scheme is required`() {
            assertThatIllegalArgumentException().isThrownBy {
                CorsUtils.parseAsOriginParts("example.com")
            }.withMessage("scheme delimiter :// must exist")
        }

        @Test
        fun `specified scheme must follow rfc rules`() {
            assertThatIllegalArgumentException().isThrownBy {
                CorsUtils.parseAsOriginParts("c-${SHAN_ZERO}://example.com")
            }.withMessage("specified scheme is not valid")
        }

        @Test
        fun `works for valid inputs`() {
            val (scheme, host, port) = CorsUtils.parseAsOriginParts("https://example.com:8443")
            assertThat(scheme).isEqualTo("https")
            assertThat(host).isEqualTo("example.com")
            assertThat(port).isEqualTo(8443)
        }

        @Test
        fun `works with default ports`() {
            val (scheme, host, port) = CorsUtils.parseAsOriginParts("https://example.com")
            assertThat(scheme).isEqualTo("https")
            assertThat(host).isEqualTo("example.com")
            assertThat(port).isEqualTo(443)
        }

        @Test
        fun `does not resolve wildcard hosts`() {
            val (scheme, host, port) = CorsUtils.parseAsOriginParts("https://*.example.com:8443")
            assertThat(scheme).isEqualTo("https")
            assertThat(host).isEqualTo("*.example.com")
            assertThat(port).isEqualTo(8443)
        }

        @Test
        fun `ip4 works`() {
            val (scheme, host, port) = CorsUtils.parseAsOriginParts("https://127.0.0.1")
            assertThat(scheme).isEqualTo("https")
            assertThat(host).isEqualTo("127.0.0.1")
            assertThat(port).isEqualTo(443)
        }

        @Test
        @Disabled
        fun `ip6 works`() {
            // will be fixed by switching implementations
            val (scheme, host, port) = CorsUtils.parseAsOriginParts("https://[::1]")
            assertThat(scheme).isEqualTo("https")
            assertThat(host).isEqualTo("[::1]")
            assertThat(port).isEqualTo(443)
        }
    }

    @Nested
    inner class TestOriginPartsJdk {
        @Test
        fun `scheme is required`() {
            assertThatIllegalArgumentException().isThrownBy {
                CorsUtils.parseAsOriginPartsJdk("example.com")
            }.withMessage("Scheme is required!")
        }

        @Test
        fun `specified scheme must follow rfc rules`() {
            assertThatExceptionOfType(URISyntaxException::class.java).isThrownBy {
                CorsUtils.parseAsOriginPartsJdk("c-${SHAN_ZERO}://example.com")
            }
        }

        @Test
        fun `works for valid inputs`() {
            val (scheme, host, port) = CorsUtils.parseAsOriginPartsJdk("https://example.com:8443")
            assertThat(scheme).isEqualTo("https")
            assertThat(host).isEqualTo("example.com")
            assertThat(port).isEqualTo(8443)
        }

        @Test
        fun `works with default ports`() {
            val (scheme, host, port) = CorsUtils.parseAsOriginPartsJdk("https://example.com")
            assertThat(scheme).isEqualTo("https")
            assertThat(host).isEqualTo("example.com")
            assertThat(port).isEqualTo(443)
        }

        @Test
        fun `does not resolve wildcard hosts`() {
            val (scheme, host, port) = CorsUtils.parseAsOriginPartsJdk("https://*.example.com:8443")
            assertThat(scheme).isEqualTo("https")
            assertThat(host).isEqualTo("*.example.com")
            assertThat(port).isEqualTo(8443)
        }

        @Test
        fun `ip4 works`() {
            val (scheme, host, port) = CorsUtils.parseAsOriginPartsJdk("https://127.0.0.1")
            assertThat(scheme).isEqualTo("https")
            assertThat(host).isEqualTo("127.0.0.1")
            assertThat(port).isEqualTo(443)
        }

        @Test
        fun `ip6 works`() {
            val (scheme, host, port) = CorsUtils.parseAsOriginPartsJdk("https://[::1]")
            assertThat(scheme).isEqualTo("https")
            assertThat(host).isEqualTo("[::1]")
            assertThat(port).isEqualTo(443)
        }

        @Test
        fun `ip6 without brackets is rejected`() {
            assertThatExceptionOfType(URISyntaxException::class.java).isThrownBy { CorsUtils.parseAsOriginPartsJdk("https://::1") }
        }
    }

    @Nested
    inner class OriginsMatch {
        @Test
        fun `does not match for mismatching schemes`() {
            val client = OriginParts("http", "example.com", 80)
            val server = OriginParts("https", "example.com", 80)
            assertThat(CorsUtils.originsMatch(client, server)).isFalse
        }

        @Test
        fun `does not match for mismatching ports`() {
            val client = OriginParts("https", "example.com", 80)
            val server = OriginParts("https", "example.com", 443)
            assertThat(CorsUtils.originsMatch(client, server)).isFalse
        }

        @Test
        fun `does match for equal values`() {
            val client = OriginParts("https", "example.com", 443)
            val server = OriginParts("https", "example.com", 443)
            assertThat(CorsUtils.originsMatch(client, server)).isTrue
        }

        @Test
        fun `does match for wildcard on server-side`() {
            val client = OriginParts("https", "sub.example.com", 443)
            val server = OriginParts("https", "*.example.com", 443)
            assertThat(CorsUtils.originsMatch(client, server)).isTrue
        }

        @Test
        fun `same port and scheme but different host does not match`() {
            val client = OriginParts("https", "foo.example.com", 443)
            val server = OriginParts("https", "bar.example.com", 443)
            assertThat(CorsUtils.originsMatch(client, server)).isFalse
        }

        @Test
        fun `dotless client host part does not crash`() {
            val client = OriginParts("https", "dotless", 443)
            val server = OriginParts("https", "*.example.com", 443)
            assertThat(CorsUtils.originsMatch(client, server)).isFalse
        }
    }

    @Nested
    inner class AddSchemeIfMissing {
        @ParameterizedTest
        @CsvSource(
            value = ["*,*", "null,null", "example.com,https://example.com", "example.com:8080,https://example.com:8080", "EXAMPLE.COM,https://example.com", "HTTPS://EXAMPLE.COM/,https://example.com"]
        )
        fun works(input: String, expected: String) {
            assertThat(CorsUtils.addSchemeIfMissing(input, "https")).describedAs(input).isEqualTo(expected)
        }
    }

    @Nested
    inner class WildcardRequirements {
        @Test
        fun `no wildcard origins are okay`() {
            assertThat(CorsUtils.originFulfillsWildcardRequirements("https://example.com")).isEqualTo(WildcardResult.NoWildcardDetected)
        }

        @Test
        fun `wildcards at the start of the host are accepted`() {
            assertThat(CorsUtils.originFulfillsWildcardRequirements("https://*.example.com")).isEqualTo(WildcardResult.WildcardOkay)
        }

        @Test
        fun `at most one wildcard is allowed`() {
            assertThat(CorsUtils.originFulfillsWildcardRequirements("https://*.look.*.multiple.wildcards.com")).isEqualTo(WildcardResult.ErrorState.TooManyWildcards)
        }

        @Test
        fun `wildcards in the middle are not accepted`() {
            assertThat(CorsUtils.originFulfillsWildcardRequirements("https://subsub.*.example.com")).isEqualTo(WildcardResult.ErrorState.WildcardNotAtTheStartOfTheHost)
        }
    }
}
