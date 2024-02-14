/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.plugin.bundled

import io.javalin.router.ParsedEndpoint
import io.javalin.security.Roles
import io.javalin.util.implementingClassName
import io.javalin.util.isClass
import io.javalin.util.isJavaAnonymousLambda
import io.javalin.util.isJavaField
import io.javalin.util.isKotlinAnonymousLambda
import io.javalin.util.isKotlinField
import io.javalin.util.isKotlinMethodReference
import io.javalin.util.javaFieldName
import io.javalin.util.kotlinFieldName
import io.javalin.util.parentClass
import io.javalin.util.runMethod
import io.javalin.websocket.WsHandlerEntry

object RouteOverviewUtil {

    @JvmStatic
    fun createHtmlOverview(handlerInfo: List<ParsedEndpoint>, wsHandlerInfo: List<WsHandlerEntry>): String {
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
                ${
                    handlerInfo
                        .map { it.endpoint }
                        .joinToString("") {
                            """
                            <tr class="method ${it.method}">
                                <td>${it.method}</span></td>
                                <td>${it.path}</td>
                                <td><b>${it.handler.metaInfo}</b></td>
                                <td>${it.metadata<Roles>()?.roles ?: emptySet()}</td>
                            </tr>
                            """
                        }
                }
                ${
            wsHandlerInfo.map { (wsHandlerType, path, handler, roles) ->
                """
                    <tr class="method $wsHandlerType">
                        <td>$wsHandlerType</span></td>
                        <td>$path</td>
                        <td><b>${handler.metaInfo}</b></td>
                        <td>$roles</td>
                    </tr>
                    """
            }.joinToString("")
        }
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
    fun createJsonOverview(handlerInfo: List<ParsedEndpoint>, wsHandlerInfo: List<WsHandlerEntry>): String {
        return """
            {
                "handlers": [
                ${
            handlerInfo.map { it.endpoint }.joinToString(",") {
                """
                    {
                        "path": "${it.path}",
                        "handlerType": "${it.method}",
                        "metaInfo": "${it.handler.metaInfo}",
                        "roles": "${it.roles}"
                    }
                    """
            }
        }
                ],
                "wsHandlers": [
                ${
            wsHandlerInfo.map { (wsHandlerType, path, wsConfig, roles) ->
                """
                    {
                        "path": "$path",
                        "handlerType": "$wsHandlerType",
                        "metaInfo": "${wsConfig.metaInfo}",
                        "roles": "$roles"
                    }
                    """
            }.joinToString(",")
        }
                ]
            }
    """
    }

    @JvmStatic
    val Any.metaInfo: String
        get() {
            // this is just guesswork...
            // every new version of Java or Kotlin seems to break something here
            return when {
                isClass -> (this as Class<*>).name + ".class"
                isKotlinMethodReference -> {
                    val fieldName = this.javaClass.declaredFields.find { it.name == "function" || it.name == "\$tmp0" }!!.name
                    val field = this.javaClass.getDeclaredField(fieldName).apply { isAccessible = true }.get(this)
                    when (fieldName) {
                        "function" -> field.runMethod("getOwner").runMethod("getJClass").runMethod("getName").toString() + "::" + field.runMethod("getName")
                        else -> "${field.implementingClassName}::$lambdaSign"
                    }

                }

                isKotlinAnonymousLambda -> parentClass.name + "::" + lambdaSign
                isKotlinField -> parentClass.name + "." + kotlinFieldName
                isJavaField -> parentClass.name + "." + javaFieldName
                isJavaAnonymousLambda -> parentClass.name + "::" + lambdaSign
                else -> "$implementingClassName.class"
            }
        }

}

private const val lambdaSign = "??? (anonymous lambda)"
