package io.javalin.performance

import com.carrotsearch.junitbenchmarks.BenchmarkOptions
import com.carrotsearch.junitbenchmarks.BenchmarkRule
import com.carrotsearch.junitbenchmarks.Clock
import io.javalin.core.util.Util
import io.javalin.http.Handler
import io.javalin.http.HandlerEntry
import io.javalin.http.HandlerType
import io.javalin.http.util.ContextUtil.urlDecode
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test

typealias NewHandlerEntry = HandlerEntry

@BenchmarkOptions(callgc = false, benchmarkRounds = 100000, warmupRounds = 500, concurrency = 4, clock = Clock.NANO_TIME)
class RouteMatcherPerformanceTest {

    @get:Rule
    val benchmarkRun = BenchmarkRule()

    data class OldHandlerEntry(val type: HandlerType, val path: String, val handler: Handler)

    fun oldMatch(handlerPath: String, fullRequestPath: String): Boolean {
        val hpp = Util.pathToList(handlerPath) // handler-path-parts
        val rpp = Util.pathToList(fullRequestPath) // request-path-parts

        fun isLastAndSplat(i: Int) = i == hpp.lastIndex && hpp[i] == "*"
        fun isNotPathOrSplat(i: Int) = hpp[i].first() != ':' && hpp[i] != "*"

        if (hpp.size == rpp.size) {
            for (i in hpp.indices) {
                when {
                    isLastAndSplat(i) && handlerPath.endsWith('*') -> return true
                    isNotPathOrSplat(i) && hpp[i] != rpp[i] -> return false
                }
            }
            return true
        }
        if (hpp.size < rpp.size && handlerPath.endsWith('*')) {
            for (i in hpp.indices) {
                when {
                    isLastAndSplat(i) -> return true
                    isNotPathOrSplat(i) && hpp[i] != rpp[i] -> return false
                }
            }
            return false
        }
        return false
    }

    fun oldParams(requestPaths: List<String>, handlerPaths: List<String>): Map<String, String> {
        val params = HashMap<String, String>()
        var i = 0
        while (i < requestPaths.size && i < handlerPaths.size) {
            val matchedPart = handlerPaths[i]
            if (matchedPart.startsWith(":")) {
                params[matchedPart.toLowerCase()] = urlDecode(requestPaths[i])
            }
            i++
        }
        return params
    }

    fun newMatch(entry: NewHandlerEntry, path: String) = entry.matches(path)

    fun newParams(entry: NewHandlerEntry, path: String) = entry.extractPathParams(path)

    companion object {
        val routes = listOf(
                "/test/:user/some/path/here",
                "/test/*/some/more/path/here",
                "/test/path/route/without/wildcards",
                "/test/has/wildcard/at/the/end/*",
                "/test/:id/simple/route/:user/create/",
                "/matches/all/*/user",
                "/test/*"
        )

        val oldEntries = routes.map { OldHandlerEntry(HandlerType.AFTER, it, Handler { }) }
        val newEntries = routes.map { NewHandlerEntry(HandlerType.AFTER, it, false, Handler { }, Handler { }) }

        val testEntries = listOf(
                "/test/1234/some/path/here",
                "/test/1234/some/here/path",
                "/test/3322/some/more/path/here",
                "/test/more/path/some/more/path/here",
                "/test/path/route/without/wildcards",
                "/test/path/route/without/solats",
                "/test/has/wildcard/at/the/end/for/everything",
                "/test/has/wildcard/at/the/and/something/else",
                "/test/1/simple/route/John/create/",
                "/test/2/simple/route/Lisa/create/",
                "/test/3/simple/route/Temp/create/",
                "/matches/all/that/ends/with/user/",
                "/matches/all/that/user/"
        )
    }

    @Ignore("Manual execution")
    @Test
    fun testOldMatchingPerformance() {
        testEntries.forEach { requestUri ->
            oldEntries.forEach { entry ->
                oldMatch(entry.path, requestUri)
            }
        }
    }

    @Ignore("Manual execution")
    @Test
    fun testNewMatchingPerformance() {
        testEntries.forEach { requestUri ->
            newEntries.forEach { entry ->
                newMatch(entry, requestUri)
            }
        }
    }

    @Ignore("Manual execution")
    @Test
    fun testOldParamAndSplatPerformance() {
        testEntries.forEach { requestUri ->
            oldEntries.forEach { entry ->
                val requestList = Util.pathToList(requestUri)
                val entryList = Util.pathToList(entry.path)
                oldParams(requestList, entryList)
            }
        }
    }

    @Ignore("Manual execution")
    @Test
    fun testNewParamAndSplatPerformance() {
        testEntries.forEach { requestUri ->
            newEntries.forEach { entry ->
                newParams(entry, requestUri)
            }
        }
    }
}
