package io.javalin.http.servlet

import jakarta.servlet.Servlet
import jakarta.servlet.ServletContainerInitializer

data class ServletEntry(
    val initializer: ServletContainerInitializer? = null,
    val servlet: Servlet
)
