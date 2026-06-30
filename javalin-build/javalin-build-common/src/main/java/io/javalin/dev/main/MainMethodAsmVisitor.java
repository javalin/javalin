package io.javalin.dev.main;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.Optional;

class MainMethodAsmVisitor extends ClassVisitor {
    private ApplicationMainClassType result;

    private boolean hasStaticMainWithArgs;
    private boolean hasStaticMainNoArgs;
    private boolean hasInstanceMainWithArgs;
    private boolean hasInstanceMainNoArgs;
    private boolean hasValidConstructor;

    MainMethodAsmVisitor() {
        super(Opcodes.ASM9);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor,
                                     String signature, String[] exceptions) {
        // Check for non-private zero-arg constructor
        if ("<init>".equals(name) && "()V".equals(descriptor)
            && (access & Opcodes.ACC_PRIVATE) == 0) {
            hasValidConstructor = true;
        }

        // Check for main methods (non-private, void return)
        if ("main".equals(name) && (access & Opcodes.ACC_PRIVATE) == 0) {
            boolean isStatic = (access & Opcodes.ACC_STATIC) != 0;
            if ("([Ljava/lang/String;)V".equals(descriptor)) {
                if (isStatic) {
                    hasStaticMainWithArgs = true;
                } else {
                    hasInstanceMainWithArgs = true;
                }
            } else if ("()V".equals(descriptor)) {
                if (isStatic) {
                    hasStaticMainNoArgs = true;
                } else {
                    hasInstanceMainNoArgs = true;
                }
            }
        }

        return null;
    }

    @Override
    public void visitEnd() {
        if (hasStaticMainWithArgs) {
            result = ApplicationMainClassType.STATIC_MAIN_WITH_ARGS;
        } else if (hasStaticMainNoArgs) {
            result = ApplicationMainClassType.STATIC_MAIN_WITHOUT_ARGS;
        } else if (hasInstanceMainWithArgs && hasValidConstructor) {
            result = ApplicationMainClassType.INSTANCE_MAIN_WITH_ARGS;
        } else if (hasInstanceMainNoArgs && hasValidConstructor) {
            result = ApplicationMainClassType.INSTANCE_MAIN_WITHOUT_ARGS;
        }
    }

    public Optional<ApplicationMainClassType> result() {
        return Optional.ofNullable(result);
    }
}
