/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.core.util

import io.javalin.Javalin
import io.javalin.apibuilder.CrudFunctionHandler
import io.javalin.core.event.HandlerMetaInfo
import io.javalin.core.event.WsHandlerMetaInfo
import io.javalin.core.security.Role
import io.javalin.http.Context
import io.javalin.http.Handler
import io.javalin.plugin.openapi.annotations.ContentType
import io.javalin.plugin.openapi.annotations.OpenApi
import io.javalin.plugin.openapi.annotations.OpenApiContent
import io.javalin.plugin.openapi.annotations.OpenApiResponse

data class RouteOverviewConfig(val path: String, val roles: Set<Role>)

class RouteOverviewRenderer(val app: Javalin) : Handler {

    val handlerMetaInfoList = mutableListOf<HandlerMetaInfo>()
    val wsHandlerMetaInfoList = mutableListOf<WsHandlerMetaInfo>()

    init {
        app.events { it.handlerAdded { handlerInfo -> handlerMetaInfoList.add(handlerInfo) } }
        app.events { it.wsHandlerAdded { handlerInfo -> wsHandlerMetaInfoList.add(handlerInfo) } }
    }

    @OpenApi(
            summary = "Get an overview of all the routes in the application",
            responses = [
                OpenApiResponse("200", content = [OpenApiContent(type = ContentType.HTML)])
            ]
    )
    override fun handle(ctx: Context) {
        if (ctx.header(Header.ACCEPT)?.toLowerCase()?.contains("application/json") == true) {
            ctx.header("Content-Type", "application/json")
            ctx.result(RouteOverviewUtil.createJsonOverview(handlerMetaInfoList, wsHandlerMetaInfoList))
        } else {
            ctx.html(RouteOverviewUtil.createHtmlOverview(handlerMetaInfoList, wsHandlerMetaInfoList))
        }
    }
}

object RouteOverviewUtil {

    @JvmStatic
    fun createHtmlOverview(handlerInfo: List<HandlerMetaInfo>, wsHandlerInfo: List<WsHandlerMetaInfo>): String {
        return """
        <meta name='viewport' content='width=device-width, initial-scale=1'>
        <style>
            * {
                box-sizing: border-box;
            }
            b, thead {
                font-weight: 700;
            }
            html {
                background: #363e4c;
            }
            body {
                font-family: monospace;
                padding: 25px;
            }
            table {
                background: #fff;
                border-spacing: 0;
                font-size: 14px;
                width: 100%;
                white-space: pre;
                box-shadow: 0 5px 25px rgba(0,0,0,0.25);
            }
            thead {
                background: #1a202b;
                color: #fff;
            }
            thead td {
                border-bottom: 2px solid #000;
                cursor: pointer;
            }
            tr + tr td {
                border-top: 1px solid rgba(0, 0, 0, 0.25);
            }
            tr + tr td:first-of-type {
                border-top: 1px solid rgba(0, 0, 0, 0.35);
            }
            td {
                padding: 10px 15px;
            }
            tbody td:not(:first-of-type) {
                background-color: rgba(255,255,255,0.925);
            }
            tbody tr:hover td:not(:first-of-type) {
                background-color: rgba(255,255,255,0.85);
            }
            .method td:first-of-type {
                text-align: center;
                max-width: 90px;
            }
            tbody .method td:first-of-type {
                font-weight: 700;
                color: #fff;
                text-shadow: 1px 1px 0px rgba(0,0,0,0.5);
                border-left: 6px solid rgba(0, 0, 0, 0.35);
                border-right: 1px solid rgba(0, 0, 0, 0.15);
            }
            .GET {
                background: #5a76ff;
            }
            .POST {
                background: #5dca5d;
            }
            .PUT {
                background: #d9cc00;
            }
            .PATCH {
                background: #ef9a00;
            }
            .DELETE {
                background: #e44848;
            }
            .HEAD, .TRACE, .OPTIONS, .CONNECT  {
                background: #00b9b9;
            }
            .BEFORE, .AFTER {
                background: #777;
            }
            .WEBSOCKET, .WS_BEFORE, .WS_AFTER {
                background: #546E7A;
            }
        </style>
        <body>
            <table>
                <thead>
                    <tr class="method">
                        <td width="105px">Method</td>
                        <td>Path</td>
                        <td>Handler</td>
                        <td>Roles</td>
                    </tr>
                </thead>
                ${handlerInfo.map { (handlerType, path, handler, roles) ->
            """
                    <tr class="method $handlerType">
                        <td>$handlerType</span></td>
                        <td>$path</td>
                        <td><b>${handler.metaInfo}</b></td>
                        <td>$roles</td>
                    </tr>
                    """
        }.joinToString("")}
                ${wsHandlerInfo.map { (wsHandlerType, path, handler, roles) ->
            """
                    <tr class="method $wsHandlerType">
                        <td>$wsHandlerType</span></td>
                        <td>$path</td>
                        <td><b>${handler.metaInfo}</b></td>
                        <td>$roles</td>
                    </tr>
                    """
        }.joinToString("")}
            </table>
            <script>
                const cachedRows = Array.from(document.querySelectorAll("tbody tr"));
                const verbOrder = ["BEFORE", "GET", "POST", "PUT", "PATCH", "DELETE", "CONNECT", "OPTIONS", "TRACE", "HEAD", "AFTER", "WS_BEFORE", "WEBSOCKET", "WS_AFTER"];
                document.querySelector("thead").addEventListener("click", function (e) {
                    cachedRows.map(function (el) {
                        return {key: el.children[e.target.cellIndex].textContent, row: el};
                    }).sort((a, b) => {
                        if (e.target.textContent === "Method") {
                            return verbOrder.indexOf(a.key) - verbOrder.indexOf(b.key);
                        }
                        return a.key.localeCompare(b.key);
                    }).forEach((pair, i) => {
                        document.querySelector("tbody").children[i].outerHTML = pair.row.outerHTML
                    });
                });
            </script>
        </body>
    """
    }

