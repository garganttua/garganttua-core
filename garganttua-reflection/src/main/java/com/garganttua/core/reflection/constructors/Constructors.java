package com.garganttua.core.reflection.constructors;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.stream.Collectors;

public class Constructors {

    public static String prettyColored(Constructor<?> c) {
        String className = "\u001B[36m" + c.getDeclaringClass().getSimpleName() + "\u001B[0m";
        String params = Arrays.stream(c.getParameters())
                .map(p -> "\u001B[33m" + p.getType().getSimpleName() + "\u001B[0m " +
                        "\u001B[32m" + p.getName() + "\u001B[0m")
                .collect(Collectors.joining(", "));
        String modifiers = Modifier.toString(c.getModifiers());
        return className + "(" + params + ") " + (modifiers.isBlank() ? "" : "(" + modifiers + ")");
    }

}
