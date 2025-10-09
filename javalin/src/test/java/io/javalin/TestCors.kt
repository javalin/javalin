/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin

import io.javalin.http.Header.ACCESS_CONTROL_ALLOW_CREDENTIALS
import io.javalin.http.Header.ACCESS_CONTROL_ALLOW_HEADERS
import io.javalin.http.Header.ACCESS_CONTROL_ALLOW_METHODS
import io.javalin.http.Header.ACCESS_CONTROL_ALLOW_ORIGIN
import io.javalin.http.Header.ACCESS_CONTROL_EXPOSE_HEADERS
import io.javalin.http.Header.ACCESS_CONTROL_MAX_AGE
import io.javalin.http.Header.ACCESS_CONTROL_REQUEST_HEADERS
import io.javalin.http.Header.ACCESS_CONTROL_REQUEST_METHOD
import io.javalin.http.Header.ORIGIN
import io.javalin.http.Header.REFERER
import io.javalin.http.HttpStatus.UNAUTHORIZED
import io.javalin.plugin.bundled.CorsPlugin
import io.javalin.testing.HttpUtil
import io.javalin.testing.TestUtil
import kong.unirest.core.HttpResponse
import kong.unirest.core.HttpStatus
import kong.unirest.core.Unirest
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class TestCors {

    @Nested
    inner class ExceptionTests {
        @Test
        fun `throws for zero configurations`() {
            assertThatExceptionOfType(IllegalArgumentException::class.java)
                .isThrownBy { Javalin.create { it.registerPlugin(CorsPlugin()) } }
                .withMessageStartingWith("At least one cors config has to be provided. Use CorsPluginConfig.addRule() to add one.")
        }

        @Test
        fun `throws for empty origins if reflectClientOrigin is false`() {
            assertThatExceptionOfType(IllegalArgumentException::class.java)
                .isThrownBy { Javalin.create { it.registerPlugin(CorsPlugin({ cors -> cors.addRule {} })) } }
                .withMessageStartingWith("Origins cannot be empty if `reflectClientOrigin` is false.")
        }

        @Test
        fun `throws for non-empty if reflectClientOrigin is true`() {
            assertThatExceptionOfType(IllegalArgumentException::class.java).isThrownBy {
                Javalin.create { config ->
                    config.registerPlugin(CorsPlugin { cors ->
                        cors.addRule {
                            it.reflectClientOrigin = true
                            it.allowHost("A", "B")
                        }
                    })
                }
            }.withMessageStartingWith("Cannot set `allowedOrigins` if `reflectClientOrigin` is true")
        }

        @Test
        fun `throws for combination of anyHost and allowCredentials true`() {
            assertThatExceptionOfType(IllegalArgumentException::class.java).isThrownBy {
                Javalin.create { config ->
                    config.registerPlugin(CorsPlugin({ cors ->
                        cors.addRule {
                            it.anyHost()
                            it.allowCredentials = true
                        }
                    }))
                }
            }.withMessageStartingWith("Cannot use `anyHost()` / Origin: * if `allowCredentials` is true as that is rejected by all browsers.")
        }

        @Test
        fun `passing in the null origin as an allowed host does not work`() {
            assertThatExceptionOfType(IllegalArgumentException::class.java).isThrownBy {
                Javalin.create { config ->
                    config.registerPlugin(CorsPlugin { cors ->
                        cors.addRule { it.allowHost("null") }
                    })
                }
            }
                .withMessageStartingWith("Adding the string null as an allowed host is forbidden. Consider calling anyHost() instead")
        }

        @Test
        fun `exception for untransformable hosts exists`() {
            assertThatExceptionOfType(IllegalArgumentException::class.java).isThrownBy {
                Javalin.create { config ->
                    config.registerPlugin(CorsPlugin { cors ->
                        cors.addRule { it.allowHost("example.com?query=true") }
                    })
                }
            }
                .withMessageStartingWith("The given value 'example.com?query=true' could not be transformed into a valid origin")
        }

        @Test
        fun `multiple wildcards lead to an exception`() {
            assertThatExceptionOfType(IllegalArgumentException::class.java).isThrownBy {
                Javalin.create { config ->
                    config.registerPlugin(CorsPlugin { cors ->
                        cors.addRule { it.allowHost("*.*.example.com") }
                    })
                }
            }
                .withMessageStartingWith("Too many wildcards detected inside '*.*.example.com'. Only one at the start of the host is allowed!")
        }

        @Test
        fun `wildcard in the middle leads to an exception`() {
            assertThatExceptionOfType(IllegalArgumentException::class.java).isThrownBy {
                Javalin.create { config ->
                    config.registerPlugin(CorsPlugin { cors ->
                        cors.addRule { it.allowHost("subsub.*.example.com") }
                    })
                }
            }.withMessageStartingWith("The wildcard must be at the start of the passed in host. The value 'subsub.*.example.com' violates this requirement!")
        }
    }

    @Nested
    inner class HappyPath {
        @Test
        fun `can enable cors for specific origins`() = TestUtil.test(Javalin.create {
            it.registerPlugin(CorsPlugin { cors ->
                cors.addRule { it.allowHost("https://origin-1", "https://referer-1") }
            })
        }) { app, http ->
            app.get("/") { it.result("Hello") }
            assertThat(http.get("/").header(ACCESS_CONTROL_ALLOW_ORIGIN)).isEmpty()
            assertThat(
                http.get("/", mapOf(ORIGIN to "https://origin-1")).header(ACCESS_CONTROL_ALLOW_ORIGIN)
            ).isEqualTo("https://origin-1")
            assertThat(
                http.get("/", mapOf(ORIGIN to "https://referer-1")).header(ACCESS_CONTROL_ALLOW_ORIGIN)
            ).isEqualTo("https://referer-1")
            // referer gets ignored
            assertThat(
                http.get("/", mapOf(REFERER to "https://referer-1")).header(ACCESS_CONTROL_ALLOW_ORIGIN)
            ).isEqualTo("")
        }

        @Test
        fun `can enable cors for star origins`() = TestUtil.test(Javalin.create {
            it.registerPlugin(CorsPlugin { cors ->
                cors.addRule { it.anyHost() }
            })
        }) { app, http ->
            app.get("/") { it.result("Hello") }
            assertThat(http.get("/", mapOf(ORIGIN to "https://A")).header(ACCESS_CONTROL_ALLOW_ORIGIN)).isEqualTo("*")
            // referer gets ignored
            assertThat(http.get("/", mapOf(REFERER to "https://B")).header(ACCESS_CONTROL_ALLOW_ORIGIN)).isEqualTo("")
        }

        @Test
        fun `has allowsCredentials false by default`() = TestUtil.test(Javalin.create {
            it.registerPlugin(CorsPlugin { cors ->
                cors.addRule { it.reflectClientOrigin = true }
            })
        }) { app, http ->
            app.get("/") { it.result("Hello") }
            assertThat(http.get("/").header(ACCESS_CONTROL_ALLOW_ORIGIN)).isEmpty()
            assertThat(
                http.get("/", mapOf(ORIGIN to "https://some-origin")).header(ACCESS_CONTROL_ALLOW_ORIGIN)
            ).isEqualTo(
                "https://some-origin"
            )
            assertThat(
                http.get("/", mapOf(ORIGIN to "https://some-origin")).header(ACCESS_CONTROL_ALLOW_CREDENTIALS)
            ).isEmpty() // cookies not allowed
            // referer gets ignored
            assertThat(
                http.get("/", mapOf(REFERER to "https://some-referer")).header(ACCESS_CONTROL_ALLOW_ORIGIN)
            ).isEqualTo("")
        }

        @Test
        fun `can have allowsCredentials set true`() = TestUtil.test(Javalin.create { cfg ->
            cfg.registerPlugin(CorsPlugin { cors ->
                cors.addRule {
                    it.reflectClientOrigin = true
                    it.allowCredentials = true
                }
            })
        }) { app, http ->
            app.get("/") { it.result("Hello") }
            assertThat(http.get("/").header(ACCESS_CONTROL_ALLOW_ORIGIN)).isEmpty()
            assertThat(
                http.get("/", mapOf(ORIGIN to "https://some-origin")).header(ACCESS_CONTROL_ALLOW_ORIGIN)
            ).isEqualTo(
                "https://some-origin"
            )
            assertThat(
                http.get("/", mapOf(ORIGIN to "https://some-origin")).header(ACCESS_CONTROL_ALLOW_CREDENTIALS)
            ).isEqualTo("true") // cookies allowed
            // referer gets ignored
            assertThat(
                http.get("/", mapOf(REFERER to "https://some-referer")).header(ACCESS_CONTROL_ALLOW_ORIGIN)
            ).isEqualTo("")
        }

        @Test
        fun `works for 404s`() =
            TestUtil.test(Javalin.create {
                it.registerPlugin(CorsPlugin { cors ->
                    cors.addRule { it.reflectClientOrigin = true }
                })
            }) { _, http ->
                val optionsResponse = Unirest.options(http.origin + "/not-found")
                    .headers(mapOf(ORIGIN to "https://some-origin"))
                    .asString()
                assertThat(optionsResponse.status)
                    .describedAs("options status code")
                    .isEqualTo(HttpStatus.OK)
                assertThat(optionsResponse.header(ACCESS_CONTROL_ALLOW_ORIGIN)).isEqualTo("https://some-origin")

                val getResponse = http.get("/not-found", mapOf(ORIGIN to "https://some-origin"))
                assertThat(getResponse.status).describedAs("get status code").isEqualTo(HttpStatus.NOT_FOUND)
                assertThat(getResponse.header(ACCESS_CONTROL_ALLOW_ORIGIN)).isEqualTo("https://some-origin")
            }

        @Test
        fun `works with options endpoint mapping`() = TestUtil.test(Javalin.create {
            it.registerPlugin(CorsPlugin { cors ->
                cors.addRule { it.reflectClientOrigin = true }
            })
        }) { app, http ->
            app.options("/") { it.result("Hello") }
            val response = Unirest.options(http.origin)
                .header(ORIGIN, "https://example.com")
                .header(ACCESS_CONTROL_REQUEST_HEADERS, "123")
                .header(ACCESS_CONTROL_REQUEST_METHOD, "TEST")
                .asString()
            assertThat(response.header(ACCESS_CONTROL_ALLOW_HEADERS)).isEqualTo("123")
            assertThat(response.header(ACCESS_CONTROL_ALLOW_METHODS)).isEqualTo("TEST")
            assertThat(response.body).isEqualTo("Hello")
        }
    }

    @Nested
    inner class NegativeTests {
        @Test
        fun `headers are not set when origin doesn't match`() = TestUtil.test(Javalin.create { cfg ->
            cfg.registerPlugin(CorsPlugin { cors ->
                cors.addRule { it.allowHost("https://origin-1.com") }
            })
        }) { app, http ->
            app.get("/") { it.result("Hello") }
            assertThat(
                http.get("/", mapOf(ORIGIN to "https://origin-2.com")).header(ACCESS_CONTROL_ALLOW_ORIGIN)
            ).isEmpty()
            assertThat(
                http.get("/", mapOf(ORIGIN to "https://origin-1.com.au")).header(ACCESS_CONTROL_ALLOW_ORIGIN)
            ).isEmpty()
        }

        @Test
        fun `same hostname with different ports is detected as different origins`() =
            TestUtil.test(Javalin.create { cfg ->
                cfg.registerPlugin(CorsPlugin { cors ->
                    cors.addRule {
                        it.allowHost("https://example.com:8443")
                    }
                })
            }) { app, http ->
                app.get("/") { it.result("Hello") }
                val response = Unirest.get(http.origin)
                    .header(ORIGIN, "https://example.com")
                    .asString()
                assertThat(response.header(ACCESS_CONTROL_ALLOW_ORIGIN)).isEmpty()
            }
    }

    @Nested
    inner class ExposingHeaders {
        @Test
        fun `allows exposing a single header`() = TestUtil.test(Javalin.create { cfg ->
            cfg.registerPlugin(CorsPlugin { cors ->
                cors.addRule {
                    it.reflectClientOrigin = true
                    it.exposeHeader("x-test")
                }
            })
        }) { app, http ->
            app.get("/") { it.result("Hello") }
            val response = Unirest.get(http.origin)
                .header(ORIGIN, "https://example.com")
                .asString()
            assertThat(response.header(ACCESS_CONTROL_EXPOSE_HEADERS)).isEqualTo("x-test")
            assertThat(response.body).isEqualTo("Hello")
        }

        @Test
        fun `allows exposing multiple headers`() = TestUtil.test(Javalin.create { cfg ->
            cfg.registerPlugin(CorsPlugin { cors ->
                cors.addRule {
                    it.reflectClientOrigin = true
                    it.exposeHeader("x-test")
                    it.exposeHeader("x-world")
                }
            })
        }) { app, http ->
            app.get("/") { it.result("Hello") }
            val response = Unirest.get(http.origin)
                .header(ORIGIN, "https://example.com")
                .asString()
            assertThat(response.header(ACCESS_CONTROL_EXPOSE_HEADERS)).isEqualTo("x-test,x-world")
            assertThat(response.body).isEqualTo("Hello")
        }
    }

    @Nested
    inner class ConvenienceFeatures {
        @Test
        fun `default scheme can be overridden`() = TestUtil.test(Javalin.create { cfg ->
            cfg.registerPlugin(CorsPlugin { cors ->
                cors.addRule {
                    it.defaultScheme = "http"
                    it.allowHost("example.com")
                }
            })
        }) { app, http ->
            app.get("/") { it.result("Hello") }
            val response = Unirest.get(http.origin)
                .header(ORIGIN, "http://example.com")
                .asString()
            assertThat(response.header(ACCESS_CONTROL_ALLOW_ORIGIN)).isEqualTo("http://example.com")
            assertThat(response.body).isEqualTo("Hello")
        }

        @Test
        fun `wildcard subdomain work`() = TestUtil.test(Javalin.create { cfg ->
            cfg.registerPlugin(CorsPlugin { cors ->
                cors.addRule {
                    it.allowHost("*.example.com")
                }
            })
        }) { app, http ->
            app.get("/") { it.result("Hello") }
            val response = Unirest.get(http.origin)
                .header(ORIGIN, "https://sub.example.com")
                .asString()
            assertThat(response.header(ACCESS_CONTROL_ALLOW_ORIGIN)).isEqualTo("https://sub.example.com")
            assertThat(response.body).isEqualTo("Hello")
        }

        @Test
        fun `default port detection works`() = TestUtil.test(Javalin.create { cfg ->
            cfg.registerPlugin(CorsPlugin { cors ->
                cors.addRule {
                    it.allowHost("https://example.com:443")
                }
            })
        }) { app, http ->
            app.get("/") { it.result("Hello") }
            val response = Unirest.get(http.origin)
                .header(ORIGIN, "https://example.com")
                .asString()
            assertThat(response.header(ACCESS_CONTROL_ALLOW_ORIGIN)).isEqualTo("https://example.com")
            assertThat(response.body).isEqualTo("Hello")
        }

        @Test
        fun `capitalization does not matter`() = TestUtil.test(Javalin.create { cfg ->
            cfg.registerPlugin(CorsPlugin { cors ->
                cors.addRule {
                    it.allowHost("HTTPS://EXAMPLE.COM")
                }
            })
        }) { app, http ->
            app.get("/") { it.result("Hello") }
            val response = Unirest.get(http.origin)
                .header(ORIGIN, "https://example.com")
                .asString()
            assertThat(response.header(ACCESS_CONTROL_ALLOW_ORIGIN)).isEqualTo("https://example.com")
            assertThat(response.body).isEqualTo("Hello")
        }

        @Test
        fun `maxAge is present in preflight`() = TestUtil.test(Javalin.create() { cfg ->
            cfg.registerPlugin(CorsPlugin { cors ->
                cors.addRule {
                    it.anyHost()
                    it.maxAge = 100
                }
            })
        }) { app, http ->
            app.get("/") { it.result("Hello") }
            val optionsResponse = Unirest.options(http.origin)
                .headers(mapOf(ACCESS_CONTROL_REQUEST_METHOD to "GET", ACCESS_CONTROL_REQUEST_HEADERS to "origin", ORIGIN to "https://example.com"))
                .asString()
            assertThat(optionsResponse.header(ACCESS_CONTROL_MAX_AGE)).isEqualTo("100")
        }

        @Test
        fun `maxAge is absent outside of preflight`() = TestUtil.test(Javalin.create() { cfg ->
            cfg.registerPlugin(CorsPlugin { cors ->
                cors.addRule {
                    it.anyHost()
                    it.maxAge = 100
                }
            })
        }) { app, http ->
            app.get("/") { it.result("Hello") }
            val response = Unirest.get(http.origin)
                .header(ORIGIN, "https://example.com")
                .asString()
            assertThat(response.header(ACCESS_CONTROL_MAX_AGE)).isEmpty()
        }

    }

    @Nested
    inner class EdgeCases {

        @Test
        fun `cors plugin works with prefer405over404`() = TestUtil.test(Javalin.create { cfg ->
            cfg.http.prefer405over404 = true
            cfg.registerPlugin(CorsPlugin { cors ->
                cors.addRule {
                    it.allowHost("example.com")
                }
            })
        }) { app, http ->
            app.post("/") { it.result("Hello") }
            val optionsResponse = Unirest.options(http.origin)
                .headers(mapOf(ORIGIN to "https://example.com"))
                .asString()
            assertThat(optionsResponse.status)
                .describedAs("options status code")
                .isEqualTo(HttpStatus.OK)
            assertThat(optionsResponse.header(ACCESS_CONTROL_ALLOW_ORIGIN)).isEqualTo("https://example.com")

            val getResponse = http.get("/", mapOf(ORIGIN to "https://example.com"))
            assertThat(getResponse.status)
                .describedAs("get status code")
                .isEqualTo(HttpStatus.METHOD_NOT_ALLOWED)
            assertThat(getResponse.header(ACCESS_CONTROL_ALLOW_ORIGIN)).isEqualTo("https://example.com")
        }

        @Test
        fun `GH-2104 client sends dotless origin - wildcard configured`() = TestUtil.test(Javalin.create { cfg ->
            cfg.bundledPlugins.enableCors { cors ->
                cors.addRule {
                    it.allowHost("*.example.com")
                }
            }
        }) { app, http ->
            app.get("/") { it.result("Hello") }

            val response = http.get("/", mapOf(ORIGIN to "https://single"))
            assertThat(response.status)
                .describedAs("get status code")
                .isEqualTo(HttpStatus.BAD_REQUEST)
            assertThat(response.header(ACCESS_CONTROL_ALLOW_ORIGIN)).isEmpty()
        }
    }

    @Nested
    inner class ComplexSetup {
        @Test
        fun works() = TestUtil.test(Javalin.create { cfg ->
            cfg.registerPlugin(CorsPlugin { cors ->
                cors
                    .addRule {
                        it.path = "images*"
                        it.allowHost("https://images.local")
                    }
                    .addRule {
                        it.path = "videos*"
                        it.allowHost("https://videos.local")
                    }
                    .addRule {
                        it.path = "music*"
                        it.allowHost("https://music.local")
                    }
            })
        }) { app, http ->
            app.get("/") { it.result("Hello") }
            app.get("/images/{id}") { it.result(it.pathParam("id")) }
            app.get("/videos/{id}") { it.result(it.pathParam("id")) }
            app.get("/music/{id}") { it.result(it.pathParam("id")) }
            val response = Unirest.get(http.origin)
                .header(ORIGIN, "https://example.com")
                .asString()
            assertThat(response.header(ACCESS_CONTROL_ALLOW_ORIGIN)).isEmpty()
            assertThat(response.body).isEqualTo("Hello")

            checkHappyPath(http, "images")
            checkHappyPath(http, "videos")
            checkHappyPath(http, "music")

            checkUnhappyPath(http, "images")
            checkUnhappyPath(http, "videos")
            checkUnhappyPath(http, "music")
        }

        private fun checkHappyPath(http: HttpUtil, input: String) {
            val response = Unirest.get("${http.origin}/$input/media-id")
                .header(ORIGIN, "https://$input.local")
                .asString()

            assertThat(response.header(ACCESS_CONTROL_ALLOW_ORIGIN)).isEqualTo("https://$input.local")
            assertThat(response.body).isEqualTo("media-id")
        }

        private fun checkUnhappyPath(http: HttpUtil, input: String) {
            val response = Unirest.get("${http.origin}/$input/media-id")
                .header(ORIGIN, "https://example.local")
                .asString()

            assertThat(response.header(ACCESS_CONTROL_ALLOW_ORIGIN)).isEmpty()
        }
    }

    private fun HttpResponse<String>.header(name: String): String = this.headers.getFirst(name)

}
