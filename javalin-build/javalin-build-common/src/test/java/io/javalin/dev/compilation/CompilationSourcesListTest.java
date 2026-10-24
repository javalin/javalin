package io.javalin.dev.compilation;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CompilationSourcesListTest {

    @Test
    void isEmpty_trueWhenAllEmpty() {
        var list = new CompilationSourcesList(List.of(), List.of(), List.of());
        assertThat(list.isEmpty()).isTrue();
    }

    @Test
    void isEmpty_falseWhenAddedNonEmpty() {
        var list = new CompilationSourcesList(List.of(Path.of("a.java")), List.of(), List.of());
        assertThat(list.isEmpty()).isFalse();
    }

    @Test
    void isEmpty_falseWhenModifiedNonEmpty() {
        var list = new CompilationSourcesList(List.of(), List.of(Path.of("b.java")), List.of());
        assertThat(list.isEmpty()).isFalse();
    }

    @Test
    void isEmpty_falseWhenDeletedNonEmpty() {
        var list = new CompilationSourcesList(List.of(), List.of(), List.of(Path.of("c.java")));
        assertThat(list.isEmpty()).isFalse();
    }

    @Test
    void added_returnsUnmodifiableList() {
        var list = new CompilationSourcesList(new ArrayList<>(List.of(Path.of("a.java"))), List.of(), List.of());
        assertThatThrownBy(() -> list.added().add(Path.of("x.java")))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void modified_returnsUnmodifiableList() {
        var list = new CompilationSourcesList(List.of(), new ArrayList<>(List.of(Path.of("b.java"))), List.of());
        assertThatThrownBy(() -> list.modified().add(Path.of("x.java")))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void deleted_returnsUnmodifiableList() {
        var list = new CompilationSourcesList(List.of(), List.of(), new ArrayList<>(List.of(Path.of("c.java"))));
        assertThatThrownBy(() -> list.deleted().add(Path.of("x.java")))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void accessors_returnCorrectContent() {
        var a = Path.of("added.java");
        var m = Path.of("modified.java");
        var d = Path.of("deleted.java");
        var list = new CompilationSourcesList(List.of(a), List.of(m), List.of(d));
        assertThat(list.added()).containsExactly(a);
        assertThat(list.modified()).containsExactly(m);
        assertThat(list.deleted()).containsExactly(d);
    }

    @Test
    void equalsAndHashCode() {
        var a = new CompilationSourcesList(List.of(Path.of("x")), List.of(), List.of());
        var b = new CompilationSourcesList(List.of(Path.of("x")), List.of(), List.of());
        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }
}
