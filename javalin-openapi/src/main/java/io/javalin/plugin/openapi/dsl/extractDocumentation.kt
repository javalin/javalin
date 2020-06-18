package io.javalin.plugin.openapi.dsl

import io.javalin.apibuilder.CrudFunctionHandler
import io.javalin.core.event.HandlerMetaInfo
import io.javalin.core.util.OptionalDependency
import io.javalin.core.util.Reflection.Companion.rfl
import io.javalin.core.util.Util
import io.javalin.core.util.methodReferenceReflectionMethodName
import io.javalin.http.Handler
import io.javalin.http.HandlerType
import io.javalin.plugin.openapi.CreateSchemaOptions
import io.javalin.plugin.openapi.annotations.*
import org.slf4j.LoggerFactory
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.util.logging.Logger
import kotlin.reflect.KFunction
import kotlin.reflect.jvm.javaMethod

class ExtractDocumentation {
    // needed for logging purposes only, since most of the Kotlin functions below are defined globally and
    // we want to have source code/file localised messages to ease debugging
}
private val localLogger = LoggerFactory.getLogger(ExtractDocumentation::class.java)

fun HandlerMetaInfo.extractDocumentation(options: CreateSchemaOptions): OpenApiDocumentation {
    val documentation = document()

    options.default?.apply(documentation)

    val userDocumentation: OpenApiDocumentation = when (handler) {
        is DocumentedHandler -> (handler as DocumentedHandler).documentation
        is CrudFunctionHandler -> extractDocumentationFromCrudHandler(handler as CrudFunctionHandler)
        else -> {
            val openApiAnnotation: OpenApi? = getOpenApiAnnotationFromReference()
                    ?: methodReferenceOfHandler?.getOpenApiAnnotation()
                    ?: fieldReferenceOfHandler?.getOpenApiAnnotation()
                    ?: getOpenApiAnnotationFromHandler()
            openApiAnnotation?.asOpenApiDocumentation()
                    ?: extractDocumentationWithPathScanning(options)
                    ?: document()
        }
    }

    documentation.apply(userDocumentation)

    return documentation
}

private fun extractDocumentationFromCrudHandler(handler: CrudFunctionHandler): OpenApiDocumentation {
    val crudHandler = handler.crudHandler
    return if (crudHandler is DocumentedCrudHandler) {
        crudHandler.crudHandlerDocumentation.asMap()[handler.function]
                ?: throw IllegalStateException("No documentation for this function")
    } else {
        val method = rfl(handler.crudHandler).getMethodByName(handler.function.value) ?: throw NoSuchMethodException(handler.function.value)
        val openApi: OpenApi? = method.getDeclaredAnnotation(OpenApi::class.java)
        openApi?.asOpenApiDocumentation()
                ?: document()
    }
}

private fun HandlerMetaInfo.extractDocumentationWithPathScanning(options: CreateSchemaOptions): OpenApiDocumentation? {
    if (options.packagePrefixesToScan.isEmpty()) {
        return null
    }
    Util.ensureDependencyPresent(OptionalDependency.CLASS_GRAPH)
    return pathInfo?.let { pathInfo ->
        val documentationFromScan = scanForAnnotations(options.packagePrefixesToScan)
        documentationFromScan[pathInfo]
    }
}

private fun HandlerMetaInfo.getOpenApiAnnotationFromReference(): OpenApi? {
    return try {
        methodReferenceOfHandler?.getOpenApiAnnotation()
                ?: rfl(handler).lambdaField?.getOpenApiAnnotation()
    } catch (e: NoSuchFieldException) {
        null
    } catch (e: Error) {
        return if (e::class.qualifiedName == "kotlin.reflect.jvm.internal.KotlinReflectionInternalError") {
            Logger.getGlobal()
                    .warning("Local functions, lambdas, anonymous functions and local variables with @OpenApi annotations are currently not supported. " +
                            "The annotation of the handler for \"$httpMethod $path\" will be ignored. To fix this, move the handler into a global function.")
            null
        } else {
            throw e
        }
    }
}

