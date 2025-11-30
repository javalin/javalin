package io.javalin

import io.javalin.util.NamedThreadFactory
import io.javalin.util.NamedVirtualThreadFactory
import io.javalin.util.Util
import io.javalin.util.VirtualThreadBuilder
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledForJreRange
import org.junit.jupiter.api.condition.JRE
import java.io.ByteArrayInputStream

class TestUtil {
    @Test
    fun `normalizeContextPath works`() {
        assertThat(Util.normalizeContextPath("/")).isEqualTo("/")
        assertThat(Util.normalizeContextPath("api")).isEqualTo("/api")
        assertThat(Util.normalizeContextPath("/api/")).isEqualTo("/api")
        assertThat(Util.normalizeContextPath("//api//v1//")).isEqualTo("/api/v1")
    }

    @Test
    fun `prefixContextPath works`() {
        assertThat(Util.prefixContextPath("/api", "*")).isEqualTo("*")
        assertThat(Util.prefixContextPath("", "users")).isEqualTo("/users")
        assertThat(Util.prefixContextPath("/api", "users")).isEqualTo("/api/users")
        assertThat(Util.prefixContextPath("//api", "//users")).isEqualTo("/api/users")
    }

    @Test
    fun `classExists works`() {
        assertThat(Util.classExists("java.lang.String")).isTrue()
        assertThat(Util.classExists("com.example.NonExistent")).isFalse()
    }

    @Test
    fun `checksumAndReset works`() {
        val data = "test".toByteArray()
        val stream = ByteArrayInputStream(data)
        val checksum = Util.checksumAndReset(stream)
        assertThat(checksum).isNotEmpty()
        assertThat(stream.readBytes()).isEqualTo(data)
    }

    @Test
    fun `checksumAndReset is consistent`() {
        val data = "test".toByteArray()
        assertThat(Util.checksumAndReset(ByteArrayInputStream(data)))
            .isEqualTo(Util.checksumAndReset(ByteArrayInputStream(data)))
    }

    @Test
    fun `port extracts from exception`() {
        assertThat(Util.port(Exception("Failed to bind to 0.0.0.0:8080"))).isEqualTo("8080")
    }

    @Test
    fun `findByClass finds exact and superclass matches`() {
        val map = mapOf<Class<out Exception>, String>(RuntimeException::class.java to "Runtime")
        assertThat(Util.findByClass(map, RuntimeException::class.java)).isEqualTo("Runtime")
        assertThat(Util.findByClass(map, IllegalArgumentException::class.java)).isEqualTo("Runtime")
        assertThat(Util.findByClass(map, java.io.IOException::class.java)).isNull()
    }

    @Test
    fun `createLazy works`() {
        var count = 0
        val lazy = Util.createLazy { count++; "value" }
        assertThat(count).isEqualTo(0)
        assertThat(lazy.value).isEqualTo("value")
        assertThat(count).isEqualTo(1)
    }

    @Test
    fun `NamedThreadFactory names threads correctly`() {
        val factory = NamedThreadFactory("test")
        assertThat(factory.newThread {}.name).isEqualTo("test-0")
        assertThat(factory.newThread {}.name).isEqualTo("test-1")
        assertThat(factory.newThread {}.threadGroup).isEqualTo(Thread.currentThread().threadGroup)
    }

    @Test
    @EnabledForJreRange(min = JRE.JAVA_21)
    fun `VirtualThreadBuilder works`() {
        var executed = false
        val thread = VirtualThreadBuilder.create().name("test").unstarted { executed = true }
        assertThat(thread.name).isEqualTo("test")
        thread.start()
        thread.join()
        assertThat(executed).isTrue()
    }

    @Test
    @EnabledForJreRange(min = JRE.JAVA_21)
    fun `NamedVirtualThreadFactory works`() {
        val factory = NamedVirtualThreadFactory("virt")
        assertThat(factory.newThread {}.name).isEqualTo("virt-Virtual-0")
        assertThat(factory.newThread {}.name).isEqualTo("virt-Virtual-1")
    }
}