    @JvmStatic
    fun createJsonOverview(handlerInfo: List<HandlerMetaInfo>, wsHandlerInfo: List<WsHandlerMetaInfo>): String {
        return """
            {
                "handlers": [
                ${handlerInfo.map { (handlerType, path, handler, roles) ->
            """
                    {
                        "path": "$path",
                        "handlerType": "$handlerType",
                        "metaInfo": "$handler.metaInfo",
                        "roles": "$roles"
                    }
                    """
        }.joinToString(",")}
                ],
                "wsHandlers": [
                ${wsHandlerInfo.map { (wsHandlerType, path, handler, roles) ->
            """
                    {
                        "path": "$path",
                        "handlerType": "$wsHandlerType",
                        "metaInfo": "$handler.metaInfo",
                        "roles": "$roles"
                    }
                    """
        }.joinToString(",")}
                ]
            }
    """
    }

    @JvmStatic
    val Any.metaInfo: String
        get() {
            // this is just guesswork...
            return when {
                isClass -> (this as Class<*>).name + ".class"
                isCrudFunction -> "ApiBuilder#crud::${(this as CrudFunctionHandler).function.value}"
                isKotlinMethodReference -> {
                    val f = this.javaClass.getDeclaredField("function")
                            .apply { isAccessible = true }
                            .get(this)
                    f.runMethod("getOwner").runMethod("getJClass").runMethod("getName").toString() + "::" + f.runMethod("getName")
                }
                isKotlinAnonymousLambda -> parentClass.name + "::" + lambdaSign
                isKotlinField -> parentClass.name + "." + kotlinFieldName

                hasMethodName -> parentClass.name + "::" + methodName
                isJavaField -> parentClass.name + "." + javaFieldName
                isJavaAnonymousLambda -> parentClass.name + "::" + lambdaSign

                else -> implementingClassName + ".class"
            }
        }
}

private const val lambdaSign = "??? (anonymous lambda)"

