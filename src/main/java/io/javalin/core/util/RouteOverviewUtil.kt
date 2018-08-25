/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.core.util

import io.javalin.Context
import io.javalin.Handler
import io.javalin.Javalin

class RouteOverviewRenderer(val app: Javalin) : Handler {
    override fun handle(ctx: Context) {
        ctx.html(RouteOverviewUtil.createHtmlOverview(app))
    }
}

object RouteOverviewUtil {

    @JvmStatic
    fun createHtmlOverview(app: Javalin): String {
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
            .HEAD, .TRACE, .OPTIONS  {
                background: #00b9b9;
            }
            .BEFORE, .AFTER {
                background: #777;
            }

            .WEBSOCKET {
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
                ${app.handlerMetaInfo.map { (httpMethod, path, handler, roles) ->
            """
                    <tr class="method $httpMethod">
                        <td>$httpMethod</span></td>
                        <td>$path</td>
                        <td><b>${handler.metaInfo}</b></td>
                        <td>$roles</td>
                    </tr>
                    """
        }.joinToString("")}
            </table>
            <script>
                const cachedRows = Array.from(document.querySelectorAll("tbody tr"));
                const verbOrder = ["BEFORE", "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "TRACE", "HEAD", "AFTER", "WEBSOCKET"];
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
    val Any.metaInfo: String
        get() {
            // this is just guesswork...
            return when {
                isClass -> (this as Class<*>).name + ".class"
                isKotlinMethodReference -> {
                    val f = this.javaClass.getDeclaredField("function")
                            .apply { isAccessible = true }
                            .get(this)
                    f.runMethod("getOwner").runMethod("getJClass").runMethod("getName").toString() + "::" + f.runMethod("getName")
                }
                isKotlinAnonymousLambda -> parentClass.name + "::" + lambdaSign
                isKotlinField -> parentClass.name + "." + kotlinFieldName

                isJavaMethodReference -> parentClass.name + "::" + methodName
                isJavaField -> parentClass.name + "." + javaFieldName
                isJavaAnonymousLambda -> parentClass.name + "::" + lambdaSign

                else -> implementingClassName + ".class"
            }
        }
}

private val Any.kotlinFieldName // this is most likely a very stupid solution
    get() = this.javaClass.toString().removePrefix(this.parentClass.toString() + "$").takeWhile { it != '$' }

private val Any.javaFieldName: String?
    get() = try {
        parentClass.declaredFields.find { it.isAccessible = true; it.get(it) == this }?.name
    } catch (ignored: Exception) { // Nothing really matters.
        null
    }

private val Any.methodName: String? // broken in jdk9+ since ConstantPool has been removed
    get() {
//        val constantPool = Class::class.java.getDeclaredMethod("getConstantPool").apply { isAccessible = true }.invoke(javaClass) as ConstantPool
//        for (i in constantPool.size downTo 0) {
//            try {
//                val name = constantPool.getMemberRefInfoAt(i)[1]
//                // Autogenerated ($), constructor, or kotlin's check (fix maybe?)
//                if (name.contains("(\\$|<init>|checkParameterIsNotNull)".toRegex())) {
//                    continue
//                } else {
//                    return name
//                }
//            } catch (ignored: Exception) {
//            }
//        }
        return null
    }

private const val lambdaSign = "??? (anonymous lambda)"

private val Any.parentClass: Class<*> get() = Class.forName(this.javaClass.name.takeWhile { it != '$' })
private val Any.implementingClassName: String? get() = this.javaClass.name

private val Any.isClass: Boolean get() = this is Class<*>

private val Any.isKotlinAnonymousLambda: Boolean get() = this.javaClass.enclosingMethod != null
private val Any.isKotlinMethodReference: Boolean get() = this.javaClass.declaredFields.any { it.name == "function" }
private val Any.isKotlinField: Boolean get() = this.javaClass.fields.any { it.name == "INSTANCE" }

private val Any.isJavaAnonymousLambda: Boolean get() = this.javaClass.isSynthetic
private val Any.isJavaMethodReference: Boolean get() = this.methodName != null
private val Any.isJavaField: Boolean get() = this.javaFieldName != null

private fun Any.runMethod(name: String): Any = this.javaClass.getMethod(name).apply { isAccessible = true }.invoke(this)
