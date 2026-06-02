package com.garganttua.core.reflection.methods;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.garganttua.core.observability.Logger;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IMethod;
import com.garganttua.core.reflection.IMethodReturn;

/**
 * Static helpers for inspecting and formatting {@link IMethod} instances, plus
 * factory methods for {@link IMethodReturn} values.
 */
public class Methods {
    private static final Logger log = Logger.getLogger(Methods.class);

        /**
         * Returns whether {@code method} is declared {@code static}.
         *
         * @param method the method to inspect
         * @return {@code true} if the method is static
         */
        public static boolean isStatic(IMethod method) {
                return Modifier.isStatic(method.getModifiers());
        }

        /**
         * Renders a method as an ANSI-colored {@code Class.name(ParamTypes)} string for terminal output.
         *
         * @param m the method to render
         * @return the colored signature representation
         */
        public static String prettyColored(IMethod m) {
                log.trace("Creating pretty colored representation for method: {}", m);
                return "\u001B[36m" + m.getDeclaringClass().getSimpleName() + "\u001B[0m"
                                + "."
                                + "\u001B[32m" + m.getName() + "\u001B[0m"
                                + "("
                                + Arrays.stream(m.getParameterTypes())
                                                .map(c -> "\u001B[33m" + c.getSimpleName() + "\u001B[0m")
                                                .collect(Collectors.joining(", "))
                                + ")";
        }

        /**
         * Renders a method as a plain {@code Class.name(ParamTypes)} string.
         *
         * @param method the method to render
         * @return the signature representation
         */
        public static String pretty(IMethod method) {
                log.trace("Creating pretty representation for method: {}", method);

                String pretty = method.getDeclaringClass().getSimpleName()
                                + "."
                                + method.getName()
                                + "("
                                + Arrays.stream(method.getParameterTypes())
                                                .map(IClass::getSimpleName)
                                                .collect(Collectors.joining(", "))
                                + ")";

                return pretty;
        }

        /**
         * Creates a single-value MethodReturn with explicit type.
         *
         * @param <R>   the type of the value
         * @param value the single value
         * @param type  the runtime type of the value
         * @return a MethodReturn containing the single value
         */
        static <R> IMethodReturn<R> singleMethodReturn(R value, IClass<R> type) {
                return new SingleMethodReturn<>(value, type);
        }

        /**
         * Creates a multiple-value MethodReturn with explicit type.
         *
         * @param <R>    the type of the values
         * @param values the list of values
         * @param type   the runtime type of the values
         * @return a MethodReturn containing multiple values
         */
        static <R> IMethodReturn<R> multipleMethodReturn(List<R> values, IClass<R> type) {
                return new MultipleMethodReturn<>(values, type);
        }

}
