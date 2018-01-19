/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.core.util

import io.javalin.Handler
import io.javalin.Javalin
import io.javalin.core.HandlerType
import io.javalin.security.Role
import sun.reflect.ConstantPool

data class RouteOverviewEntry(val httpMethod: HandlerType, val path: String, val handler: Handler, val roles: List<Role>?)

object RouteOverviewUtil {

    fun createHtmlOverview(app: Javalin): String {
        return """
            <meta name='viewport' content='width=device-width, initial-scale=1'>
            <style>
                b, thead {
                    font-weight:700
                }
                body {
                    font-family:monospace;
                    padding:15px
                }
                table {
                    border-collapse:collapse;
                    font-size:14px;
                    border:1px solid #d5d5d5;
                    width:100%;
                    white-space:pre
                }
                thead {
                    background:#e9e9e9;
                    border-bottom:1px solid #d5d5d5
                }
                tbody tr:hover {
                    background:#f5f5f5
                }
                td {
                    padding:6px 15px
                }
                b {
                    color:#33D
                }
            </style>
            <body>
                <h1>All mapped routes</h1>
                <table>
                    <thead>
                        <tr>
                            <td>Method</td>
                            <td>Path</td>
                            <td>Handler</td>
                            <td>Roles</td>
                        </tr>
                    </thead>
                    ${app.routeOverviewEntries.map { (httpMethod, path, handler, roles) ->
            "<tr><td>$httpMethod</td><td>$path</td><td><b>${handler.metaInfo}</b></td><td>${roles?.toString() ?: "-"}</td></tr>"
        }.joinToString("")}
                </table>
            </body>
        """
    }

    private val Handler.parentClass: Class<*> get() = Class.forName(toString().split("$")[0])
    private val Handler.javaLambda: Boolean get() = toString().contains("$\$Lambda$")
    private val Handler.kotlinLambda: Boolean get() = toString().contains("\$lambda")
    private val Handler.hasTwoDollars: Boolean get() = toString().count { it == '$' } == 2
    private val Handler.implementingClassName: String? get() = toString().takeWhile { it != '@' }

    val Handler.metaInfo: String
        get() {
            println(toString())
            // this is just guesswork...
            return when {
                javaLambda && fieldName != null -> parentClass.name + "." + fieldName // java Handler-field
                javaLambda && methodName != null -> parentClass.name + "::" + methodName // java method reference
                javaLambda && methodName == null -> parentClass.name + "::??? (anonymous lambda)" // java anonymous lambda
                hasTwoDollars -> parentClass.name + "." + toString().split("$")[1] // kotlin Handler field
                kotlinLambda -> parentClass.name + "::??? (anonymous lambda)" // kotlin anonymous lambda
                else -> implementingClassName + ".class"; // java/kotlin class implementing Handler
            }
        }
    val Handler.fieldName: String?
        get() = try {
            parentClass.declaredFields.find { it.isAccessible = true; it.get(it) == this }?.name
        } catch (ignored: Exception) { // Nothing really matters.
            null
        }

    val Handler.methodName: String?
        get() {
            val constantPool = Class::class.java.getDeclaredMethod("getConstantPool").apply { isAccessible = true }.invoke(javaClass) as ConstantPool
            for (i in constantPool.size downTo 0) {
                try {
                    val name = constantPool.getMemberRefInfoAt(i)[1];
                    return if (name.contains("lambda$")) null else name;
                } catch (ignored: Exception) {
                }
            }
            return null
        }

}
