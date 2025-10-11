package io.javalin.examples

import io.javalin.Javalin
import io.javalin.http.Handler
import io.javalin.http.util.handleCustomMethod

/**
 * Example demonstrating how to handle WebDAV methods in Javalin.
 * 
 * Run this example and test with:
 * curl -X PROPFIND http://localhost:7070/dav/folder
 * curl -X MKCOL http://localhost:7070/dav/newcollection
 */
fun main() {
    val app = Javalin.create().start(7070)

    // Handle WebDAV methods using the custom HTTP method handler
    app.before("/dav/*") { ctx ->
        ctx.handleCustomMethod(
            "PROPFIND" to Handler { ctx ->
                val path = ctx.pathParam("*")
                ctx.result("""
                    <?xml version="1.0" encoding="utf-8"?>
                    <D:multistatus xmlns:D="DAV:">
                        <D:response>
                            <D:href>/dav/$path</D:href>
                            <D:propstat>
                                <D:status>HTTP/1.1 200 OK</D:status>
                            </D:propstat>
                        </D:response>
                    </D:multistatus>
                """.trimIndent()).contentType("application/xml")
            },
            "MKCOL" to Handler { ctx ->
                val path = ctx.pathParam("*")
                // In a real implementation, you would create the collection here
                ctx.result("Collection '$path' created").status(201)
            },
            "COPY" to Handler { ctx ->
                val source = ctx.pathParam("*")
                val destination = ctx.header("Destination") ?: ""
                // In a real implementation, you would copy the resource here
                ctx.result("Resource copied from '$source' to '$destination'").status(201)
            },
            "MOVE" to Handler { ctx ->
                val source = ctx.pathParam("*")
                val destination = ctx.header("Destination") ?: ""
                // In a real implementation, you would move the resource here
                ctx.result("Resource moved from '$source' to '$destination'").status(201)
            }
        )
    }

    println("WebDAV server started on http://localhost:7070/dav/")
    println("Try:")
    println("  curl -X PROPFIND http://localhost:7070/dav/folder")
    println("  curl -X MKCOL http://localhost:7070/dav/newcollection")
    println("  curl -X COPY -H 'Destination: /dav/newfolder' http://localhost:7070/dav/folder")
}
