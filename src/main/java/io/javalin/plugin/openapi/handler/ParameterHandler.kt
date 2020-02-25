package io.javalin.plugin.openapi.handler

import io.javalin.http.Context
import io.javalin.http.Handler
import io.javalin.plugin.openapi.handler.functional.ParameterMapper
import io.javalin.plugin.openapi.handler.functional.SerializableFunction
import io.javalin.plugin.openapi.handler.functional.SerializableFunction0
import io.javalin.plugin.openapi.handler.functional.SerializableFunction1
import io.javalin.plugin.openapi.handler.functional.SerializableFunction2
import io.javalin.plugin.openapi.handler.functional.SerializableFunction3
import io.javalin.plugin.openapi.handler.functional.SerializableFunction4
import io.javalin.plugin.openapi.handler.functional.SerializableFunction5
import io.javalin.plugin.openapi.handler.functional.SerializableFunction6
import io.javalin.plugin.openapi.handler.functional.SerializableFunction7
import io.javalin.plugin.openapi.handler.functional.SerializableFunction8
import io.javalin.plugin.openapi.handler.functional.SerializableFunction9
import io.javalin.plugin.openapi.handler.functional.SerializableFunction10
import io.javalin.plugin.openapi.handler.functional.SerializableFunction11
import io.javalin.plugin.openapi.handler.functional.SerializableFunction12
import io.javalin.plugin.openapi.handler.functional.SerializableFunction13
import io.javalin.plugin.openapi.handler.functional.SerializableFunction14
import io.javalin.plugin.openapi.handler.functional.SerializableFunction15
import io.javalin.plugin.openapi.handler.functional.SerializableFunction16
import io.javalin.plugin.openapi.handler.functional.SerializableFunction17
import io.javalin.plugin.openapi.handler.functional.SerializableFunction18
import io.javalin.plugin.openapi.handler.functional.SerializableFunction19
import io.javalin.plugin.openapi.handler.functional.SerializableFunction20
import io.javalin.plugin.openapi.handler.functional.SerializableFunction21
import io.javalin.plugin.openapi.handler.functional.SerializableFunction22
import io.javalin.plugin.openapi.handler.functional.SerializableNoRFunction0
import io.javalin.plugin.openapi.handler.functional.SerializableNoRFunction1
import io.javalin.plugin.openapi.handler.functional.SerializableNoRFunction10
import io.javalin.plugin.openapi.handler.functional.SerializableNoRFunction11
import io.javalin.plugin.openapi.handler.functional.SerializableNoRFunction12
import io.javalin.plugin.openapi.handler.functional.SerializableNoRFunction13
import io.javalin.plugin.openapi.handler.functional.SerializableNoRFunction14
import io.javalin.plugin.openapi.handler.functional.SerializableNoRFunction15
import io.javalin.plugin.openapi.handler.functional.SerializableNoRFunction16
import io.javalin.plugin.openapi.handler.functional.SerializableNoRFunction17
import io.javalin.plugin.openapi.handler.functional.SerializableNoRFunction18
import io.javalin.plugin.openapi.handler.functional.SerializableNoRFunction19
import io.javalin.plugin.openapi.handler.functional.SerializableNoRFunction2
import io.javalin.plugin.openapi.handler.functional.SerializableNoRFunction20
import io.javalin.plugin.openapi.handler.functional.SerializableNoRFunction21
import io.javalin.plugin.openapi.handler.functional.SerializableNoRFunction22
import io.javalin.plugin.openapi.handler.functional.SerializableNoRFunction3
import io.javalin.plugin.openapi.handler.functional.SerializableNoRFunction4
import io.javalin.plugin.openapi.handler.functional.SerializableNoRFunction5
import io.javalin.plugin.openapi.handler.functional.SerializableNoRFunction6
import io.javalin.plugin.openapi.handler.functional.SerializableNoRFunction7
import io.javalin.plugin.openapi.handler.functional.SerializableNoRFunction8
import io.javalin.plugin.openapi.handler.functional.SerializableNoRFunction9

class ParameterHandler<T : SerializableFunction>(
        val reference: T,
        val mapper: ParameterMapper<T>
) : Handler {
    @Throws(Exception::class)
    override fun handle(ctx: Context) {
        mapper.map(ctx, reference)
    }
}

