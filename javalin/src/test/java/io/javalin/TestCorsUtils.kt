package io.javalin

import io.javalin.plugin.bundled.CorsUtils
import io.javalin.plugin.bundled.OriginParts
import io.javalin.plugin.bundled.WildcardResult
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.assertj.core.api.Assertions.assertThatIllegalArgumentException
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

// used in a @MethodSource argument
@Suppress("unused")
internal object CorsArguments {
    @JvmStatic
    fun singleSpace(): Stream<String> = Stream.of(" ")
}


class TestCorsUtils {
    @Nested
    inner class IsValidOrigin {
        @ParameterizedTest
        @CsvSource(value = ["null", "https://example.com", "https://example.com:8443", "https://*.example.com"])
        fun `accepts valid origins`(origin: String) {
            assertThat(CorsUtils.isValidOrigin(origin)).describedAs(origin).isTrue
        }

        @ParameterizedTest
        @CsvSource(value = ["https://*.example.com"])
        fun `rejects wild cards in client mode`(origin: String) {
            assertThat(CorsUtils.isValidOrigin(origin, client = true)).describedAs(origin).isFalse
        }

        @ParameterizedTest
        @EmptySource
        @CsvSource(value = ["://no-scheme", "o_O://illegal-underscore", "https://example.com/", "https://example.com?query=true", "https://example.com:fakeport", "https://example.com:8${SHAN_ZERO}", "https://example.com:8${BOLD_ZERO}", "https://example.com#fragment", "https://user:pw@example.com"])
        fun `rejects invalid origins`(it: String) {
            assertThat(CorsUtils.isValidOrigin(it)).describedAs(it).isFalse
        }
    }

    @Nested
    inner class TestOriginParts {

        @ParameterizedTest
        @CsvSource(value = ["example.com", "no-#://example.com"])
        fun `scheme is required`() {
            assertThatIllegalArgumentException().isThrownBy {
                CorsUtils.parseAsOriginParts("example.com")
            }.withMessage("Scheme is required!")
        }

        @ParameterizedTest
        @EmptySource
        @MethodSource("io.javalin.CorsArguments#singleSpace")
        @CsvSource(value = ["forbidden_underscore", "no%", "c-${SHAN_ZERO}", "c-${BOLD_ZERO}"])
        fun `specified scheme must follow rfc rules`(scheme: String) {
            assertThatExceptionOfType(URISyntaxException::class.java).isThrownBy {
                CorsUtils.parseAsOriginParts("$scheme://example.com")
            }
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

            val (scheme2, host2, port2) = CorsUtils.parseAsOriginParts("http://example.com")
            assertThat(scheme2).isEqualTo("http")
            assertThat(host2).isEqualTo("example.com")
            assertThat(port2).isEqualTo(80)
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
        fun `ip6 works`() {
            val (scheme, host, port) = CorsUtils.parseAsOriginParts("https://[::1]")
            assertThat(scheme).isEqualTo("https")
            assertThat(host).isEqualTo("[::1]")
            assertThat(port).isEqualTo(443)
        }

        @Test
        fun `ip6 without brackets is rejected`() {
            assertThatExceptionOfType(URISyntaxException::class.java).isThrownBy { CorsUtils.parseAsOriginParts("https://::1") }
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
