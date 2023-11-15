package io.javalin.config

class ValidationConfig() {

    internal val converters = mutableMapOf<Class<*>, (String) -> Any?>(
        java.lang.Boolean::class.java to { it.toBoolean() },
        java.lang.Double::class.java to { it.toDouble() },
        java.lang.Float::class.java to { it.toFloat() },
        java.lang.Integer::class.java to { it.toInt() },
        java.lang.Long::class.java to { it.toLong() },
        java.lang.String::class.java to { it },
        Boolean::class.java to { it.toBoolean() },
        Double::class.java to { it.toDouble() },
        Float::class.java to { it.toFloat() },
        Int::class.java to { it.toInt() },
        Long::class.java to { it.toLong() },
        String::class.java to { it }
    )

    fun register(clazz: Class<*>, converter: (String) -> Any?) {
        converters[clazz] = converter
    }

}
