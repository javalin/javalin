package io.javalin.http.staticfiles

import org.junit.jupiter.api.Test

class TestNativeResourceHandler {

    @Test
    fun `debug welcome file issue`() {
        val config = StaticFileConfig().apply {
            hostedPath = "/url-prefix"
            directory = "/public"
            location = Location.CLASSPATH
        }
        val handler = NativeConfigurableHandler(config)
        
        println("Testing resource resolution:")
        
        // Test URL resolution
        val classLoader = handler::class.java.classLoader
        val subdirURL = classLoader.getResource("public/subdir")
        val indexURL = classLoader.getResource("public/subdir/index.html")
        
        println("subdir URL: $subdirURL")
        println("subdir URL path: ${subdirURL?.path}")
        println("subdir URL endsWith /: ${subdirURL?.path?.endsWith("/")}")
        println("subdir File isDirectory: ${subdirURL?.path?.let { java.io.File(it).isDirectory() }}")
        println("index.html URL: $indexURL")
        println("index.html exists check: ${classLoader.getResource("public/subdir/index.html") != null}")
        
        // Test direct subdir access
        val subdirResource = handler.getResource("subdir")
        println("subdir resource: $subdirResource")
        if (subdirResource != null) {
            println("subdir exists: ${subdirResource.exists}")
            println("subdir isDirectory: ${subdirResource.isDirectory}")
            println("subdir content: ${subdirResource.newInputStream()?.use { String(it.readAllBytes()) }}")
        }
        
        // Test index.html directly
        val indexResource = handler.getResource("subdir/index.html")
        println("subdir/index.html resource: $indexResource")
        if (indexResource != null) {
            println("index.html exists: ${indexResource.exists}")
            println("index.html isDirectory: ${indexResource.isDirectory}")
            println("index.html content: ${indexResource.newInputStream()?.use { String(it.readAllBytes()) }}")
        }
    }
}