package io.javalin.plugin.openapi.annotations

import io.github.classgraph.ClassGraph
import io.javalin.plugin.openapi.dsl.OpenApiDocumentation

internal fun scanForAnnotations(packagePrefixes: Set<String>): Map<PathInfo, OpenApiDocumentation> {
    // Load cached results if they exist
    cachedResults[packagePrefixes]?.also { return it }

    val documentationByPathInfo = mutableMapOf<PathInfo, OpenApiDocumentation>()

    val graph = ClassGraph()
            .enableAnnotationInfo()
            .enableClassInfo()
            .enableMethodInfo()
            .ignoreClassVisibility()
            .whitelistPackages(*packagePrefixes.toTypedArray())
            .scan()
    val annotationName = OpenApi::class.java.canonicalName
    val annotations = graph.getClassesWithMethodAnnotation(annotationName)
            .flatMap { it.methodInfo }
            .filter { it.annotationInfo.any { it.name == annotationName } }
            .map { it.loadClassAndGetMethod() }
            .map { it.getAnnotation(OpenApi::class.java) }
            .filter { it.path != NULL_STRING }

    annotations.forEach {
        documentationByPathInfo[it.pathInfo] = it.asOpenApiDocumentation()
    }

    // Cache results
    cachedResults[packagePrefixes] = documentationByPathInfo

    return documentationByPathInfo
}

private val cachedResults = mutableMapOf<Set<String>, Map<PathInfo, OpenApiDocumentation>>()
