package io.javalin.dev.testutil;

import javax.tools.*;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class ClassFileCompiler {

    private ClassFileCompiler() {}

    public static Path compile(Path outputDir, String className, String sourceCode) throws IOException {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new IllegalStateException("No JavaCompiler available - run tests with a JDK, not a JRE");
        }

        Files.createDirectories(outputDir);

        try (StandardJavaFileManager fm = compiler.getStandardFileManager(null, null, null)) {
            fm.setLocation(StandardLocation.CLASS_OUTPUT, List.of(outputDir.toFile()));

            JavaFileObject source = new SimpleJavaFileObject(
                URI.create("string:///" + className.replace('.', '/') + ".java"),
                JavaFileObject.Kind.SOURCE
            ) {
                @Override
                public CharSequence getCharContent(boolean ignoreEncodingErrors) {
                    return sourceCode;
                }
            };

            DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
            JavaCompiler.CompilationTask task = compiler.getTask(null, fm, diagnostics, null, null, List.of(source));

            if (!task.call()) {
                StringBuilder sb = new StringBuilder("Compilation failed:\n");
                for (Diagnostic<? extends JavaFileObject> d : diagnostics.getDiagnostics()) {
                    sb.append(d.toString()).append('\n');
                }
                throw new RuntimeException(sb.toString());
            }
        }

        return outputDir;
    }
}
