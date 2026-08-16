package io.javalin.dev.main;

import io.javalin.dev.testutil.ClassFileCompiler;
import io.javalin.dev.testutil.TestLogger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ApplicationMainClassScannerTest {

    @TempDir
    Path tempDir;

    private final TestLogger logger = new TestLogger();
    private final ApplicationMainClassScanner scanner = new ApplicationMainClassScanner(logger);

    @Test
    void scanDirectory_findsStaticMainWithArgs() throws Exception {
        ClassFileCompiler.compile(tempDir, "App", """
            public class App {
                public static void main(String[] args) {}
            }
            """);

        var result = scanner.scanDirectory(tempDir);
        assertThat(result).containsKey(ApplicationMainClassType.STATIC_MAIN_WITH_ARGS);
        assertThat(result.get(ApplicationMainClassType.STATIC_MAIN_WITH_ARGS))
            .extracting(ApplicationMainClassCandidate::className)
            .contains("App");
    }

    @Test
    void scanDirectory_findsMultipleCandidates() throws Exception {
        ClassFileCompiler.compile(tempDir, "StaticApp", """
            public class StaticApp {
                public static void main(String[] args) {}
            }
            """);
        ClassFileCompiler.compile(tempDir, "InstanceApp", """
            public class InstanceApp {
                public InstanceApp() {}
                public void main(String[] args) {}
            }
            """);

        var result = scanner.scanDirectory(tempDir);
        assertThat(result).containsKey(ApplicationMainClassType.STATIC_MAIN_WITH_ARGS);
        assertThat(result).containsKey(ApplicationMainClassType.INSTANCE_MAIN_WITH_ARGS);
    }

    @Test
    void scanDirectory_groupsByType() throws Exception {
        ClassFileCompiler.compile(tempDir, "App1", """
            public class App1 {
                public static void main(String[] args) {}
            }
            """);
        ClassFileCompiler.compile(tempDir, "App2", """
            public class App2 {
                public static void main(String[] args) {}
            }
            """);

        var result = scanner.scanDirectory(tempDir);
        assertThat(result.get(ApplicationMainClassType.STATIC_MAIN_WITH_ARGS)).hasSize(2);
    }

    @Test
    void scanDirectory_nonExistentDir_emptyMap() throws Exception {
        var result = scanner.scanDirectory(tempDir.resolve("nonexistent"));
        assertThat(result).isEmpty();
        assertThat(logger.hasMessage("warn", "does not exist")).isTrue();
    }

    @Test
    void scanDirectory_emptyDir_emptyMap() throws Exception {
        var emptyDir = Files.createDirectory(tempDir.resolve("empty"));
        var result = scanner.scanDirectory(emptyDir);
        assertThat(result).isEmpty();
    }

    @Test
    void scanDirectory_nonClassFiles_ignored() throws Exception {
        Files.writeString(tempDir.resolve("readme.txt"), "hello");
        Files.writeString(tempDir.resolve("App.java"), "public class App {}");

        var result = scanner.scanDirectory(tempDir);
        assertThat(result).isEmpty();
    }

    @Test
    void scanDirectory_classWithoutMain_excluded() throws Exception {
        ClassFileCompiler.compile(tempDir, "NoMain", """
            public class NoMain {
                public void doStuff() {}
            }
            """);

        var result = scanner.scanDirectory(tempDir);
        assertThat(result).isEmpty();
    }

    @Test
    void scanDirectory_returnsUnmodifiableMap() throws Exception {
        ClassFileCompiler.compile(tempDir, "App", """
            public class App {
                public static void main(String[] args) {}
            }
            """);

        var result = scanner.scanDirectory(tempDir);
        assertThatThrownBy(() -> result.put(ApplicationMainClassType.STATIC_MAIN_WITHOUT_ARGS, List.of()))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void scanFile_classFile_returnsCandidate() throws Exception {
        ClassFileCompiler.compile(tempDir, "FileApp", """
            public class FileApp {
                public static void main(String[] args) {}
            }
            """);

        var result = scanner.scanFile(tempDir.resolve("FileApp.class"));
        assertThat(result).isPresent();
        assertThat(result.get().className()).isEqualTo("FileApp");
        assertThat(result.get().type()).isEqualTo(ApplicationMainClassType.STATIC_MAIN_WITH_ARGS);
    }

    @Test
    void scanFile_nonClassFile_returnsEmpty() throws Exception {
        Files.writeString(tempDir.resolve("readme.txt"), "hello");
        var result = scanner.scanFile(tempDir.resolve("readme.txt"));
        assertThat(result).isEmpty();
    }

    @Test
    void scanFile_directory_returnsEmpty() throws Exception {
        var subdir = Files.createDirectory(tempDir.resolve("subdir"));
        var result = scanner.scanFile(subdir);
        assertThat(result).isEmpty();
    }

    @Test
    void scanFile_classWithoutMain_returnsEmpty() throws Exception {
        ClassFileCompiler.compile(tempDir, "NoMainFile", """
            public class NoMainFile {
                public void doStuff() {}
            }
            """);

        var result = scanner.scanFile(tempDir.resolve("NoMainFile.class"));
        assertThat(result).isEmpty();
    }

    @Test
    void scanDirectory_nestedPackages_correctClassNames() throws Exception {
        ClassFileCompiler.compile(tempDir, "com.example.App", """
            package com.example;
            public class App {
                public static void main(String[] args) {}
            }
            """);

        var result = scanner.scanDirectory(tempDir);
        assertThat(result.get(ApplicationMainClassType.STATIC_MAIN_WITH_ARGS))
            .extracting(ApplicationMainClassCandidate::className)
            .contains("com.example.App");
    }

    @Test
    void scanDirectory_logsCorrectCounts() throws Exception {
        ClassFileCompiler.compile(tempDir, "LogApp", """
            public class LogApp {
                public static void main(String[] args) {}
            }
            """);

        scanner.scanDirectory(tempDir);
        assertThat(logger.hasMessage("info", "1 candidate(s)")).isTrue();
    }
}
