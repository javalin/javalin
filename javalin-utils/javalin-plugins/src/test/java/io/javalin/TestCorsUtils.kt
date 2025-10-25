package io.javalin

import io.javalin.plugin.CorsUtils
import io.javalin.plugin.OriginParts
import io.javalin.plugin.PortResult
import io.javalin.plugin.WildcardResult
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalArgumentException
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

private const val SHAN_ZERO: String = "\u1090" // ႐ MYANMAR SHAN DIGIT ZERO
private const val BOLD_ZERO: String = "\uD835\uDFCE" // 𝟎 MATHEMATICAL BOLD DIGIT ZERO


class TestCorsUtils {
    @Nested
    inner class IsSchemeValid {
        @Test
        fun `accepts valid schemes`() {
            listOf("http", "https", "hi", "ftp", "sftp", "c007", "a.b.c", "a+b", "a-b").forEach {
                assertThat(CorsUtils.isSchemeValid(it)).describedAs(it).isTrue
            }
        }

        @Test
        fun `rejects invalid schemes`() {
            listOf("", " ", "forbidden_underscore", "no-#", "no%", "c-${SHAN_ZERO}", "c-${BOLD_ZERO}").forEach {
                assertThat(CorsUtils.isSchemeValid(it)).describedAs(it).isFalse
            }
        }
    }

    @Nested
    inner class IsValidOrigin {
        @Test
        fun `accepts valid origins`() {
            listOf("null", "https://example.com", "https://example.com:8443").forEach {
                assertThat(CorsUtils.isValidOrigin(it)).describedAs(it).isTrue
            }
        }

        @Test
        fun `rejects invalid origins`() {
            listOf(
                "",
                "https://example.com/",
                "https://example.com?query=true",
                "https://example.com:fakeport",
                "https://example.com:8${SHAN_ZERO}",
                "https://example.com:8${BOLD_ZERO}"
            ).forEach {
                assertThat(CorsUtils.isValidOrigin(it)).describedAs(it).isFalse
            }
        }
    }

    @Nested
    inner class ExtractPort {
        @Test
        fun `can extract port if specified`() {
            listOf(
                "https://example.com:80" to 80,
                "https://example.com:8443" to 8443
            ).forEach { (origin, port) ->
                val portResult = CorsUtils.extractPort(origin) as? PortResult.PortSpecified
                assertThat(portResult).describedAs("cast successful").isNotNull
                assertThat(portResult!!.port).describedAs("port").isEqualTo(port)
                assertThat(portResult.fromSchemeDefault).describedAs("scheme default").isFalse
            }
        }

        @Test
        fun `returns errors for invalid origins`() {
            listOf(
                "",
                "example.com"
            ).forEach {
                assertThat(CorsUtils.extractPort(it)).isEqualTo(PortResult.ErrorState.InvalidOrigin)
            }
        }

        @Test
        fun `returns special error for invalid port values`() {
            listOf(
                "https://example.com:fakeport",
                "https://example.com:8${SHAN_ZERO}",
                "https://example.com:8${BOLD_ZERO}"
            ).forEach {
                assertThat(CorsUtils.extractPort(it)).describedAs(it).isEqualTo(PortResult.ErrorState.InvalidPort)
            }
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
        fun `explicit port is required`() {
            assertThatIllegalArgumentException().isThrownBy {
                CorsUtils.parseAsOriginParts("https://example.com")
            }.withMessage("explicit port is required")
        }

        @Test
        fun `works for valid inputs`() {
            val (scheme, host, port) = CorsUtils.parseAsOriginParts("https://example.com:8443")
            assertThat(scheme).isEqualTo("https")
            assertThat(host).isEqualTo("example.com")
            assertThat(port).isEqualTo(8443)
        }

        @Test
        fun `does not resolve wildcard hosts`() {
            val (scheme, host, port) = CorsUtils.parseAsOriginParts("https://*.example.com:8443")
            assertThat(scheme).isEqualTo("https")
            assertThat(host).isEqualTo("*.example.com")
            assertThat(port).isEqualTo(8443)
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
    }

    @Nested
    inner class AddSchemeIfMissing {
        @Test
        fun works() {
            listOf(
                "*" to "*",
                "null" to "null",
                "example.com" to "https://example.com",
                "example.com:8080" to "https://example.com:8080",
                "EXAMPLE.COM" to "https://example.com",
                "HTTPS://EXAMPLE.COM/" to "https://example.com"
            ).forEach { (input, expected) ->
                assertThat(CorsUtils.addSchemeIfMissing(input, "https")).describedAs(input).isEqualTo(expected)
            }
        }
    }

    @Nested
    inner class WildcardRequirements {
        @Test
        fun `no wildcard origins are okay`() {
            assertThat(CorsUtils.originFulfillsWildcardRequirements("https://example.com"))
                .isEqualTo(WildcardResult.NoWildcardDetected)
        }

        @Test
        fun `wildcards at the start of the host are accepted`() {
            assertThat(CorsUtils.originFulfillsWildcardRequirements("https://*.example.com"))
                .isEqualTo(WildcardResult.WildcardOkay)
        }

        @Test
        fun `at most one wildcard is allowed`() {
            assertThat(CorsUtils.originFulfillsWildcardRequirements("https://*.look.*.multiple.wildcards.com"))
                .isEqualTo(WildcardResult.ErrorState.TooManyWildcards)
        }

        @Test
        fun `wildcards in the middle are not accepted`() {
            assertThat(CorsUtils.originFulfillsWildcardRequirements("https://subsub.*.example.com"))
                .isEqualTo(WildcardResult.ErrorState.WildcardNotAtTheStartOfTheHost)
        }
    }
}
