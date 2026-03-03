package com.garganttua.core.reflection.constructors;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.stream.Collectors;

import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IConstructor;
import com.garganttua.core.reflection.TypeUtils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Constructors {

    private Constructors() {
        /* This utility class should not be instantiated */
    }

    public static String prettyColored(IConstructor<?> c) {
        log.atTrace().log("Creating pretty colored representation for constructor: {}", c);
        String className = "\u001B[36m" + c.getDeclaringClass().getSimpleName() + "\u001B[0m";
        String params = Arrays.stream(c.getParameters())
                .map(p -> "\u001B[33m" + p.getType().getSimpleName() + "\u001B[0m " +
                        "\u001B[32m" + p.getName() + "\u001B[0m")
                .collect(Collectors.joining(", "));
        String modifiers = Modifier.toString(c.getModifiers());
        return className + "(" + params + ") " + (modifiers.isBlank() ? "" : "(" + modifiers + ")");
    }

    public static String pretty(IConstructor<?> c) {
        log.atTrace().log("Creating pretty representation for constructor: {}", c);
        return c.getDeclaringClass().getSimpleName()
                + "("
                + Arrays.stream(c.getParameterTypes())
                        .map(IClass::getSimpleName)
                        .collect(Collectors.joining(", "))
                + ")";
    }

    public static boolean isPublic(IConstructor<?> c) {
        return Modifier.isPublic(c.getModifiers());
    }

    public static boolean isPrivate(IConstructor<?> c) {
        return Modifier.isPrivate(c.getModifiers());
    }

    public static boolean parameterTypesMatch(IConstructor<?> c, IClass<?>... types) {
        IClass<?>[] actualParams = c.getParameterTypes();
        if (types == null || types.length == 0) {
            return actualParams.length == 0;
        }
        if (actualParams.length != types.length) {
            return false;
        }
        for (int i = 0; i < actualParams.length; i++) {
            if (!TypeUtils.isAssignable(actualParams[i], types[i])) {
                return false;
            }
        }
        return true;
    }

}
