package io.javalin.dev.compilation;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;


public record CompilationSourcesList(List<Path> added, List<Path> modified, List<Path> deleted) {
    @Override
    public List<Path> added() {
        return Collections.unmodifiableList(added);
    }

    @Override
    public List<Path> modified() {
        return Collections.unmodifiableList(modified);
    }

    @Override
    public List<Path> deleted() {
        return Collections.unmodifiableList(deleted);
    }

    public boolean isEmpty() {
        return added.isEmpty() && modified.isEmpty() && deleted.isEmpty();
    }
}
