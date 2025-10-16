package io.javalin.http

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import java.util.*

class HttpResponseExceptionTest {

    @Test
    fun `all HttpStatus values should be covered by an Exception class`() {
        for (httpStatus in HttpStatus.entries) {
            if (httpStatus == HttpStatus.UNKNOWN) {
                continue
            }
            val convertedEnumName = httpStatus.name.splitToSequence("_").map { word ->
                word.lowercase().replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase(
                        Locale.getDefault()
                    ) else it.toString()
                }
            }.joinToString("")
            val expectedClassName = "io.javalin.http.${convertedEnumName}Response"
            assertDoesNotThrow("$expectedClassName does not exist") {
                Class.forName(expectedClassName)
            }
        }
    }
}
