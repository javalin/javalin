package io.javalin

import io.javalin.json.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.io.InputStream
import java.lang.reflect.Type

class TestJsonMapperExtensions {
    data class TestData(val name: String, val value: Int)

    @Test
    fun `toJsonString reified works`() {
        val mapper = object : JsonMapper {
            override fun toJsonString(obj: Any, type: Type) = "{\"type\":\"$type\",\"obj\":\"$obj\"}"
        }
        assertThat(mapper.toJsonString(TestData("test", 42))).contains("TestData", "test")
    }

    @Test
    fun `toJsonStream reified works`() {
        val mapper = object : JsonMapper {
            override fun toJsonStream(obj: Any, type: Type) = "{\"type\":\"$type\"}".byteInputStream()
        }
        assertThat(mapper.toJsonStream(TestData("x", 1)).readBytes().decodeToString()).contains("TestData")
    }

    @Test
    fun `fromJsonString reified works`() {
        val mapper = object : JsonMapper {
            override fun <T : Any> fromJsonString(json: String, targetType: Type) = TestData("parsed", 999) as T
        }
        val result: TestData = mapper.fromJsonString("{}")
        assertThat(result.name).isEqualTo("parsed")
    }

    @Test
    fun `fromJsonStream reified works`() {
        val mapper = object : JsonMapper {
            override fun <T : Any> fromJsonStream(json: InputStream, targetType: Type) = TestData("parsed", 777) as T
        }
        val result: TestData = mapper.fromJsonStream("{}".byteInputStream())
        assertThat(result.value).isEqualTo(777)
    }

    @Test
    fun `extensions preserve type information`() {
        var captured: Type? = null
        val mapper = object : JsonMapper {
            override fun toJsonString(obj: Any, type: Type) = "".also { captured = type }
            override fun toJsonStream(obj: Any, type: Type) = "".byteInputStream().also { captured = type }
            override fun <T : Any> fromJsonString(json: String, targetType: Type): T {
                captured = targetType
                @Suppress("UNCHECKED_CAST")
                return listOf<String>() as T
            }
            override fun <T : Any> fromJsonStream(json: InputStream, targetType: Type): T {
                captured = targetType
                @Suppress("UNCHECKED_CAST")
                return mapOf<String, String>() as T
            }
        }
        
        mapper.toJsonString(listOf("a"))
        assertThat(captured.toString()).contains("List")
        
        mapper.toJsonStream(mapOf("k" to "v"))
        assertThat(captured.toString()).contains("Map")
        
        val l: List<String> = mapper.fromJsonString("{}")
        assertThat(captured.toString()).contains("List")
        
        val m: Map<String, String> = mapper.fromJsonStream("{}".byteInputStream())
        assertThat(captured.toString()).contains("Map")
    }
}
