package io.javalin

import io.javalin.testing.TestUtil
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class TestTaskObserver {

    @Test
    fun `observers see the pipeline tasks in order, with timing`() = TestUtil.test { app, http ->
        val seen = mutableListOf<String>()
        val durations = mutableListOf<Long>()
        app.unsafe.servletTaskObservers.add { _, task, durationNanos, _ ->
            seen.add(task.label)
            durations.add(durationNanos)
        }
        app.unsafe.routes.before("/*") {}
        app.unsafe.routes.get("/hello") { it.result("hi") }
        app.unsafe.routes.after("/*") {}

        http.get("/hello")
        assertThat(seen).containsSubsequence("before /*", "GET /hello", "after /*")
        assertThat(durations).isNotEmpty.allMatch { it >= 0 }
    }

    @Test
    fun `observers are told which task threw, and see the exception-handling task`() = TestUtil.test { app, http ->
        var thrownLabel: String? = null
        val labels = mutableListOf<String>()
        app.unsafe.servletTaskObservers.add { _, task, _, throwable ->
            labels.add(task.label)
            if (throwable != null) thrownLabel = task.label
        }
        app.unsafe.routes.get("/boom") { throw IllegalStateException("nope") }

        http.get("/boom")
        assertThat(thrownLabel).isEqualTo("GET /boom")
        assertThat(labels).contains("exception:IllegalStateException")
    }

    @Test
    fun `all registered observers are notified`() = TestUtil.test { app, http ->
        val a = mutableListOf<String>()
        val b = mutableListOf<String>()
        app.unsafe.servletTaskObservers.add { _, task, _, _ -> a.add(task.label) }
        app.unsafe.servletTaskObservers.add { _, task, _, _ -> b.add(task.label) }
        app.unsafe.routes.get("/hi") { it.result("hi") }

        http.get("/hi")
        assertThat(a).contains("GET /hi")
        assertThat(b).contains("GET /hi")
    }

    @Test
    fun `a throwing observer does not break the request`() = TestUtil.test { app, http ->
        app.unsafe.servletTaskObservers.add { _, _, _, _ -> throw RuntimeException("observer bug") }
        app.unsafe.routes.get("/ok") { it.result("still works") }

        val res = http.get("/ok")
        assertThat(res.status).isEqualTo(200)
        assertThat(res.body).isEqualTo("still works")
    }

}
