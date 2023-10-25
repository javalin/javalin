package io.javalin.util.mock

import io.javalin.Javalin
import io.javalin.config.JavalinConfig
import io.javalin.util.mock.HttpServletRequestMock.RequestState
import io.javalin.util.mock.HttpServletResponseMock.ResponseState
import java.util.function.Consumer

data class MockConfig internal constructor(
    val req: RequestState = RequestState(),
    val res: ResponseState = ResponseState(),
    internal var javalinConfig: JavalinConfig = Javalin.create().unsafeConfig()
) {

    /** Change Javalin config used to prepare the [Context] instance. */
    fun javalinConfiguration(config: Consumer<JavalinConfig>) {
        this.javalinConfig = Javalin.create(config).unsafeConfig()
    }

    /** Deep copy of this [MockConfig] */
    fun clone(): MockConfig =
        copy(
            req = req.copy(),
            res = res.copy(),
            javalinConfig = javalinConfig // TODO: we could clone this too (?)
        )

}