private fun HandlerMetaInfo.getOpenApiAnnotationFromHandler(): OpenApi? {
    return try {
        val method = rfl(handler).getMethodByName("handle")!!
        method.getOpenApiAnnotation()
    } catch (e: NullPointerException) {
        return null
    }
}

private val HandlerMetaInfo.methodReferenceOfHandler: Method?
    get() = rfl(handler).let { handlerReflection ->
            when {
                handlerReflection.isClass -> (handler as Class<*>).methods[0]
                handlerReflection.isKotlinMethodReference -> {
                    val functionValue = handlerReflection.getFieldValue("function") as KFunction<*>
                    functionValue.javaMethod
                }
                handlerReflection.isKotlinAnonymousLambda -> null // Cannot be parsed
                handlerReflection.isJavaNonStaticMethodReference -> methodReferenceOfNonStaticJavaHandler
                handlerReflection.isJavaAnonymousLambda -> null // taken care of by field-type handler
                else -> {
                    localLogger.debug("methodReferenceOfHandler else branch called for handler: "
                            +"$httpMethod path '$path' and parent class = " + handlerReflection.parentClass.name)
                    null
                }
            }
    }

private val HandlerMetaInfo.fieldReferenceOfHandler: Field?
    get() {
        if (handler !is Handler) {
            localLogger.error("fieldReferenceOfHandler $httpMethod path: $path -- incompatible type: "
                    + handler.javaClass.canonicalName)
            return null // early return since handler does not implement Handler interface
        }

        return rfl(handler).let { handlerReflection ->
            when {
                // handlerReflection.isClass -> (handler as Class<*>).methods[0]
                handlerReflection.isClass -> null // case handled in @see methodReferenceOfHandler
                handlerReflection.isKotlinMethodReference -> null // case handled in @see methodReferenceOfHandler
                handlerReflection.isKotlinAnonymousLambda -> null // Cannot be parsed // N.B. possibly add similar treatment as for Java
                handlerReflection.isJavaNonStaticMethodReference -> null // case handled in @see methodReferenceOfHandler
                handlerReflection.isJavaAnonymousLambda -> findFieldByOpenApiAnnotation(handlerReflection.parentClass.declaredFields)
                handlerReflection.isJavaAnonymousClass -> findFieldByOpenApiAnnotation(handlerReflection.parentClass.declaredFields)
                handlerReflection.isJavaMemberClass -> findFieldByOpenApiAnnotation(handlerReflection.parentClass.declaredFields)
                else -> {
                    localLogger.debug("fieldReferenceOfHandler else branch called for handler: "
                            +"$httpMethod path '$path' and parent class = " + handlerReflection.parentClass.name)
                    null
                }
            }
        }
    }

private val HandlerMetaInfo.methodReferenceOfNonStaticJavaHandler: Method?
    get() {
        val handlerParentClass = rfl(handler)
                .getMethodByName(methodReferenceReflectionMethodName)
                ?.parameters?.get(0)
                ?.parameterizedType as Class<*>?

        val methods = handlerParentClass
                ?.let { rfl(it) }
                ?.methodsNotDeclaredByObject
                ?: arrayOf()

        return when {
            methods.isEmpty() -> null
            methods.size == 1 -> methods[0]
            else -> {
                val methodThatMatchesHandler = findMethodByOpenApiAnnotation(methods)
                if (methodThatMatchesHandler != null) {
                    return methodThatMatchesHandler
                }

                val hasAnyMethodTheOpenApiAnnotation = methods.any { it.getOpenApiAnnotation() != null }
                if (hasAnyMethodTheOpenApiAnnotation && handlerParentClass != null) {
                    localLogger.warn("Unfortunately it is not possible to match the @OpenApi annotations to the handler in ${handlerParentClass.canonicalName}. " +
                            "Please add the `path` and the `method` information to the annotation, so the handler can be matched.")
                }
                null
            }
        }
    }

