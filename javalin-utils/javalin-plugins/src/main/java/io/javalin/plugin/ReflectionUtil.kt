package io.javalin.plugin

val Any.kotlinFieldName // this is most likely a very stupid solution
    get() = this.javaClass.toString().removePrefix(this.parentClass.toString() + "$").takeWhile { it != '$' }

val Any.javaFieldName: String?
    get() = try {
        parentClass.declaredFields.find { it.isAccessible = true; it.get(it) == this }?.name
    } catch (ignored: Exception) { // Nothing really matters.
        null
    }

val Any.parentClass: Class<*> get() = Class.forName(this.javaClass.name.takeWhile { it != '$' }, false, this.javaClass.classLoader)

val Any.implementingClassName: String? get() = this.javaClass.name

val Any.isClass: Boolean get() = this is Class<*>

val Any.isKotlinAnonymousLambda: Boolean get() = this.javaClass.enclosingMethod != null

val Any.isKotlinMethodReference: Boolean get() = this.javaClass.declaredFields.count { it.name == "function" || it.name == "\$tmp0" } == 1

val Any.isKotlinField: Boolean get() = this.javaClass.fields.any { it.name == "INSTANCE" }

val Any.isJavaAnonymousLambda: Boolean get() = this.javaClass.isSynthetic

val Any.isJavaField: Boolean get() = this.javaFieldName != null

fun Any.runMethod(name: String): Any = this.javaClass.getMethod(name).apply { isAccessible = true }.invoke(this)
