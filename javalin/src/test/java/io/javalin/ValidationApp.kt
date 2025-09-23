package io.javalin

fun main() {
    println("=== Javalin Allow Header Validation Test ===")
    
    val app = Javalin.create { config ->
        config.http.prefer405over404 = true
    }
    
    // Define some routes
    app.post("/api/users") { ctx -> ctx.result("Created user") }
    app.put("/api/users") { ctx -> ctx.result("Updated user") }
    app.delete("/api/users") { ctx -> ctx.result("Deleted user") }
    
    app.get("/api/products") { ctx -> ctx.result("Listed products") }
    
    app.start(7070)
    
    println("Server started on http://localhost:7070")
    println()
    println("Test the fix with these commands:")
    println("1. Test multiple methods: curl -i -X GET http://localhost:7070/api/users")
    println("   Expected: 405 with Allow: POST, PUT, DELETE")
    println()
    println("2. Test single method: curl -i -X POST http://localhost:7070/api/products") 
    println("   Expected: 405 with Allow: GET")
    println()
    println("3. Test JSON response: curl -i -H 'Accept: application/json' -X PATCH http://localhost:7070/api/users")
    println("   Expected: 405 with Allow header and JSON body")
    println()
    println("Press Ctrl+C to stop the server")
}