private fun HandlerMetaInfo.findMethodByOpenApiAnnotation(methods: Array<Method>): Method? {
    val methodsThatMatchesPath = methods.filter {
        val annotation = it.getOpenApiAnnotation() ?: return@filter false
        annotation.path == this.path
    }
    return when {
        methodsThatMatchesPath.size == 1 -> methodsThatMatchesPath[0]
        methodsThatMatchesPath.size > 1 -> methodsThatMatchesPath
                .find {
                    it.getOpenApiAnnotation()
                            ?.let { annotation -> annotation.pathInfo == pathInfo }
                            ?: false
                }
        else -> null
    }
}

private fun HandlerMetaInfo.findFieldByOpenApiAnnotation(allFields: Array<Field>): Field? {
    if (allFields.isEmpty()) {
        // no fields declared -> handler cannot be a field-type declaration
        return null
    }
    // filter only implementations that are derived from the Handler interface
    val handlerFields = allFields.filter { Handler::class.java.isAssignableFrom(it.type) }
    if (handlerFields.isEmpty()) {
        localLogger.warn("cannot match handler: $httpMethod path: $path -- no Handler-type fields declared in class "
                + allFields[0].declaringClass.canonicalName)
        return null
    }

    // case 1: exact match -- nominal case of OpenAPI path and HttpMethod declaration
    val fieldsExactMatch = handlerFields.filter {
        val annotation = it.getOpenApiAnnotation() ?: return@filter false
        path.contentEquals(annotation.path) && httpMethod.compatible(annotation.method)
    }
    when (fieldsExactMatch.size) {
        0 -> { /* empty */ } // no exact or partial-only match, continue with code below
        1 -> return fieldsExactMatch[0] // exact path and HttpMethod match
        else -> { // more than one exact match
            localLogger.error("cannot match handler: $httpMethod path: $path -- multiple equal OpenAPI fields declarations")
            for ((index, value) in fieldsExactMatch.withIndex()) {
                localLogger.error("  - candidate $index: $value")
            }
            return null
        }
    }

    // case 2: only one Handler-type field declared
    if (handlerFields.size == 1) {
        val annotation = handlerFields[0].getOpenApiAnnotation()
        if (annotation == null) {
            localLogger.warn("cannot match handler: $httpMethod path: $path -- missing OpenAPI field annotation for field: "
                    + handlerFields[0].declaringLocation())
            return null
        }

        if (annotation.path == NULL_STRING && httpMethod.compatible(annotation.method)) {
            // trivial match: only one field with OpenAPI
        } else if (annotation.path == NULL_STRING || path.contentEquals(annotation.path)) {
            val annotationMethod = annotation.method
            localLogger.warn("partial match for path $path and single Handler-type field -- handler type $httpMethod defined"
                    +" but only available OpenAPI annotation declared for '$annotationMethod' for field: "
                    + handlerFields[0].declaringLocation())
        } else {
            localLogger.warn("next best match for path $path and handler type $httpMethod defined -" +
                    " imperfect match between annotation '$annotation' and field: "
                    + handlerFields[0].declaringLocation())
        }
        return handlerFields[0]
    }

    // case 3: matching Path
    val fieldsThatMatchPath = handlerFields.filter {
        val annotation = it.getOpenApiAnnotation() ?: return@filter false
        annotation.path == this.path
    }
    when (fieldsThatMatchPath.size) {
        0 -> { /* empty */ } // no exact or partial-only match, continue with code below
        1 -> {
            val annotation = fieldsThatMatchPath[0].getOpenApiAnnotation() ?: return null
            if (!httpMethod.compatible(annotation.method)) {
                val annotationMethod = annotation.method
                localLogger.warn("partial match for path '$path' -- handler method '$httpMethod' mismatch "
                        +"with OpenAPI method '$annotationMethod' for field: "
                        + fieldsThatMatchPath[0].declaringLocation())
            }
            return fieldsThatMatchPath[0]
        }
        else -> {
            // fieldsThatMatchesPath.size > 1 ->
            return fieldsThatMatchPath
            .find {
                it.getOpenApiAnnotation()
                        ?.let { annotation -> annotation.pathInfo == pathInfo }
                        ?: false
            }
        }
    }

    // case 4: matching HttpMethod
    val fieldsMatchingHttpMethod = handlerFields.filter {
        val annotation = it.getOpenApiAnnotation() ?: return@filter false
        httpMethod.compatible(annotation.method)
    }
    when (fieldsMatchingHttpMethod.size) {
        0 -> { /* empty */ } // no exact or partial-only match, continue with code below
        1 -> {
            val annotation = fieldsMatchingHttpMethod[0].getOpenApiAnnotation() ?: return null
            if (annotation.path != NULL_STRING && !path.contentEquals(annotation.path)) {
                val annotationPath = annotation.path
                localLogger.warn("partial match for method $httpMethod -- handler path '$path' mismatch "
                        +"with OpenAPI path '$annotationPath' for field: "
                        + fieldsMatchingHttpMethod[0].declaringLocation())
            }
            return fieldsMatchingHttpMethod[0] // exact HttpMethod match
        }
        else -> { // more than one exact match
            localLogger.error("cannot match handler: $httpMethod path: $path -- multiple equal OpenAPI fields method declarations")
            for ((index, value) in fieldsMatchingHttpMethod.withIndex()) {
                localLogger.error("  - candidate $index: $value")
            }
            return null
        }
    }

    localLogger.warn("cannot match handler: $httpMethod path: $path -- no matching OpenAPI field annotation for eligible fields: ")
    for ((index, value) in handlerFields.withIndex()) {
        localLogger.error("  - candidate $index: $value")
    }
    return null
}

