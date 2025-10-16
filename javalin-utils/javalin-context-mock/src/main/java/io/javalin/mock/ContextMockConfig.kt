package io.javalin.mock

import io.javalin.Javalin
import io.javalin.config.JavalinConfig
import io.javalin.mock.servlet.HttpServletRequestMock.RequestState
import io.javalin.mock.servlet.HttpServletResponseMock.ResponseState
import java.util.function.Consumer

data class ContextMockConfig internal constructor(
    val req: RequestState = RequestState(),
    val res: ResponseState = ResponseState(),
    var javalinConfig: JavalinConfig = Javalin.create().unsafe
) {

    /** Change Javalin config used to prepare the [Context] instance. */
    fun javalinConfig(config: Consumer<JavalinConfig>) {
        this.javalinConfig = Javalin.create(config).unsafe
    }

    /** Deep copy of this [ContextMockConfig] */
    fun clone(): ContextMockConfig =
        copy(
            req = req.copy(),
            res = res.copy(),
            javalinConfig = javalinConfig // TODO: we could clone this too (?)
        )

}
