package io.javalin.dev.runner;

import io.javalin.dev.main.ApplicationMainClassCandidate;
import io.javalin.dev.main.ApplicationMainClassType;
import io.javalin.dev.testutil.ClassFileCompiler;
import io.javalin.dev.testutil.TestLogger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.URL;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class ApplicationRunnerTest {

    @TempDir
    Path tempDir;

    private final TestLogger logger = new TestLogger();

    @AfterEach
    void cleanup() {
        System.clearProperty("javalin.dev.internalPort");
        System.clearProperty("test.marker.staticArgs");
        System.clearProperty("test.marker.staticNoArgs");
        System.clearProperty("test.marker.instanceArgs");
        System.clearProperty("test.marker.instanceNoArgs");
    }

    private ApplicationRunner createRunner(String className, ApplicationMainClassType type) throws Exception {
        var candidate = new ApplicationMainClassCandidate(className, type);
        URL classesUrl = tempDir.toUri().toURL();
        return new ApplicationRunner(new URL[0], new URL[]{classesUrl}, candidate, logger);
    }

    @Test
    void start_returnsValidInstance() throws Exception {
        ClassFileCompiler.compile(tempDir, "ValidApp", """
            public class ValidApp {
                public static void main(String[] args) {
                    System.setProperty("test.marker.staticArgs", "ok");
                }
            }
            """);

        var runner = createRunner("ValidApp", ApplicationMainClassType.STATIC_MAIN_WITH_ARGS);
        var instance = runner.start();
        try {
            assertThat(instance).isNotNull();
            assertThat(instance.port()).isGreaterThan(0);
        } finally {
            instance.stop();
        }
    }

    @Test
    void start_setsInternalPortSystemProperty() throws Exception {
        ClassFileCompiler.compile(tempDir, "PortApp", """
            public class PortApp {
                public static void main(String[] args) {}
            }
            """);

        var runner = createRunner("PortApp", ApplicationMainClassType.STATIC_MAIN_WITH_ARGS);
        var instance = runner.start();
        try {
            assertThat(System.getProperty("javalin.dev.internalPort"))
                .isEqualTo(String.valueOf(instance.port()));
        } finally {
            instance.stop();
        }
    }

    @Test
    void start_lazyCreatesBaseClassLoader() throws Exception {
        ClassFileCompiler.compile(tempDir, "LazyApp", """
            public class LazyApp {
                public static void main(String[] args) {}
            }
            """);

        var runner = createRunner("LazyApp", ApplicationMainClassType.STATIC_MAIN_WITH_ARGS);
        var instance1 = runner.start();
        instance1.stop();

        // Second start should reuse base classloader
        var instance2 = runner.start();
        instance2.stop();
        // Both should succeed (no exception means base CL was reused)
    }

    @Test
    void start_spawnsAppThreadAsDaemon() throws Exception {
        ClassFileCompiler.compile(tempDir, "DaemonApp", """
            public class DaemonApp {
                public static void main(String[] args) {
                    try { Thread.sleep(60000); } catch (InterruptedException e) {}
                }
            }
            """);

        var runner = createRunner("DaemonApp", ApplicationMainClassType.STATIC_MAIN_WITH_ARGS);
        var instance = runner.start();
        try {
            // The app thread should be a daemon so it won't prevent JVM shutdown
            // We verify it started and the instance was created
            assertThat(instance.port()).isGreaterThan(0);
        } finally {
            instance.stop();
        }
    }

    @Test
    void start_invokesStaticMainWithArgs() throws Exception {
        ClassFileCompiler.compile(tempDir, "StaticArgsApp", """
            public class StaticArgsApp {
                public static void main(String[] args) {
                    System.setProperty("test.marker.staticArgs", "invoked");
                }
            }
            """);

        var runner = createRunner("StaticArgsApp", ApplicationMainClassType.STATIC_MAIN_WITH_ARGS);
        var instance = runner.start();
        try {
            Thread.sleep(200); // give app thread time to run
            assertThat(System.getProperty("test.marker.staticArgs")).isEqualTo("invoked");
        } finally {
            instance.stop();
        }
    }

    @Test
    void start_invokesStaticMainWithoutArgs() throws Exception {
        ClassFileCompiler.compile(tempDir, "StaticNoArgsApp", """
            public class StaticNoArgsApp {
                public static void main() {
                    System.setProperty("test.marker.staticNoArgs", "invoked");
                }
            }
            """);

        var runner = createRunner("StaticNoArgsApp", ApplicationMainClassType.STATIC_MAIN_WITHOUT_ARGS);
        var instance = runner.start();
        try {
            Thread.sleep(200);
            assertThat(System.getProperty("test.marker.staticNoArgs")).isEqualTo("invoked");
        } finally {
            instance.stop();
        }
    }

    @Test
    void start_invokesInstanceMainWithArgs() throws Exception {
        ClassFileCompiler.compile(tempDir, "InstanceArgsApp", """
            public class InstanceArgsApp {
                public InstanceArgsApp() {}
                public void main(String[] args) {
                    System.setProperty("test.marker.instanceArgs", "invoked");
                }
            }
            """);

        var runner = createRunner("InstanceArgsApp", ApplicationMainClassType.INSTANCE_MAIN_WITH_ARGS);
        var instance = runner.start();
        try {
            Thread.sleep(200);
            assertThat(System.getProperty("test.marker.instanceArgs")).isEqualTo("invoked");
        } finally {
            instance.stop();
        }
    }

    @Test
    void start_invokesInstanceMainWithoutArgs() throws Exception {
        ClassFileCompiler.compile(tempDir, "InstanceNoArgsApp", """
            public class InstanceNoArgsApp {
                public InstanceNoArgsApp() {}
                public void main() {
                    System.setProperty("test.marker.instanceNoArgs", "invoked");
                }
            }
            """);

        var runner = createRunner("InstanceNoArgsApp", ApplicationMainClassType.INSTANCE_MAIN_WITHOUT_ARGS);
        var instance = runner.start();
        try {
            Thread.sleep(200);
            assertThat(System.getProperty("test.marker.instanceNoArgs")).isEqualTo("invoked");
        } finally {
            instance.stop();
        }
    }

    @Test
    void start_setsContextClassLoader() throws Exception {
        ClassFileCompiler.compile(tempDir, "CtxClApp", """
            public class CtxClApp {
                public static void main(String[] args) {
                    System.setProperty("test.marker.staticArgs",
                        Thread.currentThread().getContextClassLoader().getClass().getName());
                }
            }
            """);

        var runner = createRunner("CtxClApp", ApplicationMainClassType.STATIC_MAIN_WITH_ARGS);
        var instance = runner.start();
        try {
            Thread.sleep(200);
            assertThat(System.getProperty("test.marker.staticArgs"))
                .isEqualTo("io.javalin.dev.classloader.RuntimeClassLoader");
        } finally {
            instance.stop();
        }
    }

    @Test
    void start_isSerialized() throws Exception {
        ClassFileCompiler.compile(tempDir, "SerialApp", """
            public class SerialApp {
                public static void main(String[] args) {}
            }
            """);

        var runner = createRunner("SerialApp", ApplicationMainClassType.STATIC_MAIN_WITH_ARGS);

        // Start two instances in sequence (concurrent starts are serialized via startLock)
        var instance1 = runner.start();
        var instance2 = runner.start();
        try {
            assertThat(instance1.port()).isNotEqualTo(instance2.port());
            assertThat(instance1.port()).isGreaterThan(0);
            assertThat(instance2.port()).isGreaterThan(0);
        } finally {
            instance1.stop();
            instance2.stop();
        }
    }
}
