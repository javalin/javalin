package io.javalin.util

internal val Any.kotlinFieldName // this is most likely a very stupid solution
    get() = this.javaClass.toString().removePrefix(this.parentClass.toString() + "$").takeWhile { it != '$' }

internal val Any.javaFieldName: String?
    get() = try {
        parentClass.declaredFields.find { it.isAccessible = true; it.get(it) == this }?.name
    } catch (ignored: Exception) { // Nothing really matters.
        null
    }

internal val Any.parentClass: Class<*> get() = Class.forName(this.javaClass.name.takeWhile { it != '$' }, false, this.javaClass.classLoader)

internal val Any.implementingClassName: String? get() = this.javaClass.name

internal val Any.isClass: Boolean get() = this is Class<*>

internal val Any.isKotlinAnonymousLambda: Boolean get() = this.javaClass.enclosingMethod != null

internal val Any.isKotlinMethodReference: Boolean get() = this.javaClass.declaredFields.count { it.name == "function" || it.name == "\$tmp0" } == 1

internal val Any.isKotlinField: Boolean get() = this.javaClass.fields.any { it.name == "INSTANCE" }

internal val Any.isJavaAnonymousLambda: Boolean get() = this.javaClass.isSynthetic

internal val Any.isJavaField: Boolean get() = this.javaFieldName != null

internal fun Any.runMethod(name: String): Any = this.javaClass.getMethod(name).apply { isAccessible = true }.invoke(this)
