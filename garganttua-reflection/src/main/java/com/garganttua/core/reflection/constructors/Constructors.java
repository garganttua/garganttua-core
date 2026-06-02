package com.garganttua.core.reflection.constructors;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.stream.Collectors;

import com.garganttua.core.observability.Logger;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IConstructor;
import com.garganttua.core.reflection.TypeUtils;

/**
 * Static helpers for inspecting and rendering {@link IConstructor}s (pretty printing, modifier
 * checks, parameter-type matching). Stateless utility class.
 */
public class Constructors {
    private static final Logger log = Logger.getLogger(Constructors.class);

    private Constructors() {
        /* This utility class should not be instantiated */
    }

    /**
     * {@return an ANSI-colored single-line rendering of the constructor} including its declaring
     * class, parameters and modifiers.
     *
     * @param c the constructor to render
     */
    public static String prettyColored(IConstructor<?> c) {
        log.trace("Creating pretty colored representation for constructor: {}", c);
        String className = "\u001B[36m" + c.getDeclaringClass().getSimpleName() + "\u001B[0m";
        String params = Arrays.stream(c.getParameters())
                .map(p -> "\u001B[33m" + p.getType().getSimpleName() + "\u001B[0m " +
                        "\u001B[32m" + p.getName() + "\u001B[0m")
                .collect(Collectors.joining(", "));
        String modifiers = Modifier.toString(c.getModifiers());
        return className + "(" + params + ") " + (modifiers.isBlank() ? "" : "(" + modifiers + ")");
    }

    /**
     * {@return a plain single-line rendering of the constructor} as {@code SimpleName(Type, Type)}.
     *
     * @param c the constructor to render
     */
    public static String pretty(IConstructor<?> c) {
        log.trace("Creating pretty representation for constructor: {}", c);
        return c.getDeclaringClass().getSimpleName()
                + "("
                + Arrays.stream(c.getParameterTypes())
                        .map(IClass::getSimpleName)
                        .collect(Collectors.joining(", "))
                + ")";
    }

    /**
     * {@return whether the constructor is {@code public}}
     *
     * @param c the constructor to test
     */
    public static boolean isPublic(IConstructor<?> c) {
        return Modifier.isPublic(c.getModifiers());
    }

    /**
     * {@return whether the constructor is {@code private}}
     *
     * @param c the constructor to test
     */
    public static boolean isPrivate(IConstructor<?> c) {
        return Modifier.isPrivate(c.getModifiers());
    }

    /**
     * {@return whether the constructor's declared parameters are assignable from {@code types}}
     * A {@code null} or empty {@code types} matches only a no-arg constructor.
     *
     * @param c     the constructor to test
     * @param types the candidate argument types, in order
     */
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
