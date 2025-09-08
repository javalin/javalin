package io.javalin

import io.javalin.apibuilder.ApiBuilder.crud
import io.javalin.apibuilder.ApiBuilder.path
import io.javalin.apibuilder.ApiBuilder.get
import io.javalin.apibuilder.CrudHandler
import io.javalin.http.Context
import io.javalin.http.HandlerType
import io.javalin.router.Endpoint
import io.javalin.router.EndpointMetadata
import io.javalin.security.Roles
import io.javalin.security.RouteRole
import io.javalin.testing.TestUtil
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

data class CustomMetaData(val name: String): EndpointMetadata

class TestCrudHandler: CrudHandler {
    override fun getAll(ctx: Context) {
        ctx.result("Test crud handler")
    }

    override fun getOne(ctx: Context, resourceId: String) {
        ctx.result("Test crud handler")
    }

    override fun create(ctx: Context) {
        ctx.result("Test crud handler")
    }

    override fun update(ctx: Context, resourceId: String) {
        ctx.result("Test crud handler")
    }

    override fun delete(ctx: Context, resourceId: String) {
        ctx.result("Test crud handler")
    }

}

class TestCustomMetaData {
    val customMetaData = CustomMetaData("This is custom meta data")
    val roles = Roles(setOf(UserRole.ADMIN))

    private val app: Javalin by lazy {
        Javalin.create { config ->
            config.router.apiBuilder {
                path("/test5") {
                    get({ctx -> ctx.result("Test5")}, setOf(roles, customMetaData))
                }
                get("/test6", {ctx -> ctx.result("Test6")}, setOf(roles, customMetaData))
                path("/test7/{id}") {
                    crud(TestCrudHandler(), setOf(customMetaData, roles))
                }
                crud("/test8/{id}", TestCrudHandler(), setOf(roles, customMetaData))
            }
            config.router.mount { defaultRouting ->
                with(defaultRouting) {
                    addEndpoint(
                        Endpoint(
                            HandlerType.GET,
                            "/test1",
                            setOf(roles, customMetaData)
                        ) { ctx -> ctx.result("Test1") }
                    )
                    addHttpHandler(HandlerType.GET, "/test2", { ctx -> ctx.result("Test2") }, roles, customMetaData)
                    addHttpHandler(HandlerType.GET, "/test3", { ctx -> ctx.result("Test3") }, setOf(roles, customMetaData))
                }
            }
        }
    }


    @Test
    fun `test custom metadata with different types of endpoints`() = TestUtil.test(app) { app, http ->
        app.get("/test4", {ctx -> ctx.result("Test4")}, setOf(roles, customMetaData))
        val endpoints = listOf("/test1", "/test2", "/test3", "/test4", "/test5", "/test6", "/test7", "/test8")
        var data = ""
        app.beforeMatched {
            data= it.matchedEndpoint()?.metadata(CustomMetaData::class.java)?.name ?: "NOT FOUND"
        }

        endpoints.map {
            http.get(it)
            assertThat(data).isEqualTo(this.customMetaData.name)
        }
    }
}
