package io.javalin.plugin.openapi.handler.functional

interface SerializableFunction: java.io.Serializable

/** A function that takes 0 arguments. */
@FunctionalInterface
public interface SerializableNoRFunction0 : SerializableFunction {
    /** Invokes the function. */
    public operator fun invoke()
}
/** A function that takes 1 argument. */
@FunctionalInterface
public interface SerializableNoRFunction1<in P1> : SerializableFunction {
    /** Invokes the function with the specified argument. */
    public operator fun invoke(p1: P1)
}
/** A function that takes 2 arguments. */
@FunctionalInterface
public interface SerializableNoRFunction2<in P1, in P2> : SerializableFunction {
    /** Invokes the function with the specified arguments. */
    public operator fun invoke(p1: P1, p2: P2)
}
/** A function that takes 3 arguments. */
@FunctionalInterface
public interface SerializableNoRFunction3<in P1, in P2, in P3> : SerializableFunction {
    /** Invokes the function with the specified arguments. */
    public operator fun invoke(p1: P1, p2: P2, p3: P3)
}
/** A function that takes 4 arguments. */
@FunctionalInterface
public interface SerializableNoRFunction4<in P1, in P2, in P3, in P4> : SerializableFunction {
    /** Invokes the function with the specified arguments. */
    public operator fun invoke(p1: P1, p2: P2, p3: P3, p4: P4)
}
/** A function that takes 5 arguments. */
@FunctionalInterface
public interface SerializableNoRFunction5<in P1, in P2, in P3, in P4, in P5> : SerializableFunction {
    /** Invokes the function with the specified arguments. */
    public operator fun invoke(p1: P1, p2: P2, p3: P3, p4: P4, p5: P5)
}
/** A function that takes 6 arguments. */
@FunctionalInterface
public interface SerializableNoRFunction6<in P1, in P2, in P3, in P4, in P5, in P6> : SerializableFunction {
    /** Invokes the function with the specified arguments. */
    public operator fun invoke(p1: P1, p2: P2, p3: P3, p4: P4, p5: P5, p6: P6)
}
/** A function that takes 7 arguments. */
@FunctionalInterface
public interface SerializableNoRFunction7<in P1, in P2, in P3, in P4, in P5, in P6, in P7> : SerializableFunction {
    /** Invokes the function with the specified arguments. */
    public operator fun invoke(p1: P1, p2: P2, p3: P3, p4: P4, p5: P5, p6: P6, p7: P7)
}
/** A function that takes 8 arguments. */
@FunctionalInterface
public interface SerializableNoRFunction8<in P1, in P2, in P3, in P4, in P5, in P6, in P7, in P8> : SerializableFunction {
    /** Invokes the function with the specified arguments. */
    public operator fun invoke(p1: P1, p2: P2, p3: P3, p4: P4, p5: P5, p6: P6, p7: P7, p8: P8)
}
/** A function that takes 9 arguments. */
@FunctionalInterface
public interface SerializableNoRFunction9<in P1, in P2, in P3, in P4, in P5, in P6, in P7, in P8, in P9> : SerializableFunction {
    /** Invokes the function with the specified arguments. */
    public operator fun invoke(p1: P1, p2: P2, p3: P3, p4: P4, p5: P5, p6: P6, p7: P7, p8: P8, p9: P9)
}
/** A function that takes 10 arguments. */
@FunctionalInterface
public interface SerializableNoRFunction10<in P1, in P2, in P3, in P4, in P5, in P6, in P7, in P8, in P9, in P10> : SerializableFunction {
    /** Invokes the function with the specified arguments. */
    public operator fun invoke(p1: P1, p2: P2, p3: P3, p4: P4, p5: P5, p6: P6, p7: P7, p8: P8, p9: P9, p10: P10)
}
/** A function that takes 11 arguments. */
@FunctionalInterface
public interface SerializableNoRFunction11<in P1, in P2, in P3, in P4, in P5, in P6, in P7, in P8, in P9, in P10, in P11> : SerializableFunction {
    /** Invokes the function with the specified arguments. */
    public operator fun invoke(p1: P1, p2: P2, p3: P3, p4: P4, p5: P5, p6: P6, p7: P7, p8: P8, p9: P9, p10: P10, p11: P11)
}
/** A function that takes 12 arguments. */
@FunctionalInterface
public interface SerializableNoRFunction12<in P1, in P2, in P3, in P4, in P5, in P6, in P7, in P8, in P9, in P10, in P11, in P12> : SerializableFunction {
    /** Invokes the function with the specified arguments. */
    public operator fun invoke(p1: P1, p2: P2, p3: P3, p4: P4, p5: P5, p6: P6, p7: P7, p8: P8, p9: P9, p10: P10, p11: P11, p12: P12)
}
/** A function that takes 13 arguments. */
@FunctionalInterface
public interface SerializableNoRFunction13<in P1, in P2, in P3, in P4, in P5, in P6, in P7, in P8, in P9, in P10, in P11, in P12, in P13> : SerializableFunction {
    /** Invokes the function with the specified arguments. */
    public operator fun invoke(p1: P1, p2: P2, p3: P3, p4: P4, p5: P5, p6: P6, p7: P7, p8: P8, p9: P9, p10: P10, p11: P11, p12: P12, p13: P13)
}
/** A function that takes 14 arguments. */
@FunctionalInterface
public interface SerializableNoRFunction14<in P1, in P2, in P3, in P4, in P5, in P6, in P7, in P8, in P9, in P10, in P11, in P12, in P13, in P14> : SerializableFunction {
    /** Invokes the function with the specified arguments. */
    public operator fun invoke(p1: P1, p2: P2, p3: P3, p4: P4, p5: P5, p6: P6, p7: P7, p8: P8, p9: P9, p10: P10, p11: P11, p12: P12, p13: P13, p14: P14)
}
/** A function that takes 15 arguments. */
@FunctionalInterface
public interface SerializableNoRFunction15<in P1, in P2, in P3, in P4, in P5, in P6, in P7, in P8, in P9, in P10, in P11, in P12, in P13, in P14, in P15> : SerializableFunction {
    /** Invokes the function with the specified arguments. */
    public operator fun invoke(p1: P1, p2: P2, p3: P3, p4: P4, p5: P5, p6: P6, p7: P7, p8: P8, p9: P9, p10: P10, p11: P11, p12: P12, p13: P13, p14: P14, p15: P15)
}
/** A function that takes 16 arguments. */
@FunctionalInterface
public interface SerializableNoRFunction16<in P1, in P2, in P3, in P4, in P5, in P6, in P7, in P8, in P9, in P10, in P11, in P12, in P13, in P14, in P15, in P16> : SerializableFunction {
    /** Invokes the function with the specified arguments. */
    public operator fun invoke(p1: P1, p2: P2, p3: P3, p4: P4, p5: P5, p6: P6, p7: P7, p8: P8, p9: P9, p10: P10, p11: P11, p12: P12, p13: P13, p14: P14, p15: P15, p16: P16)
}
/** A function that takes 17 arguments. */
@FunctionalInterface
public interface SerializableNoRFunction17<in P1, in P2, in P3, in P4, in P5, in P6, in P7, in P8, in P9, in P10, in P11, in P12, in P13, in P14, in P15, in P16, in P17> : SerializableFunction {
    /** Invokes the function with the specified arguments. */
    public operator fun invoke(p1: P1, p2: P2, p3: P3, p4: P4, p5: P5, p6: P6, p7: P7, p8: P8, p9: P9, p10: P10, p11: P11, p12: P12, p13: P13, p14: P14, p15: P15, p16: P16, p17: P17)
}
/** A function that takes 18 arguments. */
@FunctionalInterface
public interface SerializableNoRFunction18<in P1, in P2, in P3, in P4, in P5, in P6, in P7, in P8, in P9, in P10, in P11, in P12, in P13, in P14, in P15, in P16, in P17, in P18> : SerializableFunction {
    /** Invokes the function with the specified arguments. */
    public operator fun invoke(p1: P1, p2: P2, p3: P3, p4: P4, p5: P5, p6: P6, p7: P7, p8: P8, p9: P9, p10: P10, p11: P11, p12: P12, p13: P13, p14: P14, p15: P15, p16: P16, p17: P17, p18: P18)
}
/** A function that takes 19 arguments. */
@FunctionalInterface
public interface SerializableNoRFunction19<in P1, in P2, in P3, in P4, in P5, in P6, in P7, in P8, in P9, in P10, in P11, in P12, in P13, in P14, in P15, in P16, in P17, in P18, in P19> : SerializableFunction {
    /** Invokes the function with the specified arguments. */
    public operator fun invoke(p1: P1, p2: P2, p3: P3, p4: P4, p5: P5, p6: P6, p7: P7, p8: P8, p9: P9, p10: P10, p11: P11, p12: P12, p13: P13, p14: P14, p15: P15, p16: P16, p17: P17, p18: P18, p19: P19)
}
/** A function that takes 20 arguments. */
@FunctionalInterface
public interface SerializableNoRFunction20<in P1, in P2, in P3, in P4, in P5, in P6, in P7, in P8, in P9, in P10, in P11, in P12, in P13, in P14, in P15, in P16, in P17, in P18, in P19, in P20> : SerializableFunction {
    /** Invokes the function with the specified arguments. */
    public operator fun invoke(p1: P1, p2: P2, p3: P3, p4: P4, p5: P5, p6: P6, p7: P7, p8: P8, p9: P9, p10: P10, p11: P11, p12: P12, p13: P13, p14: P14, p15: P15, p16: P16, p17: P17, p18: P18, p19: P19, p20: P20)
}
/** A function that takes 21 arguments. */
@FunctionalInterface
public interface SerializableNoRFunction21<in P1, in P2, in P3, in P4, in P5, in P6, in P7, in P8, in P9, in P10, in P11, in P12, in P13, in P14, in P15, in P16, in P17, in P18, in P19, in P20, in P21> : SerializableFunction {
    /** Invokes the function with the specified arguments. */
    public operator fun invoke(p1: P1, p2: P2, p3: P3, p4: P4, p5: P5, p6: P6, p7: P7, p8: P8, p9: P9, p10: P10, p11: P11, p12: P12, p13: P13, p14: P14, p15: P15, p16: P16, p17: P17, p18: P18, p19: P19, p20: P20, p21: P21)
}
/** A function that takes 22 arguments. */
@FunctionalInterface
public interface SerializableNoRFunction22<in P1, in P2, in P3, in P4, in P5, in P6, in P7, in P8, in P9, in P10, in P11, in P12, in P13, in P14, in P15, in P16, in P17, in P18, in P19, in P20, in P21, in P22> : SerializableFunction {
    /** Invokes the function with the specified arguments. */
    public operator fun invoke(p1: P1, p2: P2, p3: P3, p4: P4, p5: P5, p6: P6, p7: P7, p8: P8, p9: P9, p10: P10, p11: P11, p12: P12, p13: P13, p14: P14, p15: P15, p16: P16, p17: P17, p18: P18, p19: P19, p20: P20, p21: P21, p22: P22)
}