fun Method.getOpenApiAnnotation(): OpenApi? {
    val result = getAnnotation(OpenApi::class.java) ?: return null
    result.warnUserAboutPotentialBugs(this.declaringClass)
    return result
}

fun Field.getOpenApiAnnotation(): OpenApi? {
    val result = getAnnotation(OpenApi::class.java) ?: return null
    result.warnUserAboutPotentialBugs(this.declaringClass)
    return result
}

fun HandlerType.compatible(other: Any?) : Boolean {
    if (other == null) {
        return false
    }
    if (equals(other)) {
        return true
    }
    if (other !is HttpMethod) {
        return false
    }
    val httpMethod : HttpMethod = other
    return when (this) { // N.B. this strange comparison is needed because HttpMethod and HandlerType enums differ
        HandlerType.GET -> httpMethod == HttpMethod.GET
        HandlerType.POST -> httpMethod == HttpMethod.POST
        HandlerType.PUT -> httpMethod == HttpMethod.PUT
        HandlerType.PATCH -> httpMethod == HttpMethod.PATCH
        HandlerType.DELETE -> httpMethod == HttpMethod.DELETE
        HandlerType.HEAD -> httpMethod == HttpMethod.HEAD
        HandlerType.OPTIONS -> httpMethod == HttpMethod.OPTIONS
        HandlerType.TRACE -> httpMethod == HttpMethod.TRACE
        HandlerType.CONNECT -> false // not mapped/no equivalent in HttpMethod
        HandlerType.BEFORE -> false // not mapped/no equivalent in HttpMethod
        HandlerType.AFTER -> false // not mapped/no equivalent in HttpMethod
        HandlerType.INVALID -> false // not mapped/no equivalent in HttpMethod
        else -> false
    }
}

private fun Method.declaringLocation() : String = returnType.toGenericString() + " " + declaringClass.name+"::"+ name

private fun Field.declaringLocation() : String = declaringClass.canonicalName+"::"+ type +" "+ name
