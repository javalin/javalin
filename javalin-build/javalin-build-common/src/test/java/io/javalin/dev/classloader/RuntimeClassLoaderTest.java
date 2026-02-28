package io.javalin.dev.classloader;

import io.javalin.dev.testutil.ClassFileCompiler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.URL;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RuntimeClassLoaderTest {

    @TempDir
    Path tempDir;

    @Test
    void childFirst_loadsFromOwnUrlsBeforeParent() throws Exception {
        // Compile same class name in two different directories with different markers
        Path parentDir = tempDir.resolve("parent");
        Path childDir = tempDir.resolve("child");

        ClassFileCompiler.compile(parentDir, "Marker", """
            public class Marker {
                public static String value() { return "parent"; }
            }
            """);
        ClassFileCompiler.compile(childDir, "Marker", """
            public class Marker {
                public static String value() { return "child"; }
            }
            """);

        try (var parent = new BaseClassLoader(new URL[]{parentDir.toUri().toURL()});
             var child = new RuntimeClassLoader(new URL[]{childDir.toUri().toURL()}, parent)) {
            Class<?> clazz = child.loadClass("Marker");
            String value = (String) clazz.getMethod("value").invoke(null);
            assertThat(value).isEqualTo("child");
            assertThat(clazz.getClassLoader()).isSameAs(child);
        }
    }

    @Test
    void jdkClasses_delegatedToParent() throws Exception {
        try (var parent = new BaseClassLoader(new URL[0]);
             var child = new RuntimeClassLoader(new URL[0], parent)) {
            Class<?> clazz = child.loadClass("java.lang.String");
            assertThat(clazz).isSameAs(String.class);
        }
    }

    @Test
    void javaxClasses_delegatedToParent() throws Exception {
        try (var parent = new BaseClassLoader(new URL[0]);
             var child = new RuntimeClassLoader(new URL[0], parent)) {
            Class<?> clazz = child.loadClass("javax.net.ssl.SSLContext");
            assertThat(clazz).isEqualTo(javax.net.ssl.SSLContext.class);
        }
    }

    @Test
    void jdkPrefixClasses_delegated() throws Exception {
        try (var parent = new BaseClassLoader(new URL[0]);
             var child = new RuntimeClassLoader(new URL[0], parent)) {
            // jdk.internal classes are typically not accessible, but the delegation path should be exercised.
            // Use a well-known jdk.* class if available or just test delegation doesn't NPE.
            // jdk.jfr.Event is available in standard JDK distributions
            Class<?> clazz = child.loadClass("jdk.jfr.Event");
            assertThat(clazz).isNotNull();
        }
    }

    @Test
    void sunClasses_delegated() throws Exception {
        try (var parent = new BaseClassLoader(new URL[0]);
             var child = new RuntimeClassLoader(new URL[0], parent)) {
            // sun.misc.Unsafe is commonly available
            Class<?> clazz = child.loadClass("sun.nio.ch.IOUtil");
            assertThat(clazz).isNotNull();
        }
    }

    @Test
    void comSunClasses_delegated() throws Exception {
        try (var parent = new BaseClassLoader(new URL[0]);
             var child = new RuntimeClassLoader(new URL[0], parent)) {
            // com.sun classes should be delegated
            Class<?> clazz = child.loadClass("com.sun.net.httpserver.HttpServer");
            assertThat(clazz).isNotNull();
        }
    }

    @Test
    void classNotFound_throwsCNFE() throws Exception {
        try (var parent = new BaseClassLoader(new URL[0]);
             var child = new RuntimeClassLoader(new URL[0], parent)) {
            assertThatThrownBy(() -> child.loadClass("com.nonexistent.FakeClass"))
                .isInstanceOf(ClassNotFoundException.class);
        }
    }

    @Test
    void fallsBackToParent() throws Exception {
        Path parentDir = tempDir.resolve("parent-only");
        ClassFileCompiler.compile(parentDir, "ParentOnly", """
            public class ParentOnly {
                public static String value() { return "from-parent"; }
            }
            """);

        try (var parent = new BaseClassLoader(new URL[]{parentDir.toUri().toURL()});
             var child = new RuntimeClassLoader(new URL[0], parent)) {
            Class<?> clazz = child.loadClass("ParentOnly");
            String value = (String) clazz.getMethod("value").invoke(null);
            assertThat(value).isEqualTo("from-parent");
        }
    }

    @Test
    void alreadyLoaded_returnsCached() throws Exception {
        Path childDir = tempDir.resolve("cached");
        ClassFileCompiler.compile(childDir, "CachedClass", """
            public class CachedClass {}
            """);

        try (var parent = new BaseClassLoader(new URL[0]);
             var child = new RuntimeClassLoader(new URL[]{childDir.toUri().toURL()}, parent)) {
            Class<?> first = child.loadClass("CachedClass");
            Class<?> second = child.loadClass("CachedClass");
            assertThat(first).isSameAs(second);
        }
    }

    @Test
    void close_releasesResources() throws Exception {
        var parent = new BaseClassLoader(new URL[0]);
        var child = new RuntimeClassLoader(new URL[0], parent);
        child.close(); // Should not throw
        parent.close();
    }
}
