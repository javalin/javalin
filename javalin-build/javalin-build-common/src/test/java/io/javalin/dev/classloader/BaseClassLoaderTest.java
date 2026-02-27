package io.javalin.dev.classloader;

import io.javalin.dev.testutil.ClassFileCompiler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.URL;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BaseClassLoaderTest {

    @TempDir
    Path tempDir;

    @Test
    void parent_isPlatformClassLoader() throws Exception {
        try (var cl = new BaseClassLoader(new URL[0])) {
            assertThat(cl.getParent()).isSameAs(ClassLoader.getPlatformClassLoader());
        }
    }

    @Test
    void loadsClassFromProvidedUrl() throws Exception {
        ClassFileCompiler.compile(tempDir, "BaseTest", """
            public class BaseTest {
                public static String marker() { return "from-base"; }
            }
            """);

        try (var cl = new BaseClassLoader(new URL[]{tempDir.toUri().toURL()})) {
            Class<?> clazz = cl.loadClass("BaseTest");
            assertThat(clazz.getClassLoader()).isSameAs(cl);
        }
    }

    @Test
    void cannotLoadAppClasspath() throws Exception {
        try (var cl = new BaseClassLoader(new URL[0])) {
            // org.junit.jupiter.api.Test is on the test classpath but NOT provided to BaseClassLoader.
            // BaseClassLoader's parent is PlatformClassLoader which doesn't have JUnit.
            assertThatThrownBy(() -> cl.loadClass("org.junit.jupiter.api.Test"))
                .isInstanceOf(ClassNotFoundException.class);
        }
    }

    @Test
    void loadsJdkClasses() throws Exception {
        try (var cl = new BaseClassLoader(new URL[0])) {
            Class<?> clazz = cl.loadClass("java.util.ArrayList");
            assertThat(clazz).isEqualTo(java.util.ArrayList.class);
        }
    }

    @Test
    void close_releasesResources() throws Exception {
        var cl = new BaseClassLoader(new URL[0]);
        cl.close(); // Should not throw
    }
}
