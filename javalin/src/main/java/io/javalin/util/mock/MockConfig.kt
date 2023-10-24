package io.javalin.util.mock

import io.javalin.Javalin
import io.javalin.config.JavalinConfig
import io.javalin.util.mock.HttpServletRequestMock.RequestState
import io.javalin.util.mock.HttpServletResponseMock.ResponseState
import java.util.function.Consumer

data class MockConfig(
    val req: RequestState,
    val res: ResponseState,
    internal var javalinConfig: JavalinConfig = Javalin.create().unsafeConfig()
) {

    fun javalinConfiguration(config: Consumer<JavalinConfig>) {
        this.javalinConfig = Javalin.create(config).unsafeConfig()
    }

    fun clone(): MockConfig =
        copy(
            req = req.copy(),
            res = res.copy(),
            javalinConfig = javalinConfig // TODO: we could clone this too (?)
        )

}
