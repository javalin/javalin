package io.javalin.router

import io.javalin.http.Context
import io.javalin.http.Handler
import io.javalin.http.HandlerType
import io.javalin.security.RouteRole
import io.javalin.util.mock.ContextMock
import io.javalin.util.mock.Body
import io.javalin.util.mock.MockConfig
import java.util.function.Consumer

class Endpoint @JvmOverloads constructor(
    val method: HandlerType,
    val path: String,
    val roles: Set<RouteRole> = emptySet(),
    val handler: Handler
) {

    fun handle(ctx: Context): Context {
        handler.handle(ctx)
        return ctx
    }

    fun interface EndpointExecutor {
        fun execute(endpoint: Endpoint): Context
    }

    fun handle(executor: EndpointExecutor): Context {
        return executor.execute(this)
    }

//    @JvmOverloads
//    fun handle(
//        ctx: ContextMock,
//        uri: String = path,
//        body: Body? = null,
//        cfg: (Consumer<MockConfig>)? = null
//    ): Context =
//        ctx.withMockConfig { cfg?.accept(it) }.execute(this, uri, body)

}
