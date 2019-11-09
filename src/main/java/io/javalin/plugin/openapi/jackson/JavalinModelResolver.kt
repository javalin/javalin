package io.javalin.plugin.openapi.jackson

import com.fasterxml.jackson.databind.JavaType
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import io.swagger.v3.core.converter.AnnotatedType
import io.swagger.v3.core.converter.ModelConverter
import io.swagger.v3.core.converter.ModelConverterContext
import io.swagger.v3.core.jackson.ModelResolver
import io.swagger.v3.oas.models.media.DateTimeSchema
import io.swagger.v3.oas.models.media.Schema
import java.time.Instant

class JavalinModelResolver(mapper: ObjectMapper) : ModelResolver(mapper) {
    override fun resolve(annotatedType: AnnotatedType?, context: ModelConverterContext?, next: MutableIterator<ModelConverter>?): Schema<*> {
        if (annotatedType == null || shouldIgnoreClass(annotatedType.type)) {
            return super.resolve(annotatedType, context, next)
        }
        val type = extractJavaType(annotatedType)

        if (type.isTypeOrSubTypeOf(Instant::class.java) && !_mapper.isEnabled(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)) {
            return DateTimeSchema()
        }

        return super.resolve(annotatedType, context, next)
    }

    private fun extractJavaType(annotatedType: AnnotatedType): JavaType {
        return if (annotatedType.type is JavaType) {
            annotatedType.type as JavaType
        } else {
            this._mapper.constructType(annotatedType.type)
        }
    }
}

