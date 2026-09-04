package io.javalin.dev.main;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ApplicationMainClassCandidateTest {

    @Test
    void accessors() {
        var candidate = new ApplicationMainClassCandidate("com.example.App", ApplicationMainClassType.STATIC_MAIN_WITH_ARGS);
        assertThat(candidate.className()).isEqualTo("com.example.App");
        assertThat(candidate.type()).isEqualTo(ApplicationMainClassType.STATIC_MAIN_WITH_ARGS);
    }

    @Test
    void equalsAndHashCode() {
        var a = new ApplicationMainClassCandidate("com.example.App", ApplicationMainClassType.STATIC_MAIN_WITH_ARGS);
        var b = new ApplicationMainClassCandidate("com.example.App", ApplicationMainClassType.STATIC_MAIN_WITH_ARGS);
        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    void notEqual_differentClassName() {
        var a = new ApplicationMainClassCandidate("com.example.A", ApplicationMainClassType.STATIC_MAIN_WITH_ARGS);
        var b = new ApplicationMainClassCandidate("com.example.B", ApplicationMainClassType.STATIC_MAIN_WITH_ARGS);
        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void notEqual_differentType() {
        var a = new ApplicationMainClassCandidate("com.example.App", ApplicationMainClassType.STATIC_MAIN_WITH_ARGS);
        var b = new ApplicationMainClassCandidate("com.example.App", ApplicationMainClassType.STATIC_MAIN_WITHOUT_ARGS);
        assertThat(a).isNotEqualTo(b);
    }
}