object ParameterHandlerHelpers {
    @JvmStatic fun parameterHandler(reference: SerializableNoRFunction0, mapper: ParameterMapper<SerializableNoRFunction0>) = ParameterHandler(reference, mapper)
    @JvmStatic fun <P0> parameterHandler(reference: SerializableNoRFunction1<P0>, mapper: ParameterMapper<SerializableNoRFunction1<P0>>) = ParameterHandler(reference, mapper)
    @JvmStatic fun <P0, P1> parameterHandler(reference: SerializableNoRFunction2<P0, P1>, mapper: ParameterMapper<SerializableNoRFunction2<P0, P1>>) = ParameterHandler(reference, mapper)
    @JvmStatic fun <P0, P1, P2> parameterHandler(reference: SerializableNoRFunction3<P0, P1, P2>, mapper: ParameterMapper<SerializableNoRFunction3<P0, P1, P2>>) = ParameterHandler(reference, mapper)
    @JvmStatic fun <P0, P1, P2, P3> parameterHandler(reference: SerializableNoRFunction4<P0, P1, P2, P3>, mapper: ParameterMapper<SerializableNoRFunction4<P0, P1, P2, P3>>) = ParameterHandler(reference, mapper)
    @JvmStatic fun <P0, P1, P2, P3, P4> parameterHandler(reference: SerializableNoRFunction5<P0, P1, P2, P3, P4>, mapper: ParameterMapper<SerializableNoRFunction5<P0, P1, P2, P3, P4>>) = ParameterHandler(reference, mapper)
    @JvmStatic fun <P0, P1, P2, P3, P4, P5> parameterHandler(reference: SerializableNoRFunction6<P0, P1, P2, P3, P4, P5>, mapper: ParameterMapper<SerializableNoRFunction6<P0, P1, P2, P3, P4, P5>>) = ParameterHandler(reference, mapper)
    @JvmStatic fun <P0, P1, P2, P3, P4, P5, P6> parameterHandler(reference: SerializableNoRFunction7<P0, P1, P2, P3, P4, P5, P6>, mapper: ParameterMapper<SerializableNoRFunction7<P0, P1, P2, P3, P4, P5, P6>>) = ParameterHandler(reference, mapper)
    @JvmStatic fun <P0, P1, P2, P3, P4, P5, P6, P7> parameterHandler(reference: SerializableNoRFunction8<P0, P1, P2, P3, P4, P5, P6, P7>, mapper: ParameterMapper<SerializableNoRFunction8<P0, P1, P2, P3, P4, P5, P6, P7>>) = ParameterHandler(reference, mapper)
    @JvmStatic fun <P0, P1, P2, P3, P4, P5, P6, P7, P8> parameterHandler(reference: SerializableNoRFunction9<P0, P1, P2, P3, P4, P5, P6, P7, P8>, mapper: ParameterMapper<SerializableNoRFunction9<P0, P1, P2, P3, P4, P5, P6, P7, P8>>) = ParameterHandler(reference, mapper)
    @JvmStatic fun <P0, P1, P2, P3, P4, P5, P6, P7, P8, P9> parameterHandler(reference: SerializableNoRFunction10<P0, P1, P2, P3, P4, P5, P6, P7, P8, P9>, mapper: ParameterMapper<SerializableNoRFunction10<P0, P1, P2, P3, P4, P5, P6, P7, P8, P9>>) = ParameterHandler(reference, mapper)
    @JvmStatic fun <P0, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10> parameterHandler(reference: SerializableNoRFunction11<P0, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10>, mapper: ParameterMapper<SerializableNoRFunction11<P0, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10>>) = ParameterHandler(reference, mapper)
    @JvmStatic fun <P0, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11> parameterHandler(reference: SerializableNoRFunction12<P0, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11>, mapper: ParameterMapper<SerializableNoRFunction12<P0, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11>>) = ParameterHandler(reference, mapper)
    @JvmStatic fun <P0, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12> parameterHandler(reference: SerializableNoRFunction13<P0, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12>, mapper: ParameterMapper<SerializableNoRFunction13<P0, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12>>) = ParameterHandler(reference, mapper)
    @JvmStatic fun <P0, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13> parameterHandler(reference: SerializableNoRFunction14<P0, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13>, mapper: ParameterMapper<SerializableNoRFunction14<P0, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13>>) = ParameterHandler(reference, mapper)
    @JvmStatic fun <P0, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14> parameterHandler(reference: SerializableNoRFunction15<P0, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14>, mapper: ParameterMapper<SerializableNoRFunction15<P0, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14>>) = ParameterHandler(reference, mapper)
    @JvmStatic fun <P0, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15> parameterHandler(reference: SerializableNoRFunction16<P0, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15>, mapper: ParameterMapper<SerializableNoRFunction16<P0, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15>>) = ParameterHandler(reference, mapper)
    @JvmStatic fun <P0, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16> parameterHandler(reference: SerializableNoRFunction17<P0, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16>, mapper: ParameterMapper<SerializableNoRFunction17<P0, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16>>) = ParameterHandler(reference, mapper)
    @JvmStatic fun <P0, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17> parameterHandler(reference: SerializableNoRFunction18<P0, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17>, mapper: ParameterMapper<SerializableNoRFunction18<P0, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17>>) = ParameterHandler(reference, mapper)
    @JvmStatic fun <P0, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18> parameterHandler(reference: SerializableNoRFunction19<P0, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18>, mapper: ParameterMapper<SerializableNoRFunction19<P0, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18>>) = ParameterHandler(reference, mapper)
    @JvmStatic fun <P0, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19> parameterHandler(reference: SerializableNoRFunction20<P0, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19>, mapper: ParameterMapper<SerializableNoRFunction20<P0, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19>>) = ParameterHandler(reference, mapper)
    @JvmStatic fun <P0, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19, P20> parameterHandler(reference: SerializableNoRFunction21<P0, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19, P20>, mapper: ParameterMapper<SerializableNoRFunction21<P0, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19, P20>>) = ParameterHandler(reference, mapper)
    @JvmStatic fun <P0, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19, P20, P21> parameterHandler(reference: SerializableNoRFunction22<P0, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19, P20, P21>, mapper: ParameterMapper<SerializableNoRFunction22<P0, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19, P20, P21>>) = ParameterHandler(reference, mapper)

