package io.javalin.dev.main;

import io.javalin.dev.testutil.ClassFileCompiler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.objectweb.asm.ClassReader;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class MainMethodAsmVisitorTest {

    @TempDir
    Path tempDir;

    private Optional<ApplicationMainClassType> scan(String className, String source) throws Exception {
        ClassFileCompiler.compile(tempDir, className, source);
        Path classFile = tempDir.resolve(className.replace('.', '/') + ".class");
        try (InputStream in = Files.newInputStream(classFile)) {
            ClassReader cr = new ClassReader(in);
            MainMethodAsmVisitor visitor = new MainMethodAsmVisitor();
            cr.accept(visitor, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            return visitor.result();
        }
    }

    @Test
    void detectsStaticMainWithArgs() throws Exception {
        var result = scan("StaticMainArgs", """
            public class StaticMainArgs {
                public static void main(String[] args) {}
            }
            """);
        assertThat(result).contains(ApplicationMainClassType.STATIC_MAIN_WITH_ARGS);
    }

    @Test
    void detectsStaticMainWithoutArgs() throws Exception {
        var result = scan("StaticMainNoArgs", """
            public class StaticMainNoArgs {
                public static void main() {}
            }
            """);
        assertThat(result).contains(ApplicationMainClassType.STATIC_MAIN_WITHOUT_ARGS);
    }

    @Test
    void detectsInstanceMainWithArgs() throws Exception {
        var result = scan("InstanceMainArgs", """
            public class InstanceMainArgs {
                public InstanceMainArgs() {}
                public void main(String[] args) {}
            }
            """);
        assertThat(result).contains(ApplicationMainClassType.INSTANCE_MAIN_WITH_ARGS);
    }

    @Test
    void detectsInstanceMainWithoutArgs() throws Exception {
        var result = scan("InstanceMainNoArgs", """
            public class InstanceMainNoArgs {
                public InstanceMainNoArgs() {}
                public void main() {}
            }
            """);
        assertThat(result).contains(ApplicationMainClassType.INSTANCE_MAIN_WITHOUT_ARGS);
    }

    @Test
    void instanceMainWithArgs_privateCtor_notDetected() throws Exception {
        var result = scan("PrivateCtorArgs", """
            public class PrivateCtorArgs {
                private PrivateCtorArgs() {}
                public void main(String[] args) {}
            }
            """);
        assertThat(result).isEmpty();
    }

    @Test
    void instanceMainNoArgs_privateCtor_notDetected() throws Exception {
        var result = scan("PrivateCtorNoArgs", """
            public class PrivateCtorNoArgs {
                private PrivateCtorNoArgs() {}
                public void main() {}
            }
            """);
        assertThat(result).isEmpty();
    }

    @Test
    void instanceMainWithArgs_protectedCtor_detected() throws Exception {
        var result = scan("ProtectedCtor", """
            public class ProtectedCtor {
                protected ProtectedCtor() {}
                public void main(String[] args) {}
            }
            """);
        assertThat(result).contains(ApplicationMainClassType.INSTANCE_MAIN_WITH_ARGS);
    }

    @Test
    void instanceMainWithArgs_packagePrivateCtor_detected() throws Exception {
        var result = scan("PkgPrivateCtor", """
            public class PkgPrivateCtor {
                PkgPrivateCtor() {}
                public void main(String[] args) {}
            }
            """);
        assertThat(result).contains(ApplicationMainClassType.INSTANCE_MAIN_WITH_ARGS);
    }

    @Test
    void staticWithArgs_priorityOverStaticNoArgs() throws Exception {
        var result = scan("BothStatic", """
            public class BothStatic {
                public static void main(String[] args) {}
                public static void main() {}
            }
            """);
        assertThat(result).contains(ApplicationMainClassType.STATIC_MAIN_WITH_ARGS);
    }

    @Test
    void staticWithArgs_priorityOverInstanceWithArgs() throws Exception {
        var result = scan("StaticVsInstance", """
            public class StaticVsInstance {
                public StaticVsInstance() {}
                public static void main(String[] args) {}
                public void main(String[] args) { /* won't compile with same sig */ }
            }
            """.replace("public void main(String[] args) { /* won't compile with same sig */ }",
                "// instance main with args not compilable with same signature as static"));
        // Since we can't have both static and instance with same signature, test with different combos
        var result2 = scan("StaticArgsVsInstanceNoArgs", """
            public class StaticArgsVsInstanceNoArgs {
                public StaticArgsVsInstanceNoArgs() {}
                public static void main(String[] args) {}
                public void main() {}
            }
            """);
        assertThat(result2).contains(ApplicationMainClassType.STATIC_MAIN_WITH_ARGS);
    }

    @Test
    void staticNoArgs_priorityOverInstanceWithArgs() throws Exception {
        var result = scan("StaticNoArgsVsInstanceArgs", """
            public class StaticNoArgsVsInstanceArgs {
                public StaticNoArgsVsInstanceArgs() {}
                public static void main() {}
                public void main(String[] args) {}
            }
            """);
        assertThat(result).contains(ApplicationMainClassType.STATIC_MAIN_WITHOUT_ARGS);
    }

    @Test
    void instanceWithArgs_priorityOverInstanceNoArgs() throws Exception {
        var result = scan("InstanceArgsPriority", """
            public class InstanceArgsPriority {
                public InstanceArgsPriority() {}
                public void main(String[] args) {}
                public void main() {}
            }
            """);
        assertThat(result).contains(ApplicationMainClassType.INSTANCE_MAIN_WITH_ARGS);
    }

    @Test
    void noMainMethod_returnsEmpty() throws Exception {
        var result = scan("NoMain", """
            public class NoMain {
                public void notMain() {}
            }
            """);
        assertThat(result).isEmpty();
    }

    @Test
    void privateMainMethod_notDetected() throws Exception {
        var result = scan("PrivateMain", """
            public class PrivateMain {
                private static void main(String[] args) {}
            }
            """);
        assertThat(result).isEmpty();
    }

    @Test
    void mainWithWrongReturnType_notDetected() throws Exception {
        var result = scan("WrongReturn", """
            public class WrongReturn {
                public static int main(String[] args) { return 0; }
            }
            """);
        assertThat(result).isEmpty();
    }

    @Test
    void mainWithExtraParams_notDetected() throws Exception {
        var result = scan("ExtraParams", """
            public class ExtraParams {
                public static void main(String[] args, int extra) {}
            }
            """);
        assertThat(result).isEmpty();
    }

    @Test
    void classWithOnlyConstructor_returnsEmpty() throws Exception {
        var result = scan("OnlyCtor", """
            public class OnlyCtor {
                public OnlyCtor() {}
            }
            """);
        assertThat(result).isEmpty();
    }
}
