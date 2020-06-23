package io.javalin.plugin.tracing

import io.jaegertracing.Configuration
import io.jaegertracing.internal.JaegerTracer
import io.jaegertracing.internal.samplers.ProbabilisticSampler
import io.javalin.Javalin
import io.javalin.core.plugin.Plugin
import io.opentracing.util.GlobalTracer

class JaegerPlugin @JvmOverloads constructor(
        private val tracer: JaegerTracer = Configuration("javalin-app")
                .tracerBuilder
                .withSampler(ProbabilisticSampler(0.8))
                .build()
) : Plugin {
    override fun apply(app: Javalin) {
        // register tracer as global tracer
        GlobalTracer.registerIfAbsent(tracer)
        app.before {
            // start root span
            val span = tracer.buildSpan("root-span")
                    .withTag("method", it.method())
                    .withTag("path", it.path())
                    .start()
            // set as active span in scope manager
            tracer.scopeManager().activate(span)
        }
        // finish root span
        app.after { tracer.scopeManager().activeSpan().finish() }

        // in case of exception mark active span as error - and log the exception message
        // note this won't get triggered if the application also defines an exception handler
        app.exception(Exception::class.java) { e, _ ->
            tracer.scopeManager().activeSpan().setTag("error", true).log(e.message).finish()
        }
    }
}