    @JvmStatic fun <P0> parameterHandler(reference: SerializableFunction0<P0>, mapper: ParameterMapper<SerializableFunction0<P0>>) = ParameterHandler(reference, mapper)
    @JvmStatic fun <P0, P1> parameterHandler(reference: SerializableFunction1<P0, P1>, mapper: ParameterMapper<SerializableFunction1<P0, P1>>) = ParameterHandler(reference, mapper)
    @JvmStatic fun <P0, P1, P2> parameterHandler(reference: SerializableFunction2<P0, P1, P2>, mapper: ParameterMapper<SerializableFunction2<P0, P1, P2>>) = ParameterHandler(reference, mapper)
    @JvmStatic fun <P0, P1, P2, P3> parameterHandler(reference: SerializableFunction3<P0, P1, P2, P3>, mapper: ParameterMapper<SerializableFunction3<P0, P1, P2, P3>>) = ParameterHandler(reference, mapper)
    @JvmStatic fun <P0, P1, P2, P3, P4> parameterHandler(reference: SerializableFunction4<P0, P1, P2, P3, P4>, mapper: ParameterMapper<SerializableFunction4<P0, P1, P2, P3, P4>>) = ParameterHandler(reference, mapper)
    @JvmStatic fun <P0, P1, P2, P3, P4, P5> parameterHandler(reference: SerializableFunction5<P0, P1, P2, P3, P4, P5>, mapper: ParameterMapper<SerializableFunction5<P0, P1, P2, P3, P4, P5>>) = ParameterHandler(reference, mapper)
    @JvmStatic fun <P0, P1, P2, P3, P4, P5, P6> parameterHandler(reference: SerializableFunction6<P0, P1, P2, P3, P4, P5, P6>, mapper: ParameterMapper<SerializableFunction6<P0, P1, P2, P3, P4, P5, P6>>) = ParameterHandler(reference, mapper)
    @JvmStatic fun <P0, P1, P2, P3, P4, P5, P6, P7> parameterHandler(reference: SerializableFunction7<P0, P1, P2, P3, P4, P5, P6, P7>, mapper: ParameterMapper<SerializableFunction7<P0, P1, P2, P3, P4, P5, P6, P7>>) = ParameterHandler(reference, mapper)
    @JvmStatic fun <P0, P1, P2, P3, P4, P5, P6, P7, P8> parameterHandler(reference: SerializableFunction8<P0, P1, P2, P3, P4, P5, P6, P7, P8>, mapper: ParameterMapper<SerializableFunction8<P0, P1, P2, P3, P4, P5, P6, P7, P8>>) = ParameterHandler(reference, mapper)
    @JvmStatic fun <P0, P1, P2, P3, P4, P5, P6, P7, P8, P9> parameterHandler(reference: SerializableFunction9<P0, P1, P2, P3, P4, P5, P6, P7, P8, P9>, mapper: ParameterMapper<SerializableFunction9<P0, P1, P2, P3, P4, P5, P6, P7, P8, P9>>) = ParameterHandler(reference, mapper)
    @JvmStatic fun <P0, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10> parameterHandler(reference: SerializableFunction10<P0, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10>, mapper: ParameterMapper<SerializableFunction10<P0, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10>>) = ParameterHandler(reference, mapper)
    @JvmStatic fun <P0, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11> parameterHandler(reference: SerializableFunction11<P0, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11>, mapper: ParameterMapper<SerializableFunction11<P0, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11>>) = ParameterHandler(reference, mapper)
    @JvmStatic fun <P0, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12> parameterHandler(reference: SerializableFunction12<P0, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12>, mapper: ParameterMapper<SerializableFunction12<P0, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12>>) = ParameterHandler(reference, mapper)
    @JvmStatic fun <P0, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13> parameterHandler(reference: SerializableFunction13<P0, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13>, mapper: ParameterMapper<SerializableFunction13<P0, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13>>) = ParameterHandler(reference, mapper)
    @JvmStatic fun <P0, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14> parameterHandler(reference: SerializableFunction14<P0, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14>, mapper: ParameterMapper<SerializableFunction14<P0, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14>>) = ParameterHandler(reference, mapper)
    @JvmStatic fun <P0, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15> parameterHandler(reference: SerializableFunction15<P0, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15>, mapper: ParameterMapper<SerializableFunction15<P0, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15>>) = ParameterHandler(reference, mapper)
    @JvmStatic fun <P0, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16> parameterHandler(reference: SerializableFunction16<P0, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16>, mapper: ParameterMapper<SerializableFunction16<P0, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16>>) = ParameterHandler(reference, mapper)
    @JvmStatic fun <P0, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17> parameterHandler(reference: SerializableFunction17<P0, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17>, mapper: ParameterMapper<SerializableFunction17<P0, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17>>) = ParameterHandler(reference, mapper)
    @JvmStatic fun <P0, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18> parameterHandler(reference: SerializableFunction18<P0, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18>, mapper: ParameterMapper<SerializableFunction18<P0, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18>>) = ParameterHandler(reference, mapper)
    @JvmStatic fun <P0, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19> parameterHandler(reference: SerializableFunction19<P0, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19>, mapper: ParameterMapper<SerializableFunction19<P0, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19>>) = ParameterHandler(reference, mapper)
    @JvmStatic fun <P0, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19, P20> parameterHandler(reference: SerializableFunction20<P0, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19, P20>, mapper: ParameterMapper<SerializableFunction20<P0, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19, P20>>) = ParameterHandler(reference, mapper)
    @JvmStatic fun <P0, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19, P20, P21> parameterHandler(reference: SerializableFunction21<P0, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19, P20, P21>, mapper: ParameterMapper<SerializableFunction21<P0, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19, P20, P21>>) = ParameterHandler(reference, mapper)
    @JvmStatic fun <P0, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19, P20, P21, P22> parameterHandler(reference: SerializableFunction22<P0, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19, P20, P21, P22>, mapper: ParameterMapper<SerializableFunction22<P0, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19, P20, P21, P22>>) = ParameterHandler(reference, mapper)
}
