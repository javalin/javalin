package io.javalin

import io.javalin.apibuilder.ApiBuilder.path
import io.javalin.apibuilder.ApiBuilder.get
import io.javalin.security.Roles
import io.javalin.security.RouteRole
import io.javalin.testing.TestUtil
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

enum class UserRole: RouteRole {
    ADMIN
}

class TestMatchedEndpoint {
    private val app: Javalin by lazy {
        Javalin.create { config ->
            config.router.apiBuilder {
                path("/test") {
                    get({ctx -> ctx.result("Test endpoint metadata")}, UserRole.ADMIN)
                }
            }
        }
    }


    @Test
    fun `test matched endpoints`() = TestUtil.test(app) { app, http ->
        var data = emptySet<RouteRole>()
        app.beforeMatched {
            data = it.matchedEndpoint()?.metadata(Roles::class.java)?.roles ?: emptySet()
        }
        http.get("/test")

        assertThat(data).isEqualTo(setOf<RouteRole>(UserRole.ADMIN))
    }
}
