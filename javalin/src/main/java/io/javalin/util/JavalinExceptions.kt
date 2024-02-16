package io.javalin.util

open class JavalinException : RuntimeException {
    constructor(message: String) : super(message)
    constructor(message: String, cause: Throwable) : super(message, cause)
    constructor(cause: Throwable) : super(cause)
}

class JavalinBindException(message: String, cause: Throwable) : JavalinException(message, cause)

class BodyAlreadyReadException(msg: String = "Request body has already been read") : JavalinException(msg)
