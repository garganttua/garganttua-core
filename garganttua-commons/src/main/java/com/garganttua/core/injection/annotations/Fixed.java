package com.garganttua.core.injection.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface Fixed {

    int valueInt() default -1;
    double valueDouble() default -1;
    float valueFloat() default -1;
    long valueLong() default -1;
    String valueString() default "default";
    byte valueByte() default -1;
    short valueShort()default -1;
    boolean valueBoolean() default false;
    char valueChar() default '\u0000';

}
