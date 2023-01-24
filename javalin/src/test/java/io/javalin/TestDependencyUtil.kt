package io.javalin

import io.javalin.util.CoreDependency
import io.javalin.util.DependencyUtil
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class TestDependencyUtil {

    @Test
    fun `DependencyUtil#missingDependencyMessage works`() {
        val message = DependencyUtil.missingDependencyMessage(CoreDependency.JACKSON)
        assertThat(message).contains("You're missing the 'Jackson' dependency in your project. Add the dependency:")
    }

    @Test
    fun `DependencyUtil#mavenAndGradleSnippets works`() {
        val dependency = CoreDependency.JACKSON
        val message = DependencyUtil.mavenAndGradleSnippets(dependency)
        assertThat(message).contains("pom.xml:")
        assertThat(message).contains("build.gradle or build.gradle.kts:")
        assertThat(message).contains("""implementation("${dependency.groupId}:${dependency.artifactId}:${dependency.version}")""")
    }

}