/** A function that takes 0 arguments. */
@FunctionalInterface
public interface SerializableFunction0<out R> : SerializableFunction {
    /** Invokes the function. */
    public operator fun invoke(): R
}
/** A function that takes 1 argument. */
@FunctionalInterface
public interface SerializableFunction1<in P1, out R> : SerializableFunction {
    /** Invokes the function with the specified argument. */
    public operator fun invoke(p1: P1): R
}
/** A function that takes 2 arguments. */
@FunctionalInterface
public interface SerializableFunction2<in P1, in P2, out R> : SerializableFunction {
    /** Invokes the function with the specified arguments. */
    public operator fun invoke(p1: P1, p2: P2): R
}
/** A function that takes 3 arguments. */
@FunctionalInterface
public interface SerializableFunction3<in P1, in P2, in P3, out R> : SerializableFunction {
    /** Invokes the function with the specified arguments. */
    public operator fun invoke(p1: P1, p2: P2, p3: P3): R
}
/** A function that takes 4 arguments. */
@FunctionalInterface
public interface SerializableFunction4<in P1, in P2, in P3, in P4, out R> : SerializableFunction {
    /** Invokes the function with the specified arguments. */
    public operator fun invoke(p1: P1, p2: P2, p3: P3, p4: P4): R
}
/** A function that takes 5 arguments. */
@FunctionalInterface
public interface SerializableFunction5<in P1, in P2, in P3, in P4, in P5, out R> : SerializableFunction {
    /** Invokes the function with the specified arguments. */
    public operator fun invoke(p1: P1, p2: P2, p3: P3, p4: P4, p5: P5): R
}
/** A function that takes 6 arguments. */
@FunctionalInterface
public interface SerializableFunction6<in P1, in P2, in P3, in P4, in P5, in P6, out R> : SerializableFunction {
    /** Invokes the function with the specified arguments. */
    public operator fun invoke(p1: P1, p2: P2, p3: P3, p4: P4, p5: P5, p6: P6): R
}
/** A function that takes 7 arguments. */
@FunctionalInterface
public interface SerializableFunction7<in P1, in P2, in P3, in P4, in P5, in P6, in P7, out R> : SerializableFunction {
    /** Invokes the function with the specified arguments. */
    public operator fun invoke(p1: P1, p2: P2, p3: P3, p4: P4, p5: P5, p6: P6, p7: P7): R
}
/** A function that takes 8 arguments. */
@FunctionalInterface
public interface SerializableFunction8<in P1, in P2, in P3, in P4, in P5, in P6, in P7, in P8, out R> : SerializableFunction {
    /** Invokes the function with the specified arguments. */
    public operator fun invoke(p1: P1, p2: P2, p3: P3, p4: P4, p5: P5, p6: P6, p7: P7, p8: P8): R
}
/** A function that takes 9 arguments. */
@FunctionalInterface
public interface SerializableFunction9<in P1, in P2, in P3, in P4, in P5, in P6, in P7, in P8, in P9, out R> : SerializableFunction {
    /** Invokes the function with the specified arguments. */
    public operator fun invoke(p1: P1, p2: P2, p3: P3, p4: P4, p5: P5, p6: P6, p7: P7, p8: P8, p9: P9): R
}
/** A function that takes 10 arguments. */
@FunctionalInterface
public interface SerializableFunction10<in P1, in P2, in P3, in P4, in P5, in P6, in P7, in P8, in P9, in P10, out R> : SerializableFunction {
    /** Invokes the function with the specified arguments. */
    public operator fun invoke(p1: P1, p2: P2, p3: P3, p4: P4, p5: P5, p6: P6, p7: P7, p8: P8, p9: P9, p10: P10): R
}
/** A function that takes 11 arguments. */
@FunctionalInterface
public interface SerializableFunction11<in P1, in P2, in P3, in P4, in P5, in P6, in P7, in P8, in P9, in P10, in P11, out R> : SerializableFunction {
    /** Invokes the function with the specified arguments. */
    public operator fun invoke(p1: P1, p2: P2, p3: P3, p4: P4, p5: P5, p6: P6, p7: P7, p8: P8, p9: P9, p10: P10, p11: P11): R
}
/** A function that takes 12 arguments. */
@FunctionalInterface
public interface SerializableFunction12<in P1, in P2, in P3, in P4, in P5, in P6, in P7, in P8, in P9, in P10, in P11, in P12, out R> : SerializableFunction {
    /** Invokes the function with the specified arguments. */
    public operator fun invoke(p1: P1, p2: P2, p3: P3, p4: P4, p5: P5, p6: P6, p7: P7, p8: P8, p9: P9, p10: P10, p11: P11, p12: P12): R
}
/** A function that takes 13 arguments. */
@FunctionalInterface
public interface SerializableFunction13<in P1, in P2, in P3, in P4, in P5, in P6, in P7, in P8, in P9, in P10, in P11, in P12, in P13, out R> : SerializableFunction {
    /** Invokes the function with the specified arguments. */
    public operator fun invoke(p1: P1, p2: P2, p3: P3, p4: P4, p5: P5, p6: P6, p7: P7, p8: P8, p9: P9, p10: P10, p11: P11, p12: P12, p13: P13): R
}
/** A function that takes 14 arguments. */
@FunctionalInterface
public interface SerializableFunction14<in P1, in P2, in P3, in P4, in P5, in P6, in P7, in P8, in P9, in P10, in P11, in P12, in P13, in P14, out R> : SerializableFunction {
    /** Invokes the function with the specified arguments. */
    public operator fun invoke(p1: P1, p2: P2, p3: P3, p4: P4, p5: P5, p6: P6, p7: P7, p8: P8, p9: P9, p10: P10, p11: P11, p12: P12, p13: P13, p14: P14): R
}
/** A function that takes 15 arguments. */
@FunctionalInterface
public interface SerializableFunction15<in P1, in P2, in P3, in P4, in P5, in P6, in P7, in P8, in P9, in P10, in P11, in P12, in P13, in P14, in P15, out R> : SerializableFunction {
    /** Invokes the function with the specified arguments. */
    public operator fun invoke(p1: P1, p2: P2, p3: P3, p4: P4, p5: P5, p6: P6, p7: P7, p8: P8, p9: P9, p10: P10, p11: P11, p12: P12, p13: P13, p14: P14, p15: P15): R
}
/** A function that takes 16 arguments. */
@FunctionalInterface
public interface SerializableFunction16<in P1, in P2, in P3, in P4, in P5, in P6, in P7, in P8, in P9, in P10, in P11, in P12, in P13, in P14, in P15, in P16, out R> : SerializableFunction {
    /** Invokes the function with the specified arguments. */
    public operator fun invoke(p1: P1, p2: P2, p3: P3, p4: P4, p5: P5, p6: P6, p7: P7, p8: P8, p9: P9, p10: P10, p11: P11, p12: P12, p13: P13, p14: P14, p15: P15, p16: P16): R
}
/** A function that takes 17 arguments. */
@FunctionalInterface
public interface SerializableFunction17<in P1, in P2, in P3, in P4, in P5, in P6, in P7, in P8, in P9, in P10, in P11, in P12, in P13, in P14, in P15, in P16, in P17, out R> : SerializableFunction {
    /** Invokes the function with the specified arguments. */
    public operator fun invoke(p1: P1, p2: P2, p3: P3, p4: P4, p5: P5, p6: P6, p7: P7, p8: P8, p9: P9, p10: P10, p11: P11, p12: P12, p13: P13, p14: P14, p15: P15, p16: P16, p17: P17): R
}
/** A function that takes 18 arguments. */
@FunctionalInterface
public interface SerializableFunction18<in P1, in P2, in P3, in P4, in P5, in P6, in P7, in P8, in P9, in P10, in P11, in P12, in P13, in P14, in P15, in P16, in P17, in P18, out R> : SerializableFunction {
    /** Invokes the function with the specified arguments. */
    public operator fun invoke(p1: P1, p2: P2, p3: P3, p4: P4, p5: P5, p6: P6, p7: P7, p8: P8, p9: P9, p10: P10, p11: P11, p12: P12, p13: P13, p14: P14, p15: P15, p16: P16, p17: P17, p18: P18): R
}
/** A function that takes 19 arguments. */
@FunctionalInterface
public interface SerializableFunction19<in P1, in P2, in P3, in P4, in P5, in P6, in P7, in P8, in P9, in P10, in P11, in P12, in P13, in P14, in P15, in P16, in P17, in P18, in P19, out R> : SerializableFunction {
    /** Invokes the function with the specified arguments. */
    public operator fun invoke(p1: P1, p2: P2, p3: P3, p4: P4, p5: P5, p6: P6, p7: P7, p8: P8, p9: P9, p10: P10, p11: P11, p12: P12, p13: P13, p14: P14, p15: P15, p16: P16, p17: P17, p18: P18, p19: P19): R
}
/** A function that takes 20 arguments. */
@FunctionalInterface
public interface SerializableFunction20<in P1, in P2, in P3, in P4, in P5, in P6, in P7, in P8, in P9, in P10, in P11, in P12, in P13, in P14, in P15, in P16, in P17, in P18, in P19, in P20, out R> : SerializableFunction {
    /** Invokes the function with the specified arguments. */
    public operator fun invoke(p1: P1, p2: P2, p3: P3, p4: P4, p5: P5, p6: P6, p7: P7, p8: P8, p9: P9, p10: P10, p11: P11, p12: P12, p13: P13, p14: P14, p15: P15, p16: P16, p17: P17, p18: P18, p19: P19, p20: P20): R
}
/** A function that takes 21 arguments. */
@FunctionalInterface
public interface SerializableFunction21<in P1, in P2, in P3, in P4, in P5, in P6, in P7, in P8, in P9, in P10, in P11, in P12, in P13, in P14, in P15, in P16, in P17, in P18, in P19, in P20, in P21, out R> : SerializableFunction {
    /** Invokes the function with the specified arguments. */
    public operator fun invoke(p1: P1, p2: P2, p3: P3, p4: P4, p5: P5, p6: P6, p7: P7, p8: P8, p9: P9, p10: P10, p11: P11, p12: P12, p13: P13, p14: P14, p15: P15, p16: P16, p17: P17, p18: P18, p19: P19, p20: P20, p21: P21): R
}
/** A function that takes 22 arguments. */
@FunctionalInterface
public interface SerializableFunction22<in P1, in P2, in P3, in P4, in P5, in P6, in P7, in P8, in P9, in P10, in P11, in P12, in P13, in P14, in P15, in P16, in P17, in P18, in P19, in P20, in P21, in P22, out R> : SerializableFunction {
    /** Invokes the function with the specified arguments. */
    public operator fun invoke(p1: P1, p2: P2, p3: P3, p4: P4, p5: P5, p6: P6, p7: P7, p8: P8, p9: P9, p10: P10, p11: P11, p12: P12, p13: P13, p14: P14, p15: P15, p16: P16, p17: P17, p18: P18, p19: P19, p20: P20, p21: P21, p22: P22): R
}
