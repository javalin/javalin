package io.javalin.http.servlet

import jakarta.servlet.ServletInputStream
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletRequestWrapper

class JavalinServletRequest(request: HttpServletRequest) : HttpServletRequestWrapper(request) {
    internal var inputStreamRead: Boolean = false
        private set

    override fun getInputStream(): ServletInputStream =
        super.getInputStream().also { inputStreamRead = true }
}
